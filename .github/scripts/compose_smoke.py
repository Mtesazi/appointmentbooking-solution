#!/usr/bin/env python3
"""Verify the Compose deployment through the gateway and Mailpit."""

import datetime
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

FRONTEND = os.environ.get("SMOKE_FRONTEND_URL", "http://localhost:4201")
GATEWAY = os.environ.get("SMOKE_GATEWAY_URL", "http://localhost:8080")
MAILPIT = os.environ.get("SMOKE_MAILPIT_URL", "http://localhost:8026")
ORIGIN = "http://localhost:4201"
TOKEN = None


def request(url, method="GET", body=None, expected=200, headers=None):
    headers = dict(headers or {})
    if body is not None:
        body = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            data = response.read()
            status = response.status
            response_headers = response.headers
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"{method} {url}: HTTP {error.code}") from error
    if status != expected:
        raise RuntimeError(f"{method} {url}: expected HTTP {expected}, got {status}")
    return data, response_headers


def api(path, method="GET", body=None, expected=200):
    headers = {"Origin": ORIGIN}
    if TOKEN:
        headers["Authorization"] = f"Bearer {TOKEN}"
    data, response_headers = request(GATEWAY + "/api" + path, method, body, expected, headers)
    if response_headers.get("Access-Control-Allow-Origin") != ORIGIN:
        raise RuntimeError(f"{method} {path}: incorrect CORS response")
    return json.loads(data) if data else None


def wait_for(description, check, timeout=180):
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            if check():
                print(f"{description}: passed", flush=True)
                return
        except (OSError, RuntimeError, ValueError) as error:
            last_error = error
        time.sleep(3)
    raise RuntimeError(f"{description}: timed out; last error: {last_error}")


def main():
    global TOKEN
    wait_for("Frontend", lambda: b"<app-root" in request(FRONTEND + "/")[0])
    wait_for("Gateway and appointment service", lambda: bool(json.loads(request(GATEWAY + "/v3/api-docs")[0])["paths"]))
    request(GATEWAY + "/swagger-ui/index.html")
    _, headers = request(
        GATEWAY + "/api/auth/register",
        method="OPTIONS",
        headers={
            "Origin": ORIGIN,
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "content-type, authorization",
        },
    )
    if headers.get("Access-Control-Allow-Origin") != ORIGIN:
        raise RuntimeError("CORS preflight did not allow the Compose frontend")
    print("Swagger and CORS: passed", flush=True)

    email = f"ci-{uuid.uuid4().hex}@example.invalid"
    password = secrets.token_urlsafe(24)
    api("/auth/register", "POST", {
        "firstName": "CI", "lastName": "Smoke", "email": email,
        "phone": "1234567890", "password": password,
    }, expected=201)
    TOKEN = api("/auth/login", "POST", {"email": email, "password": password})["accessToken"]
    print("Registration and login: passed", flush=True)

    branches = api("/branches")
    services = api("/services")
    if not branches or not services:
        raise RuntimeError("No branches or services available")
    selection = None
    for branch in branches:
        for service in services:
            for days in range(2, 32):
                date = (datetime.date.today() + datetime.timedelta(days=days)).isoformat()
                query = urllib.parse.urlencode({"branchId": branch["id"], "serviceId": service["id"], "date": date})
                slots = api("/appointments/availability?" + query)
                if slots:
                    selection = (branch["id"], service["id"], date, slots[0])
                    break
            if selection:
                break
        if selection:
            break
    if not selection:
        raise RuntimeError("No appointment slots available in the next 31 days")
    branch_id, service_id, date, slot = selection
    appointment = api("/appointments", "POST", {
        "branchId": branch_id, "serviceId": service_id,
        "appointmentDate": date, "time": slot,
    }, expected=201)
    appointment_id = appointment["id"]
    if appointment["status"] != "CONFIRMED" or not any(
        item["id"] == appointment_id and item["status"] == "CONFIRMED" for item in api("/appointments")
    ):
        raise RuntimeError("Created appointment was not confirmed and listed")
    cancelled = api(f"/appointments/{appointment_id}/cancel", "PATCH", {})
    if cancelled["status"] != "CANCELLED" or not any(
        item["id"] == appointment_id and item["status"] == "CANCELLED" for item in api("/appointments")
    ):
        raise RuntimeError("Cancelled appointment was not cancelled and listed")
    print("Availability, booking and cancellation: passed", flush=True)

    expected_subjects = {"Appointment booking confirmed", "Appointment cancelled"}

    def received_emails():
        data, _ = request(MAILPIT + "/api/v1/messages?limit=100")
        messages = json.loads(data).get("messages") or []
        subjects = {
            message["Subject"] for message in messages
            if any(recipient.get("Address", "").lower() == email for recipient in message.get("To", []))
        }
        return expected_subjects <= subjects

    wait_for("Booking and cancellation emails", received_emails, timeout=60)


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, ValueError, KeyError) as error:
        print(f"Compose smoke test failed: {error}", file=sys.stderr)
        sys.exit(1)
