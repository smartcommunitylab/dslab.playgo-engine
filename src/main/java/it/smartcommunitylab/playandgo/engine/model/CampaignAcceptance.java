package it.smartcommunitylab.playandgo.engine.model;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="campaignAcceptances")
public class CampaignAcceptance {
    @Id
    private String id;
    @Indexed
    private String playerId;
    @Indexed
    private String campaignId;
    private boolean privacyAcceptance;
    private boolean rulesAcceptance;
    private Date acceptanceDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public boolean isPrivacyAcceptance() {
        return privacyAcceptance;
    }

    public void setPrivacyAcceptance(boolean privacyAcceptance) {
        this.privacyAcceptance = privacyAcceptance;
    }

    public boolean isRulesAcceptance() {
        return rulesAcceptance;
    }

    public void setRulesAcceptance(boolean rulesAcceptance) {
        this.rulesAcceptance = rulesAcceptance;
    }

    public Date getAcceptanceDate() {
        return acceptanceDate;
    }

    public void setAcceptanceDate(Date acceptanceDate) {
        this.acceptanceDate = acceptanceDate;
    }
}
