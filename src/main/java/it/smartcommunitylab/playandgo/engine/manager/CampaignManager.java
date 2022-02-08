package it.smartcommunitylab.playandgo.engine.manager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;

@Component
public class CampaignManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignManager.class);
	
	@Autowired
	private CampaignRepository campaignRepository;
	
	public void saveTerritory(Campaign campaign) {
		campaignRepository.save(campaign);
	}
	
	public Campaign getCampaign(String campaignId) {
		return campaignRepository.findById(campaignId).orElse(null);
	}
	
	public List<Campaign> getCampaigns() {
		return campaignRepository.findAll();
	}
	
	public Campaign deleteCampaign(String campaignId) {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign != null) {
			campaignRepository.deleteById(campaignId);
		}
		return campaign;
	}
}
