package it.smartcommunitylab.playandgo.engine.campaign.city;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignGameStatusManager;

@Component
public class CityCampaignGameStatusManager  extends BasicCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CityCampaignGameStatusManager.class);

    @Override
    public void updatePlayerGameStatus(Map<String, Object> msg) {
        // TODO Auto-generated method stub
        updatePlayerGameStatus(msg, null); 
    }
	
	
}
