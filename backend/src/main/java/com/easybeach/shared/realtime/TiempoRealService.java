package com.easybeach.shared.realtime;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Canal SSE de ADR-003: servidor→cliente sobre HTTP plano, con reconexión
 * automática nativa del protocolo (lo que la conectividad de playa necesita).
 *
 * <p><b>SSE es una optimización de latencia, nunca la única vía</b> (regla de
 * diseño de ADR-003): todo lo que se emite acá es también consultable por
 * GET. Si el stream se cae, la app sigue funcionando con polling.
 *
 * <p>Registro in-memory: válido para instancia única (escala del MVP, ADR-003).
 * Para múltiples instancias haría falta pub/sub externo detrás de esta misma
 * interfaz - documentado, no construido.
 */
@Service
public class TiempoRealService {

    private static final Logger log = LoggerFactory.getLogger(TiempoRealService.class);
    private static final Duration TIMEOUT_CONEXION = Duration.ofMinutes(30);

    /** clave = "CLIENTE:<usuarioPublicId>" o "OPERATIVO:<balnearioId>". */
    private final Map<String, List<SseEmitter>> suscriptores = new ConcurrentHashMap<>();

    public SseEmitter suscribir(CanalTiempoReal canal, String clave) {
        String key = claveDe(canal, clave);
        SseEmitter emitter = new SseEmitter(TIMEOUT_CONEXION.toMillis());

        suscriptores.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remover(key, emitter));
        emitter.onTimeout(() -> remover(key, emitter));
        emitter.onError(e -> remover(key, emitter));

        try {
            // Evento inicial: confirma la conexión y evita que proxies la corten
            // antes del primer dato real.
            emitter.send(SseEmitter.event().name("conectado").data(Map.of("canal", canal.name())));
        } catch (IOException e) {
            remover(key, emitter);
        }
        return emitter;
    }

    public void emitirACliente(String usuarioPublicId, String evento, Object payload) {
        emitir(claveDe(CanalTiempoReal.CLIENTE, usuarioPublicId), evento, payload);
    }

    public void emitirAOperativo(Long balnearioId, String evento, Object payload) {
        emitir(claveDe(CanalTiempoReal.OPERATIVO, String.valueOf(balnearioId)), evento, payload);
    }

    private void emitir(String key, String evento, Object payload) {
        List<SseEmitter> emitters = suscriptores.get(key);
        if (emitters == null || emitters.isEmpty()) {
            return; // nadie escuchando: el estado se recupera por GET igual
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(payload));
            } catch (Exception e) {
                // Una conexión muerta no puede romper la transacción de negocio
                // que la disparó: se descarta y el cliente reconecta.
                remover(key, emitter);
            }
        }
    }

    /** Heartbeat de ADR-003: detecta conexiones muertas y evita timeouts de proxies. */
    @Scheduled(fixedDelayString = "PT25S")
    public void heartbeat() {
        suscriptores.forEach((key, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (Exception e) {
                    remover(key, emitter);
                }
            }
        });
    }

    public int cantidadSuscriptores(CanalTiempoReal canal, String clave) {
        return suscriptores.getOrDefault(claveDe(canal, clave), List.of()).size();
    }

    private void remover(String key, SseEmitter emitter) {
        List<SseEmitter> emitters = suscriptores.get(key);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                suscriptores.remove(key);
            }
        }
        log.debug("Suscriptor SSE removido de {}", key);
    }

    private String claveDe(CanalTiempoReal canal, String clave) {
        return canal.name() + ":" + clave;
    }
}
