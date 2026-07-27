import type { ProblemDetail } from './types';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly detail: string;
  readonly sinConexion: boolean;

  constructor(problem: ProblemDetail | null, status: number, sinConexion = false) {
    super(problem?.detail ?? (sinConexion ? 'Sin conexión' : `Error ${status}`));
    this.status = status;
    this.code = problem?.code ?? null;
    this.detail = problem?.detail ?? this.message;
    this.sinConexion = sinConexion;
  }
}
