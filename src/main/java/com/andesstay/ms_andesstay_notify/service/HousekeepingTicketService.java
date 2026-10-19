package com.andesstay.ms_andesstay_notify.service;

import com.andesstay.ms_andesstay_notify.messaging.HousekeepingTicketCommand;
import com.andesstay.ms_andesstay_notify.messaging.support.NonRecoverableCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Tickets de preparacion/limpieza de una unidad antes del check-in. */
@Service
public class HousekeepingTicketService {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingTicketService.class);

    private final SimulatedProvider provider;

    public HousekeepingTicketService(SimulatedProvider provider) {
        this.provider = provider;
    }

    public void createTicket(HousekeepingTicketCommand command) {
        CommandValidation.requireText(command.eventId(), "eventId");
        CommandValidation.requireText(command.reservationId(), "reservationId");
        if (command.unitId() == null) {
            throw new NonRecoverableCommandException("unitId es obligatorio");
        }

        // TODO: crear el ticket en el sistema de housekeeping (interno o de terceros).
        provider.call("Sistema de housekeeping");
        log.info("Ticket de preparacion para unidad {} (reserva {}): {}",
                command.unitId(), command.reservationId(), command.instructions());
    }
}
