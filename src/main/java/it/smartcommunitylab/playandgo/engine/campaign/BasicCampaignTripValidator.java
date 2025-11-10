package it.smartcommunitylab.playandgo.engine.campaign;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.playandgo.engine.ge.GamificationEngineManager;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.lock.UserCampaignLock;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager.VirtualTrackOp;
import it.smartcommunitylab.playandgo.engine.manager.ext.CampaignMsgManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.UpdateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerStatsTransportRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
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
    CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	PlayerStatsTransportRepository playerStatsTrackRepository;
	
	@Autowired
	ValidationService validationService;
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
	
	@Autowired
	GamificationEngineManager gamificationEngineManager;
   
	@Autowired
    CampaignMsgManager campaignMsgManager;

	@Autowired
	UserCampaignLock campaignLock;
	
	protected String groupIdKey = null;
	
	//SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
	    try {
			campaignLock.lock(campaignLock.getKey(msg.getPlayerId(), msg.getCampaignId()));
			List<TrackedInstance> trackList = getTrackedInstance(msg.getPlayerId(), msg.getMultimodalId());
			for(TrackedInstance track : trackList) {
				CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(msg.getPlayerId(), 
						msg.getCampaignId(), track.getId());
				if((playerTrack != null) && ScoreStatus.UNASSIGNED.equals(playerTrack.getScoreStatus())) {
					try {
						if (!StringUtils.hasText(track.getSharedTravelId())) {
							validateFreeTrackingTripRequest(msg, playerTrack, track);
						} else {
							validateSharedTripRequest(msg, playerTrack, track);
						}
					} catch (Exception e) {
						logger.error(String.format("error in validateTripRequest[%s]:%s", track.getId(), e.getMessage()));
					}
				}
			}		
		} catch (Exception e) {
            logger.error("validateTripRequest error:" + e.getMessage());
		} finally {
			campaignLock.unlock(campaignLock.getKey(msg.getPlayerId(), msg.getCampaignId()));
		}
	}
	
    private List<TrackedInstance> getTrackedInstance(String userId, String multimodalId) {
        return trackedInstanceRepository.findByUserIdAndMultimodalId(userId, multimodalId, Sort.by(Direction.ASC, "startTime"));
    }

	private void validateSharedTripRequest(ValidateCampaignTripRequest msg, CampaignPlayerTrack playerTrack, TrackedInstance track) throws ParseException {
		String sharedId = track.getSharedTravelId();
		Map<String, Object> trackingData = null;
		if (ValidationConstants.isDriver(sharedId)) {
			//TODO check firstPair
		    trackingData = validationService.computeSharedTravelDistanceForDriver(track.getTerritoryId(), track.getGeolocationEvents(), 
		            track.getValidationResult().getValidationStatus(), track.getOverriddenDistances(), true);
		} else {
			trackingData = validationService.computeSharedTravelDistanceForPassenger(track.getTerritoryId(), track.getGeolocationEvents(), 
			        track.getValidationResult().getValidationStatus(), track.getOverriddenDistances());
		}
        populatePlayerTrack(playerTrack, track, trackingData);
        campaignPlayerTrackRepository.save(playerTrack);
        playerReportManager.updatePlayerCampaignPlacings(playerTrack);
        sendWebhookRequest(playerTrack);
		Campaign campaign = campaignRepository.findById(msg.getCampaignId()).orElse(null);
		if(campaign != null) {
			if(Utils.isNotEmpty(campaign.getGameId())) {			    
				boolean action = gamificationEngineManager.sendSaveItineraryAction(msg.getPlayerId(), campaign.getGameId(), trackingData, true);
                if(action) {
                    playerTrack.setScoreStatus(ScoreStatus.SENT);
                    campaignPlayerTrackRepository.save(playerTrack);
                }
			} else {
                playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
                campaignPlayerTrackRepository.save(playerTrack);
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
				boolean action = gamificationEngineManager.sendSaveItineraryAction(msg.getPlayerId(), campaign.getGameId(), trackingData, true);
				if(action) {
				    playerTrack.setScoreStatus(ScoreStatus.SENT);
				    campaignPlayerTrackRepository.save(playerTrack);
				}
			} else {
			    playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
			    campaignPlayerTrackRepository.save(playerTrack);
			}
		}
	}

	private void populatePlayerTrack(CampaignPlayerTrack playerTrack, TrackedInstance track,
			Map<String, Object> trackingData) throws ParseException {
		trackingData.put(TRAVEL_ID, track.getClientId());
		trackingData.put(TRACK_ID, track.getId());
		trackingData.put(START_TIME, track.getStartTime().getTime());
		playerTrack.setTrackingData(trackingData);
		
		playerTrack.setScoreStatus(ScoreStatus.UNASSIGNED);
		playerTrack.setValid(true);
		ValidationStatus validationStatus = track.getValidationResult().getValidationStatus();
		playerTrack.setModeType(validationStatus.getModeType().toString());
		playerTrack.setDuration(validationStatus.getDuration());
		playerTrack.setDistance(Utils.getTrackDistance(track));
		playerTrack.setCo2(Utils.getSavedCo2(playerTrack.getModeType(), playerTrack.getDistance()));
		
		playerTrack.setStartTime(track.getStartTime());
		playerTrack.setEndTime(Utils.getEndTime(track));
		
		playerTrack.setVirtualScore(0.0);
		playerTrack.setVirtualTrack(false);
		
        if(Utils.isNotEmpty(groupIdKey)) {
            CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(playerTrack.getCampaignId(), playerTrack.getPlayerId());
            if(cs != null) {
                String groupId = (String) cs.getCampaignData().get(groupIdKey);
                if(Utils.isNotEmpty(groupId)) {
                    playerTrack.setGroupId(groupId); 
                }
            }                                           
        }		    
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
	public void invalidateTripRequest(UpdateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			playerTrack.setValid(false);
			TrackedInstance track = trackedInstanceRepository.findById(playerTrack.getTrackedInstanceId()).orElse(null);
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
	        TrackedInstance track = trackedInstanceRepository.findById(playerTrack.getTrackedInstanceId()).orElse(null);
	        Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
	        if((track != null) && (campaign != null)) {
	            double deltaDistance = Utils.getTrackDistance(track) - playerTrack.getDistance();
	            double deltaCo2 = Utils.getSavedCo2(playerTrack.getModeType(), Math.abs(Utils.getTrackDistance(track))) - playerTrack.getCo2();
	            ScoreStatus oldStatus = playerTrack.getScoreStatus();
	            if(oldStatus.equals(ScoreStatus.UNASSIGNED) || oldStatus.equals(ScoreStatus.SENT)) {
	                revalidateTripRequest(msg);
	            } else if(oldStatus.equals(ScoreStatus.COMPUTED)) {
	                playerTrack.setDistance(Utils.getTrackDistance(track));
	                playerTrack.setCo2(Utils.getSavedCo2(playerTrack.getModeType(), Math.abs(playerTrack.getDistance())));
	                campaignPlayerTrackRepository.save(playerTrack);
	                if(deltaDistance != 0) {
	                    playerReportManager.updatePlayerCampaignPlacings(playerTrack, deltaDistance, deltaCo2, 0.0, VirtualTrackOp.nothing);
	                    if(Utils.isNotEmpty(campaign.getGameId()) && (deltaDistance > 0)) {
	                        Map<String,Object> trackingData = getTrackingData(track, deltaDistance);
	                        gamificationEngineManager.sendSaveItineraryAction(playerTrack.getPlayerId(), campaign.getGameId(), trackingData, false);
	                    }                   
	                }
	            }
	        }		    
		}
	}
	
	private Map<String, Object> getTrackingData(TrackedInstance track, double deltaDistance) {
	    Map<String, Object> trackingData = new HashMap<>();
	    trackingData.put(track.getFreeTrackingTransport() + "Distance", deltaDistance / 1000.0);
        trackingData.put(TRAVEL_ID, track.getClientId());
        trackingData.put(TRACK_ID, track.getId());
        trackingData.put(START_TIME, track.getStartTime().getTime());
	    return trackingData;
	}

	@Override
	public void revalidateTripRequest(UpdateCampaignTripRequest msg) {
        CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
        if(playerTrack != null) {
            Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
            TrackedInstance track = trackedInstanceRepository.findById(playerTrack.getTrackedInstanceId()).orElse(null);
            if((campaign != null) && (track != null)) {
                try {
                    ScoreStatus oldStatus = playerTrack.getScoreStatus();
                    double deltaDistance = Utils.getTrackDistance(track) - playerTrack.getDistance();
                    Map<String, Object> trackingData = validationService.computeFreeTrackingDistances(track.getTerritoryId(), 
                            track.getGeolocationEvents(), track.getFreeTrackingTransport(), track.getValidationResult().getValidationStatus(), track.getOverriddenDistances());
                    populatePlayerTrack(playerTrack, track, trackingData);
                    campaignPlayerTrackRepository.save(playerTrack);
                    if(Utils.isNotEmpty(campaign.getGameId()) && (oldStatus.equals(ScoreStatus.UNASSIGNED) || oldStatus.equals(ScoreStatus.SENT))) {
                        boolean action = gamificationEngineManager.sendSaveItineraryAction(playerTrack.getPlayerId(), campaign.getGameId(), trackingData, true);
                        if(action) {
                            playerTrack.setScoreStatus(ScoreStatus.SENT);
                            campaignPlayerTrackRepository.save(playerTrack);
                        }
                    } else {
                        playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
                        campaignPlayerTrackRepository.save(playerTrack);                   
                        if(deltaDistance != 0) {
                            double deltaCo2 = Utils.getSavedCo2(playerTrack.getModeType(), Math.abs(deltaDistance));
                            playerReportManager.updatePlayerCampaignPlacings(playerTrack, deltaDistance, deltaCo2, 0.0, VirtualTrackOp.nothing);
                        }
                    }
                } catch (Exception e) {
                    logger.error("revalidateTripRequest error:" + e.getMessage());
                    campaignMsgManager.addRevalidateTripRequest(msg, campaign.getType(), e.getMessage(), ErrorCode.OPERATION_ERROR);
                }                
            }
        }	    
	}

}
