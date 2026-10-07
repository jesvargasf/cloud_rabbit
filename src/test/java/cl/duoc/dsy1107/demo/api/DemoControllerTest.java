package cl.duoc.dsy1107.demo.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.dsy1107.demo.mensaje.Bandeja;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.AvisoMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.EventoMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.LogMensaje;
import cl.duoc.dsy1107.demo.mensaje.Mensajes.PedidoMensaje;

@WebMvcTest(DemoController.class)
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private Bandeja bandeja;

    @Test
    void infoSaleConRoutingKeyInfo() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"INFO\",\"message\":\"pedido creado\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.routingKey").value("INFO"));

        verify(rabbitTemplate).convertAndSend(eq("logs_direct_exchange"), eq("INFO"), any(LogMensaje.class));
    }

    @Test
    void errorSaleConRoutingKeyError() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"ERROR\",\"message\":\"fallo al cobrar\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.routingKey").value("ERROR"));

        verify(rabbitTemplate).convertAndSend(eq("logs_direct_exchange"), eq("ERROR"), any(LogMensaje.class));
    }

    @Test
    void nivelDistintoDeLosTresSeRechaza() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"error\",\"message\":\"minuscula\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void avisoPublicaEnElFanoutSinRoutingKey() throws Exception {
        mockMvc.perform(post("/api/avisos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"pedido listo\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.exchange").value("avisos"));

        verify(rabbitTemplate).convertAndSend(eq("avisos"), eq(""), any(AvisoMensaje.class));
    }

    @Test
    void eventoPublicaLaRoutingKeyDelTopic() throws Exception {
        mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routingKey\":\"pedido.creado\",\"message\":\"pedido 18\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.routingKey").value("pedido.creado"));

        verify(rabbitTemplate).convertAndSend(eq("eventos"), eq("pedido.creado"), any(EventoMensaje.class));
    }

    @Test
    void pedidoQueFallaPublicaEnOrderCreated() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"19\",\"fallar\":true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.routingKey").value("order.created"));

        verify(rabbitTemplate).convertAndSend(eq("orders.exchange"), eq("order.created"), any(PedidoMensaje.class));
    }

    @Test
    void esperaPublicaEnLaColaSinListener() throws Exception {
        mockMvc.perform(post("/api/pedidos/espera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"20\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.routingKey").value("order.espera"));

        verify(rabbitTemplate).convertAndSend(eq("orders.exchange"), eq("order.espera"), any(PedidoMensaje.class));
    }
}
