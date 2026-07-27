'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../store/authStore';
import { EstadoCargando } from './EstadoCarga';
import { rutaInicioPorRol } from '../utils/rutas';

/** Guard de ruta client-side: sin cookies de servidor no hay proxy/middleware que pueda leer la sesión (vive en memoria + localStorage). */
export function RequireAuth({ rolesPermitidos, children }: { rolesPermitidos: string[]; children: React.ReactNode }) {
  const router = useRouter();
  const estado = useAuthStore(s => s.estado);
  const rol = useAuthStore(s => s.rol);

  useEffect(() => {
    if (estado === 'cargando') return;
    if (estado === 'anonimo') {
      router.replace('/login');
      return;
    }
    if (rol && !rolesPermitidos.includes(rol)) {
      router.replace(rutaInicioPorRol(rol));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [estado, rol]);

  if (estado !== 'autenticado' || (rol && !rolesPermitidos.includes(rol))) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <EstadoCargando />
      </div>
    );
  }

  return <>{children}</>;
}
