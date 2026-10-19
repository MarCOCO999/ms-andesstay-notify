package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.MessagingProperties;
import com.andesstay.ms_andesstay_notify.messaging.HousekeepingTicketCommand;
import com.andesstay.ms_andesstay_notify.messaging.ProcessedEventTracker;
import com.andesstay.ms_andesstay_notify.messaging.support.CommandFailureHandler;
import com.andesstay.ms_andesstay_notify.service.HousekeepingTicketService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor de q.cmd.housekeeping. ACK solo tras crear el ticket; los errores los resuelve CommandFailureHandler. */
@Component
public class HousekeepingTicketListener {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingTicketListener.class);

    private final HousekeepingTicketService housekeepingService;
    private final ProcessedEventTracker processedEventTracker;
    private final CommandFailureHandler failureHandler;
    private final MessagingProperties.Flow flow;

    public HousekeepingTicketListener(HousekeepingTicketService housekeepingService,
                                      ProcessedEventTracker processedEventTracker,
                                      CommandFailureHandler failureHandler,
                                      MessagingProperties properties) {
        this.housekeepingService = housekeepingService;
        this.processedEventTracker = processedEventTracker;
        this.failureHandler = failureHandler;
        this.flow = properties.flows().housekeeping();
    }

    @RabbitListener(queues = "${andesstay.messaging.flows.housekeeping.queue}")
    public void onHousekeepingTicket(HousekeepingTicketCommand command, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (processedEventTracker.isProcessed(command.eventId())) {
            log.info("Evento {} ya procesado, se descarta duplicado (unidad {})", command.eventId(), command.unitId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            housekeepingService.createTicket(command);
            processedEventTracker.markProcessed(command.eventId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            failureHandler.handle(message, channel, flow, command.eventId(), ex);
        }
    }
}
