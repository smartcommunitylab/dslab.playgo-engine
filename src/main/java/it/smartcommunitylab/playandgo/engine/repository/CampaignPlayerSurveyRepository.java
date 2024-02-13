package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerSurvey;

@Repository
public interface CampaignPlayerSurveyRepository extends MongoRepository<CampaignPlayerSurvey, String> {
	public CampaignPlayerSurvey findByPlayerIdAndGameIdAndSurveyName(String playerId, String gameId, String surveyName);
	public CampaignPlayerSurvey findByPlayerIdAndCampaignIdAndSurveyName(String playerId, String campaignId, String surveyName);
}
