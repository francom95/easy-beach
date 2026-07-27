'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { RequireAuth } from '../../components/RequireAuth';
import { useAuth } from '../../providers/AuthProvider';
import { obtenerMiBalnearioStaff } from '../../api/balnearios';
import { useColaPedidos, useColaServicios, useEstadiasPendientes } from '../../hooks/useOperativoQueries';
import { useOperativoRealtime } from '../../hooks/useOperativoRealtime';
import styles from './layout.module.css';

const TABS = [
  { href: '/operativo/pedidos', label: 'Pedidos' },
  { href: '/operativo/servicios', label: 'Servicios' },
  { href: '/operativo/estadias', label: 'Estadías' },
];

function Reloj() {
  const [hora, setHora] = useState('');
  useEffect(() => {
    const actualizar = () => setHora(new Date().toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' }));
    actualizar();
    const id = setInterval(actualizar, 30000);
    return () => clearInterval(id);
  }, []);
  return <span className={styles.reloj}>{hora}</span>;
}

function OperativoShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { logout } = useAuth();
  useOperativoRealtime();

  const balnearioQuery = useQuery({ queryKey: ['mi-balneario-staff'], queryFn: obtenerMiBalnearioStaff });
  const pedidosQuery = useColaPedidos();
  const serviciosQuery = useColaServicios();
  const estadiasQuery = useEstadiasPendientes();

  const contadores: Record<string, number> = {
    '/operativo/pedidos': pedidosQuery.data?.length ?? 0,
    '/operativo/servicios': serviciosQuery.data?.length ?? 0,
    '/operativo/estadias': estadiasQuery.data?.length ?? 0,
  };

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <span className={styles.logo}>EB</span>
          <div>
            <div className={styles.balnearioNombre}>{balnearioQuery.data?.nombre ?? 'EasyBeach'}</div>
            <div className={styles.enVivo}>
              <span className={styles.pulso} /> EN VIVO
            </div>
          </div>
        </div>
        <div className={styles.headerRight}>
          <Reloj />
          <button className={styles.logoutBtn} onClick={() => logout()}>
            Salir
          </button>
        </div>
      </header>
      <nav className={styles.tabs}>
        {TABS.map(tab => {
          const activo = pathname.startsWith(tab.href);
          const contador = contadores[tab.href];
          return (
            <button
              key={tab.href}
              className={[styles.tab, activo ? styles.tabActivo : ''].join(' ')}
              onClick={() => router.push(tab.href)}
            >
              {tab.label}
              {contador > 0 ? <span className={styles.tabBadge}>{contador}</span> : null}
            </button>
          );
        })}
      </nav>
      <main className={styles.main}>{children}</main>
    </div>
  );
}

export default function OperativoLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth rolesPermitidos={['CARPERO', 'OPERADOR', 'ADMIN_BALNEARIO']}>
      <OperativoShell>{children}</OperativoShell>
    </RequireAuth>
  );
}
