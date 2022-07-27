package it.smartcommunitylab.playandgo.engine.campaign.company;

import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.manager.azienda.PgAziendaleManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;

@Component
public class CompanyCampaignSubscription {
	private static Logger logger = LoggerFactory.getLogger(CompanyCampaignSubscription.class);
	
	public static String companyKey = "companyKey";
	public static String employeeCode = "employeeCode";
	
	@Autowired
	PgAziendaleManager aziendaleManager;

	public CampaignSubscription subscribeCampaign(Player player, Campaign campaign, 
			Map<String, Object> campaignData) throws Exception {		
		aziendaleManager.subscribeCampaign(campaign.getCampaignId(), player.getPlayerId(), 
				(String)campaignData.get(companyKey), (String)campaignData.get(employeeCode));
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(new Date());
		if(campaignData != null) {
			sub.setCampaignData(campaignData);
		}
		return sub;			
	}
	
	public void unsubscribeCampaign(Player player, Campaign campaign) throws Exception {
		aziendaleManager.unsubscribeCampaign(campaign.getCampaignId(), player.getPlayerId());		
	}
}
