export function minutosDesde(iso: string): number {
  return Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
}

/** Umbral de demora del mockup de etapa 08: >10 min = advertencia, >20 min = urgente. */
export function tonoPorAntiguedad(minutos: number): 'neutro' | 'advertencia' | 'error' {
  if (minutos >= 20) return 'error';
  if (minutos >= 10) return 'advertencia';
  return 'neutro';
}

export function formatearAntiguedad(minutos: number): string {
  if (minutos < 1) return 'recién';
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.floor(minutos / 60);
  return `${horas}h ${minutos % 60}min`;
}
