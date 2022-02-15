package it.smartcommunitylab.playandgo.engine.validation;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;

@Component
public class DummyCampaignTripValidator implements ManageValidateCampaignTripRequest {
	@Autowired
	private MessageQueueManager queueManager;
	
	@Autowired
	private CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@PostConstruct
	public void init() {
		queueManager.setManageValidateCampaignTripRequest(this, "TAA", "TAA.test1");
	}

	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			playerTrack.setScore(100l);
			playerTrack.setScoreStatus(ScoreStatus.ASSIGNED);
			campaignPlayerTrackRepository.save(playerTrack);
		}
	}

}
