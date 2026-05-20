package it.smartcommunitylab.playandgo.engine.config;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import it.smartcommunitylab.playandgo.engine.model.conf.CampaignCityConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignCompanyConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignPersonalConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignSchoolConf;

/**
 * Custom converter for MongoDB that converts the generic specificConf object
 * to the appropriate campaign configuration class based on the campaign type.
 */
@ReadingConverter
public class CampaignConfReadConverter implements Converter<Document, CampaignConf> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Configure ObjectMapper to ignore unknown properties during deserialization
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public CampaignConf convert(Document source) {
        if (source == null) {
            return null;
        }
        String typeStr = source.getString("_class");
        switch (typeStr) {
            case "CampaignCityConf":
                return objectMapper.convertValue(source, CampaignCityConf.class);
            case "CampaignCompanyConf":
                return objectMapper.convertValue(source, CampaignCompanyConf.class);
            case "CampaignPersonalConf":
                return objectMapper.convertValue(source, CampaignPersonalConf.class);
            case "CampaignSchoolConf":
                return objectMapper.convertValue(source, CampaignSchoolConf.class);
            default:
                throw new IllegalArgumentException("Unknown campaign configuration type: " + typeStr);
        }
    }
}
