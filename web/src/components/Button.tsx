'use client';

import styles from './Button.module.css';

type Props = {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost';
  size?: 'md' | 'lg';
  type?: 'button' | 'submit';
  disabled?: boolean;
  cargando?: boolean;
  fullWidth?: boolean;
};

export function Button({
  children,
  onClick,
  variant = 'primary',
  size = 'md',
  type = 'button',
  disabled,
  cargando,
  fullWidth,
}: Props) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || cargando}
      className={[styles.btn, styles[variant], styles[size], fullWidth ? styles.fullWidth : ''].join(' ')}
    >
      {cargando ? 'Guardando…' : children}
    </button>
  );
}
