package cl.duoc.dsy1107.demo.mensaje;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

@Component
public class Bandeja {

    private final CopyOnWriteArrayList<Registro> registros = new CopyOnWriteArrayList<>();

    public void anotar(String listener, String texto) {
        registros.add(new Registro(listener, texto, Instant.now()));
    }

    public List<Registro> todos() {
        return List.copyOf(registros);
    }

    public void limpiar() {
        registros.clear();
    }

    public record Registro(String listener, String texto, Instant cuando) {
    }
}
