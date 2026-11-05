import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof TimeoutError) {
    return 'The server took too long to respond. Check that the backend and database are running, then try again.';
  }

  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Could not reach the server. Check that the backend is running and try again.';
    }

    return error.error?.message ?? 'The request could not be completed. Please try again.';
  }

  return 'An unexpected error occurred. Please try again.';
}
