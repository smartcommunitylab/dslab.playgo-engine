package it.smartcommunitylab.playandgo.engine.campaign;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;

@Component
public class CityCampaignSubscription {
	private static Logger logger = LoggerFactory.getLogger(CityCampaignSubscription.class);
	
	public static String nickRecommendation = "nick_recommandation";
	
	@Autowired
	SurveyManager surveyManager;
	
	@Autowired
	PlayerRepository playerRepository;
	
	public CampaignSubscription subscribeCampaign(Player player, Campaign campaign, 
			Map<String, Object> campaignData) throws Exception {
		
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());
		if(campaignData != null) {
			sub.setCampaignData(campaignData);
			//check player recommendation
			if(campaignData.containsKey(nickRecommendation)) {
				String nickname = (String) campaignData.get(nickRecommendation);
				Player recommender = playerRepository.findByNickname(nickname);
				if(recommender != null) {
					sub.getCampaignData().put(Campaign.recommenderPlayerId, player.getPlayerId());
					sub.getCampaignData().put(Campaign.recommendationPlayerToDo, Boolean.TRUE);
				}
			}
		}
		//check default survey
		if(campaign.hasDefaultSurvey()) {
			SurveyRequest sr = campaign.getDefaultSurvey();
			surveyManager.assignSurveyChallenges(campaign.getCampaignId(), Arrays.asList(player.getPlayerId()), sr);
		}
		return sub;
	}
}
