package com.andesstay.ms_andesstay_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de la topologia de mensajeria, leidos desde andesstay.messaging en application.yml.
 * Es la unica fuente de nombres de colas, exchanges y routing keys del servicio.
 */
@ConfigurationProperties(prefix = "andesstay.messaging")
public record MessagingProperties(Exchanges exchanges, Retry retry, Flows flows) {

    public record Exchanges(String direct, String topic, String deadLetter, String retry) {
    }

    /** maxAttempts cuenta el intento original: 3 = 1 original + 2 reintentos. */
    public record Retry(int maxAttempts, long delayMs) {
    }

    public record Flows(Flow email, Flow housekeeping, Flow voucher) {
    }

    /**
     * Un flujo de comando: su cola principal, su DLQ, su cola de reintento y las routing keys
     * con que se enlaza a cada exchange.
     */
    public record Flow(
            String queue,
            String dlq,
            String retryQueue,
            String directRoutingKey,
            String topicPattern,
            String deadRoutingKey,
            String retryRoutingKey
    ) {
    }
}
