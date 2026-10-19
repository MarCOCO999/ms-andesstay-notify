package com.andesstay.ms_andesstay_notify.messaging.support;

/**
 * Error que no se arregla reintentando (comando incompleto o invalido): el mensaje va
 * directo a la DLQ de su flujo, sin pasar por la cola de reintento.
 */
public class NonRecoverableCommandException extends RuntimeException {

    public NonRecoverableCommandException(String message) {
        super(message);
    }
}
