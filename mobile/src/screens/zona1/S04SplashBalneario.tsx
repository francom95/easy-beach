import React, {useEffect, useState} from 'react';
import {Image, StyleSheet, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {misEstadiasVigentes} from '../../api/estadias';
import {obtenerBalneario} from '../../api/balnearios';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

const MIN_MS = 600;
const MAX_MS = 2000;

/**
 * Primera pantalla 100% del balneario (etapa 06): min 600ms/max 2s. Es
 * también el punto de entrada del re-ingreso diario (splash con theme
 * cacheado -> directo a home, sin onboarding/re-validación - etapa 07 S10).
 * Acá se decide a qué pantalla de la zona 2/3 corresponde entrar según si
 * ya existe una estadía vigente para ESTE balneario - el match es por
 * `balnearioId` (se resuelve el slug primero), nunca por nombre.
 */
export function S04SplashBalneario({route, navigation}: RootScreenProps<'S04SplashBalneario'>) {
  const {slug, nombre} = route.params;
  const {tokens, cargarThemeDeBalneario, usandoCacheOffline} = useTheme();
  const entrarABalneario = useStayStore(s => s.entrarABalneario);
  const setEstadia = useStayStore(s => s.setEstadia);
  const [pasoMinimoCumplido, setPasoMinimoCumplido] = useState(false);

  const balnearioQuery = useQuery({queryKey: ['balneario', slug], queryFn: () => obtenerBalneario(slug)});
  const vigentesQuery = useQuery({queryKey: ['estadias-vigentes'], queryFn: misEstadiasVigentes});

  useEffect(() => {
    const timer = setTimeout(() => setPasoMinimoCumplido(true), MIN_MS);
    const tiempoLimite = setTimeout(() => setPasoMinimoCumplido(true), MAX_MS);
    return () => {
      clearTimeout(timer);
      clearTimeout(tiempoLimite);
    };
  }, []);

  useEffect(() => {
    // Entrar al balneario (slug/nombre, ya conocidos por los route params) no
    // puede quedar atado a la promesa del theme: son operaciones async
    // independientes y con tiempos propios - si el theme tarda más que
    // balnearioQuery/vigentesQuery, S05 monta con balnearioSlug todavía en
    // null y su fetch de ubicaciones sale roto (bug real encontrado en
    // verificación con emulador).
    entrarABalneario(slug, nombre);
    let cancelado = false;
    cargarThemeDeBalneario(slug).catch(() => {
      if (!cancelado) {
        // Sin theme y sin cache (primera vez en este balneario, sin red):
        // no hay nada que mostrar todavía.
        navigation.replace('S33SinConexion', {
          alReintentar: () => navigation.replace('S04SplashBalneario', {slug, nombre}),
        });
      }
    });
    return () => {
      cancelado = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug, nombre]);

  useEffect(() => {
    if (balnearioQuery.isError) {
      const error = balnearioQuery.error as ApiError;
      if (error.code === 'BALNEARIO_NO_OPERATIVO' || error.status === 404) {
        navigation.replace('S34FueraDeTemporada', {nombreBalneario: nombre});
      }
    }
  }, [balnearioQuery.isError, balnearioQuery.error, navigation, nombre]);

  useEffect(() => {
    if (!pasoMinimoCumplido || vigentesQuery.isLoading || balnearioQuery.isLoading || !balnearioQuery.data) {
      return;
    }
    const balnearioId = balnearioQuery.data.id;
    const estadiaDeEsteBalneario = (vigentesQuery.data ?? []).find(e => e.balnearioId === balnearioId);
    if (estadiaDeEsteBalneario) {
      setEstadia(estadiaDeEsteBalneario);
      if (estadiaDeEsteBalneario.estado === 'PENDIENTE_VALIDACION') {
        navigation.replace('S06PendienteValidacion', {estadiaPublicId: estadiaDeEsteBalneario.publicId});
      } else {
        navigation.replace('MainTabs');
      }
      return;
    }
    navigation.replace('S05ElegirUbicacion');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pasoMinimoCumplido, vigentesQuery.data, vigentesQuery.isLoading, balnearioQuery.data, balnearioQuery.isLoading]);

  return (
    <View style={[styles.contenedor, {backgroundColor: tokens['color.background']}]}>
      {tokens['asset.splash'] ? (
        <Image source={{uri: tokens['asset.splash']}} style={StyleSheet.absoluteFill} resizeMode="cover" />
      ) : null}
      {tokens['asset.logo'] ? (
        <Image source={{uri: tokens['asset.logo']}} style={styles.logo} resizeMode="contain" />
      ) : (
        <Text style={{fontSize: 28, fontWeight: '800', color: tokens['color.on-background']}}>{nombre}</Text>
      )}
      {usandoCacheOffline ? (
        <Text style={{color: tokens['color.on-surface-muted'], marginTop: 12}}>Mostrando datos guardados</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  contenedor: {flex: 1, alignItems: 'center', justifyContent: 'center'},
  logo: {width: 160, height: 160},
});
