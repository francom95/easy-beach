import React, {useEffect, useState} from 'react';
import {FlatList, Pressable, Text, View} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {Card} from '../../components/Card';
import {EstadoVacio} from '../../components/EstadoCarga';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {usePaymentStore} from '../../store/paymentStore';
import {obtenerMenu} from '../../api/catalogo';
import {crearPedido} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import {generarIdempotencyKey} from '../../utils/idempotency';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

/**
 * S15 = checkout (etapa 07): 4 cosas visibles sin scroll (qué pediste,
 * cuánto, adónde va, cómo pagás). "Lo cobra {balneario}" - el pago va
 * literalmente a la cuenta del balneario, `application_fee=0` (ADR-004),
 * nunca a EasyBeach. La clave de idempotencia se genera UNA vez al entrar
 * y se reutiliza en cada reintento del MISMO intento de pago (ver
 * criterio de aceptación: "modo avión a mitad de un pedido" no debe
 * duplicar el pedido al reconectar).
 */
export function S15Carrito({navigation}: RootScreenProps<'S15Carrito'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const estadia = useStayStore(s => s.estadia);
  const balnearioNombre = useStayStore(s => s.balnearioNombre);
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const cart = useCartStore();
  const {cardTokenGuardado, ultimosCuatroDigitos} = usePaymentStore();
  const [idempotencyKey] = useState(() => generarIdempotencyKey());

  // Revalidación de precio/stock antes de cobrar (S35): si el menú cambió
  // desde que se armó el carrito, esto lo detecta antes de intentar pagar.
  const menuQuery = useQuery({queryKey: ['menu', balnearioSlug], queryFn: () => obtenerMenu(balnearioSlug)});

  const pagarMutation = useMutation({
    mutationFn: () => crearPedido(estadia!.publicId, cart.items.map(i => ({
      productoId: i.productoId,
      productoVarianteId: i.productoVarianteId,
      cantidad: i.cantidad,
    })), cardTokenGuardado!, idempotencyKey),
    onSuccess: pedido => {
      cart.vaciar();
      if (pedido.estado === 'CONFIRMADO') {
        navigation.replace('S18PagoAprobado', {pedidoPublicId: pedido.publicId});
      } else if (pedido.estado === 'PAGO_RECHAZADO') {
        navigation.replace('S19PagoRechazado', {pedidoPublicId: pedido.publicId, motivoDetalle: 'El pago no fue aprobado'});
      } else {
        navigation.replace('S17ProcesandoPago', {pedidoPublicId: pedido.publicId});
      }
    },
  });

  function alConfirmar() {
    if (!estadia || estadia.estado !== 'ACTIVA') {
      return; // el botón ya está deshabilitado en este caso, ver más abajo
    }
    if (!cardTokenGuardado) {
      navigation.navigate('S16HojaMedioPago');
      return;
    }
    pagarMutation.mutate();
  }

  useEffect(() => {
    if (pagarMutation.isError) {
      const error = pagarMutation.error as ApiError;
      if (error.status === 422) {
        // Un ítem dejó de estar disponible o cambió de precio entre que se
        // armó el carrito y el checkout (etapa 03: revalidación obligatoria).
        navigation.navigate('S14ProductoAgotado', {productoId: cart.items[0]?.productoId ?? 0});
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagarMutation.isError]);

  if (cart.items.length === 0) {
    return (
      <ScreenContainer>
        <EstadoVacio titulo="Tu carrito está vacío" descripcion="Volvé al menú para agregar algo." />
      </ScreenContainer>
    );
  }

  const combosDisponibles = (menuQuery.data ?? [])
    .flatMap(c => c.productos)
    .flatMap(p => p.promociones)
    .filter(p => p.tipo === 'COMBO');

  return (
    <ScreenContainer>
      <FlatList
        style={{flex: 1}}
        data={cart.items}
        keyExtractor={i => i.clave}
        ListHeaderComponent={
          <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.md}}>
            Tu pedido
          </Text>
        }
        renderItem={({item}) => (
          <Card style={{marginBottom: spacing.sm}}>
            <View style={{flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'}}>
              <View style={{flex: 1}}>
                <Text style={{color: tokens['color.on-surface'], fontWeight: '700'}}>{item.nombreProducto}</Text>
                {item.nombreVariante ? (
                  <Text style={{color: tokens['color.on-surface-muted']}}>{item.nombreVariante}</Text>
                ) : null}
              </View>
              <View style={{flexDirection: 'row', alignItems: 'center', gap: spacing.sm}}>
                <Pressable onPress={() => cart.quitarUno(item.clave)}>
                  <Text style={{fontSize: 18, color: tokens['color.on-surface']}}>-</Text>
                </Pressable>
                <Text style={{fontWeight: '700'}}>{item.cantidad}</Text>
                <Pressable onPress={() => cart.agregar({id: item.productoId, nombre: item.nombreProducto, descripcion: null, precioBase: item.precioUnitario, fotoUrl: item.fotoUrl, variantes: [], promociones: []}, item.nombreVariante ? {id: item.productoVarianteId!, nombre: item.nombreVariante, precio: item.precioUnitario} : null)}>
                  <Text style={{fontSize: 18, color: tokens['color.on-surface']}}>+</Text>
                </Pressable>
              </View>
              <Text style={{fontWeight: '700', marginLeft: spacing.md, color: tokens['color.on-surface']}}>
                {formatearMonto(Number(item.precioUnitario) * item.cantidad)}
              </Text>
            </View>
          </Card>
        )}
        ListFooterComponent={
          <View>
            {combosDisponibles.length > 0 ? (
              <Card style={{backgroundColor: tokens['color.warning'], marginBottom: spacing.md}}>
                <StateRow tipo="info" texto="Hay combos disponibles que te convienen" />
              </Card>
            ) : null}

            <View style={{flexDirection: 'row', justifyContent: 'space-between', marginVertical: spacing.md}}>
              <Text style={{color: tokens['color.on-surface-muted']}}>Subtotal (estimado)</Text>
              <Text style={{fontWeight: '700', color: tokens['color.on-surface']}}>{formatearMonto(cart.subtotal())}</Text>
            </View>
            <Text style={{color: tokens['color.on-surface-muted'], fontSize: typeScale.label, marginBottom: spacing.md}}>
              El total final (con descuentos si aplican) lo calcula {balnearioNombre}, no esta app.
            </Text>

            <Card style={{marginBottom: spacing.md}}>
              <Text style={{color: tokens['color.on-surface-muted'], fontSize: typeScale.label}}>Entrega en</Text>
              <Text style={{color: tokens['color.on-surface'], fontWeight: '700'}}>{estadia?.ubicacionIdentificador}</Text>
            </Card>

            <Pressable onPress={() => navigation.navigate('S16HojaMedioPago')}>
              <Card style={{marginBottom: spacing.lg, borderRadius: radii.control}}>
                <Text style={{color: tokens['color.on-surface-muted'], fontSize: typeScale.label}}>Pagás con</Text>
                <Text style={{color: tokens['color.on-surface'], fontWeight: '700'}}>
                  {ultimosCuatroDigitos ? `Tarjeta terminada en ${ultimosCuatroDigitos}` : 'Elegir medio de pago'}
                </Text>
              </Card>
            </Pressable>

            {estadia?.estado !== 'ACTIVA' ? (
              <View style={{marginBottom: spacing.md}}>
                <StateRow tipo="advertencia" texto="Esperá a que se confirme tu estadía para poder pagar" />
              </View>
            ) : null}

            {pagarMutation.isError && (pagarMutation.error as ApiError).status !== 422 ? (
              <View style={{marginBottom: spacing.md}}>
                <StateRow tipo="error" texto={(pagarMutation.error as ApiError).detail} />
              </View>
            ) : null}
          </View>
        }
      />

      <Button
        titulo={`Pagar y confirmar · ${formatearMonto(cart.subtotal())}`}
        onPress={alConfirmar}
        cargando={pagarMutation.isPending}
        deshabilitado={estadia?.estado !== 'ACTIVA'}
      />
    </ScreenContainer>
  );
}
