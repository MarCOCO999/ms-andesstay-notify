package com.andesstay.ms_andesstay_notify.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    /**
     * Al declarar este bean, Spring Boot lo usa automaticamente tanto en el RabbitTemplate
     * como en el listener container factory, para que los @RabbitListener reciban
     * directamente los records (EmailCommand, HousekeepingTicketCommand, VoucherCommand)
     * en vez de bytes crudos. Spring Boot 4 usa Jackson 3 (paquete tools.jackson) por
     * defecto, por eso el converter es JacksonJsonMessageConverter y no el
     * Jackson2JsonMessageConverter legado (ese requiere Jackson 2 classico en el classpath).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
