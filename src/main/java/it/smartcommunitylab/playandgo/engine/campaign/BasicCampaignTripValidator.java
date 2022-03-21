package it.smartcommunitylab.playandgo.engine.campaign;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatsTrack;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;
import it.smartcommunitylab.playandgo.engine.validation.ValidationService;

@Component
public class BasicCampaignTripValidator implements ManageValidateCampaignTripRequest {
	private static Logger logger = LoggerFactory.getLogger(BasicCampaignTripValidator.class);
	
	public static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";
	public static final String TRACK_ID = "trackId";
	
	static FastDateFormat shortSdf = FastDateFormat.getInstance("yyyy/MM/dd");
	static FastDateFormat fullSdf = FastDateFormat.getInstance("yyyy/MM/dd HH:mm");

	@Autowired
	MessageQueueManager queueManager;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	PlayerStatsTrackRepository playerStatsTrackRepository;
	
	@Autowired
	ValidationService validationService;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
	
	SimpleDateFormat sdf = new SimpleDateFormat("YYYY/MM/dd HH:mm");
	
	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			TrackedInstance track = trackedInstanceRepository.findById(msg.getTrackedInstanceId()).orElse(null);
			if(track != null) {
				try {
					Map<String, Object> trackingData = validationService.computeFreeTrackingDistances(msg.getTerritoryId(), 
							track.getGeolocationEvents(), track.getFreeTrackingTransport(), track.getValidationResult().getValidationStatus(), track.getOverriddenDistances());
					
					trackingData.put(TRAVEL_ID, track.getClientId());
					trackingData.put(TRACK_ID, track.getId());
					trackingData.put(START_TIME, getStartTime(track));
					
					playerTrack.setTrackingData(trackingData);
					playerTrack.setScoreStatus(ScoreStatus.SENT);
					playerTrack.setValid(true);
					campaignPlayerTrackRepository.save(playerTrack);
					
					PlayerStatsTrack statsTrack = new PlayerStatsTrack();
					statsTrack.setPlayerId(playerTrack.getPlayerId());
					statsTrack.setCampaignId(playerTrack.getCampaignId());
					statsTrack.setTrackedInstanceId(playerTrack.getTrackedInstanceId());
					statsTrack.setModeType(track.getValidationResult().getValidationStatus().getModeType().toString());
					statsTrack.setDuration(track.getValidationResult().getValidationStatus().getDuration());
					statsTrack.setDistance(track.getValidationResult().getValidationStatus().getDistance());
					statsTrack.setCo2(getSavedCo2(track.getValidationResult().getValidationStatus().getModeType().toString(), 
							track.getValidationResult().getValidationStatus().getDistance()));
					Date startTime = sdf.parse(track.getDay() + " " + track.getTime());
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(startTime);
					calendar.add(Calendar.SECOND, (int) track.getValidationResult().getValidationStatus().getDuration());
					Date endTime = calendar.getTime();
					statsTrack.setStartTime(startTime);
					statsTrack.setEndTime(endTime);
					playerStatsTrackRepository.save(statsTrack);
					
					Campaign campaign = campaignRepository.findById(msg.getCampaignId()).orElse(null);
					if(campaign != null) {
						if(Utils.isNotEmpty(campaign.getGameId())) {
							gamificationEngineManager.sendSaveItineraryAction(msg.getPlayerId(), campaign.getGameId(), trackingData);
						}
					}
				} catch (Exception e) {
					logger.warn(String.format("error in validateTripRequest[%s]:%s", msg.getCampaignPlayerTrackId(), e.getMessage()));
				}
			}
		}
	}
	
	private int getSavedCo2(String modeType, double distance) {
		// TODO calculate co2
		return 0;
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
