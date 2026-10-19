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

Todos los nombres de colas, exchanges y routing keys están centralizados en `application.yml` (`andesstay.messaging`), y los declara una única clase de configuración, `RabbitTopologyConfig`. Ninguna clase de negocio ni listener tiene nombres escritos a mano.

Tres flujos de comando (`email`, `housekeeping`, `voucher`), cada uno con:

- Un exchange **directo** (`cmd.direct`) para el comando exacto (p. ej. `email.send`).
- Un exchange **topic** (`cmd.topic`) para variantes del mismo dominio (p. ej. `email.*`).
- Una **cola principal** (`q.cmd.email`, `q.cmd.housekeeping`, `q.cmd.voucher`) con ACK/NACK manual.
- Una **cola de reintento** (`q.cmd.email.retry`, etc.), enlazada a `cmd.retry`, sin consumidores: su TTL (`andesstay.messaging.retry.delay-ms`, 5 s) hace de espera y, al vencer, RabbitMQ devuelve el mensaje a la cola principal por `cmd.direct`.
- Una **dead-letter queue** (`q.cmd.email.dlq`, etc.): un NACK sin *requeue* en la cola principal reenvía el mensaje al exchange muerto (`cmd.dead.dlx`) y de ahí a su DLQ. Los mensajes quedan en la DLQ para revisarlos o reprocesarlos.

`ms-andesstay-reservations` solo declara los exchanges (para poder publicar sin depender del orden de arranque); este servicio es quien declara las colas, bindings y DLQs.

```
reservations ──email.send──► cmd.direct ──► q.cmd.email ──► EmailNotificationListener
                                 ▲              │ NACK                 │ error recuperable
                                 │ TTL 5 s      ▼                      ▼
                        q.cmd.email.retry   cmd.dead.dlx ──► q.cmd.email.dlq
                                 ▲
                                 └──email.retry── cmd.retry
```

## Confirmación de mensajes y manejo de errores

Los listeners (`messaging/listener`, uno por dominio) usan **ACK manual** y delegan el procesamiento en un servicio por dominio (`service/`). Solo hacen ACK después de procesar con éxito; ante un error, `CommandFailureHandler` aplica una única regla para los tres flujos:

| Situación | Decisión | Resultado |
|---|---|---|
| Procesado con éxito | `basicAck` | Se marca el `eventId` como procesado |
| Mismo `eventId` ya procesado | `basicAck` sin reprocesar | Evita notificaciones duplicadas |
| `NonRecoverableCommandException` (comando inválido: falta `eventId`, email sin `@`, etc.) | `basicNack(requeue=false)` | Directo a la DLQ, sin reintentos |
| Cualquier otro error, con intentos disponibles | Copia a `cmd.retry` con el header `x-andesstay-attempt` + 1, y `basicAck` del original | Reintento tras 5 s |
| Cualquier otro error en el último intento (`max-attempts`, 3 por defecto) | `basicNack(requeue=false)` | A la DLQ |
| Mensaje que no es JSON válido | Rechazo del contenedor (error fatal de conversión) | A la DLQ |

Nunca se usa `requeue=true`: reencolar al instante un mensaje que acaba de fallar solo genera un bucle de reprocesos. Cada paso a la DLQ y cada reintento quedan registrados en el log con la cola, el `eventId`, el intento y el motivo.

Para demostrar los reintentos sin modificar código, `NOTIFY_SIMULATED_FAILURE_RATE` (0.0–1.0) hace que el proveedor simulado falle al azar de forma transitoria.

## Stack

- Spring Boot 4.1.1 / Java 21
- Spring AMQP / RabbitMQ (consumidor con ACK manual)

No tiene base de datos propia: el "envío" de notificaciones se simula (se registra en el log), ya que el caso no exige integración con un proveedor real de email/SMS.

## Configuración

| Variable | Descripción | Default |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP (solo expone `/actuator`) | `8090` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST` | Conexión a RabbitMQ | `localhost` / `5672` / `guest` / `guest` / `/` |
| `MESSAGING_RETRY_MAX_ATTEMPTS` | Intentos totales ante un error recuperable antes de ir a la DLQ | `3` |
| `MESSAGING_RETRY_DELAY_MS` | Espera entre intentos (TTL de las colas de reintento) | `5000` |
| `NOTIFY_SIMULATED_FAILURE_RATE` | Probabilidad de fallo transitorio simulado, para la demo | `0.0` |

## Cómo ejecutarlo en local

Requiere Java 21 y RabbitMQ levantado (ver [andesstay-infra](https://github.com/MarCOCO999/andesstay-infra)).

```bash
./mvnw spring-boot:run
./mvnw test   # CommandFailureHandlerTest y EmailNotificationListenerTest: ACK, NACK, reintento y DLQ
```

> Si una cola ya existe en el broker con otros argumentos (p. ej. creada por una versión anterior), RabbitMQ rechaza volver a declararla; en local se resuelve con `docker compose -f mq/compose.yml down -v` en andesstay-infra.

## Repos relacionados

[frontend-andesstay](https://github.com/MarCOCO999/frontend-andesstay) · [ms-andesstay-bff](https://github.com/MarCOCO999/ms-andesstay-bff) · [ms-andesstay-reservations](https://github.com/MarCOCO999/ms-andesstay-reservations) · [ms-andesstay-catalog](https://github.com/MarCOCO999/ms-andesstay-catalog) · [ms-andesstay-audit](https://github.com/MarCOCO999/ms-andesstay-audit) · [ms-andesstay-report](https://github.com/MarCOCO999/ms-andesstay-report) · [ms-andesstay-rabbit-admin](https://github.com/MarCOCO999/ms-andesstay-rabbit-admin) · [andesstay-infra](https://github.com/MarCOCO999/andesstay-infra)
