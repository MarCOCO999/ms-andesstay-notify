# ms-andesstay-notify

Microservicio de notificaciones del proyecto **AndesStay** (caso DSY1107 – Desarrollo Cloud Native I, Duoc UC): consume comandos de **RabbitMQ** publicados por `ms-andesstay-reservations` y simula el envío de emails, vouchers y tickets de housekeeping.

## Rol en la arquitectura

No expone ninguna API HTTP para el frontend (no tiene ruta en el BFF): es puramente un **consumidor asíncrono**. `ms-andesstay-reservations` publica un comando cada vez que una reserva cambia de estado, y este servicio lo procesa:

| Transición de la reserva | Comando(s) que dispara |
|---|---|
| `CREADA → CONFIRMADA` | Email de confirmación + generación de voucher |
| `CONFIRMADA → CHECKIN_PENDIENTE` | Ticket de housekeeping para preparar la unidad + email de recordatorio de check-in |
| `EN_ESTADIA → CHECKOUT` | Email de checkout |

## Topología de mensajería

Tres flujos de comando (`email`, `housekeeping`, `voucher`), cada uno con:

- Un exchange **directo** (`cmd.direct`) para el comando exacto (p. ej. `email.send`).
- Un exchange **topic** (`cmd.topic`) para variantes del mismo dominio (p. ej. `email.*`).
- Una cola principal (`q.cmd.email`, `q.cmd.housekeeping`, `q.cmd.voucher`) con ACK/NACK manual.
- Una **dead-letter queue** propia (`q.cmd.email.dlq`, etc.): un NACK sin *requeue* en la cola principal reenvía el mensaje al exchange muerto (`cmd.dead.dlx`) y de ahí a su DLQ, para no perder comandos que fallan repetidamente.

`ms-andesstay-reservations` solo declara los exchanges (para poder publicar sin depender del orden de arranque); este servicio es quien declara las colas, bindings y DLQs.

## Stack

- Spring Boot 4.1.1 / Java 21
- Spring AMQP / RabbitMQ (consumidor con ACK manual)

No tiene base de datos propia: el "envío" de notificaciones se simula (se registra en el log), ya que el caso no exige integración con un proveedor real de email/SMS.

## Configuración

| Variable | Descripción | Default |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP (solo expone `/actuator`) | `8090` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST` | Conexión a RabbitMQ | `localhost` / `5672` / `guest` / `guest` / `/` |

## Cómo ejecutarlo en local

Requiere Java 21 y RabbitMQ levantado (ver [andesstay-infra](https://github.com/MarCOCO999/andesstay-infra)).

```bash
./mvnw spring-boot:run
```

## Repos relacionados

[frontend-andesstay](https://github.com/MarCOCO999/frontend-andesstay) · [ms-andesstay-bff](https://github.com/MarCOCO999/ms-andesstay-bff) · [ms-andesstay-reservations](https://github.com/MarCOCO999/ms-andesstay-reservations) · [ms-andesstay-catalog](https://github.com/MarCOCO999/ms-andesstay-catalog) · [ms-andesstay-audit](https://github.com/MarCOCO999/ms-andesstay-audit) · [ms-andesstay-report](https://github.com/MarCOCO999/ms-andesstay-report) · [andesstay-infra](https://github.com/MarCOCO999/andesstay-infra)
