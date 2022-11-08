package it.smartcommunitylab.playandgo.engine.campaign;

import java.text.ParseException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus.MODE_TYPE;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.UpdateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;
import it.smartcommunitylab.playandgo.engine.validation.ValidationConstants;
import it.smartcommunitylab.playandgo.engine.validation.ValidationService;

@Component
public class BasicCampaignTripValidator implements ManageValidateCampaignTripRequest {
	private static Logger logger = LoggerFactory.getLogger(BasicCampaignTripValidator.class);
	
	public static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";
	public static final String TRACK_ID = "trackId";
	
	//static FastDateFormat shortSdf = FastDateFormat.getInstance("yyyy/MM/dd");
	//static FastDateFormat fullSdf = FastDateFormat.getInstance("yyyy/MM/dd HH:mm");

	@Autowired
	protected MessageQueueManager queueManager;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerStatsTransportRepository playerStatsTrackRepository;
	
	@Autowired
	ValidationService validationService;
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	//SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			TrackedInstance track = trackedInstanceRepository.findById(msg.getTrackedInstanceId()).orElse(null);
			if(track != null) {
				try {
					if (!StringUtils.hasText(track.getSharedTravelId())) {
						validateFreeTrackingTripRequest(msg, playerTrack, track);
					} else {
						validateSharedTripRequest(msg, playerTrack, track);
					}
				} catch (Exception e) {
					logger.warn(String.format("error in validateTripRequest[%s]:%s", msg.getCampaignPlayerTrackId(), e.getMessage()));
				}
			}
		}
	}

	private void validateSharedTripRequest(ValidateCampaignTripRequest msg, CampaignPlayerTrack playerTrack, TrackedInstance track) throws ParseException {
		String sharedId = track.getSharedTravelId();
		Map<String, Object> trackingData = null;
		if (ValidationConstants.isDriver(sharedId)) {
			boolean firstTime = !ScoreStatus.SENT.equals(playerTrack.getScoreStatus()) && !ScoreStatus.ASSIGNED.equals(playerTrack.getScoreStatus());
			trackingData = validationService.computeSharedTravelDistanceForDriver(track.getTerritoryId(), track.getGeolocationEvents(), track.getValidationResult().getValidationStatus(), track.getOverriddenDistances(), firstTime);
			populatePlayerTrack(playerTrack, track, trackingData);
			if (firstTime) {
				campaignPlayerTrackRepository.save(playerTrack);
				playerReportManager.updatePlayerCampaignPlacings(playerTrack);
			}
		} else {
			trackingData = validationService.computeSharedTravelDistanceForPassenger(track.getTerritoryId(), track.getGeolocationEvents(), track.getValidationResult().getValidationStatus(), track.getOverriddenDistances());
			populatePlayerTrack(playerTrack, track, trackingData);
			campaignPlayerTrackRepository.save(playerTrack);
			playerReportManager.updatePlayerCampaignPlacings(playerTrack);
			sendWebhookRequest(playerTrack);
		}
		Campaign campaign = campaignRepository.findById(msg.getCampaignId()).orElse(null);
		if(campaign != null) {
			if(Utils.isNotEmpty(campaign.getGameId())) {
				gamificationEngineManager.sendSaveItineraryAction(msg.getPlayerId(), campaign.getGameId(), trackingData);
			}
		}
	}

	private void validateFreeTrackingTripRequest(ValidateCampaignTripRequest msg, CampaignPlayerTrack playerTrack,
			TrackedInstance track) throws Exception, ParseException {
		Map<String, Object> trackingData = validationService.computeFreeTrackingDistances(msg.getTerritoryId(), 
				track.getGeolocationEvents(), track.getFreeTrackingTransport(), track.getValidationResult().getValidationStatus(), track.getOverriddenDistances());
		
		populatePlayerTrack(playerTrack, track, trackingData);
		campaignPlayerTrackRepository.save(playerTrack);
		
		playerReportManager.updatePlayerCampaignPlacings(playerTrack);
		
		sendWebhookRequest(playerTrack);
		
		Campaign campaign = campaignRepository.findById(msg.getCampaignId()).orElse(null);
		if(campaign != null) {
			if(Utils.isNotEmpty(campaign.getGameId())) {
				gamificationEngineManager.sendSaveItineraryAction(msg.getPlayerId(), campaign.getGameId(), trackingData);
			} else {
			    playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
			}
		}
	}

	private void populatePlayerTrack(CampaignPlayerTrack playerTrack, TrackedInstance track,
			Map<String, Object> trackingData) throws ParseException {
		trackingData.put(TRAVEL_ID, track.getClientId());
		trackingData.put(TRACK_ID, track.getId());
		trackingData.put(START_TIME, track.getStartTime().getTime());
		playerTrack.setTrackingData(trackingData);
		
		playerTrack.setScoreStatus(ScoreStatus.SENT);
		playerTrack.setValid(true);
		ValidationStatus validationStatus = track.getValidationResult().getValidationStatus();
		playerTrack.setModeType(validationStatus.getModeType().toString());
		playerTrack.setDuration(validationStatus.getDuration());
		if(validationStatus.getEffectiveDistances().containsKey(validationStatus.getModeType())) {
			playerTrack.setDistance(validationStatus.getEffectiveDistances().get(validationStatus.getModeType()));
		} else {
			playerTrack.setDistance(validationStatus.getDistance());
		}
		playerTrack.setCo2(Utils.getSavedCo2(playerTrack.getModeType(), playerTrack.getDistance()));
		
		playerTrack.setStartTime(track.getStartTime());
		playerTrack.setEndTime(Utils.getEndTime(track));
	}
	
	private void sendWebhookRequest(CampaignPlayerTrack pt) {
		WebhookRequest req = new  WebhookRequest();
		req.setCampaignId(pt.getCampaignId());
		req.setPlayerId(pt.getPlayerId());
		req.setEventType(EventType.validTrack);
		req.getData().put("trackedInstanceId", pt.getTrackedInstanceId());
		try {
			queueManager.sendCallWebhookRequest(req);
		} catch (Exception e) {
			logger.error("sendWebhookRequest:" + e.getMessage());
		}
	}
	

//	private long getStartTime(TrackedInstance trackedInstance) throws ParseException {
//		long time = 0;
//		if (trackedInstance.getGeolocationEvents() != null && !trackedInstance.getGeolocationEvents().isEmpty()) {
//			Geolocation event = trackedInstance.getGeolocationEvents().stream().sorted().findFirst().get();
//			time = event.getRecorded_at().getTime();
//		} else if (trackedInstance.getDay() != null && trackedInstance.getTime() != null) {
//			String dt = trackedInstance.getDay() + " " + trackedInstance.getTime();
//			time = fullSdf.parse(dt).getTime();
//		} else if (trackedInstance.getDay() != null) {
//			time = shortSdf.parse(trackedInstance.getDay()).getTime();
//		}
//		return time;
//	}

	@Override
	public void invalidateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			playerTrack.setValid(false);
			TrackedInstance track = trackedInstanceRepository.findById(msg.getTrackedInstanceId()).orElse(null);
			if(track != null) {
				playerTrack.setErrorCode(track.getValidationResult().getValidationStatus().getError().toString());
			}
			campaignPlayerTrackRepository.save(playerTrack);
			playerReportManager.removePlayerCampaignPlacings(playerTrack);
			//TODO send action to GamificationEngine?
		}
	}

	@Override
	public void updateTripRequest(UpdateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			playerTrack.setDistance(playerTrack.getDistance() + msg.getDeltaDistance());
			double co2 = Utils.getSavedCo2(playerTrack.getModeType(), Math.abs(playerTrack.getDistance()));
			if(msg.getDeltaDistance() > 0) {
				playerTrack.setCo2(playerTrack.getCo2() + co2);
			} else if(msg.getDeltaDistance() < 0) {
				playerTrack.setCo2(playerTrack.getCo2() - co2);
			}
			campaignPlayerTrackRepository.save(playerTrack);
			playerReportManager.updatePlayerCampaignPlacings(playerTrack, msg.getDeltaDistance(), co2);
			//TODO send action to GamificationEngine? 
		}
	}

	@Override
	public void revalidateTripRequest(UpdateCampaignTripRequest msg) {
	}

}
