// DTOs del backend relevantes al panel operativo + admin de balneario (etapa 17).
// No incluye lo que solo usa el Super Admin (etapa 18) ni lo que solo usa mobile (etapa 16).

export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
};

export type TokenResponse = {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  tipo: 'STAFF' | 'CLIENTE' | 'SUPER_ADMIN';
  rol: string;
  balnearioId: number | null;
  debeCambiarPassword: boolean;
};

export type BalnearioResponse = {
  id: number;
  slug: string;
  nombre: string;
  emailContacto: string;
  telefono: string;
  estado: 'ACTIVO' | 'SUSPENDIDO';
  operativo: boolean;
};

export type MiembroResponse = {
  usuarioPublicId: string;
  usuarioNombre: string;
  usuarioEmail: string;
  rol: 'CARPERO' | 'OPERADOR' | 'ADMIN_BALNEARIO';
  balnearioId: number;
};

export type InvitarStaffResponse = {
  usuarioPublicId: string;
  email: string;
  nombre: string;
  rol: string;
  passwordTemporal: string;
};

// ---------------------------------------------------------------- catálogo

export type TipoUbicacion = 'CARPA' | 'SOMBRILLA' | 'MESA' | 'SECTOR';
export type EstadoUbicacion = 'ACTIVA' | 'INACTIVA';

export type UbicacionResponse = {
  id: number;
  tipo: TipoUbicacion;
  identificador: string;
  estado: EstadoUbicacion;
};

export type CategoriaMenuResponse = {
  id: number;
  nombre: string;
  orden: number;
  activa: boolean;
};

export type ProductoResponse = {
  id: number;
  categoriaId: number;
  nombre: string;
  descripcion: string | null;
  precioBase: string;
  fotoUrl: string | null;
  disponible: boolean;
  orden: number;
};

export type ProductoVarianteResponse = {
  id: number;
  productoId: number;
  nombre: string;
  precio: string;
  disponible: boolean;
  orden: number;
};

export type TipoServicioResponse = {
  id: number;
  nombre: string;
  activo: boolean;
  orden: number;
};

// ---------------------------------------------------------------- estadía

export type EstadoEstadia = 'PENDIENTE_VALIDACION' | 'ACTIVA' | 'RECHAZADA' | 'CERRADA' | 'CERRADA_POR_SISTEMA';

export type EstadiaPendienteResponse = {
  publicId: string;
  balnearioId: number;
  ubicacionId: number;
  ubicacionIdentificador: string;
  clienteNombre: string | null;
  estado: EstadoEstadia;
  fechaSolicitud: string;
};

export type EstadiaResponse = {
  publicId: string;
  balnearioId: number;
  balnearioNombre: string;
  ubicacionId: number;
  ubicacionIdentificador: string;
  estado: EstadoEstadia;
  permitePedidos: boolean;
  fechaSolicitud: string;
  fechaValidacion: string | null;
  fechaCierre: string | null;
  motivoRechazo: string | null;
};

// ---------------------------------------------------------------- pedidos

export type EstadoPedido = 'CREADO' | 'PAGO_PENDIENTE' | 'PAGO_RECHAZADO' | 'CONFIRMADO' | 'EN_PREPARACION' | 'EN_CAMINO' | 'ENTREGADO' | 'CANCELADO';

export type PedidoItemResponse = {
  productoNombre: string;
  varianteNombre: string | null;
  cantidad: number;
  precioUnitario: string;
};

export type PedidoResponse = {
  publicId: string;
  balnearioId: number;
  ubicacionIdentificador: string;
  estado: EstadoPedido;
  subtotal: string;
  descuentoTotal: string;
  total: string;
  items: PedidoItemResponse[];
  promociones: string[];
  motivoCancelacion: string | null;
  createdAt: string;
};

export type PedidoEventoResponse = {
  estado: string;
  actor: string | null;
  motivo: string | null;
  createdAt: string;
};

// ---------------------------------------------------------------- servicios

export type EstadoSolicitudServicio = 'PENDIENTE' | 'EN_CURSO' | 'RESUELTA' | 'CANCELADA';

export type SolicitudServicioResponse = {
  publicId: string;
  tipoServicioNombre: string;
  ubicacionIdentificador: string;
  nota: string | null;
  estado: EstadoSolicitudServicio;
  createdAt: string;
};

// ---------------------------------------------------------------- promociones

export type TipoPromocion = 'DESCUENTO_PORCENTUAL' | 'COMBO' | 'HAPPY_HOUR';
export type EstadoPromocion = 'ACTIVA' | 'INACTIVA';
export type TipoAlcance = 'PRODUCTO' | 'CATEGORIA';

export type AlcancePromocion = { tipoAlcance: TipoAlcance; referenciaId: number };
export type ComboItem = { productoId: number; cantidad: number };

export type PromocionResponse = {
  id: number;
  nombre: string;
  tipo: TipoPromocion;
  estado: EstadoPromocion;
  valor: string;
  vigenciaDesde: string | null;
  vigenciaHasta: string | null;
  franjaHoraDesde: string | null;
  franjaHoraHasta: string | null;
  /** Códigos separados por coma ("LUN,MAR,..."), solo relevante para HAPPY_HOUR. */
  diasSemana: string | null;
  alcances: AlcancePromocion[] | null;
  comboItems: ComboItem[] | null;
};

export type PromocionInput = {
  nombre: string;
  tipo: TipoPromocion;
  activa: boolean;
  valor: string;
  vigenciaDesde: string | null;
  vigenciaHasta: string | null;
  franjaHoraDesde: string | null;
  franjaHoraHasta: string | null;
  diasSemana: string | null;
  alcances: AlcancePromocion[] | null;
  comboItems: ComboItem[] | null;
};

// ---------------------------------------------------------------- branding

export type BrandingTokens = Record<string, unknown>;

export type BrandingUpdateResult = {
  aplicado: boolean;
  tokens: BrandingTokens | null;
  ajustesPropuestos: Record<string, string>;
};

// ---------------------------------------------------------------- mercado pago

export type EstadoVinculacionMp = 'VINCULADA' | 'DESVINCULADA' | 'EXPIRADA';

export type EstadoVinculacionResponse = {
  vinculado: boolean;
  mpUserId: string | null;
  estado: EstadoVinculacionMp;
};

export type IniciarVinculacionResponse = {
  url: string;
};

// ---------------------------------------------------------------- reportes

export type VentasPorDia = {
  dia: string;
  cantidadPedidos: number;
  facturacion: string;
};

export type VentasReporteResponse = {
  facturacionTotal: string;
  cantidadPedidos: number;
  ticketPromedio: string;
  porDia: VentasPorDia[];
};

export type ProductoVendidoResponse = {
  productoId: number;
  nombreProducto: string;
  unidadesVendidas: number;
  facturacion: string;
};

export type PromocionRendimientoResponse = {
  promocionId: number;
  nombrePromocion: string;
  usos: number;
  montoDescontado: string;
};

export type AperturasPorDia = { dia: string; cantidad: number };

export type EstadiasReporteResponse = {
  aperturasPorDia: AperturasPorDia[];
  duracionPromedioHoras: number | null;
  consumoPromedioPorEstadia: string;
};

export type SolicitudesPorTipo = { tipoServicio: string; cantidad: number };

export type ServiciosReporteResponse = {
  porTipo: SolicitudesPorTipo[];
  tiempoResolucionPromedioMinutos: number | null;
};

export type DashboardResumenResponse = {
  facturacionHoy: string;
  pedidosHoy: number;
  ticketPromedioHoy: string;
  pedidosEnCurso: number;
};
