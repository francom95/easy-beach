package com.easybeach.branding;

import com.easybeach.branding.service.ConfiguracionVisualService;
import com.easybeach.platform.event.BalnearioCreado;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Siembra el theme default de EasyBeach al crear un balneario (etapa 10).
 * {@code @EventListener} plano (no {@code @TransactionalEventListener}):
 * corre síncrono, en la MISMA transacción que {@code BalnearioService.crear}
 * - si sembrar el theme fallara, la creación del balneario se revierte
 * también (no debería poder existir un balneario sin branding válido).
 */
@Component
public class BalnearioCreadoListener {

    private final ConfiguracionVisualService configuracionVisualService;

    public BalnearioCreadoListener(ConfiguracionVisualService configuracionVisualService) {
        this.configuracionVisualService = configuracionVisualService;
    }

    @EventListener
    public void onBalnearioCreado(BalnearioCreado event) {
        configuracionVisualService.sembrarDefault(event.balnearioId());
    }
}
