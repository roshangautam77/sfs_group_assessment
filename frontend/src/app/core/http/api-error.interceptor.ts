import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
export interface FieldViolation {
  readonly field: string;
  readonly message: string;
}
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly violations: readonly FieldViolation[] = [],
  ) {
    super(message);
  }
}
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) return throwError(() => error);
      const body: unknown = error.error;
      const problem =
        body !== null && typeof body === 'object' ? (body as Record<string, unknown>) : {};
      const violations = Array.isArray(problem['errors'])
        ? problem['errors'].filter(
            (value): value is FieldViolation =>
              value !== null &&
              typeof value === 'object' &&
              typeof value.field === 'string' &&
              typeof value.message === 'string',
          )
        : [];
      const message =
        error.status === 0
          ? 'Cannot reach the task service. Check your connection and try again.'
          : error.status >= 500
            ? 'The task service is unavailable. Please try again.'
            : typeof problem['detail'] === 'string'
              ? problem['detail']
              : 'The request could not be completed.';
      return throwError(() => new ApiError(message, error.status, violations));
    }),
  );
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
}
