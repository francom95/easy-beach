/**
 * Módulo <b>reporting</b> (ADR-002): reportes y KPIs, solo lectura. No
 * depende de ningún otro módulo de negocio - lee las tablas directamente
 * vía JDBC plano (read models propios, ver {@code PedidoReportingRepository}),
 * nunca reutiliza los repositorios JPA de {@code ordering}/{@code stay}/
 * {@code concierge}/{@code platform}. Construido en la etapa 15.
 */
package com.easybeach.reporting;
