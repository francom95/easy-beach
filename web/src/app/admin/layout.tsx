'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { RequireAuth } from '../../components/RequireAuth';
import { useAuth } from '../../providers/AuthProvider';
import { obtenerMiBalneario } from '../../api/balnearios';
import { estadoVinculacionMp } from '../../api/mercadopago';
import styles from './layout.module.css';

const SECCIONES = [
  { href: '/admin', label: 'Inicio', exact: true },
  { href: '/admin/menu', label: 'Menú' },
  { href: '/admin/ubicaciones', label: 'Ubicaciones' },
  { href: '/admin/tipos-servicio', label: 'Tipos de servicio' },
  { href: '/admin/promociones', label: 'Promociones' },
  { href: '/admin/staff', label: 'Staff' },
  { href: '/admin/identidad-visual', label: 'Identidad visual' },
  { href: '/admin/cobros', label: 'Cobros' },
  { href: '/admin/reportes', label: 'Reportes' },
];

function AdminShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { logout } = useAuth();
  const balnearioQuery = useQuery({ queryKey: ['mi-balneario'], queryFn: obtenerMiBalneario });
  const mpQuery = useQuery({ queryKey: ['mp-estado'], queryFn: estadoVinculacionMp });

  const mpSinVincular = mpQuery.data && !mpQuery.data.vinculado;

  return (
    <div className={styles.page}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.logo}>EB</span>
          <div>
            <div className={styles.balnearioNombre}>{balnearioQuery.data?.nombre ?? 'EasyBeach'}</div>
            <div className={styles.balnearioEstado}>
              {balnearioQuery.data?.operativo ? '● Operativo' : '○ No operativo'}
            </div>
          </div>
        </div>
        <nav className={styles.nav}>
          {SECCIONES.map(sec => {
            const activo = sec.exact ? pathname === sec.href : pathname.startsWith(sec.href);
            return (
              <button
                key={sec.href}
                className={[styles.navItem, activo ? styles.navItemActivo : ''].join(' ')}
                onClick={() => router.push(sec.href)}
              >
                {sec.label}
                {sec.href === '/admin/cobros' && mpSinVincular ? <span className={styles.alerta}>!</span> : null}
              </button>
            );
          })}
        </nav>
        <button className={styles.logoutBtn} onClick={() => logout()}>
          Salir
        </button>
      </aside>
      <main className={styles.main}>
        {mpSinVincular ? (
          <div className={styles.mpWarning}>
            ⚠ No tenés Mercado Pago vinculado — los clientes no pueden pagar pedidos.{' '}
            <button className={styles.mpWarningLink} onClick={() => router.push('/admin/cobros')}>
              Vincular ahora
            </button>
          </div>
        ) : null}
        {children}
      </main>
    </div>
  );
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth rolesPermitidos={['ADMIN_BALNEARIO']}>
      <AdminShell>{children}</AdminShell>
    </RequireAuth>
  );
}
