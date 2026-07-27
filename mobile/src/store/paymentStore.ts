import {create} from 'zustand';

/**
 * "Tarjeta guardada" simplificada (etapa 16): habilita el camino de 4 taps
 * (con tarjeta guardada, S16 se salta por completo). El token NO es un dato
 * sensible real - ver el Javadoc de S16HojaMedioPago sobre el alcance real
 * de la integración de Mercado Pago en esta etapa.
 */
type PaymentState = {
  cardTokenGuardado: string | null;
  ultimosCuatroDigitos: string | null;
  guardarTarjeta: (token: string, ultimosCuatro: string) => void;
  olvidarTarjeta: () => void;
};

export const usePaymentStore = create<PaymentState>(set => ({
  cardTokenGuardado: null,
  ultimosCuatroDigitos: null,
  guardarTarjeta: (token, ultimosCuatro) => set({cardTokenGuardado: token, ultimosCuatroDigitos: ultimosCuatro}),
  olvidarTarjeta: () => set({cardTokenGuardado: null, ultimosCuatroDigitos: null}),
}));
