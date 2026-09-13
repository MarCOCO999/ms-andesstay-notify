package com.andesstay.ms_andesstay_notify.messaging;

import java.time.Instant;

/** Comando consumido desde q.cmd.housekeeping: ticket de preparacion/limpieza de una unidad. */
public record HousekeepingTicketCommand(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        Long unitId,
        String instructions
) {
}
