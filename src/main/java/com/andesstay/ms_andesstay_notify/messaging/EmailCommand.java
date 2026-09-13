package com.andesstay.ms_andesstay_notify.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * Comando consumido desde q.cmd.email (confirmacion de reserva, recordatorio de
 * check-in, checkout). Los campos type/eventId/timestamp/traceId/correlationId son el
 * envelope comun acordado para las 3 colas de comando del caso AndesStay.
 */
public record EmailCommand(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        String guestEmail,
        String template,
        Map<String, String> variables
) {
}
