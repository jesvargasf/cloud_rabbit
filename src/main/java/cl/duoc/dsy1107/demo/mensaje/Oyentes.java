package cl.duoc.dsy1107.demo.mensaje;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.dsy1107.demo.config.RabbitConfig;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.AvisoMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.EventoMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.LogMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.PedidoMensaje;

@Component
public class Oyentes {

    private static final Logger log = LoggerFactory.getLogger(Oyentes.class);

    private final Bandeja bandeja;

    public Oyentes(Bandeja bandeja) {
        this.bandeja = bandeja;
    }

    @RabbitListener(queues = RabbitConfig.ALL_LOGS_QUEUE)
    public void monitor(LogMensaje mensaje) {
        String texto = mensaje.level() + " " + mensaje.message();
        log.info("[LISTENER] monitor cola={} {}", RabbitConfig.ALL_LOGS_QUEUE, texto);
        bandeja.anotar("monitor", texto);
    }

    @RabbitListener(queues = RabbitConfig.ERRORS_ONLY_QUEUE)
    public void alerta(LogMensaje mensaje) {
        String texto = mensaje.level() + " " + mensaje.message();
        log.info("[LISTENER] alerta cola={} {}", RabbitConfig.ERRORS_ONLY_QUEUE, texto);
        bandeja.anotar("alerta", texto);
    }

    @RabbitListener(queues = RabbitConfig.COLA_CORREO)
    public void correo(AvisoMensaje mensaje) {
        log.info("[LISTENER] correo cola={} {}", RabbitConfig.COLA_CORREO, mensaje.message());
        bandeja.anotar("correo", mensaje.message());
    }

    @RabbitListener(queues = RabbitConfig.COLA_APP)
    public void app(AvisoMensaje mensaje) {
        log.info("[LISTENER] app cola={} {}", RabbitConfig.COLA_APP, mensaje.message());
        bandeja.anotar("app", mensaje.message());
    }

    @RabbitListener(queues = RabbitConfig.COLA_PEDIDOS)
    public void pedidos(EventoMensaje mensaje) {
        log.info("[LISTENER] pedidos cola={} key={} {}", RabbitConfig.COLA_PEDIDOS, mensaje.routingKey(), mensaje.message());
        bandeja.anotar("pedidos", mensaje.routingKey() + " " + mensaje.message());
    }

    @RabbitListener(queues = RabbitConfig.COLA_CREADOS)
    public void creados(EventoMensaje mensaje) {
        log.info("[LISTENER] creados cola={} key={} {}", RabbitConfig.COLA_CREADOS, mensaje.routingKey(), mensaje.message());
        bandeja.anotar("creados", mensaje.routingKey() + " " + mensaje.message());
    }

    @RabbitListener(queues = RabbitConfig.COLA_FACTURAS)
    public void facturas(EventoMensaje mensaje) {
        log.info("[LISTENER] facturas cola={} key={} {}", RabbitConfig.COLA_FACTURAS, mensaje.routingKey(), mensaje.message());
        bandeja.anotar("facturas", mensaje.routingKey() + " " + mensaje.message());
    }

    @RabbitListener(queues = RabbitConfig.ORDERS_QUEUE, containerFactory = "manualFactory")
    public void pedido(PedidoMensaje mensaje, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (mensaje.fallar()) {
            log.info("[LISTENER] orders.queue id={} basicNack requeue=false", mensaje.id());
            bandeja.anotar("orders.queue", "nack sin requeue, id=" + mensaje.id());
            channel.basicNack(tag, false, false);
            return;
        }
        log.info("[LISTENER] orders.queue id={} basicAck", mensaje.id());
        bandeja.anotar("orders.queue", "ack, id=" + mensaje.id());
        channel.basicAck(tag, false);
    }

    @RabbitListener(queues = RabbitConfig.ORDERS_DLQ)
    public void cuarentena(PedidoMensaje mensaje) {
        log.info("[LISTENER] orders.dlq cuarentena id={} {}", mensaje.id(), mensaje.texto());
        bandeja.anotar("orders.dlq", "cuarentena id=" + mensaje.id() + " " + mensaje.texto());
    }
}
