package com.andesstay.ms_andesstay_notify.messaging.listener;

import com.andesstay.ms_andesstay_notify.config.MessagingProperties;
import com.andesstay.ms_andesstay_notify.messaging.EmailCommand;
import com.andesstay.ms_andesstay_notify.messaging.ProcessedEventTracker;
import com.andesstay.ms_andesstay_notify.messaging.support.CommandFailureHandler;
import com.andesstay.ms_andesstay_notify.messaging.support.NonRecoverableCommandException;
import com.andesstay.ms_andesstay_notify.service.EmailNotificationService;
import com.andesstay.ms_andesstay_notify.service.SimulatedProvider;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotificationListenerTest {

    private static final long TAG = 42L;

    private final MessagingProperties.Flow flow = new MessagingProperties.Flow(
            "q.cmd.email", "q.cmd.email.dlq", "q.cmd.email.retry",
            "email.send", "email.*", "email.dead", "email.retry");
    private final MessagingProperties properties = new MessagingProperties(
            new MessagingProperties.Exchanges("cmd.direct", "cmd.topic", "cmd.dead.dlx", "cmd.retry"),
            new MessagingProperties.Retry(3, 5000),
            new MessagingProperties.Flows(flow, flow, flow));

    private ProcessedEventTracker tracker;
    private CommandFailureHandler failureHandler;
    private Channel channel;
    private EmailNotificationListener listener;

    @BeforeEach
    void setUp() {
        tracker = new ProcessedEventTracker();
        failureHandler = mock(CommandFailureHandler.class);
        channel = mock(Channel.class);
        EmailNotificationService service = new EmailNotificationService(new SimulatedProvider(0.0));
        listener = new EmailNotificationListener(service, tracker, failureHandler, properties);
    }

    @Test
    void comandoValidoHaceAckYQuedaMarcadoComoProcesado() throws Exception {
        listener.onEmailCommand(command("evt-1", "huesped@correo.cl"), message(), channel);

        verify(channel).basicAck(TAG, false);
        assertThat(tracker.isProcessed("evt-1")).isTrue();
    }

    @Test
    void comandoDuplicadoHaceAckSinVolverAEnviar() throws Exception {
        tracker.markProcessed("evt-1");

        listener.onEmailCommand(command("evt-1", "huesped@correo.cl"), message(), channel);

        verify(channel).basicAck(TAG, false);
        verify(failureHandler, never()).handle(any(), any(), any(), any(), any());
    }

    @Test
    void emailInvalidoSeDelegaComoErrorNoRecuperableYNoSeMarcaProcesado() throws Exception {
        Message message = message();

        listener.onEmailCommand(command("evt-2", "sin-arroba"), message, channel);

        verify(failureHandler).handle(eq(message), eq(channel), eq(flow), eq("evt-2"), isA(NonRecoverableCommandException.class));
        verify(channel, never()).basicAck(TAG, false);
        assertThat(tracker.isProcessed("evt-2")).isFalse();
    }

    private static EmailCommand command(String eventId, String email) {
        return new EmailCommand("email.confirmacion", eventId, Instant.now(), "trace", "corr",
                "15", email, "confirmacion-reserva", Map.of());
    }

    private static Message message() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        return new Message("{}".getBytes(), props);
    }
}
