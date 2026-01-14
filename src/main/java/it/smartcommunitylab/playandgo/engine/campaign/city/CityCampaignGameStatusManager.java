package it.smartcommunitylab.playandgo.engine.campaign.city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignGameStatusManager;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.Player;

@Component
public class CityCampaignGameStatusManager  extends BasicCampaignGameStatusManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CityCampaignGameStatusManager.class);

	@Override
	protected String getGroupId(CampaignPlayerTrack playerTrack, Player p) {
		if(p.getGroup()) {
			return p.getPlayerId();
		}
		return null;
	}

}
