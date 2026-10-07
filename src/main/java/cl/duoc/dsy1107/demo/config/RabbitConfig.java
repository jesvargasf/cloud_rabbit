package cl.duoc.dsy1107.demo.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String LOGS_EXCHANGE = "logs_direct_exchange";
    public static final String ALL_LOGS_QUEUE = "all_logs_queue";
    public static final String ERRORS_ONLY_QUEUE = "errors_only_queue";

    public static final String AVISOS_EXCHANGE = "avisos";
    public static final String COLA_CORREO = "cola_correo";
    public static final String COLA_APP = "cola_app";

    public static final String EVENTOS_EXCHANGE = "eventos";
    public static final String COLA_PEDIDOS = "cola_pedidos";
    public static final String COLA_CREADOS = "cola_creados";
    public static final String COLA_FACTURAS = "cola_facturas";

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDERS_QUEUE = "orders.queue";
    public static final String ORDERS_ESPERA_QUEUE = "orders.espera";
    public static final String ORDERS_DLX = "orders.dlx";
    public static final String ORDERS_DLQ = "orders.dlq";

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_ESPERA = "order.espera";

    @Bean
    MessageConverter jsonConverter() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("cl.duoc.dsy1107.demo.mensaje");
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    SimpleRabbitListenerContainerFactory manualFactory(ConnectionFactory connectionFactory,
                                                       MessageConverter jsonConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    DirectExchange logsDirectExchange() {
        return new DirectExchange(LOGS_EXCHANGE);
    }

    @Bean
    Queue allLogsQueue() {
        return new Queue(ALL_LOGS_QUEUE, true);
    }

    @Bean
    Queue errorsOnlyQueue() {
        return new Queue(ERRORS_ONLY_QUEUE, true);
    }

    @Bean
    Binding allLogsInfo(Queue allLogsQueue, DirectExchange logsDirectExchange) {
        return BindingBuilder.bind(allLogsQueue).to(logsDirectExchange).with("INFO");
    }

    @Bean
    Binding allLogsWarning(Queue allLogsQueue, DirectExchange logsDirectExchange) {
        return BindingBuilder.bind(allLogsQueue).to(logsDirectExchange).with("WARNING");
    }

    @Bean
    Binding allLogsError(Queue allLogsQueue, DirectExchange logsDirectExchange) {
        return BindingBuilder.bind(allLogsQueue).to(logsDirectExchange).with("ERROR");
    }

    @Bean
    Binding errorsOnly(Queue errorsOnlyQueue, DirectExchange logsDirectExchange) {
        return BindingBuilder.bind(errorsOnlyQueue).to(logsDirectExchange).with("ERROR");
    }

    @Bean
    FanoutExchange avisos() {
        return new FanoutExchange(AVISOS_EXCHANGE);
    }

    @Bean
    Queue colaCorreo() {
        return new Queue(COLA_CORREO, true);
    }

    @Bean
    Queue colaApp() {
        return new Queue(COLA_APP, true);
    }

    @Bean
    Binding correoBinding(Queue colaCorreo, FanoutExchange avisos) {
        return BindingBuilder.bind(colaCorreo).to(avisos);
    }

    @Bean
    Binding appBinding(Queue colaApp, FanoutExchange avisos) {
        return BindingBuilder.bind(colaApp).to(avisos);
    }

    @Bean
    TopicExchange eventos() {
        return new TopicExchange(EVENTOS_EXCHANGE);
    }

    @Bean
    Queue colaPedidos() {
        return new Queue(COLA_PEDIDOS, true);
    }

    @Bean
    Queue colaCreados() {
        return new Queue(COLA_CREADOS, true);
    }

    @Bean
    Queue colaFacturas() {
        return new Queue(COLA_FACTURAS, true);
    }

    @Bean
    Binding pedidosBinding(Queue colaPedidos, TopicExchange eventos) {
        return BindingBuilder.bind(colaPedidos).to(eventos).with("pedido.*");
    }

    @Bean
    Binding creadosBinding(Queue colaCreados, TopicExchange eventos) {
        return BindingBuilder.bind(colaCreados).to(eventos).with("*.creado");
    }

    @Bean
    Binding facturasBinding(Queue colaFacturas, TopicExchange eventos) {
        return BindingBuilder.bind(colaFacturas).to(eventos).with("factura.#");
    }

    @Bean
    DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE);
    }

    @Bean
    FanoutExchange ordersDlx() {
        return new FanoutExchange(ORDERS_DLX);
    }

    @Bean
    Queue ordersQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDERS_DLX);
        args.put("x-message-ttl", 30_000);
        return new Queue(ORDERS_QUEUE, true, false, false, args);
    }

    @Bean
    Queue ordersEspera() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDERS_DLX);
        args.put("x-message-ttl", 8_000);
        return new Queue(ORDERS_ESPERA_QUEUE, true, false, false, args);
    }

    @Bean
    Queue ordersDlq() {
        return new Queue(ORDERS_DLQ, true);
    }

    @Bean
    Binding orderCreated(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue).to(ordersExchange).with(ORDER_CREATED);
    }

    @Bean
    Binding orderEspera(Queue ordersEspera, DirectExchange ordersExchange) {
        return BindingBuilder.bind(ordersEspera).to(ordersExchange).with(ORDER_ESPERA);
    }

    @Bean
    Binding dlqBinding(Queue ordersDlq, FanoutExchange ordersDlx) {
        return BindingBuilder.bind(ordersDlq).to(ordersDlx);
    }
}
