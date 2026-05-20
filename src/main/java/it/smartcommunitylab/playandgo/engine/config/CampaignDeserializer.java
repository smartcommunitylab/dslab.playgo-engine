package it.smartcommunitylab.playandgo.engine.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignDetail;
import it.smartcommunitylab.playandgo.engine.model.CampaignWeekConf;
import it.smartcommunitylab.playandgo.engine.model.Image;
import it.smartcommunitylab.playandgo.engine.model.ValidationData;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignCityConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignCompanyConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignPersonalConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignSchoolConf;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Custom JSON deserializer for Campaign that converts the generic specificConf object
 * to the appropriate campaign configuration class based on the campaign type.
 * Uses TypeReference for proper generic type handling.
 */
public class CampaignDeserializer extends StdDeserializer<Campaign> {

    public CampaignDeserializer() {
        super(Campaign.class);
    }

    @Override
    public Campaign deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Read the entire JSON tree
        JsonNode node = p.getCodec().readTree(p);

        // Create Campaign object
        Campaign campaign = new Campaign();

        // Get the type field
        String typeStr = null;
        if (node.has("type")) {
            typeStr = node.get("type").asText();
            campaign.setType(Campaign.Type.valueOf(typeStr));
        }

        // Extract specificConf node
        JsonNode specificConfNode = node.has("specificConf") ? node.get("specificConf") : null;

        // Deserialize all other fields manually using ObjectMapper
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        
        // Deserialize simple fields
        if (node.has("_id")) {
            campaign.setCampaignId(node.get("_id").asText());
        }
        if (node.has("campaignId")) {
            campaign.setCampaignId(node.get("campaignId").asText());
        }
        if (node.has("territoryId")) {
            campaign.setTerritoryId(node.get("territoryId").asText());
        }
        if (node.has("name")) {
            campaign.setName(mapper.convertValue(node.get("name"), new TypeReference<Map<String, String>>() {}));
        }
        if (node.has("description")) {
            campaign.setDescription(mapper.convertValue(node.get("description"), new TypeReference<Map<String, String>>() {}));
        }
        if (node.has("dateFrom")) {
            campaign.setDateFrom(mapper.treeToValue(node.get("dateFrom"), Date.class));
        }
        if (node.has("dateTo")) {
            campaign.setDateTo(mapper.treeToValue(node.get("dateTo"), Date.class));
        }
        if (node.has("registrationFrom")) {
            campaign.setRegistrationFrom(mapper.treeToValue(node.get("registrationFrom"), Date.class));
        }
        if (node.has("registrationTo")) {
            campaign.setRegistrationTo(mapper.treeToValue(node.get("registrationTo"), Date.class));
        }
        if (node.has("active")) {
            campaign.setActive(node.get("active").asBoolean());
        }
        if (node.has("communications")) {
            campaign.setCommunications(node.get("communications").asBoolean());
        }
        if (node.has("visible")) {
            campaign.setVisible(node.get("visible").asBoolean());
        }
        if (node.has("startDayOfWeek")) {
            campaign.setStartDayOfWeek(node.get("startDayOfWeek").asInt());
        }
        if (node.has("gameId")) {
            campaign.setGameId(node.get("gameId").asText());
        }
        if (node.has("details")) {
            campaign.setDetails(mapper.convertValue(node.get("details"), new TypeReference<Map<String, List<CampaignDetail>>>() {}));
        }
        if (node.has("logo")) {
            campaign.setLogo(mapper.convertValue(node.get("logo"), new TypeReference<Image>() {}));
        }
        if (node.has("banner")) {
            campaign.setBanner(mapper.convertValue(node.get("banner"), new TypeReference<Image>() {}));
        }
        if (node.has("colorPalette")) {
            campaign.setColorPalette(node.get("colorPalette").asText());
        }
        if (node.has("validationData")) {
            campaign.setValidationData(mapper.treeToValue(node.get("validationData"), ValidationData.class));
        }
        if (node.has("surveys")) {
            campaign.setSurveys(mapper.convertValue(node.get("surveys"), new TypeReference<List<SurveyRequest>>() {}));
        }
        if (node.has("weekConfs")) {
            campaign.setWeekConfs(mapper.convertValue(node.get("weekConfs"), new TypeReference<List<CampaignWeekConf>>() {}));
        }

        // Now convert specificConf based on type and set it
        if (specificConfNode != null && typeStr != null) {
            try {
                Campaign.Type type = Campaign.Type.valueOf(typeStr);
                CampaignConf convertedSpecificConf = convertSpecificConf(type, specificConfNode, mapper);
                campaign.setSpecificConf(convertedSpecificConf);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        return campaign;
    }

    /**
     * Converts a JsonNode to the appropriate campaign configuration class.
     *
     * @param type   the campaign type
     * @param node   the JSON node containing the configuration
     * @param mapper the ObjectMapper instance
     * @return the converted configuration object
     */
    private CampaignConf convertSpecificConf(Campaign.Type type, JsonNode node, ObjectMapper mapper) {
        try {
            switch (type) {
                case city:
                    return mapper.treeToValue(node, CampaignCityConf.class);
                case company:
                    return mapper.treeToValue(node, CampaignCompanyConf.class);
                case personal:
                    return mapper.treeToValue(node, CampaignPersonalConf.class);
                case school:
                    return mapper.treeToValue(node, CampaignSchoolConf.class);
                case group:
                    return mapper.treeToValue(node, CampaignCityConf.class);                
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
