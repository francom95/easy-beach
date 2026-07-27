package com.easybeach;

import com.easybeach.branding.domain.ConfiguracionVisual;
import com.easybeach.branding.repository.ConfiguracionVisualRepository;
import com.easybeach.branding.theming.ThemeTokenAssembler;
import com.easybeach.branding.theming.TypographyFamily;
import com.easybeach.branding.web.dto.BrandingUpdateRequest;
import com.easybeach.catalog.domain.CategoriaMenu;
import com.easybeach.catalog.domain.Producto;
import com.easybeach.catalog.domain.ProductoVariante;
import com.easybeach.catalog.repository.CategoriaMenuRepository;
import com.easybeach.catalog.repository.ProductoRepository;
import com.easybeach.catalog.repository.ProductoVarianteRepository;
import com.easybeach.concierge.domain.EstadoSolicitudServicio;
import com.easybeach.concierge.domain.SolicitudServicio;
import com.easybeach.concierge.domain.TipoServicio;
import com.easybeach.concierge.repository.SolicitudServicioRepository;
import com.easybeach.concierge.repository.TipoServicioRepository;
import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.domain.Pedido;
import com.easybeach.ordering.domain.PedidoEvento;
import com.easybeach.ordering.domain.PedidoItem;
import com.easybeach.ordering.domain.PedidoPromocion;
import com.easybeach.ordering.repository.PedidoEventoRepository;
import com.easybeach.ordering.repository.PedidoRepository;
import com.easybeach.payments.TokenEncryptionService;
import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import com.easybeach.payments.repository.BalnearioMpCredencialRepository;
import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.domain.EstadoSuscripcion;
import com.easybeach.platform.domain.EstadoTemporada;
import com.easybeach.platform.domain.Plan;
import com.easybeach.platform.domain.SuscripcionTemporada;
import com.easybeach.platform.domain.Temporada;
import com.easybeach.platform.event.BalnearioCreado;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.platform.repository.PlanRepository;
import com.easybeach.platform.repository.SuscripcionTemporadaRepository;
import com.easybeach.platform.repository.TemporadaRepository;
import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import com.easybeach.promotions.domain.PromocionAlcance;
import com.easybeach.promotions.domain.PromocionComboItem;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.repository.PromocionAlcanceRepository;
import com.easybeach.promotions.repository.PromocionComboItemRepository;
import com.easybeach.promotions.repository.PromocionRepository;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import com.easybeach.stay.domain.EstadoEstadia;
import com.easybeach.stay.domain.EstadoUbicacion;
import com.easybeach.stay.domain.Estadia;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.domain.Ubicacion;
import com.easybeach.stay.repository.EstadiaRepository;
import com.easybeach.stay.repository.UbicacionRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Etapa 19 §4: "script reproducible que crea 2-3 balnearios ficticios... un
 * comando lo regenera de cero". Se activa con el profile {@code demo}
 * (nunca corre en {@code default}/{@code local}/{@code test}):
 *
 * <pre>mvn spring-boot:run -Dspring-boot.run.profiles=local,demo</pre>
 *
 * Al arrancar, borra TODO el contenido de negocio (conserva {@code rol} y
 * {@code flyway_schema_history}) y vuelve a sembrar dos balnearios completos
 * -para que "un comando lo regenera de cero" sea literal, no solo la primera
 * vez. Construye entidades directo por repositorio (no vía HTTP): más rápido
 * y evita la dependencia de tener el propio servidor ya arriba para
 * pegarle a su propia API.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String PASSWORD_DEMO = "Demo1234!";
    private static final int DIAS_DE_HISTORIA = 10;

    private final JdbcTemplate jdbc;
    private final BalnearioRepository balnearioRepository;
    private final PlanRepository planRepository;
    private final TemporadaRepository temporadaRepository;
    private final SuscripcionTemporadaRepository suscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    private final RolRepository rolRepository;
    private final BalnearioMpCredencialRepository credencialRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final ConfiguracionVisualRepository configuracionVisualRepository;
    private final ThemeTokenAssembler themeTokenAssembler;
    private final UbicacionRepository ubicacionRepository;
    private final CategoriaMenuRepository categoriaMenuRepository;
    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository productoVarianteRepository;
    private final com.easybeach.shared.storage.AssetStorageService assetStorageService;
    private final TipoServicioRepository tipoServicioRepository;
    private final SolicitudServicioRepository solicitudServicioRepository;
    private final PromocionRepository promocionRepository;
    private final PromocionAlcanceRepository promocionAlcanceRepository;
    private final PromocionComboItemRepository promocionComboItemRepository;
    private final EstadiaRepository estadiaRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoEventoRepository pedidoEventoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random(42); // semilla fija: mismo dataset en cada corrida

    @Autowired
    public DemoDataSeeder(JdbcTemplate jdbc, BalnearioRepository balnearioRepository, PlanRepository planRepository,
                           TemporadaRepository temporadaRepository, SuscripcionTemporadaRepository suscripcionRepository,
                           UsuarioRepository usuarioRepository, UsuarioBalnearioRolRepository usuarioBalnearioRolRepository,
                           RolRepository rolRepository, BalnearioMpCredencialRepository credencialRepository,
                           TokenEncryptionService tokenEncryptionService,
                           ConfiguracionVisualRepository configuracionVisualRepository, ThemeTokenAssembler themeTokenAssembler,
                           UbicacionRepository ubicacionRepository, CategoriaMenuRepository categoriaMenuRepository,
                           ProductoRepository productoRepository, ProductoVarianteRepository productoVarianteRepository,
                           com.easybeach.shared.storage.AssetStorageService assetStorageService,
                           TipoServicioRepository tipoServicioRepository, SolicitudServicioRepository solicitudServicioRepository,
                           PromocionRepository promocionRepository, PromocionAlcanceRepository promocionAlcanceRepository,
                           PromocionComboItemRepository promocionComboItemRepository, EstadiaRepository estadiaRepository,
                           PedidoRepository pedidoRepository, PedidoEventoRepository pedidoEventoRepository,
                           PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher) {
        this.jdbc = jdbc;
        this.balnearioRepository = balnearioRepository;
        this.planRepository = planRepository;
        this.temporadaRepository = temporadaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioBalnearioRolRepository = usuarioBalnearioRolRepository;
        this.rolRepository = rolRepository;
        this.credencialRepository = credencialRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.configuracionVisualRepository = configuracionVisualRepository;
        this.themeTokenAssembler = themeTokenAssembler;
        this.ubicacionRepository = ubicacionRepository;
        this.categoriaMenuRepository = categoriaMenuRepository;
        this.productoRepository = productoRepository;
        this.productoVarianteRepository = productoVarianteRepository;
        this.assetStorageService = assetStorageService;
        this.tipoServicioRepository = tipoServicioRepository;
        this.solicitudServicioRepository = solicitudServicioRepository;
        this.promocionRepository = promocionRepository;
        this.promocionAlcanceRepository = promocionAlcanceRepository;
        this.promocionComboItemRepository = promocionComboItemRepository;
        this.estadiaRepository = estadiaRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoEventoRepository = pedidoEventoRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== EasyBeach demo seed: empezando (borra todo lo anterior) ===");
        borrarTodo();

        String superAdmin = sembrarSuperAdmin();
        List<String> resumen = new ArrayList<>();
        resumen.add(sembrarBalneario("sol-y-mar-demo", "Sol y Mar", "#C95100", "#17437B", "contacto@solymar.demo"));
        resumen.add(sembrarBalneario("costa-azul-demo", "Costa Azul", "#1D62B4", "#0F3D2E", "contacto@costaazul.demo"));
        List<String> clientes = sembrarClientesDemo();

        log.info("=== EasyBeach demo seed: listo ===");
        log.info("Super Admin: {} (contraseña {})", superAdmin, PASSWORD_DEMO);
        resumen.forEach(log::info);
        log.info("Clientes demo (contraseña {} para todos): {}", PASSWORD_DEMO, String.join(", ", clientes));
    }

    // ---------------------------------------------------------------- borrado

    private void borrarTodo() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        List<String> tablas = List.of(
                "pedido_promocion", "pedido_evento", "pedido_item", "pedido_pago", "pedido",
                "solicitud_servicio", "tipo_servicio", "estadia_ubicacion_historial", "estadia",
                "promocion_combo_item", "promocion_alcance", "promocion",
                "producto_variante", "producto", "categoria_menu",
                "ubicacion", "configuracion_visual", "balneario_mp_credencial",
                "mp_oauth_solicitud", "mp_webhook_notificacion",
                "auditoria_plataforma", "sesion_refresh",
                "usuario_balneario_rol", "usuario",
                "suscripcion_temporada", "temporada", "plan", "balneario");
        for (String tabla : tablas) {
            jdbc.execute("TRUNCATE TABLE " + tabla);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    // ---------------------------------------------------------------- balneario completo

    private String sembrarBalneario(String slug, String nombre, String colorPrimary, String colorSecondary, String email) {
        Balneario balneario = new Balneario();
        balneario.setSlug(slug);
        balneario.setNombre("Balneario " + nombre);
        balneario.setEmailContacto(email);
        balneario.setTelefono("+549110000" + Math.abs(slug.hashCode() % 1000));
        balneario = balnearioRepository.save(balneario);
        Long balnearioId = balneario.getId();

        // Mismo evento que BalnearioService.crear() en el flujo real de Super
        // Admin (etapa 10): siembra el theme default; lo pisamos abajo con
        // colores propios del balneario demo.
        eventPublisher.publishEvent(new BalnearioCreado(balnearioId));
        personalizarBranding(balnearioId, nombre, colorPrimary, colorSecondary);

        darDeAltaSuscripcionVigente(balnearioId);
        vincularMercadoPago(balnearioId);

        Usuario admin = crearStaff(balnearioId, RolCodigo.ADMIN_BALNEARIO, "admin." + slug);
        Usuario carpero = crearStaff(balnearioId, RolCodigo.CARPERO, "carpero." + slug);
        Usuario operador = crearStaff(balnearioId, RolCodigo.OPERADOR, "operador." + slug);

        List<Ubicacion> ubicaciones = sembrarUbicaciones(balnearioId);
        List<Producto> productos = sembrarMenu(balnearioId);
        List<TipoServicio> tiposServicio = sembrarTiposServicio(balnearioId);
        Promocion descuentoBebidas = sembrarPromociones(balnearioId, productos);

        sembrarHistoria(balnearioId, ubicaciones, productos, tiposServicio, admin, carpero, operador, descuentoBebidas);

        return "%s (%s) -> admin: %s / carpero: %s / operador: %s (contraseña %s para los 3)".formatted(
                balneario.getNombre(), slug, admin.getEmail(), carpero.getEmail(), operador.getEmail(), PASSWORD_DEMO);
    }

    private void personalizarBranding(Long balnearioId, String nombre, String colorPrimary, String colorSecondary) {
        BrandingUpdateRequest request = new BrandingUpdateRequest("Theme " + nombre, colorPrimary, colorSecondary,
                "#F5EFE2", "#FFFFFF", "#1E7D3C", "#B25E00", "#C22F2F", "#1D62B4", TypographyFamily.CLARA, true);
        ThemeTokenAssembler.Resultado resultado = themeTokenAssembler.assemble(request,
                new ThemeTokenAssembler.AssetUrls("/assets/easybeach/logo.svg", "/assets/easybeach/logo-compact.svg",
                        "/assets/easybeach/cover.jpg", "/assets/easybeach/splash.png",
                        "/assets/easybeach/product-placeholder.png"));
        ConfiguracionVisual entidad = configuracionVisualRepository.findByBalnearioId(balnearioId).orElseThrow();
        entidad.setTokens(toJson(resultado.tokens()));
        configuracionVisualRepository.save(entidad);
    }

    private String toJson(java.util.Map<String, Object> tokens) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(tokens);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void darDeAltaSuscripcionVigente(Long balnearioId) {
        Plan plan = new Plan();
        plan.setNombre("Plan Full");
        plan.setDescripcion("Plan demo con todas las funcionalidades habilitadas");
        plan.setPrecio(new BigDecimal("25000.00"));
        plan.setActivo(true);
        plan = planRepository.save(plan);

        Temporada temporada = temporadaRepository.findByEstado(EstadoTemporada.EN_CURSO).stream().findFirst()
                .orElseGet(() -> {
                    Temporada nueva = new Temporada();
                    nueva.setNombre("Verano " + LocalDate.now().getYear());
                    nueva.setFechaInicio(LocalDate.now().minusMonths(1));
                    nueva.setFechaFin(LocalDate.now().plusMonths(3));
                    nueva.setEstado(EstadoTemporada.EN_CURSO);
                    return temporadaRepository.save(nueva);
                });

        SuscripcionTemporada suscripcion = new SuscripcionTemporada();
        suscripcion.setBalnearioId(balnearioId);
        suscripcion.setPlan(plan);
        suscripcion.setTemporada(temporada);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcionRepository.save(suscripcion);
    }

    private void vincularMercadoPago(Long balnearioId) {
        BalnearioMpCredencial credencial = new BalnearioMpCredencial();
        credencial.setBalnearioId(balnearioId);
        credencial.setMpUserId("demo-mp-user-" + balnearioId);
        credencial.setAccessTokenCifrado(tokenEncryptionService.encrypt("demo-access-token"));
        credencial.setRefreshTokenCifrado(tokenEncryptionService.encrypt("demo-refresh-token"));
        credencial.setTokenExpiraAt(Instant.now().plus(Duration.ofDays(180)));
        credencial.setEstado(EstadoCredencialMp.VINCULADA);
        credencialRepository.save(credencial);
    }

    private Usuario crearStaff(Long balnearioId, RolCodigo rol, String emailPrefix) {
        Usuario usuario = new Usuario();
        usuario.setEmail(emailPrefix + "@easybeach.dev");
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
        usuario.setNombre(nombrePorRol(rol));
        usuario.setTipo(TipoUsuario.STAFF);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(usuario);
        vinculo.setBalnearioId(balnearioId);
        vinculo.setRol(rolRepository.findByCodigo(rol).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);
        return usuario;
    }

    private String nombrePorRol(RolCodigo rol) {
        return switch (rol) {
            case ADMIN_BALNEARIO -> "Admin Demo";
            case CARPERO -> "Carpero Demo";
            case OPERADOR -> "Operador Demo";
            default -> "Staff Demo";
        };
    }

    // ---------------------------------------------------------------- catálogo y ubicaciones

    private List<Ubicacion> sembrarUbicaciones(Long balnearioId) {
        List<Ubicacion> resultado = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Ubicacion ubicacion = new Ubicacion();
            ubicacion.setBalnearioId(balnearioId);
            ubicacion.setTipo(i <= 4 ? TipoUbicacion.CARPA : TipoUbicacion.SOMBRILLA);
            ubicacion.setIdentificador((i <= 4 ? "Carpa " : "Sombrilla ") + i);
            ubicacion.setEstado(EstadoUbicacion.ACTIVA);
            resultado.add(ubicacionRepository.save(ubicacion));
        }
        return resultado;
    }

    private List<Producto> sembrarMenu(Long balnearioId) {
        byte[] foto = generarFotoPlaceholder();
        List<Producto> productos = new ArrayList<>();

        CategoriaMenu bebidas = crearCategoria(balnearioId, "Bebidas", 1);
        productos.add(crearProducto(balnearioId, bebidas, "Cerveza", new BigDecimal("2500.00"), foto, 1));
        productos.add(crearProducto(balnearioId, bebidas, "Gaseosa", new BigDecimal("1800.00"), foto, 2));
        Producto agua = crearProducto(balnearioId, bebidas, "Agua mineral", new BigDecimal("1200.00"), foto, 3);
        productos.add(agua);
        crearVariante(balnearioId, agua, "500ml", new BigDecimal("1200.00"), 1);
        crearVariante(balnearioId, agua, "1.5L", new BigDecimal("2000.00"), 2);

        CategoriaMenu comidas = crearCategoria(balnearioId, "Comidas", 2);
        productos.add(crearProducto(balnearioId, comidas, "Papas fritas", new BigDecimal("3500.00"), foto, 1));
        productos.add(crearProducto(balnearioId, comidas, "Sandwich de milanesa", new BigDecimal("5200.00"), foto, 2));
        productos.add(crearProducto(balnearioId, comidas, "Ensalada de mariscos", new BigDecimal("6800.00"), foto, 3));

        CategoriaMenu postres = crearCategoria(balnearioId, "Postres y helados", 3);
        productos.add(crearProducto(balnearioId, postres, "Helado 2 bochas", new BigDecimal("2800.00"), foto, 1));
        productos.add(crearProducto(balnearioId, postres, "Flan casero", new BigDecimal("2400.00"), foto, 2));

        return productos;
    }

    private CategoriaMenu crearCategoria(Long balnearioId, String nombre, int orden) {
        CategoriaMenu categoria = new CategoriaMenu();
        categoria.setBalnearioId(balnearioId);
        categoria.setNombre(nombre);
        categoria.setOrden(orden);
        categoria.setActiva(true);
        return categoriaMenuRepository.save(categoria);
    }

    private Producto crearProducto(Long balnearioId, CategoriaMenu categoria, String nombre, BigDecimal precio,
                                    byte[] foto, int orden) {
        Producto producto = new Producto();
        producto.setBalnearioId(balnearioId);
        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setPrecioBase(precio);
        producto.setDisponible(true);
        producto.setOrden(orden);
        producto = productoRepository.save(producto);
        var stored = assetStorageService.storeProductoFoto(balnearioId, new BytesMultipartFile(foto, "foto.png", "image/png"));
        producto.setFotoUrl(stored.publicUrl());
        return productoRepository.save(producto);
    }

    private ProductoVariante crearVariante(Long balnearioId, Producto producto, String nombre, BigDecimal precio, int orden) {
        ProductoVariante variante = new ProductoVariante();
        variante.setBalnearioId(balnearioId);
        variante.setProducto(producto);
        variante.setNombre(nombre);
        variante.setPrecio(precio);
        variante.setDisponible(true);
        variante.setOrden(orden);
        return productoVarianteRepository.save(variante);
    }

    /** PNG de 1 color por categoría (sin activos externos): suficiente para que la foto exista y se sirva de verdad. */
    private byte[] generarFotoPlaceholder() {
        BufferedImage imagen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(new Color(0xC9, 0x51, 0x00));
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<TipoServicio> sembrarTiposServicio(Long balnearioId) {
        List<TipoServicio> resultado = new ArrayList<>();
        for (String nombre : List.of("Hielo", "Toallas", "Cambio de sombrilla")) {
            TipoServicio tipo = new TipoServicio();
            tipo.setBalnearioId(balnearioId);
            tipo.setNombre(nombre);
            tipo.setActivo(true);
            resultado.add(tipoServicioRepository.save(tipo));
        }
        return resultado;
    }

    private Promocion sembrarPromociones(Long balnearioId, List<Producto> productos) {
        Promocion descuento = new Promocion();
        descuento.setBalnearioId(balnearioId);
        descuento.setNombre("10% en bebidas");
        descuento.setTipo(TipoPromocion.DESCUENTO_PORCENTUAL);
        descuento.setEstado(EstadoPromocion.ACTIVA);
        descuento.setValor(new BigDecimal("10"));
        descuento = promocionRepository.save(descuento);
        PromocionAlcance alcance = new PromocionAlcance();
        alcance.setPromocionId(descuento.getId());
        alcance.setBalnearioId(balnearioId);
        alcance.setTipoAlcance(TipoAlcance.CATEGORIA);
        alcance.setReferenciaId(productos.get(0).getCategoria().getId());
        promocionAlcanceRepository.save(alcance);

        Promocion happyHour = new Promocion();
        happyHour.setBalnearioId(balnearioId);
        happyHour.setNombre("Happy hour 18-20hs");
        happyHour.setTipo(TipoPromocion.HAPPY_HOUR);
        happyHour.setEstado(EstadoPromocion.ACTIVA);
        happyHour.setValor(new BigDecimal("15"));
        happyHour.setFranjaHoraDesde(java.time.LocalTime.of(18, 0));
        happyHour.setFranjaHoraHasta(java.time.LocalTime.of(20, 0));
        happyHour.setDiasSemana("VIE,SAB,DOM");
        happyHour = promocionRepository.save(happyHour);
        PromocionAlcance alcanceHh = new PromocionAlcance();
        alcanceHh.setPromocionId(happyHour.getId());
        alcanceHh.setBalnearioId(balnearioId);
        alcanceHh.setTipoAlcance(TipoAlcance.PRODUCTO);
        alcanceHh.setReferenciaId(productos.get(0).getId());
        promocionAlcanceRepository.save(alcanceHh);

        Producto papas = productos.stream().filter(p -> p.getNombre().equals("Papas fritas")).findFirst().orElseThrow();
        Producto cerveza = productos.get(0);
        Promocion combo = new Promocion();
        combo.setBalnearioId(balnearioId);
        combo.setNombre("Combo cerveza + papas");
        combo.setTipo(TipoPromocion.COMBO);
        combo.setEstado(EstadoPromocion.ACTIVA);
        combo.setValor(new BigDecimal("5000.00"));
        combo = promocionRepository.save(combo);
        PromocionComboItem itemCerveza = new PromocionComboItem();
        itemCerveza.setPromocionId(combo.getId());
        itemCerveza.setBalnearioId(balnearioId);
        itemCerveza.setProductoId(cerveza.getId());
        itemCerveza.setCantidad(1);
        promocionComboItemRepository.save(itemCerveza);
        PromocionComboItem itemPapas = new PromocionComboItem();
        itemPapas.setPromocionId(combo.getId());
        itemPapas.setBalnearioId(balnearioId);
        itemPapas.setProductoId(papas.getId());
        itemPapas.setCantidad(1);
        promocionComboItemRepository.save(itemPapas);

        return descuento;
    }

    // ---------------------------------------------------------------- historia (estadías, pedidos, servicios)

    @Transactional
    void sembrarHistoria(Long balnearioId, List<Ubicacion> ubicaciones, List<Producto> productos,
                          List<TipoServicio> tiposServicio, Usuario admin, Usuario carpero, Usuario operador,
                          Promocion descuentoBebidas) {
        List<Usuario> clientes = crearClientesParaHistoria(balnearioId);

        for (int diasAtras = DIAS_DE_HISTORIA; diasAtras >= 1; diasAtras--) {
            Instant momentoBase = Instant.now().minus(Duration.ofDays(diasAtras));
            int estadiasHoy = 2 + random.nextInt(3); // 2 a 4 estadías por día
            for (int i = 0; i < estadiasHoy; i++) {
                Usuario cliente = clientes.get(random.nextInt(clientes.size()));
                Ubicacion ubicacion = ubicaciones.get(random.nextInt(ubicaciones.size()));
                Instant apertura = momentoBase.plus(Duration.ofMinutes(random.nextInt(600)));
                Estadia estadia = crearEstadiaCerrada(balnearioId, cliente, ubicacion, carpero, apertura);

                int pedidosPorEstadia = 1 + random.nextInt(2);
                for (int p = 0; p < pedidosPorEstadia; p++) {
                    boolean cancelado = random.nextInt(10) == 0; // 10% cancelados
                    crearPedidoHistorico(balnearioId, estadia, cliente, productos, operador,
                            apertura.plus(Duration.ofMinutes(10 + p * 20)), cancelado, descuentoBebidas);
                }

                if (random.nextBoolean()) {
                    crearSolicitudServicioResuelta(balnearioId, estadia, cliente, tiposServicio, carpero,
                            apertura.plus(Duration.ofMinutes(15)));
                }
            }
        }

        // Estado operativo "hoy": una en cola de validación y una activa con un pedido en curso,
        // para que el panel operativo no arranque vacío al abrir la demo.
        Usuario clientePendiente = clientes.get(0);
        crearEstadiaPendiente(balnearioId, clientePendiente, ubicaciones.get(0));

        Usuario clienteActivo = clientes.get(1);
        Estadia estadiaActiva = crearEstadiaActivaHoy(balnearioId, clienteActivo, ubicaciones.get(1), carpero);
        crearPedidoEnCurso(balnearioId, estadiaActiva, clienteActivo, productos, operador);
    }

    private List<Usuario> crearClientesParaHistoria(Long balnearioId) {
        List<Usuario> resultado = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Usuario cliente = new Usuario();
            cliente.setEmail("historia" + i + "." + balnearioId + "@easybeach.dev");
            cliente.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
            cliente.setNombre("Cliente Historial " + i);
            cliente.setTipo(TipoUsuario.CLIENTE);
            cliente.setEstado(EstadoUsuario.ACTIVO);
            resultado.add(usuarioRepository.save(cliente));
        }
        return resultado;
    }

    private Estadia crearEstadiaCerrada(Long balnearioId, Usuario cliente, Ubicacion ubicacion, Usuario carpero,
                                         Instant apertura) {
        Estadia estadia = new Estadia();
        estadia.setBalnearioId(balnearioId);
        estadia.setClienteId(cliente.getId());
        estadia.setUbicacionId(ubicacion.getId());
        estadia.setEstado(EstadoEstadia.CERRADA);
        estadia.setFechaSolicitud(apertura);
        estadia.setValidadaPorUsuarioId(carpero.getId());
        estadia.setFechaValidacion(apertura.plus(Duration.ofMinutes(2)));
        estadia.setFechaCierre(apertura.plus(Duration.ofHours(3)));
        estadia = estadiaRepository.save(estadia);
        backdatearCreatedAt("estadia", estadia.getId(), apertura);
        return estadia;
    }

    private void crearEstadiaPendiente(Long balnearioId, Usuario cliente, Ubicacion ubicacion) {
        Estadia estadia = new Estadia();
        estadia.setBalnearioId(balnearioId);
        estadia.setClienteId(cliente.getId());
        estadia.setUbicacionId(ubicacion.getId());
        estadia.setEstado(EstadoEstadia.PENDIENTE_VALIDACION);
        estadia.setFechaSolicitud(Instant.now().minus(Duration.ofMinutes(5)));
        estadiaRepository.save(estadia);
    }

    private Estadia crearEstadiaActivaHoy(Long balnearioId, Usuario cliente, Ubicacion ubicacion, Usuario carpero) {
        Estadia estadia = new Estadia();
        estadia.setBalnearioId(balnearioId);
        estadia.setClienteId(cliente.getId());
        estadia.setUbicacionId(ubicacion.getId());
        estadia.setEstado(EstadoEstadia.ACTIVA);
        estadia.setFechaSolicitud(Instant.now().minus(Duration.ofMinutes(30)));
        estadia.setValidadaPorUsuarioId(carpero.getId());
        estadia.setFechaValidacion(Instant.now().minus(Duration.ofMinutes(28)));
        return estadiaRepository.save(estadia);
    }

    private void crearPedidoHistorico(Long balnearioId, Estadia estadia, Usuario cliente, List<Producto> productos,
                                       Usuario operador, Instant momento, boolean cancelado, Promocion descuentoBebidas) {
        Producto producto = productos.get(random.nextInt(productos.size()));
        int cantidad = 1 + random.nextInt(3);
        BigDecimal precioUnitario = producto.getPrecioBase();
        BigDecimal subtotalLinea = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        boolean esBebida = "Bebidas".equals(producto.getCategoria().getNombre());
        BigDecimal descuento = esBebida
                ? subtotalLinea.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Pedido pedido = new Pedido();
        pedido.setBalnearioId(balnearioId);
        pedido.setEstadiaId(estadia.getId());
        pedido.setClienteId(cliente.getId());
        pedido.setClientePublicId(cliente.getPublicId());
        pedido.setUbicacionId(estadia.getUbicacionId());
        pedido.setIdempotencyKey(UUID.randomUUID().toString());
        pedido.setSubtotal(subtotalLinea);
        pedido.setDescuentoTotal(descuento);
        pedido.setTotal(subtotalLinea.subtract(descuento));
        pedido.setEstado(cancelado ? EstadoPedido.CANCELADO : EstadoPedido.ENTREGADO);
        if (cancelado) {
            pedido.setMotivoCancelacion("Cliente se retiró antes de la entrega (dato demo)");
        }

        PedidoItem item = new PedidoItem();
        item.setBalnearioId(balnearioId);
        item.setProductoId(producto.getId());
        item.setNombreProducto(producto.getNombre());
        item.setPrecioUnitario(precioUnitario);
        item.setCantidad(cantidad);
        item.setSubtotalLinea(subtotalLinea);
        pedido.agregarItem(item);

        if (esBebida) {
            PedidoPromocion promoAplicada = new PedidoPromocion();
            promoAplicada.setBalnearioId(balnearioId);
            promoAplicada.setPromocionId(descuentoBebidas.getId());
            promoAplicada.setNombrePromocion(descuentoBebidas.getNombre());
            promoAplicada.setMontoDescuento(descuento);
            pedido.agregarPromocion(promoAplicada);
        }

        pedido = pedidoRepository.save(pedido);
        backdatearCreatedAt("pedido", pedido.getId(), momento);

        List<EstadoPedido> pasos = cancelado
                ? List.of(EstadoPedido.CREADO, EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO)
                : List.of(EstadoPedido.CREADO, EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION,
                        EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO);
        registrarEventos(pedido, operador, momento, pasos);
    }

    private void crearPedidoEnCurso(Long balnearioId, Estadia estadia, Usuario cliente, List<Producto> productos,
                                     Usuario operador) {
        Producto producto = productos.get(0);
        BigDecimal subtotal = producto.getPrecioBase().multiply(BigDecimal.valueOf(2));

        Pedido pedido = new Pedido();
        pedido.setBalnearioId(balnearioId);
        pedido.setEstadiaId(estadia.getId());
        pedido.setClienteId(cliente.getId());
        pedido.setClientePublicId(cliente.getPublicId());
        pedido.setUbicacionId(estadia.getUbicacionId());
        pedido.setIdempotencyKey(UUID.randomUUID().toString());
        pedido.setSubtotal(subtotal);
        pedido.setDescuentoTotal(BigDecimal.ZERO);
        pedido.setTotal(subtotal);
        pedido.setEstado(EstadoPedido.EN_PREPARACION);

        PedidoItem item = new PedidoItem();
        item.setBalnearioId(balnearioId);
        item.setProductoId(producto.getId());
        item.setNombreProducto(producto.getNombre());
        item.setPrecioUnitario(producto.getPrecioBase());
        item.setCantidad(2);
        item.setSubtotalLinea(subtotal);
        pedido.agregarItem(item);

        pedido = pedidoRepository.save(pedido);
        Instant momento = Instant.now().minus(Duration.ofMinutes(15));
        backdatearCreatedAt("pedido", pedido.getId(), momento);
        registrarEventos(pedido, operador, momento,
                List.of(EstadoPedido.CREADO, EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION));
    }

    private void registrarEventos(Pedido pedido, Usuario actor, Instant desde, List<EstadoPedido> pasos) {
        EstadoPedido anterior = null;
        Instant momento = desde;
        for (EstadoPedido paso : pasos) {
            PedidoEvento evento = new PedidoEvento();
            evento.setPedidoId(pedido.getId());
            evento.setBalnearioId(pedido.getBalnearioId());
            evento.setEstadoAnterior(anterior);
            evento.setEstadoNuevo(paso);
            evento.setActorUsuarioId(paso == EstadoPedido.CREADO ? pedido.getClienteId() : actor.getId());
            evento.setActorTipo(paso == EstadoPedido.CREADO ? "CLIENTE" : "STAFF");
            evento.setCreatedAt(momento);
            pedidoEventoRepository.save(evento);
            anterior = paso;
            momento = momento.plus(Duration.ofMinutes(5));
        }
    }

    private void crearSolicitudServicioResuelta(Long balnearioId, Estadia estadia, Usuario cliente,
                                                 List<TipoServicio> tiposServicio, Usuario carpero, Instant momento) {
        TipoServicio tipo = tiposServicio.get(random.nextInt(tiposServicio.size()));
        SolicitudServicio solicitud = new SolicitudServicio();
        solicitud.setBalnearioId(balnearioId);
        solicitud.setEstadiaId(estadia.getId());
        solicitud.setClientePublicId(cliente.getPublicId());
        solicitud.setUbicacionId(estadia.getUbicacionId());
        solicitud.setTipoServicioId(tipo.getId());
        solicitud.setEstado(EstadoSolicitudServicio.RESUELTA);
        solicitud.setAtendidaPorUsuarioId(carpero.getId());
        solicitud = solicitudServicioRepository.save(solicitud);
        backdatearCreatedAt("solicitud_servicio", solicitud.getId(), momento);
    }

    /** {@code Auditable.createdAt} no tiene setter (lo pisa {@code AuditingEntityListener} en cada save) - UPDATE directo después del insert, mismo patrón que ReportesBalnearioIntegrationTest. */
    private void backdatearCreatedAt(String tabla, Long id, Instant momento) {
        jdbc.update("update " + tabla + " set created_at = ?, updated_at = ? where id = ?",
                java.sql.Timestamp.from(momento), java.sql.Timestamp.from(momento), id);
    }

    // ---------------------------------------------------------------- super admin y clientes demo compartidos

    private String sembrarSuperAdmin() {
        Usuario superAdmin = new Usuario();
        String email = "superadmin@easybeach.dev";
        superAdmin.setEmail(email);
        superAdmin.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
        superAdmin.setNombre("Super Admin Demo");
        superAdmin.setTipo(TipoUsuario.SUPER_ADMIN);
        superAdmin.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(superAdmin);
        return email;
    }

    private List<String> sembrarClientesDemo() {
        List<String> emails = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Usuario cliente = new Usuario();
            String email = "cliente" + i + "@easybeach.dev";
            cliente.setEmail(email);
            cliente.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
            cliente.setNombre("Cliente Demo " + i);
            cliente.setTipo(TipoUsuario.CLIENTE);
            cliente.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(cliente);
            emails.add(email);
        }
        return emails;
    }

    /** {@link MultipartFile} manual: en {@code src/main} no hay `MockMultipartFile` (es solo de test). */
    private static final class BytesMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String filename;
        private final String contentType;

        BytesMultipartFile(byte[] bytes, String filename, String contentType) {
            this.bytes = bytes;
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
