package com.andesstay.ms_andesstay_notify.service;

import com.andesstay.ms_andesstay_notify.messaging.EmailCommand;
import com.andesstay.ms_andesstay_notify.messaging.support.NonRecoverableCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Envio de emails al huesped (confirmacion, recordatorio de check-in, checkout). */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final SimulatedProvider provider;

    public EmailNotificationService(SimulatedProvider provider) {
        this.provider = provider;
    }

    public void send(EmailCommand command) {
        CommandValidation.requireText(command.eventId(), "eventId");
        CommandValidation.requireText(command.reservationId(), "reservationId");
        CommandValidation.requireText(command.template(), "template");
        if (command.guestEmail() == null || !command.guestEmail().contains("@")) {
            throw new NonRecoverableCommandException("guestEmail invalido: '" + command.guestEmail() + "'");
        }

        // TODO: integrar proveedor real de email/push (SES, FCM, etc.).
        provider.call("Proveedor de email");
        log.info("Enviando email '{}' a {} (reserva {})", command.template(), command.guestEmail(), command.reservationId());
    }
}
