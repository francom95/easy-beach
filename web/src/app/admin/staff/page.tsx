'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { invitarStaff, listarStaff, revocarStaff } from '../../../api/staff';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Badge } from '../../../components/Badge';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { useToast } from '../../../components/Toast';
import { ApiError } from '../../../api/ApiError';
import styles from './page.module.css';

export default function StaffPage() {
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['staff'], queryFn: listarStaff });
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [mostrarInvitar, setMostrarInvitar] = useState(false);
  const [email, setEmail] = useState('');
  const [nombre, setNombre] = useState('');
  const [rol, setRol] = useState<'CARPERO' | 'OPERADOR'>('CARPERO');
  const [ultimaInvitacion, setUltimaInvitacion] = useState<{ email: string; passwordTemporal: string } | null>(null);

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['staff'] });

  const invitarMut = useMutation({
    mutationFn: () => invitarStaff(email, nombre, rol),
    onSuccess: resultado => {
      setUltimaInvitacion({ email: resultado.email, passwordTemporal: resultado.passwordTemporal });
      setEmail('');
      setNombre('');
      setMostrarInvitar(false);
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos invitar al miembro', 'error'),
  });

  const revocarMut = useMutation({
    mutationFn: (usuarioPublicId: string) => revocarStaff(usuarioPublicId),
    onSuccess: () => {
      mostrar('Acceso revocado', 'info');
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos revocar el acceso', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando staff…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar el staff." onReintentar={() => refetch()} />;

  const miembros = data ?? [];

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Staff</h1>
        <Button onClick={() => setMostrarInvitar(true)}>+ Invitar</Button>
      </div>

      {ultimaInvitacion ? (
        <Card className={styles.tempPasswordCard}>
          <strong>Invitación creada para {ultimaInvitacion.email}</strong>
          <p>
            Contraseña temporal (no se envía por email en esta versión — compartila vos mismo):{' '}
            <code className={styles.tempPassword}>{ultimaInvitacion.passwordTemporal}</code>
          </p>
          <button className={styles.cerrarHint} onClick={() => setUltimaInvitacion(null)}>
            Cerrar
          </button>
        </Card>
      ) : null}

      <div className={styles.lista}>
        {miembros.map(m => (
          <Card key={m.usuarioPublicId} className={styles.miembroRow}>
            <div className={styles.avatar}>{m.usuarioNombre.slice(0, 2).toUpperCase()}</div>
            <div className={styles.info}>
              <strong>{m.usuarioNombre}</strong>
              <span className={styles.email}>{m.usuarioEmail}</span>
            </div>
            <Badge tono={m.rol === 'ADMIN_BALNEARIO' ? 'info' : 'neutro'}>{m.rol}</Badge>
            <button
              className={styles.revocarBtn}
              onClick={() => {
                if (confirm(`¿Quitarle el acceso a ${m.usuarioNombre}?`)) revocarMut.mutate(m.usuarioPublicId);
              }}
            >
              Quitar acceso
            </button>
          </Card>
        ))}
      </div>

      {mostrarInvitar ? (
        <div className={styles.modalOverlay} onClick={() => setMostrarInvitar(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Invitar miembro</h3>
            <input className={styles.input} placeholder="Nombre" value={nombre} onChange={e => setNombre(e.target.value)} />
            <input className={styles.input} placeholder="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} />
            <select className={styles.input} value={rol} onChange={e => setRol(e.target.value as 'CARPERO' | 'OPERADOR')}>
              <option value="CARPERO">Carpero</option>
              <option value="OPERADOR">Operador</option>
            </select>
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setMostrarInvitar(false)}>
                Cancelar
              </Button>
              <Button onClick={() => invitarMut.mutate()} cargando={invitarMut.isPending} disabled={!email.trim() || !nombre.trim()}>
                Invitar
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
