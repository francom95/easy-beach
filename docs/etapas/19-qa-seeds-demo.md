# Etapa 19 — QA integral, seeds y datos demo

- **Orden:** 19
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción / verificación
- **Depende de:** 09–18

## Objetivo

Verificar el MVP de punta a punta como sistema integrado, cubrir los huecos de
testing, y dejar un entorno demo con datos realistas que sirva tanto para QA
como para **vender la plataforma a balnearios** (demo comercial).

## Alcance / Entregables

1. **Suite E2E de flujos críticos** (automatizada donde rinda, guionada donde
   no):
   - Cliente: registro → selección de balneario (la app se transforma al
     theme del balneario) → solicitud de estadía → validación por carpero →
     pedido con variantes → **pago vía Mercado Pago (sandbox)** → tiempo real
     → cierre.
   - Operación: solicitud de estadía validada → pedido pagado entra → se
     despacha → cliente notificado.
   - Pagos: pago rechazado con reintento, webhook duplicado, reembolso por
     cancelación del local (todo contra el sandbox de MP).
   - Admin: configura menú/promoción → impacta en la app del cliente.
   - Super Admin: alta de balneario → operativo end-to-end.
2. **Testing de seguridad dirigido** (checklist de la etapa 05):
   - Batería cross-tenant sistemática: cada endpoint tenant-scoped probado con
     credenciales de otro balneario.
   - IDs ajenos (pedidos, estadías, productos), escalación de rol, tokens
     vencidos/revocados.
3. **Testing de concurrencia**: doble apertura de estadía, doble transición del
   mismo pedido desde dos tablets, reintentos idempotentes bajo carga.
4. **Seeds y datos demo**: script reproducible que crea 2–3 balnearios ficticios
   con branding distinto, menús completos con fotos, ubicaciones, promociones,
   usuarios de cada rol, estadías y pedidos históricos (para que los reportes
   muestren datos). Un comando lo regenera de cero.
5. **Prueba de carga básica**: escenario "sábado de enero" (definir números
   con el supuesto de escala de la etapa 02) sobre menú público y creación de
   pedidos. Resultado documentado, no optimización prematura.
6. **Registro de bugs** priorizado; los bloqueantes se corrigen dentro de esta
   etapa, el resto queda en backlog documentado.

## Inputs requeridos

- Todas las superficies (API, mobile, web) desplegadas juntas en un entorno de
  staging (coordinar con la etapa 20, que puede proveerlo antes de su cierre).

## Criterios de aceptación

- Los 4 flujos E2E críticos pasan en staging con las tres superficies reales.
- Cero hallazgos cross-tenant abiertos (criterio innegociable).
- El entorno demo se levanta con un comando y es presentable a un balneario
  real.
