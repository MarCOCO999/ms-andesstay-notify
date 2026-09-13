package com.andesstay.ms_andesstay_notify.messaging;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deduplicacion basica por eventId para que un reintento/redelivery de RabbitMQ no
 * dispare la misma notificacion dos veces. Al ser en memoria y sin DB (notify no tiene
 * base de datos segun el caso), no sobrevive un reinicio ni se comparte entre replicas;
 * si el servicio escala a mas de una instancia conviene reemplazarla por un store
 * compartido (p.ej. Redis) para idempotencia real.
 */
@Component
public class ProcessedEventTracker {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /** @return true si es la primera vez que se ve este eventId (hay que procesarlo). */
    public boolean markIfNew(String eventId) {
        return processedEventIds.add(eventId);
    }
}
