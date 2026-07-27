# Etapa 12 — Backend estadía activa

- **Estado:** ejecutada. Insumo directo de la etapa 13 (pedidos — cuelgan de
  una estadía `ACTIVA`) y 14 (servicios al carpero).
- **Corresponde al plan:** [docs/etapas/12-backend-estadia-activa.md](../etapas/12-backend-estadia-activa.md)
- **Depende de:** [09](09-backend-fundacional.md), [10](10-backend-super-admin.md),
  [11](11-backend-catalogo-ubicaciones.md).
- **Código:** [`backend/`](../../backend/) — módulo `stay` (completo).

## 1. Decisiones de negocio que estaban abiertas (cerradas antes de codificar)

El plan marcaba estas como bloqueantes. Se resolvieron así:

| Decisión | Resolución | Dónde se ve en el código |
|---|---|---|
| **TTL de validación** de una solicitud pendiente | **60 minutos** | `EstadiaService.TTL_VALIDACION` + `EstadiaExpiracionJob` (corre cada 5 min) |
| **Cierre con pedidos en curso** | **Bloquear** el cierre hasta que los pedidos terminen | `EstadiaService.cerrar()` consulta `ConsumoEstadiaProvider.tienePedidosEnCurso` |
| **Suspensión de balneario** con estadías abiertas | **Honrar las abiertas**, bloquear las nuevas | `esOperativo()` (etapa 10) se consulta **solo al solicitar**; las estadías ya vivas no se tocan |

## 2. Qué se construyó

### Máquina de estados como código, no como `if`s dispersos
`EstadoEstadia` (enum) contiene las transiciones válidas
(`puedeTransicionarA`), qué estados ocupan cupo (`ocupaCupo`) y cuál habilita
pedidos (`permitePedidos`). `Estadia.transicionarA()` es la **única** vía de
cambio de estado y mantiene `activaUk` sincronizado — no hay forma de mover
un estado sin que el cupo quede coherente.

### Unicidad garantizada por la base, no por el código
El UK `(balneario_id, activa_uk)` hace la regla "una estadía activa por
cliente **y por balneario**" imposible de violar bajo concurrencia:
`activa_uk = cliente_id` mientras la estadía ocupa cupo, `NULL` cuando entra
en estado terminal (MySQL trata múltiples `NULL` como distintos, así que las
estadías cerradas no colisionan entre sí). El chequeo previo en el service
existe solo para dar un mensaje de error claro; **la defensa real es el
constraint**, y el `catch (DataIntegrityViolationException)` traduce la
carrera perdida a un `409` limpio.

Como el UK incluye `balneario_id`, el mismo cliente **sí** puede tener
estadías simultáneas en balnearios distintos (etapa 01) — sin código extra:
sale del diseño del índice.

### Apertura en dos pasos
`POST /estadias` (cliente) → `PENDIENTE_VALIDACION` → `POST
/operativo/estadias/{publicId}/validacion` (carpero) → `ACTIVA`. Solo
`ACTIVA` habilita pedidos, y la validación queda registrada con actor
(`validada_por_usuario_id`) y timestamp. El rechazo exige motivo.

### Resto del ciclo
Cambio de ubicación preservando historial (`estadia_ubicacion_historial`,
cierra el tramo anterior y abre uno nuevo), cierre explícito con resumen de
consumo, expiración automática por TTL, y eventos de dominio
(`EstadiaAbierta`/`EstadiaCerrada`) para que las etapas 15+ se cuelguen sin
que `stay` las conozca.

### Deuda de la etapa 11, saldada
La regla "no desactivar/borrar una ubicación con estadía vigente" estaba como
`TODO(etapa 12)` en `UbicacionService`. **Ahora está implementada y testeada**
— `Estadia` ya existe, así que dejó de ser un hook.

## 3. El punto arquitectónico de la etapa: inversión de dependencia

El cierre necesita dos datos que **pertenecen a `ordering`** (etapa 13, que
todavía no existe): si quedan pedidos en curso y cuánto consumió el cliente.
Pero ADR-002 fija la flecha `ordering → stay`, nunca al revés.

En la etapa 10 un caso parecido (`platform` necesitaba `branding`) se resolvió
con un **evento**. Acá no sirve: el cierre necesita la respuesta de forma
**síncrona** para decidir si sigue o falla. Así que se aplicó **inversión de
dependencia**: la interfaz `ConsumoEstadiaProvider` vive en `stay` (el
consumidor la define), y la implementación real la va a aportar `ordering` en
la etapa 13.

Mientras tanto rige `SinPedidosConsumoProvider`, registrado con
`@ConditionalOnMissingBean`. **No es un stub que miente**: sin módulo de
pedidos, "no hay pedidos en curso" y "consumo cero" son literalmente
correctos. Cuando `ordering` publique su bean, esta implementación desaparece
sola, sin tocar una línea de `stay`.

## 4. Bug encontrado durante la verificación

**`@LocalServerPort` no se resuelve en un `@Component` normal.** El helper de
tests `EscenarioBalneario` (que arma balneario operativo + staff + cliente,
para no repetir 40 líneas de seed por clase) lo usaba para construir URLs
absolutas, y el contexto entero fallaba con
`Could not resolve placeholder 'local.server.port'`: el bean se construye
antes de que el servidor web arranque, cuando esa property todavía no existe.
**Fix:** usar rutas **relativas** — el `TestRestTemplate` de
`webEnvironment=RANDOM_PORT` ya las resuelve contra el puerto real.

## 5. Cómo se verificó

**35/35 tests, BUILD SUCCESS**, contra MySQL 8 real:

- `EstadiaCicloDeVidaIntegrationTest` (9): apertura en dos pasos completa
  (incluida la cola del carpero y el registro de actor+timestamp), cambio de
  ubicación, cierre con resumen, cerrar libera el cupo, segunda solicitud en
  el mismo balneario rechazada, **el mismo cliente con estadías en dos
  balnearios distintos**, rechazo con motivo que libera cupo, todas las
  transiciones inválidas (`409` con formato RFC 7807), expiración por TTL
  (envejeciendo la fila en vez de esperar 60 minutos reales), y la regla de
  ubicación con estadía vigente.
- `EstadiaConcurrenciaIntegrationTest` (1): **8 solicitudes genuinamente
  simultáneas** (con `CyclicBarrier` para que salgan en el mismo instante,
  si no se serializan solas y la carrera nunca ocurre) → exactamente **1
  creada y 7 conflictos**. La regla no depende de la suerte del scheduler.
- Suite completa de las etapas 09–11 en verde: sin regresiones.

## 6. Deuda explícita para la etapa 13

- `ConsumoEstadiaProvider` necesita su implementación real en `ordering`
  (pedidos en curso + resumen de consumo). Hasta entonces, el resumen de
  cierre devuelve ceros — correcto hoy, incompleto cuando existan pedidos.
- **Cierre administrativo por fin de temporada**: `cerrarPorSistema()` existe
  y funciona (estado `CERRADA_POR_SISTEMA`, distinguible en reportes), pero
  **no hay job que lo dispare todavía** — el disparador natural es el cambio
  de temporada a `CERRADA`, que conviene resolver junto con los reportes de
  la etapa 15. Señalado para no darlo por hecho.
