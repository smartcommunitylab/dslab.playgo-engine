package it.smartcommunitylab.playandgo.engine.config;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.model.conf.CampaignCityConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignCompanyConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignPersonalConf;
import it.smartcommunitylab.playandgo.engine.model.conf.CampaignSchoolConf;

@WritingConverter
public class CampaignConfWriteConverter implements Converter<CampaignConf, Document> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        // Configure ObjectMapper to ignore unknown properties during deserialization
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }    

    @Override
    public Document convert(CampaignConf source) {
        if (source == null) {
            return null;
        }
        
        Document doc = new Document();
        doc.put("_class", source.getClass().getSimpleName());
        
        // Convert based on the actual type
        if (source instanceof CampaignCityConf) {
            convertCampaignCityConf((CampaignCityConf) source, doc);
        } else if (source instanceof CampaignCompanyConf) {
            convertCampaignCompanyConf((CampaignCompanyConf) source, doc);
        } else if (source instanceof CampaignPersonalConf) {
            convertCampaignPersonalConf((CampaignPersonalConf) source, doc);
        } else if (source instanceof CampaignSchoolConf) {
            convertCampaignSchoolConf((CampaignSchoolConf) source, doc);
        }
        
        return doc;
    }

    /**
     * Converts CampaignCityConf fields to Document
     */
    private void convertCampaignCityConf(CampaignCityConf source, Document doc) {
        if (source.getPointName() != null) {
            doc.put("pointName", source.getPointName());
        }
        doc.put("showGeneralInfo", source.isShowGeneralInfo());
        doc.put("showBadgeSection", source.isShowBadgeSection());
        doc.put("showUserBlacklist", source.isShowUserBlacklist());
        
        if (source.getChallengeProposed() != null) {
            doc.put("challengeProposed", source.getChallengeProposed());
        }
        if (source.getChallengeAssigned() != null) {
            doc.put("challengeAssigned", source.getChallengeAssigned());
        }
        
        if (source.getGeneralPlacing() != null) {
            doc.put("generalPlacing", objectMapper.convertValue(source.getGeneralPlacing(), Document.class));
        }
        if (source.getGroupPlacing() != null) {
            doc.put("groupPlacing", objectMapper.convertValue(source.getGroupPlacing(), Document.class));
        }
        if (source.getTopTenPlacing() != null) {
            doc.put("topTenPlacing", objectMapper.convertValue(source.getTopTenPlacing(), Document.class));
        }
        if (source.getTransportStats() != null) {
            doc.put("transportStats", objectMapper.convertValue(source.getTransportStats(), Document.class));
        }
        if (source.getGameStats() != null) {
            doc.put("gameStats", objectMapper.convertValue(source.getGameStats(), Document.class));
        }
        
        doc.put("userGroups", source.isUserGroups());
        
        if (source.getGroups() != null && !source.getGroups().isEmpty()) {
            doc.put("groups", source.getGroups());
        }
        
        doc.put("useExtAuth", source.isUseExtAuth());
        
        if (source.getExtProvider() != null) {
            doc.put("extProvider", objectMapper.convertValue(source.getExtProvider(), Document.class));
        }
    }

    /**
     * Converts CampaignCompanyConf fields to Document
     */
    private void convertCampaignCompanyConf(CampaignCompanyConf source, Document doc) {
        doc.put("showGeneralInfo", source.isShowGeneralInfo());
        doc.put("hideCompanyDesc", source.isHideCompanyDesc());
        
        if (source.getRegistrationCompanyDesc() != null) {
            doc.put("registrationCompanyDesc", source.getRegistrationCompanyDesc());
        }
        
        doc.put("useMultiLocation", source.isUseMultiLocation());
        doc.put("useEmployeeLocation", source.isUseEmployeeLocation());
        
        if (source.getVirtualScoreConf() != null) {
            doc.put("virtualScoreConf", objectMapper.convertValue(source.getVirtualScoreConf(), Document.class));
        }
        if (source.getTransportPlacing() != null) {
            doc.put("transportPlacing", objectMapper.convertValue(source.getTransportPlacing(), Document.class));
        }
        if (source.getTransportStats() != null) {
            doc.put("transportStats", objectMapper.convertValue(source.getTransportStats(), Document.class));
        }
    }

    /**
     * Converts CampaignPersonalConf fields to Document
     */
    private void convertCampaignPersonalConf(CampaignPersonalConf source, Document doc) {
        doc.put("showGeneralInfo", source.isShowGeneralInfo());
        
        if (source.getTransportPlacing() != null) {
            doc.put("transportPlacing", objectMapper.convertValue(source.getTransportPlacing(), Document.class));
        }
        if (source.getTransportStats() != null) {
            doc.put("transportStats", objectMapper.convertValue(source.getTransportStats(), Document.class));
        }
    }

    /**
     * Converts CampaignSchoolConf fields to Document
     */
    private void convertCampaignSchoolConf(CampaignSchoolConf source, Document doc) {
        if (source.getPointName() != null) {
            doc.put("pointName", source.getPointName());
        }
        doc.put("showGeneralInfo", source.isShowGeneralInfo());
        doc.put("showBadgeSection", source.isShowBadgeSection());
        doc.put("showUserBlacklist", source.isShowUserBlacklist());
        
        if (source.getChallengeProposed() != null) {
            doc.put("challengeProposed", source.getChallengeProposed());
        }
        if (source.getChallengeAssigned() != null) {
            doc.put("challengeAssigned", source.getChallengeAssigned());
        }
        
        if (source.getGamePlacing() != null) {
            doc.put("gamePlacing", objectMapper.convertValue(source.getGamePlacing(), Document.class));
        }
        if (source.getTransportStats() != null) {
            doc.put("transportStats", objectMapper.convertValue(source.getTransportStats(), Document.class));
        }
        if (source.getGameStats() != null) {
            doc.put("gameStats", objectMapper.convertValue(source.getGameStats(), Document.class));
        }
    }
}
