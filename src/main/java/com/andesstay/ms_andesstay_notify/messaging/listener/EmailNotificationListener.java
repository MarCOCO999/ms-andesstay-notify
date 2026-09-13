package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.RabbitTopologyConfig;
import com.andesstay.ms_andesstay_notify.messaging.EmailCommand;
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
public class EmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final ProcessedEventTracker processedEventTracker;

    public EmailNotificationListener(ProcessedEventTracker processedEventTracker) {
        this.processedEventTracker = processedEventTracker;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_EMAIL)
    public void onEmailCommand(EmailCommand command, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (!processedEventTracker.markIfNew(command.eventId())) {
            log.info("Evento {} ya procesado, se descarta duplicado (email a {})", command.eventId(), command.guestEmail());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            // TODO: integrar proveedor real de email/push (SES, FCM, etc.).
            log.info("Enviando email '{}' a {} (reserva {})", command.template(), command.guestEmail(), command.reservationId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Fallo procesando EmailCommand {}, se envia a DLQ", command.eventId(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
