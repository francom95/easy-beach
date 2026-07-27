import styles from './EmptyState.module.css';

export function EmptyState({
  titulo,
  descripcion,
  accion,
}: {
  titulo: string;
  descripcion?: string;
  accion?: React.ReactNode;
}) {
  return (
    <div className={styles.wrap}>
      <strong className={styles.titulo}>{titulo}</strong>
      {descripcion ? <p className={styles.descripcion}>{descripcion}</p> : null}
      {accion}
    </div>
  );
}
