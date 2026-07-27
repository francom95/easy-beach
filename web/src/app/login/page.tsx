'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../../providers/AuthProvider';
import { Button } from '../../components/Button';
import { ApiError } from '../../api/ApiError';
import { rutaInicioPorRol } from '../../utils/rutas';
import styles from './page.module.css';

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const tokens = await login(email, password);
      router.replace(rutaInicioPorRol(tokens.rol));
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : 'No pudimos iniciar sesión.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className={styles.page}>
      <form className={styles.card} onSubmit={onSubmit}>
        <div className={styles.logo}>EB</div>
        <h1 className={styles.title}>EasyBeach</h1>
        <p className={styles.subtitle}>Panel de staff</p>
        <input
          className={styles.input}
          type="email"
          placeholder="Email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          required
        />
        <input
          className={styles.input}
          type="password"
          placeholder="Contraseña"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
        />
        {error ? <p className={styles.error}>{error}</p> : null}
        <Button type="submit" size="lg" fullWidth cargando={cargando}>
          Ingresar
        </Button>
      </form>
    </div>
  );
}
