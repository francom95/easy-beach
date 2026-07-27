import {QueryClient} from '@tanstack/react-query';

/**
 * `refetchInterval` por query es el mecanismo de "polling adaptativo" del
 * fallback de ADR-003 - se configura por hook (`useOrderPolling`, etc.), no
 * acá. `retry: false` por defecto: los reintentos de red con idempotencia
 * son explícitos (etapa 13), no algo que React Query decida por su cuenta.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      staleTime: 15_000,
    },
    mutations: {
      retry: false,
    },
  },
});
