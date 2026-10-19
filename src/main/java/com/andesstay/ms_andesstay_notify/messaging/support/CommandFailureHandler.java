package com.andesstay.ms_andesstay_notify.messaging.support;

import com.andesstay.ms_andesstay_notify.config.MessagingProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide que hacer con un comando cuyo procesamiento fallo. Es la unica regla de error de
 * los tres listeners:
 *
 * <ol>
 *   <li>{@link NonRecoverableCommandException} (comando invalido): NACK sin requeue, el mensaje
 *   va a la DLQ del flujo. Reintentar no lo arreglaria.</li>
 *   <li>Cualquier otro error, mientras queden intentos: se publica una copia en la cola de
 *   reintento (via cmd.retry) con el header {@value #ATTEMPT_HEADER} incrementado y se hace ACK
 *   del original. El TTL de esa cola hace de espera antes de volver a la cola principal.</li>
 *   <li>Otro error en el ultimo intento: NACK sin requeue, el mensaje va a la DLQ.</li>
 * </ol>
 *
 * Nunca se usa NACK con requeue=true: reencolar al instante un mensaje que acaba de fallar
 * solo genera un bucle de reprocesos sin espera.
 */
@Component
public class CommandFailureHandler {

    /** Numero de intento del mensaje (1 = entrega original). */
    public static final String ATTEMPT_HEADER = "x-andesstay-attempt";

    private static final Logger log = LoggerFactory.getLogger(CommandFailureHandler.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public CommandFailureHandler(RabbitTemplate rabbitTemplate, MessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void handle(Message message, Channel channel, MessagingProperties.Flow flow,
                       String eventId, Exception error) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        int attempt = attemptOf(message);
        int maxAttempts = properties.retry().maxAttempts();

        if (error instanceof NonRecoverableCommandException) {
            log.error("DLQ {} <- evento {} (intento {}): error no recuperable: {}",
                    flow.dlq(), eventId, attempt, error.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        if (attempt >= maxAttempts) {
            log.error("DLQ {} <- evento {}: agoto los {} intentos. Ultimo error: {}",
                    flow.dlq(), eventId, maxAttempts, error.getMessage(), error);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        Message retry = MessageBuilder.fromMessage(message)
                .setHeader(ATTEMPT_HEADER, attempt + 1)
                .build();
        try {
            rabbitTemplate.send(properties.exchanges().retry(), flow.retryRoutingKey(), retry);
        } catch (AmqpException publishError) {
            // Sin poder programar el reintento, la DLQ es el unico lugar donde el mensaje no se pierde.
            log.error("DLQ {} <- evento {}: no se pudo programar el reintento ({}). Error original: {}",
                    flow.dlq(), eventId, publishError.getMessage(), error.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        channel.basicAck(deliveryTag, false);
        log.warn("Evento {} fallo en el intento {}/{} ({}); se reintenta en {} ms via {}",
                eventId, attempt, maxAttempts, error.getMessage(), properties.retry().delayMs(), flow.retryQueue());
    }

    static int attemptOf(Message message) {
        Object header = message.getMessageProperties().getHeader(ATTEMPT_HEADER);
        return header instanceof Number number ? number.intValue() : 1;
    }
}
