import {create} from 'zustand';
import type {MenuProductoResponse, MenuVarianteResponse} from '../api/types';

export type ItemDeCarrito = {
  /** Clave local: productoId + varianteId, para poder tener el mismo producto con variantes distintas. */
  clave: string;
  productoId: number;
  productoVarianteId: number | null;
  nombreProducto: string;
  nombreVariante: string | null;
  precioUnitario: string;
  fotoUrl: string | null;
  cantidad: number;
};

type CartState = {
  items: ItemDeCarrito[];
  agregar: (producto: MenuProductoResponse, variante: MenuVarianteResponse | null) => void;
  quitarUno: (clave: string) => void;
  eliminar: (clave: string) => void;
  vaciar: () => void;
  cantidadTotal: () => number;
  subtotal: () => number;
};

function claveDe(productoId: number, varianteId: number | null): string {
  return `${productoId}:${varianteId ?? 'base'}`;
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],

  agregar: (producto, variante) => {
    const clave = claveDe(producto.id, variante?.id ?? null);
    set(state => {
      const existente = state.items.find(i => i.clave === clave);
      if (existente) {
        return {
          items: state.items.map(i => (i.clave === clave ? {...i, cantidad: i.cantidad + 1} : i)),
        };
      }
      const nuevo: ItemDeCarrito = {
        clave,
        productoId: producto.id,
        productoVarianteId: variante?.id ?? null,
        nombreProducto: producto.nombre,
        nombreVariante: variante?.nombre ?? null,
        // Precio congelado en el momento de agregar - el total real y
        // autoritativo lo recalcula el servidor al crear el pedido
        // (etapa 13); esto es solo lo que se le muestra al cliente antes.
        precioUnitario: variante?.precio ?? producto.precioBase,
        fotoUrl: producto.fotoUrl,
        cantidad: 1,
      };
      return {items: [...state.items, nuevo]};
    });
  },

  quitarUno: clave => {
    set(state => {
      const existente = state.items.find(i => i.clave === clave);
      if (!existente) {
        return state;
      }
      if (existente.cantidad <= 1) {
        return {items: state.items.filter(i => i.clave !== clave)};
      }
      return {items: state.items.map(i => (i.clave === clave ? {...i, cantidad: i.cantidad - 1} : i))};
    });
  },

  eliminar: clave => set(state => ({items: state.items.filter(i => i.clave !== clave)})),

  vaciar: () => set({items: []}),

  cantidadTotal: () => get().items.reduce((acc, i) => acc + i.cantidad, 0),

  subtotal: () => get().items.reduce((acc, i) => acc + Number(i.precioUnitario) * i.cantidad, 0),
}));
