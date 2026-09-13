package com.andesstay.ms_andesstay_notify.messaging;

import java.time.Instant;

/** Comando consumido desde q.cmd.voucher: generacion de PDF (voucher de reserva o boleta). */
public record VoucherCommand(
        String type,
        String eventId,
        Instant timestamp,
        String traceId,
        String correlationId,
        String reservationId,
        String guestEmail
) {
}
