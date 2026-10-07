package cl.duoc.dsy1107.demo.api;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.demo.config.RabbitConfig;
import cl.duoc.dsy1107.demo.mensaje.Bandeja;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.AvisoMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.EventoMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.LogMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.PedidoMensaje;

@RestController
@RequestMapping("/api")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private static final Set<String> NIVELES = Set.of("INFO", "WARNING", "ERROR");

    private final RabbitTemplate rabbitTemplate;
    private final Bandeja bandeja;

    public DemoController(RabbitTemplate rabbitTemplate, Bandeja bandeja) {
        this.rabbitTemplate = rabbitTemplate;
        this.bandeja = bandeja;
    }

    @GetMapping
    public Map<String, String> ayuda() {
        return Map.of(
                "logs", "POST /api/logs {\"level\":\"ERROR\",\"message\":\"fallo al cobrar\"}",
                "avisos", "POST /api/avisos {\"message\":\"pedido listo\"}",
                "eventos", "POST /api/eventos {\"routingKey\":\"pedido.creado\",\"message\":\"pedido 18\"}",
                "pedidos", "POST /api/pedidos {\"id\":\"18\",\"fallar\":true}",
                "espera", "POST /api/pedidos/espera {\"id\":\"19\"}  (cae en la DLQ a los 8 segundos)",
                "recibidos", "GET /api/recibidos"
        );
    }

    @PostMapping("/logs")
    public ResponseEntity<?> publicarLog(@RequestBody LogRequest request) {
        String level = request.level() == null ? "" : request.level().trim();
        if (!NIVELES.contains(level)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "level tiene que ser INFO, WARNING o ERROR"));
        }
        String texto = texto(request.message());
        log.info("[PUBLICA] exchange={} routingKey={} {}", RabbitConfig.LOGS_EXCHANGE, level, texto);
        rabbitTemplate.convertAndSend(RabbitConfig.LOGS_EXCHANGE, level, new LogMensaje(level, texto));
        return ResponseEntity.accepted().body(Map.of(
                "exchange", RabbitConfig.LOGS_EXCHANGE,
                "routingKey", level,
                "message", texto));
    }

    @PostMapping("/avisos")
    public ResponseEntity<?> publicarAviso(@RequestBody AvisoRequest request) {
        String texto = texto(request.message());
        log.info("[PUBLICA] exchange={} routingKey=(vacia) {}", RabbitConfig.AVISOS_EXCHANGE, texto);
        rabbitTemplate.convertAndSend(RabbitConfig.AVISOS_EXCHANGE, "", new AvisoMensaje(texto));
        return ResponseEntity.accepted().body(Map.of(
                "exchange", RabbitConfig.AVISOS_EXCHANGE,
                "routingKey", "",
                "message", texto));
    }

    @PostMapping("/eventos")
    public ResponseEntity<?> publicarEvento(@RequestBody EventoRequest request) {
        String routingKey = request.routingKey() == null ? "" : request.routingKey().trim();
        if (routingKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "routingKey es obligatoria, por ejemplo pedido.creado"));
        }
        String texto = texto(request.message());
        log.info("[PUBLICA] exchange={} routingKey={} {}", RabbitConfig.EVENTOS_EXCHANGE, routingKey, texto);
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTOS_EXCHANGE, routingKey, new EventoMensaje(routingKey, texto));
        return ResponseEntity.accepted().body(Map.of(
                "exchange", RabbitConfig.EVENTOS_EXCHANGE,
                "routingKey", routingKey,
                "message", texto));
    }

    @PostMapping("/pedidos")
    public ResponseEntity<?> publicarPedido(@RequestBody PedidoRequest request) {
        String id = request.id() == null ? "" : request.id().trim();
        if (id.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "id es obligatorio"));
        }
        boolean fallar = request.fallar();
        String texto = fallar ? "el trabajo falla" : "el trabajo sale bien";
        log.info("[PUBLICA] exchange={} routingKey={} id={} fallar={}",
                RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDER_CREATED, id, fallar);
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDER_CREATED,
                new PedidoMensaje(id, fallar, texto));
        return ResponseEntity.accepted().body(Map.of(
                "exchange", RabbitConfig.ORDERS_EXCHANGE,
                "routingKey", RabbitConfig.ORDER_CREATED,
                "id", id,
                "fallar", fallar));
    }

    @PostMapping("/pedidos/espera")
    public ResponseEntity<?> publicarEspera(@RequestBody EsperaRequest request) {
        String id = request.id() == null ? "" : request.id().trim();
        if (id.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "id es obligatorio"));
        }
        String texto = "nadie consume esta cola; a los 8 segundos pasa al DLX";
        log.info("[PUBLICA] exchange={} routingKey={} id={} ttl=8s",
                RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDER_ESPERA, id);
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDER_ESPERA,
                new PedidoMensaje(id, false, texto));
        return ResponseEntity.accepted().body(Map.of(
                "exchange", RabbitConfig.ORDERS_EXCHANGE,
                "routingKey", RabbitConfig.ORDER_ESPERA,
                "id", id,
                "esperaSegundos", 8));
    }

    @GetMapping("/recibidos")
    public Map<String, Object> recibidos() {
        return Map.of("recibidos", bandeja.todos());
    }

    @DeleteMapping("/recibidos")
    public Map<String, String> limpiar() {
        bandeja.limpiar();
        return Map.of("estado", "bandeja vacia");
    }

    private static String texto(String message) {
        if (message == null || message.isBlank()) {
            return "(sin texto)";
        }
        return message.trim();
    }

    public record LogRequest(String level, String message) {
    }

    public record AvisoRequest(String message) {
    }

    public record EventoRequest(String routingKey, String message) {
    }

    public record PedidoRequest(String id, boolean fallar) {
    }

    public record EsperaRequest(String id) {
    }
}
