# Etapa 15 — Backend reportes básicos

- **Estado:** ejecutada. Cierra el **Hito Backend MVP** (fin etapa 15): la
  API completa, operable de punta a punta por tests/Postman.
- **Corresponde al plan:** [docs/etapas/15-backend-reportes.md](../etapas/15-backend-reportes.md)
- **Depende de:** [13](13-backend-pedidos.md), [14](14-backend-carpero-promociones.md).
- **Código:** [`backend/`](../../backend/) — módulo `reporting` (completo,
  sin dependencias de módulos de negocio, ver §2).

## 1. Qué se construyó

Cinco reportes por balneario (ventas, productos más vendidos, rendimiento
de promociones, estadías, servicios al carpero), todos con filtro
`desde`/`hasta` y export CSV; un resumen de dashboard con los KPIs del día;
y el reporte de plataforma para Super Admin (el único que cruza datos entre
balnearios, por diseño).

**Todo lo financiero cuenta solo pedidos `ENTREGADO`** (criterio de
aceptación): un pedido cancelado no aporta facturación en ningún reporte,
verificado explícitamente con un dataset de 2 pedidos entregados + 1
cancelado.

## 2. El punto arquitectónico: `reporting` no depende de nada

ADR-002 es el más estricto para este módulo: `reporting -> {}` — ni
siquiera puede importar las entidades JPA de `ordering`/`stay`/`concierge`/
`platform`. La solución (ya anticipada en el `package-info` del módulo
desde la etapa 09): **JDBC plano con `JdbcTemplate`**, leyendo las tablas
directamente por SQL, sin pasar nunca por los repositorios de otros
módulos. Es un "read model propio" real, no una reutilización disfrazada.

Costo de esa decisión: **ninguna query tiene el filtro de tenant automático
de Hibernate** (`@Filter`) — cada una filtra `balneario_id` a mano. Por eso
`ReportesBalnearioIntegrationTest` incluye un test dedicado de aislamiento
cross-tenant, no incidental.

## 3. El hallazgo más importante de la etapa: TZ silenciosamente rota en JDBC plano

La app fuerza `hibernate.jdbc.time_zone: UTC` en `application.yml` para que
Hibernate escriba los campos `Instant` de las entidades como UTC real, sin
importar la zona horaria del sistema operativo. Eso es exactamente lo que
etapa 03 exige ("almacenados en UTC").

**Pero esa property es exclusiva de Hibernate.** Un `JdbcTemplate` crudo,
por fuera del ORM, no la hereda: al bindear un `java.time.Instant`
convertido a `java.sql.Timestamp`, el **driver JDBC** convierte usando la
zona horaria **por defecto de la JVM** — en esta máquina,
`America/Buenos_Aires` (UTC-3), no UTC. El resultado: cada comparación de
fecha en los reportes quedaba corrida 3 horas contra lo que Hibernate
realmente había escrito, y las queries devolvían listas vacías **sin
ningún error ni excepción** — el tipo de bug más peligroso, porque parece
"no hay datos" en vez de "algo está roto".

**Cómo se encontró:** los primeros tests de reportes fallaban con
resultados vacíos donde se esperaban datos concretos. Se descartó primero
la hipótesis de que `Instant` simplemente no bindea en JDBC 4.2 (cierto,
pero insuficiente: cambiar a `Timestamp` no arregló nada). Recién al
escribir un test dedicado de borde de zona horaria — forzando
`created_at` a un valor UTC exacto vía UPDATE directo y comparando con
`getLong()`/lectura cruda de la fila — se confirmó que el valor
almacenado por Hibernate y el valor bindeado por `JdbcTemplate` para la
MISMA hora real diferían en exactamente 3 horas.

**Fix:** en vez de bindear un `Timestamp`/`Instant` (ambigüedad de zona
horaria garantizada), cada repositorio formatea el instante como **literal
de texto UTC** (`yyyy-MM-dd HH:mm:ss.SSS`, `DateTimeFormatter.withZone(UTC)`)
antes de pasarlo a la query. Un literal de texto no sufre ninguna
conversión de zona horaria del lado del driver — se manda tal cual, y
`DATETIME` en MySQL no tiene timezone propia, así que compara dígito a
dígito contra lo que Hibernate escribió. El mismo patrón se usó para el
propio dataset de los tests (manipulaciones directas de `created_at` vía
JDBC también tenían que evitar `Timestamp`).

**Alcance de la corrección:** acotada a los repositorios nuevos de
`reporting`, que son el único código de la app que hace JDBC plano sobre
columnas de fecha. Ningún otro módulo bindea `Instant` fuera de Hibernate,
así que este bug no existía en ningún otro lugar del código - pero es
exactamente la clase de trampa que aparecería de nuevo si otra etapa futura
agregara otro acceso JDBC directo sin recordar esto.

## 4. Otras decisiones de implementación

- **Bordes de fecha:** `desde`/`hasta` son fechas de negocio (TZ Argentina,
  no UTC), convertidas a los límites `Instant` reales por
  `RangoFechasUtil` - `hasta` es inclusivo desde la perspectiva del usuario
  ("del 1 al 31"), el límite real de la query es el inicio del día
  siguiente.
- **Agrupación "por día"** usa `CONVERT_TZ(col, '+00:00', '-03:00')` con el
  **offset numérico fijo**, no el nombre de la zona: Argentina no tiene
  horario de verano desde 2009 (offset constante todo el año), y
  `CONVERT_TZ` con nombres de zona depende de que la imagen de MySQL tenga
  cargadas las tablas `mysql.time_zone_name` - muchas no las traen, y en
  ese caso la función devuelve `NULL` en silencio en vez de fallar. El
  offset numérico no tiene esa dependencia.
- **`BIGINT UNSIGNED` vía JDBC**: se lee con `rs.getLong()` + `wasNull()`,
  no con `rs.getObject()` - este último puede devolver `BigInteger` en vez
  de `Long` según el driver, rompiendo el cast.
- **CSV**: implementación mínima propia (RFC 4180: comillas/comas/saltos de
  línea escapados), sin librería externa.
- **Tiempo de resolución de servicios**: usa `updated_at` como proxy de
  "cuándo se resolvió" (no hay columna `fecha_resuelta` propia) - válido
  porque `RESUELTA` es un estado terminal (etapa 14): una vez ahí, la
  solicitud no vuelve a cambiar.

## 5. Cómo se verificó

**99/99 tests, BUILD SUCCESS**, contra MySQL 8 real, con datasets conocidos
verificados a mano (criterio de aceptación explícito):

- `ReportesBalnearioIntegrationTest` (9): ventas cuenta solo lo entregado
  (2 pedidos ENTREGADO + 1 CANCELADO, verificado que el cancelado no suma
  ni en facturación ni en ticket promedio ni en el desglose por día);
  export CSV descargable con `Content-Disposition`; productos más vendidos
  (unidades sin descuento vs. facturación); rendimiento de promociones
  (2 usos, 300 de descuento, el pedido cancelado excluido); estadías
  (aperturas por día, duración, consumo promedio); servicios (por tipo +
  tiempo de resolución con dataset de 30 minutos exactos); dashboard
  (KPIs de hoy + pedidos en curso, distinto del facturado); aislamiento
  cross-tenant (admin de otro balneario ve todo en cero); un operador no
  puede consultar reportes (403).
- `ReportesPlataformaIntegrationTest` (2): el reporte incluye el volumen
  real del balneario propio; un admin de balneario no puede verlo (403).
- `ReportesZonaHorariaIntegrationTest` (1): un pedido a las 02:00 UTC del
  día D+1 (23:00 ART del día D) se agrupa en el día D en Argentina, **no**
  en el día D+1 crudo de UTC - el test que expuso el bug de la §3.

## 6. Deuda explícita

Ninguna nueva propia de esta etapa. La deuda heredada de etapas anteriores
(job de reconciliación de pagos, SSE de instancia única, job de cierre
administrativo de estadías por fin de temporada) sigue igual - reportar
sobre datos existentes no las resuelve ni las agrava.
