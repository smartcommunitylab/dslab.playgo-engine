package it.smartcommunitylab.playandgo.engine.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConf {

    public static final String validateTripRequest = "playgo-vt-request";
    public static final String validateCampaignTripRequest = "playgo-campaign-vt-request";
    public static final String invalidateCampaignTripRequest = "playgo-campaign-invt-request";
    public static final String updateCampaignTripRequest = "playgo-campaign-updt-request";
    public static final String revalidateCampaignTripRequest = "playgo-campaign-revt-request";
    public static final String callWebhookRequest = "playgo-campaign-webhook-request";
    
    @Value("${rabbitmq_ge.geExchangeName}")
    private String geExchangeName;    
        
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
    
    @Bean("gameExchange")
    DirectExchange gameExchange() {
        DirectExchange exchange = new DirectExchange(geExchangeName, true, false);
        return exchange;
    }
    
    @Bean(validateTripRequest)
    public Queue validateTripRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(validateTripRequest, true, false, false, args);
        return queue;
    }
    
    @Bean(validateCampaignTripRequest)
    public Queue validateCampaignTripRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(validateCampaignTripRequest, true, false, false, args);
        return queue;
    }
    
    @Bean(invalidateCampaignTripRequest)
    public Queue invalidateCampaignTripRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(invalidateCampaignTripRequest, true, false, false, args);
        return queue;
    }

    @Bean(updateCampaignTripRequest)
    public Queue updateCampaignTripRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(updateCampaignTripRequest, true, false, false, args);
        return queue;
    }
    
    @Bean(revalidateCampaignTripRequest)
    public Queue revalidateCampaignTripRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(revalidateCampaignTripRequest, true, false, false, args);
        return queue;
    }
    
    @Bean(callWebhookRequest)
    public Queue callWebhookRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        Queue queue = new Queue(callWebhookRequest, true, false, false, args);
        return queue;
    }

}
