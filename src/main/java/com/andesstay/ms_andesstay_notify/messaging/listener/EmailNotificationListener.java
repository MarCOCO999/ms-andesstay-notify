package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.MessagingProperties;
import com.andesstay.ms_andesstay_notify.messaging.EmailCommand;
import com.andesstay.ms_andesstay_notify.messaging.ProcessedEventTracker;
import com.andesstay.ms_andesstay_notify.messaging.support.CommandFailureHandler;
import com.andesstay.ms_andesstay_notify.service.EmailNotificationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor de q.cmd.email. ACK solo tras enviar con exito; los errores los resuelve CommandFailureHandler. */
@Component
public class EmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final EmailNotificationService emailService;
    private final ProcessedEventTracker processedEventTracker;
    private final CommandFailureHandler failureHandler;
    private final MessagingProperties.Flow flow;

    public EmailNotificationListener(EmailNotificationService emailService,
                                     ProcessedEventTracker processedEventTracker,
                                     CommandFailureHandler failureHandler,
                                     MessagingProperties properties) {
        this.emailService = emailService;
        this.processedEventTracker = processedEventTracker;
        this.failureHandler = failureHandler;
        this.flow = properties.flows().email();
    }

    @RabbitListener(queues = "${andesstay.messaging.flows.email.queue}")
    public void onEmailCommand(EmailCommand command, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (processedEventTracker.isProcessed(command.eventId())) {
            log.info("Evento {} ya procesado, se descarta duplicado (email a {})", command.eventId(), command.guestEmail());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            emailService.send(command);
            processedEventTracker.markProcessed(command.eventId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            failureHandler.handle(message, channel, flow, command.eventId(), ex);
        }
    }
}
