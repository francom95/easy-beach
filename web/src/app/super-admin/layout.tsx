'use client';

import { usePathname, useRouter } from 'next/navigation';
import { RequireAuth } from '../../components/RequireAuth';
import { useAuth } from '../../providers/AuthProvider';
import styles from './layout.module.css';

const SECCIONES = [
  { href: '/super-admin', label: 'Balnearios', exact: true },
  { href: '/super-admin/planes', label: 'Planes' },
  { href: '/super-admin/temporadas', label: 'Temporadas' },
  { href: '/super-admin/auditoria', label: 'Auditoría' },
];

function SuperAdminShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { logout } = useAuth();

  return (
    <div className={styles.page}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.logo}>EB</span>
          <div>
            <div className={styles.nombre}>EasyBeach</div>
            <div className={styles.rotulo}>Super Admin</div>
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
              </button>
            );
          })}
        </nav>
        <button className={styles.logoutBtn} onClick={() => logout()}>
          Salir
        </button>
      </aside>
      <main className={styles.main}>{children}</main>
    </div>
  );
}

export default function SuperAdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth rolesPermitidos={['SUPER_ADMIN']}>
      <SuperAdminShell>{children}</SuperAdminShell>
    </RequireAuth>
  );
}
