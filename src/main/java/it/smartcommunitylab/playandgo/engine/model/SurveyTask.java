package it.smartcommunitylab.playandgo.engine.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;

@Document(collection="surveyTasks")
public class SurveyTask {
	@Id
	private String id;
    private String campaignId;
    private String playerId;
    private String surveyName;
    private SurveyRequest sr;

    // Constructors
    public SurveyTask() {
    }

    public SurveyTask(String campaignId, String playerId, String surveyName, SurveyRequest sr) {
        this.campaignId = campaignId;
        this.playerId = playerId;
        this.surveyName = surveyName;
        this.sr = sr;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getSurveyName() {
        return surveyName;
    }

    public void setSurveyName(String surveyName) {
        this.surveyName = surveyName;
    }

    public SurveyRequest getSr() {
        return sr;
    }

    public void setSr(SurveyRequest sr) {
        this.sr = sr;
    }

    @Override
    public String toString() {
        return "SurveyTask{" +
                "id='" + id + '\'' +
                ", campaignId='" + campaignId + '\'' +
                ", playerId='" + playerId + '\'' +
                ", surveyName='" + surveyName + '\'' +
                ", sr=" + sr +
                '}';
    }
}
