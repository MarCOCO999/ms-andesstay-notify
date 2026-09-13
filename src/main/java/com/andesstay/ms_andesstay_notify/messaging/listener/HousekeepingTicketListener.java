package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.RabbitTopologyConfig;
import com.andesstay.ms_andesstay_notify.messaging.HousekeepingTicketCommand;
import com.andesstay.ms_andesstay_notify.messaging.ProcessedEventTracker;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HousekeepingTicketListener {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingTicketListener.class);

    private final ProcessedEventTracker processedEventTracker;

    public HousekeepingTicketListener(ProcessedEventTracker processedEventTracker) {
        this.processedEventTracker = processedEventTracker;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_HOUSEKEEPING)
    public void onHousekeepingTicket(HousekeepingTicketCommand command, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (!processedEventTracker.markIfNew(command.eventId())) {
            log.info("Evento {} ya procesado, se descarta duplicado (unidad {})", command.eventId(), command.unitId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            // TODO: crear el ticket en el sistema de housekeeping (interno o de terceros).
            log.info("Ticket de preparacion para unidad {} (reserva {}): {}",
                    command.unitId(), command.reservationId(), command.instructions());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Fallo procesando HousekeepingTicketCommand {}, se envia a DLQ", command.eventId(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
