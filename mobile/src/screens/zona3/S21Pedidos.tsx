import React, {useMemo} from 'react';
import {Pressable, SectionList, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Card} from '../../components/Card';
import {EstadoCargando, EstadoError, EstadoVacio} from '../../components/EstadoCarga';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {pedidosDeEstadia} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {PedidoResponse} from '../../api/types';
import type {TabScreenProps} from '../../navigation/types';

const ESTADOS_EN_CURSO = ['CREADO', 'PAGO_PENDIENTE', 'PAGO_RECHAZADO', 'CONFIRMADO', 'EN_PREPARACION', 'EN_CAMINO'];

/**
 * Agrupado por día (la estadía puede durar toda la temporada - etapa 12);
 * "en curso" pinneado arriba. Cada pedido entregado ofrece "pedir de
 * nuevo". El total de la estadía cierra la lista, adelantando S29.
 */
export function S21Pedidos({navigation}: TabScreenProps<'S21Pedidos'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const estadia = useStayStore(s => s.estadia);
  const agregar = useCartStore(s => s.agregar);

  const pedidosQuery = useQuery({
    queryKey: ['pedidos', estadia?.publicId],
    queryFn: () => pedidosDeEstadia(estadia!.publicId),
    enabled: Boolean(estadia),
    refetchInterval: 15000,
  });

  const {secciones, enCurso, totalGeneral} = useMemo(() => {
    const pedidos = pedidosQuery.data ?? [];
    const activos = pedidos.filter(p => ESTADOS_EN_CURSO.includes(p.estado));
    const porDia = new Map<string, PedidoResponse[]>();
    for (const pedido of pedidos.filter(p => !ESTADOS_EN_CURSO.includes(p.estado))) {
      const dia = new Date(pedido.createdAt).toLocaleDateString('es-AR', {day: '2-digit', month: 'long'});
      porDia.set(dia, [...(porDia.get(dia) ?? []), pedido]);
    }
    const total = pedidos
      .filter(p => p.estado === 'ENTREGADO')
      .reduce((acc, p) => acc + Number(p.total), 0);
    return {
      secciones: Array.from(porDia.entries()).map(([title, data]) => ({title, data})),
      enCurso: activos,
      totalGeneral: total,
    };
  }, [pedidosQuery.data]);

  function pedirDeNuevo(pedido: PedidoResponse) {
    for (const item of pedido.items) {
      agregar(
        {id: 0, nombre: item.nombreProducto, descripcion: null, precioBase: item.precioUnitario, fotoUrl: null, variantes: [], promociones: []},
        item.nombreVariante ? {id: 0, nombre: item.nombreVariante, precio: item.precioUnitario} : null,
      );
    }
    navigation.navigate('S15Carrito');
  }

  if (pedidosQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }
  if (pedidosQuery.isError) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar tus pedidos." onReintentar={() => pedidosQuery.refetch()} />
      </ScreenContainer>
    );
  }
  if ((pedidosQuery.data ?? []).length === 0) {
    return (
      <ScreenContainer>
        <EstadoVacio titulo="Todavía no pediste nada" descripcion="Andá al menú para hacer tu primer pedido." />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <SectionList
        sections={secciones}
        keyExtractor={item => item.publicId}
        ListHeaderComponent={
          <View>
            <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.md}}>
              Tus pedidos
            </Text>
            {enCurso.map(pedido => (
              <Pressable key={pedido.publicId} onPress={() => navigation.navigate('S20DetallePedido', {pedidoPublicId: pedido.publicId})}>
                <Card style={{marginBottom: spacing.sm, borderColor: tokens['color.info']}}>
                  <StateRow tipo="espera" texto={`${pedido.estado.replace('_', ' ')} · ${formatearMonto(pedido.total)}`} />
                </Card>
              </Pressable>
            ))}
          </View>
        }
        renderSectionHeader={({section}) => (
          <Text style={{fontWeight: '700', color: tokens['color.on-surface-muted'], marginVertical: spacing.sm}}>
            {section.title}
          </Text>
        )}
        renderItem={({item}) => (
          <Card style={{marginBottom: spacing.sm}}>
            <View style={{flexDirection: 'row', justifyContent: 'space-between'}}>
              <Text style={{color: tokens['color.on-surface']}}>{item.items.length} producto(s)</Text>
              <Text style={{fontWeight: '700', color: tokens['color.on-surface']}}>{formatearMonto(item.total)}</Text>
            </View>
            {item.estado === 'ENTREGADO' ? (
              <Pressable onPress={() => pedirDeNuevo(item)} style={{marginTop: spacing.sm}}>
                <Text style={{color: tokens['color.primary'], fontWeight: '700'}}>Pedir de nuevo</Text>
              </Pressable>
            ) : null}
          </Card>
        )}
        ListFooterComponent={
          <View style={{marginTop: spacing.lg, padding: spacing.md, borderRadius: radii.control, backgroundColor: tokens['color.surface']}}>
            <Text style={{fontWeight: '800', color: tokens['color.on-surface']}}>
              Total consumido: {formatearMonto(totalGeneral)}
            </Text>
          </View>
        }
      />
    </ScreenContainer>
  );
}
