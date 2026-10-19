package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.MessagingProperties;
import com.andesstay.ms_andesstay_notify.messaging.ProcessedEventTracker;
import com.andesstay.ms_andesstay_notify.messaging.VoucherCommand;
import com.andesstay.ms_andesstay_notify.messaging.support.CommandFailureHandler;
import com.andesstay.ms_andesstay_notify.service.VoucherGenerationService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor de q.cmd.voucher. ACK solo tras generar el voucher; los errores los resuelve CommandFailureHandler. */
@Component
public class VoucherGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(VoucherGenerationListener.class);

    private final VoucherGenerationService voucherService;
    private final ProcessedEventTracker processedEventTracker;
    private final CommandFailureHandler failureHandler;
    private final MessagingProperties.Flow flow;

    public VoucherGenerationListener(VoucherGenerationService voucherService,
                                     ProcessedEventTracker processedEventTracker,
                                     CommandFailureHandler failureHandler,
                                     MessagingProperties properties) {
        this.voucherService = voucherService;
        this.processedEventTracker = processedEventTracker;
        this.failureHandler = failureHandler;
        this.flow = properties.flows().voucher();
    }

    @RabbitListener(queues = "${andesstay.messaging.flows.voucher.queue}")
    public void onVoucherCommand(VoucherCommand command, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (processedEventTracker.isProcessed(command.eventId())) {
            log.info("Evento {} ya procesado, se descarta duplicado (reserva {})", command.eventId(), command.reservationId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            voucherService.generate(command);
            processedEventTracker.markProcessed(command.eventId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            failureHandler.handle(message, channel, flow, command.eventId(), ex);
        }
    }
}
