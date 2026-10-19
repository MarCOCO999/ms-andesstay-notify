package com.andesstay.ms_andesstay_notify.service;

import com.andesstay.ms_andesstay_notify.messaging.support.NonRecoverableCommandException;

/** Validaciones comunes del envelope de los comandos: un campo faltante no se arregla reintentando. */
final class CommandValidation {

    private CommandValidation() {
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new NonRecoverableCommandException(field + " es obligatorio");
        }
    }
}
