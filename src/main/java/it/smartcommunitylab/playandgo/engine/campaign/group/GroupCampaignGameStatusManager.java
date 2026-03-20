package it.smartcommunitylab.playandgo.engine.campaign.group;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.campaign.BasicCampaignGameStatusManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class GroupCampaignGameStatusManager extends BasicCampaignGameStatusManager {

    @Override
    protected String getGroupId(CampaignPlayerTrack playerTrack, Player p, Campaign campaign) {
        if(playerTrack == null) {
            CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), p.getPlayerId());
            if(cs != null) {
                String groupId = (String) cs.getCampaignData().get(GroupCampaignSubscription.groupIdKey);
                if(Utils.isNotEmpty(groupId)) {
                    return groupId; 
                }
            }
        }
        if((playerTrack != null) && Utils.isNotEmpty(playerTrack.getGroupId())) {
            return playerTrack.getGroupId();
        }  
        return null; 
    }

}
