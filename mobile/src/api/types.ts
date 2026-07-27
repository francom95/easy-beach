/**
 * DTOs del backend (etapas 09-15), tal como los devuelve la API real -
 * ver docs/api/openapi.yaml. Los montos SIEMPRE viajan como string decimal
 * (etapa 04 §3): nunca number, para no perder precisión ni dar pie a que el
 * cliente "calcule" un total.
 */

export type ProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  errors?: {field: string; message: string}[];
};

export type TokenResponse = {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  tipo: 'CLIENTE' | 'STAFF' | 'SUPER_ADMIN';
  rol: string | null;
  balnearioId: number | null;
  /** Etapa 05 §1.1: staff con password temporal debe cambiarla en el primer login (no aplica a CLIENTE). */
  debeCambiarPassword: boolean;
};

// ---------------------------------------------------------------- catálogo

export type MenuVarianteResponse = {
  id: number;
  nombre: string;
  precio: string;
};

export type PromocionResumen = {
  id: number;
  nombre: string;
  tipo: 'DESCUENTO_PORCENTUAL' | 'COMBO' | 'HAPPY_HOUR';
  valor: string;
};

export type MenuProductoResponse = {
  id: number;
  nombre: string;
  descripcion: string | null;
  precioBase: string;
  fotoUrl: string | null;
  variantes: MenuVarianteResponse[];
  promociones: PromocionResumen[];
};

export type MenuCategoriaResponse = {
  id: number;
  nombre: string;
  productos: MenuProductoResponse[];
};

export type TipoServicioResponse = {
  id: number;
  nombre: string;
  activo: boolean;
  orden: number;
};

// ------------------------------------------------------------------ stay

export type EstadoEstadia =
  | 'PENDIENTE_VALIDACION'
  | 'ACTIVA'
  | 'RECHAZADA'
  | 'CERRADA'
  | 'CERRADA_POR_SISTEMA'
  | 'EXPIRADA';

export type EstadiaResponse = {
  publicId: string;
  balnearioId: number;
  balnearioNombre: string | null;
  ubicacionId: number;
  ubicacionIdentificador: string | null;
  estado: EstadoEstadia;
  permitePedidos: boolean;
  fechaSolicitud: string;
  fechaValidacion: string | null;
  fechaCierre: string | null;
  motivoRechazo: string | null;
};

export type ResumenCierreResponse = {
  publicId: string;
  balnearioNombre: string | null;
  fechaSolicitud: string;
  fechaCierre: string;
  diasDeEstadia: number;
  cantidadPedidos: number;
  montoTotal: string;
};

export enum TipoUbicacion {
  CARPA = 'CARPA',
  SOMBRILLA = 'SOMBRILLA',
  MESA = 'MESA',
  SECTOR = 'SECTOR',
}

export type UbicacionResponse = {
  id: number;
  tipo: TipoUbicacion;
  identificador: string;
  estado: 'ACTIVA' | 'INACTIVA';
};

// --------------------------------------------------------------- ordering

export type EstadoPedido =
  | 'CREADO'
  | 'PAGO_PENDIENTE'
  | 'PAGO_RECHAZADO'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'EN_CAMINO'
  | 'ENTREGADO'
  | 'CANCELADO';

export type PedidoItemResponse = {
  nombreProducto: string;
  nombreVariante: string | null;
  precioUnitario: string;
  cantidad: number;
  subtotalLinea: string;
};

export type PromocionAplicadaResponse = {
  nombre: string;
  montoDescuento: string;
};

export type PedidoResponse = {
  publicId: string;
  balnearioId: number;
  ubicacionIdentificador: string | null;
  estado: EstadoPedido;
  subtotal: string;
  descuentoTotal: string;
  total: string;
  items: PedidoItemResponse[];
  promociones: PromocionAplicadaResponse[];
  motivoCancelacion: string | null;
  createdAt: string;
};

export type PedidoEventoResponse = {
  estadoAnterior: EstadoPedido | null;
  estadoNuevo: EstadoPedido;
  actorTipo: string;
  motivo: string | null;
  momento: string;
};

// ------------------------------------------------------------- concierge

export type EstadoSolicitudServicio = 'PENDIENTE' | 'EN_CURSO' | 'RESUELTA' | 'CANCELADA';

export type SolicitudServicioResponse = {
  publicId: string;
  tipoServicioNombre: string | null;
  ubicacionIdentificador: string | null;
  nota: string | null;
  estado: EstadoSolicitudServicio;
  createdAt: string;
};

// ------------------------------------------------------------- branding

/**
 * Contrato de tokens de theming v1.0 (etapa 06, claves exactas de
 * ThemeTokenKeys.java). `on-*` siempre derivados del lado del servidor - la
 * app nunca los calcula. `theme.contract` versiona el shape: tokens
 * desconocidos se ignoran, faltantes caen al default.
 *
 * `GET /balnearios/{slug}/branding` devuelve este mapa PLANO directo (no
 * envuelto en `{balnearioId, tokens}`) - así lo implementó
 * ConfiguracionVisualService.obtenerPublico(). Tampoco trae ETag/Cache-Control
 * (a diferencia del menú de la etapa 11): el cache en la app es
 * fetch-y-guardar, no revalidación condicional - ver ThemeProvider.
 */
export type BrandingTokens = {
  'theme.contract'?: string;
  'theme.name'?: string;
  'color.primary'?: string;
  'color.on-primary'?: string;
  'color.secondary'?: string;
  'color.on-secondary'?: string;
  'color.background'?: string;
  'color.on-background'?: string;
  'color.surface'?: string;
  'color.on-surface'?: string;
  'color.on-surface-muted'?: string;
  'color.border'?: string;
  'color.success'?: string;
  'color.on-success'?: string;
  'color.warning'?: string;
  'color.on-warning'?: string;
  'color.error'?: string;
  'color.on-error'?: string;
  'color.info'?: string;
  'color.on-info'?: string;
  'typography.family'?: 'clara' | 'amigable' | 'elegante' | 'energica';
  'asset.logo'?: string;
  'asset.logo-compact'?: string;
  'asset.cover'?: string;
  'asset.splash'?: string;
  'asset.product-placeholder'?: string;
};

export type EstadoBalneario = 'ACTIVO' | 'SUSPENDIDO';

/** `BalnearioResponse.java` — nunca incluye balnearios no operativos en el listado público. */
export type BalnearioPublicoResponse = {
  id: number;
  slug: string;
  nombre: string;
  emailContacto: string;
  telefono: string | null;
  estado: EstadoBalneario;
  operativo: boolean;
};
