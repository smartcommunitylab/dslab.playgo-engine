package it.smartcommunitylab.playandgo.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

/**
 * Configuration for MongoDB custom converters.
 * Registers converters for handling type-specific Campaign configuration deserialization.
 */
@Configuration
public class MongoConversionConfig {

    /**
     * Register custom converters for MongoDB.
     * The CampaignReadingConverter and CampaignWritingConverter handle complete serialization
     * of Campaign objects including the polymorphic specificConf field.
     *
     * @return MongoCustomConversions with the registered converters
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new CampaignConfReadConverter(),
            new CampaignConfWriteConverter()
        ));
    }
}
