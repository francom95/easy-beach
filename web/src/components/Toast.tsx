'use client';

import { createContext, useCallback, useContext, useState } from 'react';
import styles from './Toast.module.css';

type Toast = { id: number; texto: string; tono: 'exito' | 'error' | 'info' };
type ToastContextValue = { mostrar: (texto: string, tono?: Toast['tono']) => void };

const ToastContext = createContext<ToastContextValue | null>(null);

let contador = 0;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const mostrar = useCallback((texto: string, tono: Toast['tono'] = 'info') => {
    const id = ++contador;
    setToasts(prev => [...prev, { id, texto, tono }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000);
  }, []);

  return (
    <ToastContext.Provider value={{ mostrar }}>
      {children}
      <div className={styles.stack}>
        {toasts.map(t => (
          <div key={t.id} className={[styles.toast, styles[t.tono]].join(' ')}>
            {t.texto}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast debe usarse dentro de ToastProvider');
  return ctx;
}
