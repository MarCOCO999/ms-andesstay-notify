package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.RabbitTopologyConfig;
import com.andesstay.ms_andesstay_notify.messaging.ProcessedEventTracker;
import com.andesstay.ms_andesstay_notify.messaging.VoucherCommand;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class VoucherGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(VoucherGenerationListener.class);

    private final ProcessedEventTracker processedEventTracker;

    public VoucherGenerationListener(ProcessedEventTracker processedEventTracker) {
        this.processedEventTracker = processedEventTracker;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_VOUCHER)
    public void onVoucherCommand(VoucherCommand command, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        if (!processedEventTracker.markIfNew(command.eventId())) {
            log.info("Evento {} ya procesado, se descarta duplicado (reserva {})", command.eventId(), command.reservationId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            // TODO: generar el PDF del voucher/boleta y enviarlo o dejarlo disponible para el huesped.
            log.info("Generando voucher PDF para la reserva {} ({})", command.reservationId(), command.guestEmail());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Fallo procesando VoucherCommand {}, se envia a DLQ", command.eventId(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
