'use client';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useColaPedidos } from '../../../hooks/useOperativoQueries';
import { cancelarPedido, transicionarPedido } from '../../../api/pedidos';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { Button } from '../../../components/Button';
import { useToast } from '../../../components/Toast';
import { formatearAntiguedad, minutosDesde, tonoPorAntiguedad } from '../../../utils/tiempo';
import { formatearMonto } from '../../../utils/money';
import type { EstadoPedido, PedidoResponse } from '../../../api/types';
import styles from './page.module.css';

const COLUMNAS: { estado: EstadoPedido; titulo: string; siguiente: EstadoPedido; accion: string }[] = [
  { estado: 'CONFIRMADO', titulo: 'Nuevos', siguiente: 'EN_PREPARACION', accion: 'Tomar' },
  { estado: 'EN_PREPARACION', titulo: 'En preparación', siguiente: 'EN_CAMINO', accion: 'Despachar' },
  { estado: 'EN_CAMINO', titulo: 'En camino', siguiente: 'ENTREGADO', accion: 'Entregar' },
];

const MOTIVOS_CANCELACION = [
  'El cliente lo canceló',
  'Producto agotado',
  'Error en el pedido',
  'Otro motivo',
];

export default function PedidosPage() {
  const { data, isLoading, isError, refetch } = useColaPedidos();
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [cancelando, setCancelando] = useState<PedidoResponse | null>(null);

  const transicion = useMutation({
    mutationFn: ({ publicId, estado }: { publicId: string; estado: EstadoPedido }) => transicionarPedido(publicId, estado),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operativo-pedidos'] });
      mostrar('Pedido actualizado', 'exito');
    },
    onError: () => mostrar('No pudimos actualizar el pedido', 'error'),
  });

  const cancelacion = useMutation({
    mutationFn: ({ publicId, motivo }: { publicId: string; motivo: string }) => cancelarPedido(publicId, motivo),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operativo-pedidos'] });
      mostrar('Pedido cancelado y reembolsado', 'info');
      setCancelando(null);
    },
    onError: () => mostrar('No pudimos cancelar el pedido', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando cola de pedidos…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar los pedidos." onReintentar={() => refetch()} />;

  const pedidos = data ?? [];
  if (pedidos.length === 0) {
    return <EmptyState titulo="Cola vacía, todo al día" descripcion="Escuchando pedidos en vivo…" />;
  }

  return (
    <>
      <div className={styles.board}>
        {COLUMNAS.map(col => {
          const items = pedidos.filter(p => p.estado === col.estado);
          return (
            <div key={col.estado} className={styles.columna}>
              <div className={styles.columnaHeader}>
                <span>{col.titulo}</span>
                <span className={styles.contador}>{items.length}</span>
              </div>
              <div className={styles.tarjetas}>
                {items.map(pedido => {
                  const minutos = minutosDesde(pedido.createdAt);
                  return (
                    <div key={pedido.publicId} className={styles.tarjeta}>
                      <div className={styles.tarjetaTop}>
                        <strong className={styles.ubicacion}>{pedido.ubicacionIdentificador}</strong>
                        <span className={[styles.antiguedad, styles[tonoPorAntiguedad(minutos)]].join(' ')}>
                          {formatearAntiguedad(minutos)}
                        </span>
                      </div>
                      <ul className={styles.items}>
                        {pedido.items.map((item, i) => (
                          <li key={i}>
                            {item.cantidad}× {item.nombreProducto}
                            {item.nombreVariante ? ` (${item.nombreVariante})` : ''}
                          </li>
                        ))}
                      </ul>
                      <div className={styles.total}>{formatearMonto(pedido.total)}</div>
                      <div className={styles.acciones}>
                        <Button
                          fullWidth
                          onClick={() => transicion.mutate({ publicId: pedido.publicId, estado: col.siguiente })}
                          cargando={transicion.isPending}
                        >
                          {col.accion}
                        </Button>
                        <button className={styles.cancelarBtn} onClick={() => setCancelando(pedido)} title="Cancelar pedido">
                          ✕
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {cancelando ? (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <h3>Cancelar pedido de {cancelando.ubicacionIdentificador}</h3>
            <p className={styles.modalHint}>Se reembolsa automáticamente el pago del cliente.</p>
            <div className={styles.motivos}>
              {MOTIVOS_CANCELACION.map(motivo => (
                <button
                  key={motivo}
                  className={styles.motivoBtn}
                  disabled={cancelacion.isPending}
                  onClick={() => cancelacion.mutate({ publicId: cancelando.publicId, motivo })}
                >
                  {motivo}
                </button>
              ))}
            </div>
            <Button variant="ghost" fullWidth onClick={() => setCancelando(null)}>
              Volver al tablero
            </Button>
          </div>
        </div>
      ) : null}
    </>
  );
}
