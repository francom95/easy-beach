package com.easybeach.payments.web;

import com.easybeach.payments.service.BalnearioMpCredencialService;
import com.easybeach.payments.web.dto.CallbackResultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Público - Mercado Pago redirige acá el navegador del admin, sin ningún
 * header de auth (por eso el tenant se resuelve por {@code state}, no por
 * JWT). Devuelve JSON simple por ahora; cuando exista el panel admin web
 * (etapa 17/18) esto redirige a una pantalla propia en vez de responder API.
 */
@RestController
@RequestMapping("/api/v1/mercadopago/oauth/callback")
public class MercadoPagoOAuthCallbackController {

    private final BalnearioMpCredencialService service;

    public MercadoPagoOAuthCallbackController(BalnearioMpCredencialService service) {
        this.service = service;
    }

    @GetMapping
    public CallbackResultResponse callback(@RequestParam String state,
                                            @RequestParam(required = false) String code,
                                            @RequestParam(required = false) String error) {
        if (error != null) {
            return new CallbackResultResponse(false, "El admin canceló la vinculación en Mercado Pago (" + error + ")");
        }
        service.manejarCallback(state, code);
        return new CallbackResultResponse(true, "Cuenta de Mercado Pago vinculada correctamente");
    }
}
