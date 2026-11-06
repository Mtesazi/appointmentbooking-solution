#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PIDS=()

cleanup() {
  trap - EXIT INT TERM
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  for pid in "${PIDS[@]}"; do
    wait "$pid" 2>/dev/null || true
  done
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

wait_for_url() {
  local name="$1" url="$2" pid="$3" attempts=60
  printf 'Waiting for %s' "$name"
  while (( attempts > 0 )); do
    if curl --silent --fail --max-time 2 "$url" >/dev/null; then
      echo " ready"
      return
    fi
    kill -0 "$pid" 2>/dev/null || {
      echo
      echo "$name exited before becoming ready." >&2
      return 1
    }
    printf '.'
    sleep 2
    attempts=$((attempts - 1))
  done
  echo
  echo "Timed out waiting for $name at $url" >&2
  return 1
}

wait_for_registration() {
  local app="$1" pid="$2" attempts=60
  printf 'Waiting for %s registration in Eureka' "$app"
  while (( attempts > 0 )); do
    if curl --silent --fail --max-time 2 \
      "http://localhost:8761/eureka/apps/${app}" | grep -q "<name>${app}</name>"; then
      echo " ready"
      return
    fi
    kill -0 "$pid" 2>/dev/null || {
      echo
      echo "$app exited before registering." >&2
      return 1
    }
    printf '.'
    sleep 2
    attempts=$((attempts - 1))
  done
  echo
  echo "Timed out waiting for $app registration." >&2
  return 1
}

require_command java
require_command curl
require_command npm
require_command ss

if [[ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]]; then
  echo "Set SPRING_DATASOURCE_PASSWORD before running this script." >&2
  exit 1
fi

jwt_secret="${JWT_SECRET:-}"
if [[ ${#jwt_secret} -lt 32 ]]; then
  echo "Set JWT_SECRET to a secret of at least 32 characters before running this script." >&2
  exit 1
fi

if [[ ! -x "$ROOT_DIR/Front-End/angular/node_modules/.bin/ng" ]]; then
  echo "Angular dependencies are missing. Run 'npm install' in Front-End/angular first." >&2
  exit 1
fi

for port in 8761 8080 8082 4200; do
  if ss -ltnH "sport = :$port" | grep -q .; then
    echo "Port $port is already in use. Stop the existing service and retry." >&2
    exit 1
  fi
done

echo "Building backend services..."
(cd "$ROOT_DIR/Back-End/service-discovery" &&
  ../appointment-service/mvnw -q -DskipTests package)
(cd "$ROOT_DIR/Back-End/api-gateway" &&
  ../appointment-service/mvnw -q -DskipTests package)
(cd "$ROOT_DIR/Back-End/appointment-service" &&
  ./mvnw -q -DskipTests package)

echo "Starting Eureka..."
java -jar "$ROOT_DIR/Back-End/service-discovery/target/service-discovery-0.0.1-SNAPSHOT.jar" &
PIDS+=("$!")
wait_for_url "Eureka" "http://localhost:8761/eureka/apps" "${PIDS[-1]}"

echo "Starting API Gateway..."
java -jar "$ROOT_DIR/Back-End/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar" &
PIDS+=("$!")
wait_for_registration "API-GATEWAY" "${PIDS[-1]}"

echo "Starting appointment API..."
java -jar "$ROOT_DIR/Back-End/appointment-service/target/appointment-service-0.0.1-SNAPSHOT.jar" &
PIDS+=("$!")
wait_for_url "Appointment API" "http://localhost:8082/actuator/health" "${PIDS[-1]}"
wait_for_registration "APPOINTMENT-SERVICE" "${PIDS[-1]}"

echo "Starting Angular at http://localhost:4200 ..."
cd "$ROOT_DIR/Front-End/angular"
npm start
