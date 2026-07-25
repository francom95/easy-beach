/**
 * Módulo <b>stay</b> (ADR-002): estadía (solicitud, validación por carpero,
 * cambio de ubicación, cierre) y ubicaciones físicas. Depende de
 * {@code identity}, {@code platform} y {@code shared}. Las ubicaciones
 * físicas (ABM) están construidas desde la etapa 11 - las necesita el
 * catálogo/menú antes de que exista la estadía en sí; el resto del módulo
 * (estadía, validación por carpero) se construye en la etapa 12.
 */
package com.easybeach.stay;
