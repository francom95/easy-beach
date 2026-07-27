import styles from './Badge.module.css';

type Tono = 'exito' | 'advertencia' | 'error' | 'info' | 'neutro';

export function Badge({ children, tono = 'neutro' }: { children: React.ReactNode; tono?: Tono }) {
  return <span className={[styles.badge, styles[tono]].join(' ')}>{children}</span>;
}
