# Etapa 05 — Seguridad, autenticación, roles y permisos

- **Orden:** 05
- **Modelo ejecutor:** opus
- **Tipo:** especificación (documento, sin código)
- **Depende de:** 02, 03

## Objetivo

Especificar el modelo de seguridad completo: autenticación, autorización por rol,
y el aislamiento entre balnearios como propiedad de seguridad (no solo de
arquitectura). En un SaaS multitenant, una fuga entre tenants es el peor bug
posible.

## Alcance / Entregables

1. **Autenticación**:
   - Clientes: registro/login (email+password mínimo; evaluar login social como
     post-MVP), sesiones largas con refresh token (la estadía puede durar toda
     la temporada — el cliente no debe reloguearse en la playa).
   - Staff (carpero/operador/admin): credenciales creadas por el admin del
     balneario; política de password; expiración de sesión más corta.
   - Mecanismo: JWT access + refresh token; contenido exacto de claims
     (`user_id`, `rol`, `balneario_id` para staff), duración, rotación,
     revocación (logout, staff dado de baja).
2. **Matriz de roles y permisos**: Cliente, Carpero, Operador, Admin de
   balneario, Super Admin × cada operación de la API (toma la matriz de la
   etapa 04 y la cierra). Reglas finas: un carpero solo ve solicitudes de su
   balneario; un cliente solo ve sus estadías y pedidos.
3. **Aislamiento multitenant como control de seguridad**:
   - Regla: el `balneario_id` operativo sale SIEMPRE del token o de la estadía
     activa validada en servidor; nunca de un parámetro libre del cliente.
   - Defensa en profundidad: validación en capa service además del filtro de
     repositorio; tests de autorización cross-tenant obligatorios (etapa 19).
4. **Protección de datos**: qué datos personales se guardan (mínimos), hashing
   de passwords (bcrypt/argon2), datos en logs (nunca tokens ni PII), baja de
   cuenta de cliente.
5. **Seguridad de pagos (Mercado Pago marketplace)**:
   - Validación de autenticidad del **webhook** de Mercado Pago
     (firma/secret) y tolerancia a notificaciones duplicadas o fuera de orden.
   - Custodia de credenciales OAuth por balneario: `access_token` y
     `refresh_token` cifrados en reposo, nunca en logs, rotación/refresh y
     revocación al desvincular.
   - Sin datos de tarjeta en EasyBeach: la tokenización ocurre del lado de
     Mercado Pago (Checkout API), fuera del alcance PCI de la plataforma.
   - El monto a cobrar lo calcula siempre el servidor; el `application_fee`
     es fijo en 0 y no es un parámetro manipulable por el cliente.
6. **Endurecimiento API**: rate limiting (login y creación de pedidos), CORS,
   validación de input, manejo de archivos subidos (logos/fotos de productos:
   tipo, tamaño, almacenamiento fuera del web root).
7. **Amenazas y mitigaciones** (STRIDE liviano): tabla de las 8–10 amenazas más
   relevantes (ej.: cliente adivina IDs de pedidos ajenos, staff de un balneario
   accede a otro, abuso de creación de pedidos falsos a una carpa ajena, webhook
   falsificado que "aprueba" pagos, robo de tokens OAuth de balnearios) con su
   mitigación concreta.

## Inputs requeridos

- Matriz endpoint → rol de la etapa 04 (puede iterarse en paralelo).
- Decisión ya tomada en etapa 01: la apertura de estadía es **validada por el
  carpero** (mitiga la amenaza "pedidos falsos a ubicación ajena"); además,
  todo pedido exige pago aprobado vía Mercado Pago, lo que desincentiva
  pedidos falsos.

## Criterios de aceptación

- Toda operación de la API tiene fila en la matriz de permisos; no hay endpoints
  "a definir".
- El documento especifica dónde se resuelve y valida el tenant en cada tipo de
  request (cliente, staff, super admin).
- Cada amenaza listada tiene mitigación asignada a una etapa concreta.
