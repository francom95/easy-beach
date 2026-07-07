# Etapa 06 — Identidad visual y sistema de diseño

- **Orden:** 06
- **Modelo ejecutor:** claude design (diseño visual — se hace a mano)
- **Tipo:** diseño
- **Depende de:** 01
- **Puede correr en paralelo con:** 02–05 y 09–15 (el backend no la necesita)

## Objetivo

Definir la identidad visual de EasyBeach como plataforma y, sobre todo, el
**sistema de theming white-label por balneario**: al seleccionar un balneario,
**toda la estética de la app se transforma como si la app fuera del
balneario** — no un acento de color, sino la identidad completa. La marca
EasyBeach solo existe antes de la selección de balneario y en los paneles web
de staff. El diseño debe empujar el objetivo de negocio: que pedir sea
irresistible y sin fricción.

## Alcance / Entregables

1. **Identidad de la plataforma EasyBeach**: logo, paleta propia, tipografías,
   tono de comunicación. Su ámbito es acotado: selección de balneario,
   onboarding previo a la selección y paneles web de staff. **Dentro de una
   estadía, la marca EasyBeach no aparece en la UI del cliente.**
2. **Sistema de theming white-label por balneario** — el entregable clave:
   - Qué personaliza cada balneario: **la capa visual completa** — logo, paleta
     íntegra (primario, secundario, superficies, fondos, estados), tipografía
     elegida de un **set curado** (para garantizar legibilidad y licencias),
     imagen de portada, splash/transición de entrada al balneario, fotos de
     productos, iconografía de acento si aplica.
   - Qué queda común: la **estructura**, no la estética — arquitectura de
     navegación, layout de componentes y patrones de interacción. Esa
     estructura debe diseñarse **visualmente neutra**, sin rasgos de marca
     EasyBeach, para que una vez aplicado el theme la app se perciba 100 %
     como la app propia del balneario.
   - Tokens de diseño (nombres y semántica: `color.primary`, `color.surface`,
     `typography.family`, `asset.splash`, etc.) que después la API sirve como
     configuración visual (etapa 10) y que mobile/web consumen para tematizar
     en runtime. El set de tokens debe ser suficiente para la transformación
     total: si un elemento visual no es tematizable por token, debe ser neutro.
   - Reglas de accesibilidad: contraste mínimo aunque el balneario elija colores
     propios (definir estrategia: derivar tonos, validar contraste al configurar).
   - Diseño de la **transición de marca**: qué ve el cliente en el momento en
     que elige balneario y la app "se convierte" en la del balneario (splash de
     entrada, animación, carga del theme).
3. **Design system base**: componentes (botones, cards de producto, badge de
   promoción, stepper de cantidad, estados de pedido con color/icono, empty
   states), espaciados, radios, sombras. Pensado para sol directo en la playa:
   contraste alto, tipografía grande, targets táctiles generosos.
4. **Direcciones de marca aplicada**: 2–3 ejemplos del mismo screen (menú) con
   theming de balnearios ficticios distintos, para validar que el sistema
   flexiona bien.

## Inputs requeridos

> Esta etapa NO arranca sin este material:

- Logo de EasyBeach (si existe) o brief para crearlo.
- Colores/preferencias de marca del proyecto, si hay algo definido.
- Referencias visuales: 3–5 apps o productos cuyo estilo guste (y alguno que
  NO guste, como contraejemplo).
- Fotos reales de balnearios argentinos objetivo (contexto de uso real).
- Glosario y user journeys de la etapa 01.

## Criterios de aceptación

- Existe una lista cerrada de tokens de theming con nombre, tipo y default —
  esta lista es contrato con las etapas 10 (API), 16 (mobile) y 17 (web).
- Los mockups de un mismo screen con 2–3 themes distintos se ven consistentes
  y correctos en contraste.
- **Test white-label:** aplicado el theme de un balneario, ninguna pantalla
  del flujo de estadía contiene un elemento visual que remita a EasyBeach; un
  usuario que no conoce la plataforma debería creer que es la app propia del
  balneario.
- El design system cubre todos los componentes que aparecen en los user
  journeys del MVP.
