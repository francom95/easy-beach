import React from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {EstadoCargando} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {pedidosDeEstadia} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {RootScreenProps} from '../../navigation/types';

/**
 * `CERRADA_POR_SISTEMA` (cierre administrativo de fin de temporada, etapa
 * 12) es un estado real del modelo, pero al día de esta etapa NO existe
 * todavía el job que lo dispare automáticamente (documentado en el
 * entregable de la etapa 12/15) - esta pantalla está lista para cuando
 * ese job exista; hoy es alcanzable solo si algo cierra la estadía así
 * manualmente.
 */
export function S36CerradaPorBalneario({route, navigation}: RootScreenProps<'S36CerradaPorBalneario'>) {
  const {estadiaPublicId} = route.params;
  const {tokens, spacing} = useTheme();
  const balnearioNombre = useStayStore(s => s.balnearioNombre);
  const volverASelectorDeBalnearios = useStayStore(s => s.volverASelectorDeBalnearios);

  const pedidosQuery = useQuery({queryKey: ['pedidos', estadiaPublicId], queryFn: () => pedidosDeEstadia(estadiaPublicId)});
  const totalDelDia = (pedidosQuery.data ?? [])
    .filter(p => p.estado === 'ENTREGADO')
    .reduce((acc, p) => acc + Number(p.total), 0);

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="info" texto={`${balnearioNombre} cerró tu estadía por hoy`} tamano={28} />
        {pedidosQuery.isLoading ? (
          <EstadoCargando />
        ) : (
          <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center'}}>
            Consumiste {formatearMonto(totalDelDia)} hoy. ¡Te esperamos mañana!
          </Text>
        )}
        <Button
          titulo="Volver a la lista de balnearios"
          onPress={() => {
            volverASelectorDeBalnearios();
            navigation.reset({index: 0, routes: [{name: 'S02SelectorBalneario'}]});
          }}
        />
      </View>
    </ScreenContainer>
  );
}
