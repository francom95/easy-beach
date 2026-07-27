/** Ruta de inicio por rol tras login/refresh — un solo lugar para las 3 llamadas (login, redirect raíz, guard de rutas). */
export function rutaInicioPorRol(rol: string | null): string {
  if (rol === 'ADMIN_BALNEARIO') return '/admin';
  if (rol === 'SUPER_ADMIN') return '/super-admin';
  return '/operativo';
}
