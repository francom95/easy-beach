# Etapa 10 — Backend Super Admin: balnearios, planes y temporadas

- **Orden:** 10
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción
- **Depende de:** 09
- **Estado: ✅ EJECUTADA** — código en [`backend/`](../../backend/) (módulos
  `platform`, `branding`, `payments`), entregable en
  [`docs/especificacion/10-backend-super-admin.md`](../especificacion/10-backend-super-admin.md).
  20/20 tests, BUILD SUCCESS, contra MySQL 8 real: flujo completo (crear
  balneario → vincular Mercado Pago OAuth → configurar branding → suscribir
  a temporada → listado público → suspender/reactivar), contrato de 25
  tokens de theming servido íntegro, autorización 401/403 correcta, upload
  de imágenes validado por magic bytes.

## Objetivo

Implementar el módulo de plataforma: el Super Admin da de alta balnearios, los
asocia a planes por temporada y controla su estado. Sin esta etapa no existen
tenants sobre los que operar.

## Alcance / Entregables

1. **ABM de balnearios**: crear, editar, listar, activar/suspender. Alta de
   balneario crea su primer usuario admin (invitación o password temporal).
2. **Vinculación de Mercado Pago por balneario (OAuth)**: paso del onboarding
   del balneario en el que su admin autoriza a EasyBeach contra su propia
   cuenta de Mercado Pago (modelo marketplace/split con `application_fee = 0`,
   decisión de la etapa 01). El backend guarda de forma segura el
   `access_token`/`refresh_token` del balneario (custodia según etapa 05) y
   expone el estado de vinculación. **Regla operativa:** un balneario sin
   cuenta MP vinculada no puede recibir pedidos pagos.
3. **Configuración visual por balneario (theme white-label completo)**:
   endpoints para leer y actualizar el set íntegro de tokens de theming
   definido en la etapa 06 — paleta completa, logo, tipografía del set curado,
   imagen de portada, splash — con subida de imágenes según reglas de la etapa
   05 (tipo, tamaño, storage). Endpoint **público** de branding que devuelve el
   theme completo: la app mobile lo consume antes de que el cliente esté
   autenticado y con él se transforma por entero a la identidad del balneario.
4. **Planes y temporadas**: ABM de planes, definición de temporada
   (fechas), suscripción de un balneario a un plan por temporada, estados de la
   suscripción (según máquina de estados de la etapa 03).
5. **Efecto operativo del estado**: un balneario suspendido o fuera de
   temporada no acepta estadías ni pedidos nuevos (regla central del modelo
   SaaS; definir qué pasa con estadías abiertas al suspender).
6. **Listado público de balnearios activos** para la pantalla de selección de
   la app (solo los operativos en la temporada vigente).
7. **Auditoría** de acciones de Super Admin (quién suspendió qué y cuándo).

## Inputs requeridos

- Lista cerrada de tokens de theming (etapa 06). Si la etapa 06 aún no cerró,
  usar los tokens del ADR de la etapa 02 como borrador y marcar el contrato
  como provisorio.

## Criterios de aceptación

- Flujo completo por API: crear balneario → vincular Mercado Pago (OAuth) →
  configurar branding → suscribir a temporada → aparece en el listado público
  → suspender → desaparece y rechaza operaciones.
- Un balneario sin cuenta de Mercado Pago vinculada es rechazado al intentar
  recibir un pedido pago (test).
- El endpoint público de branding devuelve todos los tokens del contrato de la
  etapa 06 (theme completo), no un subconjunto.
- Los endpoints de Super Admin son inaccesibles para cualquier otro rol (test).
- Las imágenes suben, se validan y se sirven por URL pública.
