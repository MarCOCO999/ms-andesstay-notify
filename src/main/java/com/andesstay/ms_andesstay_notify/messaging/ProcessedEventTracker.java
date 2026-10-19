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
 *
 * El evento se marca recien despues de procesarlo con exito: si se marcara antes, un
 * mensaje que fallo y vuelve desde la cola de reintento (o se reprocesa desde la DLQ)
 * se descartaria como duplicado sin haberse enviado nunca.
 */
@Component
public class ProcessedEventTracker {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public boolean isProcessed(String eventId) {
        return eventId != null && processedEventIds.contains(eventId);
    }

    public void markProcessed(String eventId) {
        if (eventId != null) {
            processedEventIds.add(eventId);
        }
    }
}
