'use client';

import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { reporteDashboard, reporteProductosMasVendidos } from '../../api/reportes';
import { colaPedidos } from '../../api/pedidos';
import { colaSolicitudesServicio } from '../../api/servicios';
import { pendientesDeValidacion } from '../../api/estadias';
import { listarCategorias, listarProductos } from '../../api/catalogo';
import { listarUbicaciones } from '../../api/ubicaciones';
import { estadoVinculacionMp } from '../../api/mercadopago';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { EstadoCargando } from '../../components/EstadoCarga';
import { formatearMonto } from '../../utils/money';
import styles from './page.module.css';

function hoyIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function InicioPage() {
  const router = useRouter();
  const dashboard = useQuery({ queryKey: ['reporte-dashboard'], queryFn: reporteDashboard, refetchInterval: 30000 });
  const masVendidos = useQuery({
    queryKey: ['reporte-mas-vendidos-hoy'],
    queryFn: () => reporteProductosMasVendidos(hoyIso(), hoyIso(), 5),
  });
  const pedidos = useQuery({ queryKey: ['operativo-pedidos'], queryFn: colaPedidos, refetchInterval: 15000 });
  const servicios = useQuery({ queryKey: ['operativo-servicios'], queryFn: colaSolicitudesServicio, refetchInterval: 15000 });
  const estadias = useQuery({ queryKey: ['operativo-estadias-pendientes'], queryFn: pendientesDeValidacion, refetchInterval: 15000 });
  const ubicaciones = useQuery({ queryKey: ['ubicaciones'], queryFn: listarUbicaciones });
  const categorias = useQuery({ queryKey: ['categorias'], queryFn: listarCategorias });
  const productos = useQuery({ queryKey: ['productos'], queryFn: listarProductos });
  const mp = useQuery({ queryKey: ['mp-estado'], queryFn: estadoVinculacionMp });

  if (dashboard.isLoading || ubicaciones.isLoading || categorias.isLoading || productos.isLoading || mp.isLoading) {
    return <EstadoCargando texto="Cargando dashboard…" />;
  }

  const kpis = dashboard.data;

  const pasos = [
    { hecho: (ubicaciones.data?.length ?? 0) > 0, label: 'Cargar al menos una ubicación', href: '/admin/ubicaciones' },
    { hecho: (categorias.data?.length ?? 0) > 0, label: 'Crear una categoría de menú', href: '/admin/menu' },
    { hecho: (productos.data?.length ?? 0) > 0, label: 'Cargar tu primer producto', href: '/admin/menu' },
    { hecho: !!mp.data?.vinculado, label: 'Vincular Mercado Pago', href: '/admin/cobros' },
    { hecho: false, label: 'Identidad visual (opcional)', href: '/admin/identidad-visual', opcional: true },
  ];
  const pasosObligatorios = pasos.filter(p => !p.opcional);
  const balnearioNuevo = pasosObligatorios.some(p => !p.hecho);

  if (balnearioNuevo) {
    return (
      <div className={styles.wrap}>
        <h1 className={styles.titulo}>¡Bienvenido a EasyBeach!</h1>
        <Card className={styles.onboardingCard}>
          <p className={styles.onboardingIntro}>
            Completá estos pasos para publicar tu primer menú y empezar a recibir pedidos.
          </p>
          <ul className={styles.checklist}>
            {pasos.map(p => (
              <li key={p.label} className={styles.checklistItem}>
                <span className={[styles.check, p.hecho ? styles.checkHecho : ''].join(' ')}>{p.hecho ? '✓' : ''}</span>
                <span className={p.hecho ? styles.checklistTextHecho : ''}>
                  {p.label}
                  {p.opcional ? ' (opcional)' : ''}
                </span>
                {!p.hecho ? (
                  <Button size="md" variant="outline" onClick={() => router.push(p.href)}>
                    Ir
                  </Button>
                ) : null}
              </li>
            ))}
          </ul>
        </Card>
      </div>
    );
  }

  return (
    <div className={styles.wrap}>
      <h1 className={styles.titulo}>Inicio</h1>

      <div className={styles.kpiGrid}>
        <Card>
          <div className={styles.kpiLabel}>Facturación del día</div>
          <div className={styles.kpiValor}>{kpis ? formatearMonto(kpis.facturacionHoy) : '—'}</div>
        </Card>
        <Card>
          <div className={styles.kpiLabel}>Pedidos entregados hoy</div>
          <div className={styles.kpiValor}>{kpis?.pedidosHoy ?? '—'}</div>
          <div className={styles.kpiSub}>{kpis?.pedidosEnCurso ?? 0} en curso ahora</div>
        </Card>
        <Card>
          <div className={styles.kpiLabel}>Ticket promedio</div>
          <div className={styles.kpiValor}>{kpis ? formatearMonto(kpis.ticketPromedioHoy) : '—'}</div>
        </Card>
        <Card>
          <div className={styles.kpiLabel}>Estadías a validar</div>
          <div className={styles.kpiValor}>{estadias.data?.length ?? 0}</div>
        </Card>
      </div>

      <div className={styles.rowGrid}>
        <Card>
          <div className={styles.sectionTitle}>Más vendidos hoy</div>
          {masVendidos.data && masVendidos.data.length > 0 ? (
            <ul className={styles.rankList}>
              {masVendidos.data.map(p => (
                <li key={p.productoId} className={styles.rankItem}>
                  <span>{p.nombreProducto}</span>
                  <strong>{p.unidadesVendidas}u</strong>
                </li>
              ))}
            </ul>
          ) : (
            <p className={styles.vacio}>Todavía no hay ventas hoy.</p>
          )}
        </Card>

        <Card>
          <div className={styles.sectionTitle}>Ahora mismo en el balneario</div>
          <ul className={styles.liveList}>
            <li>
              <span>Pedidos en cola</span>
              <strong>{pedidos.data?.length ?? 0}</strong>
            </li>
            <li>
              <span>Servicios pendientes</span>
              <strong>{servicios.data?.length ?? 0}</strong>
            </li>
            <li>
              <span>Estadías a validar</span>
              <strong>{estadias.data?.length ?? 0}</strong>
            </li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
