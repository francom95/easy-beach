/** Los montos viajan como string decimal (etapa 04 §3) - nunca number. Formato ARS simple, sin librería de i18n. */
export function formatearMonto(valor: string | number): string {
  const numero = typeof valor === 'string' ? Number(valor) : valor;
  if (Number.isNaN(numero)) {
    return valor.toString();
  }
  return `$${numero.toLocaleString('es-AR', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
}
