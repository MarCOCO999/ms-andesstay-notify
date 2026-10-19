package com.andesstay.ms_andesstay_notify.messaging.support;

import com.andesstay.ms_andesstay_notify.config.MessagingProperties;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CommandFailureHandlerTest {

    private static final long TAG = 7L;

    private final MessagingProperties.Flow flow = new MessagingProperties.Flow(
            "q.cmd.email", "q.cmd.email.dlq", "q.cmd.email.retry",
            "email.send", "email.*", "email.dead", "email.retry");
    private final MessagingProperties properties = new MessagingProperties(
            new MessagingProperties.Exchanges("cmd.direct", "cmd.topic", "cmd.dead.dlx", "cmd.retry"),
            new MessagingProperties.Retry(3, 5000),
            new MessagingProperties.Flows(flow, flow, flow));

    private RabbitTemplate rabbitTemplate;
    private Channel channel;
    private CommandFailureHandler handler;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        channel = mock(Channel.class);
        handler = new CommandFailureHandler(rabbitTemplate, properties);
    }

    @Test
    void errorNoRecuperableVaDirectoALaDlqSinReintentar() throws Exception {
        handler.handle(message(null), channel, flow, "evt-1", new NonRecoverableCommandException("guestEmail invalido"));

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), any(Boolean.class));
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void errorRecuperableSePublicaEnLaColaDeReintentoConElIntentoSiguiente() throws Exception {
        handler.handle(message(null), channel, flow, "evt-1", new RecoverableCommandException("proveedor caido"));

        ArgumentCaptor<Message> retried = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("cmd.retry"), eq("email.retry"), retried.capture());
        assertThat(CommandFailureHandler.attemptOf(retried.getValue())).isEqualTo(2);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void errorRecuperableEnElUltimoIntentoVaALaDlq() throws Exception {
        handler.handle(message(3), channel, flow, "evt-1", new IllegalStateException("timeout"));

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void siNoSePuedeProgramarElReintentoElMensajeVaALaDlq() throws Exception {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        handler.handle(message(1), channel, flow, "evt-1", new RecoverableCommandException("proveedor caido"));

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), any(Boolean.class));
    }

    private static Message message(Integer attempt) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        if (attempt != null) {
            props.setHeader(CommandFailureHandler.ATTEMPT_HEADER, attempt);
        }
        return new Message("{}".getBytes(), props);
    }
}
