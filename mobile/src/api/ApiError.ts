import type {ProblemDetail} from './types';

/**
 * Envuelve el error RFC 7807 de la etapa 04. `code` es el contrato estable
 * (11 valores reales, ver ErrorCode.java) para lógica de la app; `detail`
 * es el mensaje humano del servidor y es la fuente de verdad para casos
 * puntuales que `code` no distingue (ej. "ya tenés una estadía abierta" vs.
 * "esa ubicación no está disponible" son ambos CONFLICTO_DE_ESTADO).
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly detail: string;
  readonly fieldErrors: {field: string; message: string}[];
  readonly isNetworkError: boolean;

  constructor(problem: ProblemDetail | null, status: number, isNetworkError = false) {
    super(problem?.detail ?? 'Error de red');
    this.status = status;
    this.code = problem?.code ?? 'ERROR_DE_RED';
    this.detail = problem?.detail ?? 'No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.';
    this.fieldErrors = problem?.errors ?? [];
    this.isNetworkError = isNetworkError;
  }

  get esConflicto(): boolean {
    return this.status === 409;
  }

  get esValidacion(): boolean {
    return this.status === 422;
  }

  get esNoEncontrado(): boolean {
    return this.status === 404;
  }
}
