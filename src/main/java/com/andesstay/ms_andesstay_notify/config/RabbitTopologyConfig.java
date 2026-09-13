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
 * (email, housekeeping, voucher), cada uno con su propia cola principal y su DLQ.
 * Cada cola principal acepta mensajes por dos vias: el exchange directo (comando exacto,
 * p.ej. "email.send") y el exchange topic (variantes del mismo dominio, p.ej. "email.*"
 * para email.confirm, email.reminder, etc.). Un NACK sin requeue en la cola principal
 * reenvia el mensaje al exchange muerto (cmd.dead.dlx) y de ahi a su DLQ.
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_CMD_TOPIC = "cmd.topic";
    public static final String EXCHANGE_CMD_DLX = "cmd.dead.dlx";

    public static final String QUEUE_EMAIL = "q.cmd.email";
    public static final String QUEUE_EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String ROUTING_EMAIL_DIRECT = "email.send";
    public static final String ROUTING_EMAIL_TOPIC = "email.*";
    private static final String ROUTING_EMAIL_DEAD = "email.dead";

    public static final String QUEUE_HOUSEKEEPING = "q.cmd.housekeeping";
    public static final String QUEUE_HOUSEKEEPING_DLQ = "q.cmd.housekeeping.dlq";
    public static final String ROUTING_HOUSEKEEPING_DIRECT = "housekeeping.ticket";
    public static final String ROUTING_HOUSEKEEPING_TOPIC = "housekeeping.#";
    private static final String ROUTING_HOUSEKEEPING_DEAD = "housekeeping.dead";

    public static final String QUEUE_VOUCHER = "q.cmd.voucher";
    public static final String QUEUE_VOUCHER_DLQ = "q.cmd.voucher.dlq";
    public static final String ROUTING_VOUCHER_DIRECT = "voucher.gen";
    public static final String ROUTING_VOUCHER_TOPIC = "voucher.*";
    private static final String ROUTING_VOUCHER_DEAD = "voucher.dead";

    // ---- Exchanges ----

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_CMD_TOPIC, true, false);
    }

    @Bean
    public DirectExchange cmdDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_CMD_DLX, true, false);
    }

    // ---- Flujo email ----

    @Bean
    public Queue emailQueue() {
        return mainQueue(QUEUE_EMAIL, ROUTING_EMAIL_DEAD);
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(QUEUE_EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailDirectBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with(ROUTING_EMAIL_DIRECT);
    }

    @Bean
    public Binding emailTopicBinding(Queue emailQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdTopicExchange).with(ROUTING_EMAIL_TOPIC);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, DirectExchange cmdDeadLetterExchange) {
        return BindingBuilder.bind(emailDlq).to(cmdDeadLetterExchange).with(ROUTING_EMAIL_DEAD);
    }

    // ---- Flujo housekeeping ----

    @Bean
    public Queue housekeepingQueue() {
        return mainQueue(QUEUE_HOUSEKEEPING, ROUTING_HOUSEKEEPING_DEAD);
    }

    @Bean
    public Queue housekeepingDlq() {
        return QueueBuilder.durable(QUEUE_HOUSEKEEPING_DLQ).build();
    }

    @Bean
    public Binding housekeepingDirectBinding(Queue housekeepingQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(housekeepingQueue).to(cmdDirectExchange).with(ROUTING_HOUSEKEEPING_DIRECT);
    }

    @Bean
    public Binding housekeepingTopicBinding(Queue housekeepingQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(housekeepingQueue).to(cmdTopicExchange).with(ROUTING_HOUSEKEEPING_TOPIC);
    }

    @Bean
    public Binding housekeepingDlqBinding(Queue housekeepingDlq, DirectExchange cmdDeadLetterExchange) {
        return BindingBuilder.bind(housekeepingDlq).to(cmdDeadLetterExchange).with(ROUTING_HOUSEKEEPING_DEAD);
    }

    // ---- Flujo voucher ----

    @Bean
    public Queue voucherQueue() {
        return mainQueue(QUEUE_VOUCHER, ROUTING_VOUCHER_DEAD);
    }

    @Bean
    public Queue voucherDlq() {
        return QueueBuilder.durable(QUEUE_VOUCHER_DLQ).build();
    }

    @Bean
    public Binding voucherDirectBinding(Queue voucherQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(voucherQueue).to(cmdDirectExchange).with(ROUTING_VOUCHER_DIRECT);
    }

    @Bean
    public Binding voucherTopicBinding(Queue voucherQueue, TopicExchange cmdTopicExchange) {
        return BindingBuilder.bind(voucherQueue).to(cmdTopicExchange).with(ROUTING_VOUCHER_TOPIC);
    }

    @Bean
    public Binding voucherDlqBinding(Queue voucherDlq, DirectExchange cmdDeadLetterExchange) {
        return BindingBuilder.bind(voucherDlq).to(cmdDeadLetterExchange).with(ROUTING_VOUCHER_DEAD);
    }

    private static Queue mainQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", EXCHANGE_CMD_DLX)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }
}
