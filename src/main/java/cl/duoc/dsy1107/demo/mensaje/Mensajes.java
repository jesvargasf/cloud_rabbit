package cl.duoc.dsy1107.demo.mensaje;

public final class Mensajes {

    private Mensajes() {
    }

    public record LogMensaje(String level, String message) {
    }

    public record AvisoMensaje(String message) {
    }

    public record EventoMensaje(String routingKey, String message) {
    }

    public record PedidoMensaje(String id, boolean fallar, String texto) {
    }
}
