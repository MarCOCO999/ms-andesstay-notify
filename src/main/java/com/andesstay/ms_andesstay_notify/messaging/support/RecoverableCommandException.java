package com.andesstay.ms_andesstay_notify.messaging.support;

/**
 * Error transitorio (p.ej. el proveedor de email no responde): el mensaje se reintenta con
 * espera hasta agotar andesstay.messaging.retry.max-attempts, y recien ahi va a la DLQ.
 * Cualquier excepcion que no sea {@link NonRecoverableCommandException} se trata igual.
 */
public class RecoverableCommandException extends RuntimeException {

    public RecoverableCommandException(String message) {
        super(message);
    }
}
