# Etapa 05 — Seguridad, autenticación, roles y permisos

- **Estado:** ejecutada — cierra el bloque de especificación. Insumo directo de
  las etapas 09 (backend fundacional), 13 (pagos/webhook) y 19 (tests de
  seguridad).
- **Corresponde al plan:** [docs/etapas/05-seguridad-roles.md](../etapas/05-seguridad-roles.md)
- **Depende de:** [02 (arquitectura)](02-arquitectura-general.md),
  [03 (modelo de datos)](03-modelo-de-datos.md),
  [04 (contratos de API)](04-contratos-api.md) y ADRs 001/004.

> Principio rector: en un SaaS multitenant, **una fuga entre balnearios es el
> peor defecto posible**. El aislamiento no es solo arquitectura (ADR-001): es
> un control de seguridad con defensa en profundidad y tests obligatorios.

---

## 1. Autenticación

### 1.1 Mecanismo — JWT access + refresh

| Aspecto | Cliente | Staff (carpero/operador/admin) | Super Admin |
|---|---|---|---|
| Alta de credencial | Auto-registro (email+password) | Creada por el admin del balneario | Provisión manual (semilla/plataforma) |
| **Access token (TTL)** | 60 min | 30 min | 15 min |
| **Refresh token (TTL)** | **60 días** (la estadía puede durar la temporada; no reloguear en la playa) | 12 h (jornada laboral) | 8 h |
| Rotación de refresh | Sí, en cada uso (rotating refresh tokens) | Sí | Sí |
| Password mínimo | 8 caracteres | Política reforzada (8+, no trivial); cambio obligatorio en primer login | Igual staff |

- **Access token = JWT firmado** (algoritmo asimétrico **RS256**: la API firma
  con clave privada; los servicios verifican con la pública — evita compartir
  secreto simétrico). **Stateless**: no se consulta la DB para autorizar.
- **Refresh token = opaco** (no JWT), persistido con hash en servidor
  (`sesion_refresh`), de forma que se pueda **revocar** (logout, baja de staff,
  cambio de password, "cerrar todas las sesiones"). La rotación detecta reuso:
  si llega un refresh ya rotado ⇒ se revoca toda la familia (posible robo).

### 1.2 Contenido del JWT (claims)

```json
{
  "sub": "usuario.public_id (ULID)",
  "tipo": "CLIENTE | STAFF | SUPER_ADMIN",
  "rol": "CLIENTE | CARPERO | OPERADOR | ADMIN_BALNEARIO | SUPER_ADMIN",
  "balneario_id": 42,          // presente SOLO en staff; ausente/null en cliente y super admin
  "iat": 0, "exp": 0, "jti": "id-del-token"
}
```

- **`balneario_id` viaja en el token de staff y es la única fuente del tenant
  operativo** para requests de staff (ADR-001). El cliente **no** lleva
  `balneario_id`: su tenant sale del recurso (estadía/pedido) validado en
  servidor.
- `jti` permite revocación puntual (blacklist corta en cache hasta el `exp`).

### 1.3 Revocación

| Evento | Efecto |
|---|---|
| Logout | Revoca el refresh token usado |
| Baja de staff (`usuario.estado=BAJA`) | Revoca todas sus sesiones; sus access tokens vivos se rechazan por `jti`/estado en el filtro |
| Cambio de password | Revoca todas las sesiones del usuario |
| Detección de reuso de refresh rotado | Revoca la familia completa de tokens |

---

## 2. Autorización — matriz de roles y permisos (cerrada)

Cierra la matriz de la [etapa 04 §3](04-contratos-api.md). Roles: **Cli**,
**Car**, **Ope**, **AdmB**, **SA**. El admin de balneario **hereda** las
capacidades operativas de carpero y operador dentro de su balneario.

| Grupo de operaciones | Cli | Car | Ope | AdmB | SA |
|---|:-:|:-:|:-:|:-:|:-:|
| Registro/login/refresh/logout | ✔ | ✔ | ✔ | ✔ | ✔ |
| Navegación pública (balnearios, menú, branding, promos) | ✔ | ✔ | ✔ | ✔ | ✔ |
| Solicitar/consultar/cerrar **estadía propia** | ✔ | | | | |
| Crear/consultar/cancelar **pedido propio**, pagar | ✔ | | | | |
| Crear/consultar **solicitud de servicio propia** | ✔ | | | | |
| Validar/rechazar aperturas de estadía (su balneario) | | ✔ | | ✔ | |
| Atender cola de servicios (su balneario) | | ✔ | | ✔ | |
| Cola de pedidos + transición de estados (su balneario) | | | ✔ | ✔ | |
| ABM catálogo/variantes/ubicaciones/promos/staff (su balneario) | | | | ✔ | |
| Configuración visual + vinculación MP (su balneario) | | | | ✔ | |
| Reportes/dashboard (su balneario) | | | | ✔ | |
| ABM balnearios/planes/temporadas/suscripciones, suspender | | | | | ✔ |

**Reglas finas (ownership + tenant), verificadas por test (etapa 19):**
1. **Cliente:** solo accede a estadías/pedidos/solicitudes donde `cliente_id`
   = `sub` del token. Recurso ajeno ⇒ **404** (no se revela existencia).
2. **Staff:** toda operación se acota a `balneario_id` del token; el carpero y
   el operador nunca ven recursos de otro balneario. Recurso de otro tenant ⇒
   **404**.
3. **Separación cliente↔staff:** un token de cliente en un endpoint `admin/**`
   u `operativo/**` ⇒ **403**; un token de staff en un endpoint de cliente
   sobre una estadía que no le pertenece ⇒ **404/403** según corresponda.
4. **Super Admin** es el único con acceso cross-tenant, siempre auditado
   (`auditoria_plataforma`).

- **Implementación (etapa 09):** autorización declarativa por endpoint
  (`@PreAuthorize` con rol) **+** verificación de ownership/tenant en la capa
  service. La anotación no alcanza para ownership: el service siempre compara
  `recurso.balnearioId`/`recurso.clienteId` contra el contexto del token.

---

## 3. Aislamiento multitenant como control de seguridad

Recapitula y endurece ADR-001 desde la óptica de seguridad. **Tres capas
independientes** — un atacante (o un bug) necesita atravesar las tres:

1. **Resolución del tenant confiable:** el `balneario_id` operativo sale
   **siempre** del JWT (staff) o del recurso validado en servidor (cliente),
   **nunca** de un parámetro libre. Un `balneario_id` en body/query de un
   request de staff se ignora explícitamente.
2. **Filtro automático en persistencia:** Hibernate `@Filter` sobre entidades
   `@TenantScoped` con el `TenantContext` del request (ADR-001). Cubre las
   queries derivadas y JPQL.
3. **Aserción en service + tests cross-tenant:** para cargas por id (que el
   filtro no cubre), el service verifica pertenencia; y la **batería
   cross-tenant** (etapa 19) prueba cada endpoint tenant-scoped con
   credenciales de otro balneario, exigiendo 404/403 — **cero hallazgos es
   criterio innegociable de release**.

- **Regla de fuga = incidente de seguridad**, no bug funcional: si un test
  cross-tenant falla, bloquea el release.

---

## 4. Seguridad de pagos (Mercado Pago marketplace — ADR-004)

### 4.1 Webhook — autenticidad e idempotencia
- **Verificación de firma obligatoria:** MP envía `x-signature` (+ `x-request-id`);
  se valida el HMAC contra el **secret de la aplicación** antes de procesar.
  Firma inválida ⇒ **401**, sin efecto.
- **No confiar en el payload:** la notificación es solo un aviso. Ante cada
  webhook se **reconsulta el pago a la API de MP** (server-to-server con el
  token del balneario) y se decide sobre esa respuesta, no sobre el body.
- **Idempotencia:** `mp_webhook_notificacion` (UK `mp_payment_id + tipo + hash`)
  descarta duplicados; tolerante a notificaciones fuera de orden (se evalúa
  siempre el estado real en MP).
- **Reconciliación:** job periódico resuelve pagos `PAGO_PENDIENTE` con webhook
  perdido/demorado.

### 4.2 Custodia de credenciales OAuth
- `access_token`/`refresh_token` de cada balneario **cifrados en reposo**
  (AES-256-GCM; clave maestra fuera de la DB — variable de entorno/secreto
  gestionado, nunca en el repo). Columnas `VARBINARY` (etapa 03).
- **Nunca** en logs ni en respuestas de API. Refresh anticipado por job;
  revocación y borrado del token al desvincular.
- El pago se crea **siempre** con el token del balneario dueño del pedido
  (resuelto por `pedido.balneario_id`): un test verifica que jamás se use el
  token de otro balneario.

### 4.3 Datos de tarjeta y monto
- **Fuera de alcance PCI:** la tarjeta se tokeniza del lado de MP (Checkout API,
  SDK en el dispositivo). EasyBeach nunca ve, transmite ni persiste PAN/CVV;
  solo maneja el `card_token` de un solo uso.
- **Monto y `application_fee` no manipulables:** el total lo calcula el
  servidor (precios congelados + promos); `application_fee` es una **constante
  0** del servidor, jamás un parámetro del request.

---

## 5. Protección de datos (privacidad)

- **Minimización:** se guardan datos personales mínimos — email, nombre,
  password hash. Sin domicilio, sin documento, sin datos de tarjeta.
- **Passwords:** hash con **argon2id** (o bcrypt cost ≥ 12 si argon2 no está
  disponible); nunca en claro ni recuperables.
- **Logs:** estructurados con `request_id` + `balneario_id`; **prohibido**
  loguear tokens (JWT/refresh/MP), passwords, `card_token`, `x-signature` o PII
  más allá de un identificador. Redacción automática en el logger.
- **Baja de cuenta de cliente:** el cliente puede darse de baja; se anonimiza
  su PII (email/nombre) conservando la integridad de pedidos históricos
  (necesarios para reportes del balneario) — el `usuario` queda `BAJA` con
  datos anonimizados, no se borran los pedidos.
- **Transporte:** todo por **HTTPS/TLS**; HSTS. Cookies (si se usan para web)
  `Secure`+`HttpOnly`+`SameSite`.

---

## 6. Endurecimiento de la API

| Control | Detalle |
|---|---|
| **Rate limiting** | Login: por IP + por email (ej. 5/min, 20/h) para frenar fuerza bruta → **429**. Creación de pedido: por cliente. Registro: por IP. Webhook: por origen MP |
| **CORS** | Allowlist de orígenes (apps web de paneles y dominios propios); la app mobile no usa CORS |
| **Validación de input** | Bean Validation en todos los DTOs; rechazo de payloads sobredimensionados; tipos estrictos (montos como decimal string, no eval) |
| **Archivos subidos** (logos/fotos/splash) | Validar **tipo real** (magic bytes, no solo extensión) y tamaño máx.; renombrar a nombre generado; almacenar **fuera del web root** (storage/CDN); servir por URL firmada o pública sin ejecución |
| **Headers de seguridad** | HSTS, `X-Content-Type-Options: nosniff`, `Content-Security-Policy` en web, sin `Server` verboso |
| **Errores** | RFC 7807 sin stack traces ni detalles internos al cliente; el `detail` no filtra estructura de datos |
| **Dependencias** | Escaneo de vulnerabilidades en CI (etapa 09/20) |

---

## 7. Amenazas y mitigaciones (STRIDE liviano)

Cada amenaza tiene mitigación **asignada a una etapa** (criterio de aceptación):

| # | Amenaza | Categoría | Mitigación | Etapa |
|---|---|---|---|---|
| 1 | Cliente adivina/enumera IDs de pedidos/estadías ajenos | Info. Disclosure | `public_id` **ULID** no secuencial (etapa 03) + verificación de ownership en service + 404 en ajeno | 03, 09 |
| 2 | Staff del balneario A accede a datos del B | Elevation / Info | 3 capas de aislamiento (§3) + batería de tests cross-tenant | 09, 19 |
| 3 | Pedidos falsos a una carpa/ubicación ajena | Spoofing | Apertura de estadía **validada por carpero** (etapa 01) + pago aprobado obligatorio antes de entrar a cola | 12, 13 |
| 4 | **Webhook de MP falsificado** que "aprueba" un pago | Spoofing / Tampering | Verificación de firma `x-signature` + **reconsulta server-to-server** a MP; el body no es fuente de verdad | 05, 13 |
| 5 | Robo de tokens OAuth de balnearios (cobrar a cuenta ajena) | Info / Elevation | Tokens cifrados AES-GCM, clave fuera de DB, nunca en logs; pago siempre con el token del balneario del pedido | 05, 10, 13 |
| 6 | Manipulación de monto/`application_fee` en el pago | Tampering | Total calculado server-side; `application_fee=0` constante del servidor | 13 |
| 7 | Fuerza bruta / credential stuffing en login | Spoofing | Rate limiting por IP+email, hash argon2id, bloqueo temporal | 09 |
| 8 | Reuso/robo de refresh token | Spoofing | Refresh opaco, rotación con detección de reuso (revoca familia), revocación en logout/baja | 09 |
| 9 | Doble apertura de estadía / doble transición de pedido (carrera) | Tampering | Constraint DB de unicidad de estadía (`activa_uk`) + idempotencia de pedido + validación de transición | 03, 12, 13 |
| 10 | XSS/inyección vía nombres de producto o config visual del admin | Tampering | Validación/escape de input; tokens de theming validados contra contrato; sin `eval` de valores | 05, 11 |
| 11 | Subida de archivo malicioso como "logo/foto" | Tampering | Validación de magic bytes + tamaño, renombrado, storage fuera del web root sin ejecución | 05, 10, 11 |
| 12 | Repudio de acciones sensibles (suspensión, validación) | Repudiation | Auditoría (`auditoria_plataforma`, `pedido_evento`, validación de estadía con actor+timestamp) | 03, 10 |

---

## 8. Checklist de verificación (para la etapa 19)

- [ ] Batería cross-tenant: cada endpoint tenant-scoped probado con credenciales
      de otro balneario ⇒ 404/403. **Cero hallazgos.**
- [ ] IDs ajenos (pedido/estadía/solicitud de otro cliente) ⇒ 404.
- [ ] Escalación de rol: cliente→admin, operador→super admin ⇒ 403.
- [ ] Token vencido/revocado ⇒ 401; refresh rotado reusado ⇒ familia revocada.
- [ ] Webhook con firma inválida ⇒ 401, sin efecto; webhook duplicado ⇒ no
      re-procesa; webhook válido pero pago no aprobado en MP ⇒ pedido no
      confirma.
- [ ] Pago con token de otro balneario ⇒ imposible (test).
- [ ] Ningún log contiene tokens, passwords, `card_token` ni PII.
- [ ] Archivo con extensión falsa (ej. `.png` que es ejecutable) ⇒ rechazado.

---

## 9. Decisiones cerradas aquí (antes abiertas en 03/04)

- **Duración/rotación de tokens** (§1): access 60/30/15 min por rol; refresh
  60 días cliente / 12 h staff, rotativo con detección de reuso.
- **Firma del webhook de MP** (§4.1): HMAC sobre `x-signature` con secret de la
  aplicación + reconsulta a MP.
- **Algoritmo de hash** (§5): argon2id.
- **Firma del JWT** (§1.1): RS256 asimétrico.
