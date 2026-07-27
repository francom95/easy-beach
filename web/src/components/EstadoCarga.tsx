import styles from './EstadoCarga.module.css';

export function EstadoCargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className={styles.wrap}>
      <span className={styles.spinner} />
      <span>{texto}</span>
    </div>
  );
}

export function EstadoError({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div className={styles.errorWrap}>
      <span>⚠ {mensaje}</span>
      {onReintentar ? (
        <button className={styles.reintentar} onClick={onReintentar}>
          Reintentar
        </button>
      ) : null}
    </div>
  );
}
