import { HttpErrorResponse } from '@angular/common/http';

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Could not reach the server. Check that the backend is running and try again.';
    }

    return error.error?.message ?? 'The request could not be completed. Please try again.';
  }

  return 'An unexpected error occurred. Please try again.';
}
