package com.andesstay.ms_andesstay_notify.service;

import com.andesstay.ms_andesstay_notify.messaging.support.RecoverableCommandException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Proveedor externo simulado (email, ticketera, generador de PDF): el caso no exige integrar
 * uno real. Con andesstay.notify.simulated-transient-failure-rate > 0 falla al azar de forma
 * transitoria, para poder demostrar los reintentos y la DLQ sin tocar el codigo.
 */
@Component
public class SimulatedProvider {

    private final double transientFailureRate;

    public SimulatedProvider(@Value("${andesstay.notify.simulated-transient-failure-rate:0.0}") double transientFailureRate) {
        this.transientFailureRate = transientFailureRate;
    }

    public void call(String providerName) {
        if (transientFailureRate > 0 && ThreadLocalRandom.current().nextDouble() < transientFailureRate) {
            throw new RecoverableCommandException(providerName + " no disponible (fallo transitorio simulado)");
        }
    }
}
