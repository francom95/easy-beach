'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  descargarCsv,
  reporteEstadias,
  reportePromociones,
  reporteProductosMasVendidos,
  reporteServicios,
  reporteVentas,
  type ReporteTipo,
} from '../../../api/reportes';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { EstadoCargando } from '../../../components/EstadoCarga';
import { formatearMonto } from '../../../utils/money';
import styles from './page.module.css';

const TABS: { tipo: ReporteTipo; label: string }[] = [
  { tipo: 'ventas', label: 'Ventas' },
  { tipo: 'productos-mas-vendidos', label: 'Productos más vendidos' },
  { tipo: 'promociones', label: 'Promociones' },
  { tipo: 'estadias', label: 'Estadías' },
  { tipo: 'servicios', label: 'Servicios' },
];

function hace30Dias(): string {
  const d = new Date();
  d.setDate(d.getDate() - 30);
  return d.toISOString().slice(0, 10);
}
function hoy(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function ReportesPage() {
  const [tab, setTab] = useState<ReporteTipo>('ventas');
  const [desde, setDesde] = useState(hace30Dias());
  const [hasta, setHasta] = useState(hoy());
  const [descargando, setDescargando] = useState(false);

  const ventas = useQuery({ queryKey: ['reporte-ventas', desde, hasta], queryFn: () => reporteVentas(desde, hasta), enabled: tab === 'ventas' });
  const productos = useQuery({
    queryKey: ['reporte-productos', desde, hasta],
    queryFn: () => reporteProductosMasVendidos(desde, hasta, 20),
    enabled: tab === 'productos-mas-vendidos',
  });
  const promociones = useQuery({
    queryKey: ['reporte-promos', desde, hasta],
    queryFn: () => reportePromociones(desde, hasta),
    enabled: tab === 'promociones',
  });
  const estadias = useQuery({ queryKey: ['reporte-estadias', desde, hasta], queryFn: () => reporteEstadias(desde, hasta), enabled: tab === 'estadias' });
  const servicios = useQuery({ queryKey: ['reporte-servicios', desde, hasta], queryFn: () => reporteServicios(desde, hasta), enabled: tab === 'servicios' });

  async function exportarCsv() {
    setDescargando(true);
    try {
      await descargarCsv(tab, desde, hasta);
    } finally {
      setDescargando(false);
    }
  }

  return (
    <div className={styles.wrap}>
      <h1 className={styles.titulo}>Reportes</h1>

      <div className={styles.tabs}>
        {TABS.map(t => (
          <button key={t.tipo} className={[styles.tab, tab === t.tipo ? styles.tabActivo : ''].join(' ')} onClick={() => setTab(t.tipo)}>
            {t.label}
          </button>
        ))}
      </div>

      <div className={styles.filtros}>
        <label>
          Desde
          <input className={styles.input} type="date" value={desde} onChange={e => setDesde(e.target.value)} />
        </label>
        <label>
          Hasta
          <input className={styles.input} type="date" value={hasta} onChange={e => setHasta(e.target.value)} />
        </label>
        <Button variant="outline" onClick={exportarCsv} cargando={descargando}>
          Exportar CSV
        </Button>
      </div>

      {tab === 'ventas' ? (
        ventas.isLoading ? (
          <EstadoCargando />
        ) : ventas.data ? (
          <>
            <div className={styles.kpis}>
              <Card>
                <div className={styles.kpiLabel}>Facturación</div>
                <div className={styles.kpiValor}>{formatearMonto(ventas.data.facturacionTotal)}</div>
              </Card>
              <Card>
                <div className={styles.kpiLabel}>Pedidos entregados</div>
                <div className={styles.kpiValor}>{ventas.data.cantidadPedidos}</div>
              </Card>
              <Card>
                <div className={styles.kpiLabel}>Ticket promedio</div>
                <div className={styles.kpiValor}>{formatearMonto(ventas.data.ticketPromedio)}</div>
              </Card>
            </div>
            <table className={styles.tabla}>
              <thead>
                <tr>
                  <th>Día</th>
                  <th>Pedidos</th>
                  <th>Facturación</th>
                </tr>
              </thead>
              <tbody>
                {ventas.data.porDia.map(d => (
                  <tr key={d.dia}>
                    <td>{d.dia}</td>
                    <td>{d.cantidadPedidos}</td>
                    <td>{formatearMonto(d.facturacion)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        ) : null
      ) : null}

      {tab === 'productos-mas-vendidos' ? (
        productos.isLoading ? (
          <EstadoCargando />
        ) : (
          <table className={styles.tabla}>
            <thead>
              <tr>
                <th>Producto</th>
                <th>Unidades</th>
                <th>Facturación</th>
              </tr>
            </thead>
            <tbody>
              {(productos.data ?? []).map(p => (
                <tr key={p.productoId}>
                  <td>{p.nombreProducto}</td>
                  <td>{p.unidadesVendidas}</td>
                  <td>{formatearMonto(p.facturacion)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}

      {tab === 'promociones' ? (
        promociones.isLoading ? (
          <EstadoCargando />
        ) : (
          <table className={styles.tabla}>
            <thead>
              <tr>
                <th>Promoción</th>
                <th>Usos</th>
                <th>Descuento otorgado</th>
              </tr>
            </thead>
            <tbody>
              {(promociones.data ?? []).map(p => (
                <tr key={p.promocionId}>
                  <td>{p.nombrePromocion}</td>
                  <td>{p.usos}</td>
                  <td>{formatearMonto(p.montoDescontado)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}

      {tab === 'estadias' ? (
        estadias.isLoading ? (
          <EstadoCargando />
        ) : estadias.data ? (
          <>
            <div className={styles.kpis}>
              <Card>
                <div className={styles.kpiLabel}>Duración promedio</div>
                <div className={styles.kpiValor}>
                  {estadias.data.duracionPromedioHoras != null ? `${estadias.data.duracionPromedioHoras.toFixed(1)}h` : '—'}
                </div>
              </Card>
              <Card>
                <div className={styles.kpiLabel}>Consumo promedio</div>
                <div className={styles.kpiValor}>{formatearMonto(estadias.data.consumoPromedioPorEstadia)}</div>
              </Card>
            </div>
            <table className={styles.tabla}>
              <thead>
                <tr>
                  <th>Día</th>
                  <th>Aperturas</th>
                </tr>
              </thead>
              <tbody>
                {estadias.data.aperturasPorDia.map(a => (
                  <tr key={a.dia}>
                    <td>{a.dia}</td>
                    <td>{a.cantidad}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        ) : null
      ) : null}

      {tab === 'servicios' ? (
        servicios.isLoading ? (
          <EstadoCargando />
        ) : servicios.data ? (
          <>
            <Card className={styles.tiempoResolucion}>
              <div className={styles.kpiLabel}>Tiempo de resolución promedio</div>
              <div className={styles.kpiValor}>
                {servicios.data.tiempoResolucionPromedioMinutos != null
                  ? `${servicios.data.tiempoResolucionPromedioMinutos.toFixed(0)} min`
                  : '—'}
              </div>
            </Card>
            <table className={styles.tabla}>
              <thead>
                <tr>
                  <th>Tipo de servicio</th>
                  <th>Cantidad</th>
                </tr>
              </thead>
              <tbody>
                {servicios.data.porTipo.map(s => (
                  <tr key={s.tipoServicio}>
                    <td>{s.tipoServicio}</td>
                    <td>{s.cantidad}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        ) : null
      ) : null}
    </div>
  );
}
