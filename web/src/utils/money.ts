const formatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 });

export function formatearMonto(valor: string | number): string {
  return formatter.format(Number(valor));
}
