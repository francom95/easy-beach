'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '../store/authStore';
import { EstadoCargando } from '../components/EstadoCarga';
import { rutaInicioPorRol } from '../utils/rutas';

export default function Home() {
  const router = useRouter();
  const estado = useAuthStore(s => s.estado);
  const rol = useAuthStore(s => s.rol);

  useEffect(() => {
    if (estado === 'cargando') return;
    if (estado === 'anonimo') {
      router.replace('/login');
      return;
    }
    router.replace(rutaInicioPorRol(rol));
  }, [estado, rol, router]);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <EstadoCargando texto="Ingresando…" />
    </div>
  );
}
