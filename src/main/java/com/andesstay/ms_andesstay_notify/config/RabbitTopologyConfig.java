package com.andesstay.ms_andesstay_notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensajeria del caso AndesStay (seccion 8): 3 flujos de comando
 * (email, housekeeping, voucher). Todos los nombres vienen de andesstay.messaging
 * (MessagingProperties / application.yml). Cada flujo tiene tres colas:
 *
 * <ul>
 *   <li><b>Principal</b> (p.ej. q.cmd.email): recibe por el exchange directo (comando exacto,
 *   p.ej. "email.send") y por el topic (variantes, p.ej. "email.*"). Un NACK sin requeue
 *   la reenvia al exchange muerto (cmd.dead.dlx).</li>
 *   <li><b>Reintento</b> (p.ej. q.cmd.email.retry): recibe desde cmd.retry los mensajes con un
 *   error recuperable. No tiene consumidores: su TTL hace de espera y, al vencer, RabbitMQ
 *   devuelve el mensaje a la cola principal por el exchange directo.</li>
 *   <li><b>DLQ</b> (p.ej. q.cmd.email.dlq): destino final de los mensajes con error no
 *   recuperable o que agotaron sus reintentos. Se dejan ahi para revisarlos o reprocesarlos.</li>
 * </ul>
 */
@Configuration
public class RabbitTopologyConfig {

    private final MessagingProperties properties;

    public RabbitTopologyConfig(MessagingProperties properties) {
        this.properties = properties;
    }

    // ---- Exchanges ----

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(properties.exchanges().direct(), true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(properties.exchanges().topic(), true, false);
    }

    @Bean
    public DirectExchange cmdDeadLetterExchange() {
        return new DirectExchange(properties.exchanges().deadLetter(), true, false);
    }

    @Bean
    public DirectExchange cmdRetryExchange() {
        return new DirectExchange(properties.exchanges().retry(), true, false);
    }

    // ---- Flujo email ----

    @Bean
    public Queue emailQueue() {
        return mainQueue(properties.flows().email());
    }

    @Bean
    public Queue emailRetryQueue() {
        return retryQueue(properties.flows().email());
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(properties.flows().email().dlq()).build();
    }

    @Bean
    public Binding emailDirectBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with(properties.flows().email().directRoutingKey());
    }

    @Bean
    public Binding emailTopicBinding(Queue emailQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdTopicExchange).with(properties.flows().email().topicPattern());
    }

    @Bean
    public Binding emailRetryBinding(Queue emailRetryQueue, DirectExchange cmdRetryExchange) {
        return BindingBuilder.bind(emailRetryQueue).to(cmdRetryExchange).with(properties.flows().email().retryRoutingKey());
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, DirectExchange cmdDeadLetterExchange) {
        return BindingBuilder.bind(emailDlq).to(cmdDeadLetterExchange).with(properties.flows().email().deadRoutingKey());
    }

    // ---- Flujo housekeeping ----

    @Bean
    public Queue housekeepingQueue() {
        return mainQueue(properties.flows().housekeeping());
    }

    @Bean
    public Queue housekeepingRetryQueue() {
        return retryQueue(properties.flows().housekeeping());
    }

    @Bean
    public Queue housekeepingDlq() {
        return QueueBuilder.durable(properties.flows().housekeeping().dlq()).build();
    }

    @Bean
    public Binding housekeepingDirectBinding(Queue housekeepingQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(housekeepingQueue).to(cmdDirectExchange)
                .with(properties.flows().housekeeping().directRoutingKey());
    }

    @Bean
    public Binding housekeepingTopicBinding(Queue housekeepingQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(housekeepingQueue).to(cmdTopicExchange)
                .with(properties.flows().housekeeping().topicPattern());
    }

    @Bean
    public Binding housekeepingRetryBinding(Queue housekeepingRetryQueue, DirectExchange cmdRetryExchange) {
        return BindingBuilder.bind(housekeepingRetryQueue).to(cmdRetryExchange)
                .with(properties.flows().housekeeping().retryRoutingKey());
    }

    @Bean
    public Binding housekeepingDlqBinding(Queue housekeepingDlq, DirectExchange cmdDeadLetterExchange) {
        return BindingBuilder.bind(housekeepingDlq).to(cmdDeadLetterExchange)
                .with(properties.flows().housekeeping().deadRoutingKey());
    }

    // ---- Flujo voucher ----

    @Bean
    public Queue voucherQueue() {
        return mainQueue(properties.flows().voucher());
    }

    @Bean
    public Queue voucherRetryQueue() {
        return retryQueue(properties.flows().voucher());
    }

    @Bean
    public Queue voucherDlq() {
        return QueueBuilder.durable(properties.flows().voucher().dlq()).build();
    }

    @Bean
    public Binding voucherDirectBinding(Queue voucherQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(voucherQueue).to(cmdDirectExchange).with(properties.flows().voucher().directRoutingKey());
    }

    @Bean
    public Binding voucherTopicBinding(Queue voucherQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(voucherQueue).to(cmdTopicExchange).with(properties.flows().voucher().topicPattern());
    }

    @Bean
    public Binding voucherRetryBinding(Queue voucherRetryQueue, DirectExchange cmdRetryExchange) {
        return BindingBuilder.bind(voucherRetryQueue).to(cmdRetryExchange).with(properties.flows().voucher().retryRoutingKey());
    }

    @Bean
    public Binding voucherDlqBinding(Queue voucherDlq, DirectExchange cmdDeadLetterExchange) {
        return BindingBuilder.bind(voucherDlq).to(cmdDeadLetterExchange).with(properties.flows().voucher().deadRoutingKey());
    }

    private Queue mainQueue(MessagingProperties.Flow flow) {
        return QueueBuilder.durable(flow.queue())
                .withArgument("x-dead-letter-exchange", properties.exchanges().deadLetter())
                .withArgument("x-dead-letter-routing-key", flow.deadRoutingKey())
                .build();
    }

    /** Cola de espera: al vencer el TTL, el mensaje vuelve a la cola principal via cmd.direct. */
    private Queue retryQueue(MessagingProperties.Flow flow) {
        return QueueBuilder.durable(flow.retryQueue())
                .withArgument("x-message-ttl", properties.retry().delayMs())
                .withArgument("x-dead-letter-exchange", properties.exchanges().direct())
                .withArgument("x-dead-letter-routing-key", flow.directRoutingKey())
                .build();
    }
}
