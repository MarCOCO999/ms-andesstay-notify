package com.andesstay.ms_andesstay_notify.service;

import com.andesstay.ms_andesstay_notify.messaging.VoucherCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Generacion del voucher (PDF) de la reserva confirmada. */
@Service
public class VoucherGenerationService {

    private static final Logger log = LoggerFactory.getLogger(VoucherGenerationService.class);

    private final SimulatedProvider provider;

    public VoucherGenerationService(SimulatedProvider provider) {
        this.provider = provider;
    }

    public void generate(VoucherCommand command) {
        CommandValidation.requireText(command.eventId(), "eventId");
        CommandValidation.requireText(command.reservationId(), "reservationId");

        // TODO: generar el PDF del voucher/boleta y enviarlo o dejarlo disponible para el huesped.
        provider.call("Generador de vouchers");
        log.info("Generando voucher PDF para la reserva {} ({})", command.reservationId(), command.guestEmail());
    }
}
