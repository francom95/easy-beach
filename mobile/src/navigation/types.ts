import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {CompositeScreenProps} from '@react-navigation/native';
import type {BottomTabScreenProps} from '@react-navigation/bottom-tabs';

/**
 * Mapa de navegación de la etapa 07 (33 pantallas, zonas 1-4). Un stack raíz
 * único (más simple que anidar un stack por zona) + un tab navigator para
 * la zona 3 (Inicio/Menú/Pedidos/Estadía) montado como una pantalla más del
 * stack raíz - así el detalle de producto/carrito/pago empujan por ENCIMA
 * de la tab bar, que es el patrón esperado (S11->S15 no debería perder la
 * tab bar de fondo hasta que hace falta, ej. checkout).
 */
export type RootStackParamList = {
  // Zona 1 - marca EasyBeach
  S01Login: undefined;
  S02SelectorBalneario: undefined;
  S03Transicion: {slug: string; nombre: string; colorOrigen: string};
  S04SplashBalneario: {slug: string; nombre: string};

  // Zona 2 - apertura de estadía (theme del balneario)
  S05ElegirUbicacion: undefined;
  S06PendienteValidacion: {estadiaPublicId: string};
  S07EstadiaActiva: {estadiaPublicId: string};
  S08SolicitudRechazada: {estadiaPublicId: string; motivo: string | null};

  // Zona 3 - tabs
  MainTabs: undefined;
  S12DetalleProductoSimple: {productoId: number};
  S13DetalleConVariantes: {productoId: number};
  S14ProductoAgotado: {productoId: number};
  S15Carrito: undefined;
  S16HojaMedioPago: undefined;
  S17ProcesandoPago: {pedidoPublicId: string};
  S18PagoAprobado: {pedidoPublicId: string};
  S19PagoRechazado: {pedidoPublicId: string; motivoDetalle: string};
  S20DetallePedido: {pedidoPublicId: string};
  S22PedidoCancelado: {pedidoPublicId: string};
  /** S23 "hoja de servicios" y S24 "servicio en curso" son la MISMA pantalla (etapa 07: "no hay navegación nueva, se reemplaza el contenido"). */
  S23ServiciosHoja: undefined;
  S26SeccionPromociones: undefined;
  S27DetalleCombo: {promocionId: number};

  // Zona 4 - cierre y no felices
  S29ResumenConsumo: undefined;
  S30ConfirmarCierre: undefined;
  S31EstadiaCerrada: {resumen: {diasDeEstadia: number; cantidadPedidos: number; montoTotal: string}};
  S32CierreBloqueado: undefined;
  S33SinConexion: {alReintentar?: () => void};
  S34FueraDeTemporada: {nombreBalneario: string};
  S36CerradaPorBalneario: {estadiaPublicId: string};
};

export type MainTabsParamList = {
  S09Home: undefined;
  S11Menu: undefined;
  S21Pedidos: undefined;
  S28Estadia: undefined;
};

export type RootScreenProps<T extends keyof RootStackParamList> = NativeStackScreenProps<RootStackParamList, T>;

export type TabScreenProps<T extends keyof MainTabsParamList> = CompositeScreenProps<
  BottomTabScreenProps<MainTabsParamList, T>,
  NativeStackScreenProps<RootStackParamList>
>;
