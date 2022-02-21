package it.smartcommunitylab.playandgo.engine.validation;

import java.text.ParseException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.manager.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class BasicCampaignTripValidator implements ManageValidateCampaignTripRequest {
	private static Log logger = LogFactory.getLog(BasicCampaignTripValidator.class);
	
	public static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";
	
	private static FastDateFormat shortSdf = FastDateFormat.getInstance("yyyy/MM/dd");
	private static FastDateFormat fullSdf = FastDateFormat.getInstance("yyyy/MM/dd HH:mm");

	@Autowired
	MessageQueueManager queueManager;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	ValidationService validationService;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	@PostConstruct
	public void init() {
		queueManager.setManageValidateCampaignTripRequest(this, "TAA", "TAA.test1");
	}

	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			TrackedInstance track = trackedInstanceRepository.findById(msg.getTrackedInstanceId()).orElse(null);
			if(track != null) {
				try {
					Map<String, Object> trackingData = validationService.computeFreeTrackingDistances(msg.getTerritoryId(), 
							track.getGeolocationEvents(), track.getFreeTrackingTransport(), track.getValidationResult().getValidationStatus(), track.getOverriddenDistances());
					
					playerTrack.setScoreStatus(ScoreStatus.COMPUTED);					
					trackingData.put(TRAVEL_ID, track.getClientId());
					trackingData.put(START_TIME, getStartTime(track));
					playerTrack.setTrackingData(trackingData);
					campaignPlayerTrackRepository.save(playerTrack);
					
					Campaign campaign = campaignRepository.findById(msg.getCampaignId()).orElse(null);
					if(campaign != null) {
						if(Utils.isNotEmpty(campaign.getGameId())) {
							gamificationEngineManager.sendFreetrackingAction(msg.getPlayerId(), campaign.getGameId(), trackingData);
						}
					}
				} catch (Exception e) {
					logger.warn(String.format("error in validateTripRequest[%s]:%s", msg.getCampaignPlayerTrackId(), e.getMessage()));
				}
			}
		}
	}
	
	private long getStartTime(TrackedInstance trackedInstance) throws ParseException {
		long time = 0;
		if (trackedInstance.getGeolocationEvents() != null && !trackedInstance.getGeolocationEvents().isEmpty()) {
			Geolocation event = trackedInstance.getGeolocationEvents().stream().sorted().findFirst().get();
			time = event.getRecorded_at().getTime();
		} else if (trackedInstance.getDay() != null && trackedInstance.getTime() != null) {
			String dt = trackedInstance.getDay() + " " + trackedInstance.getTime();
			time = fullSdf.parse(dt).getTime();
		} else if (trackedInstance.getDay() != null) {
			time = shortSdf.parse(trackedInstance.getDay()).getTime();
		}
		return time;
	}

}
