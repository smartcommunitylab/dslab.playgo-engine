package it.smartcommunitylab.playandgo.engine.campaign.group;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignGameStatusManager;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class GroupCampaignGameStatusManager extends BasicCampaignGameStatusManager {

    @Override
    protected String getGroupId(CampaignPlayerTrack playerTrack, Player p) {
        if((playerTrack != null) && Utils.isNotEmpty(playerTrack.getGroupId())) {
            return playerTrack.getGroupId();
        }  
        return null; 
    }

}
