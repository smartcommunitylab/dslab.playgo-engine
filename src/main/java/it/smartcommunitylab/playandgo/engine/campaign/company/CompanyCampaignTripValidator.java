package it.smartcommunitylab.playandgo.engine.campaign.company;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.manager.azienda.LegData;
import it.smartcommunitylab.playandgo.engine.manager.azienda.LegResult;
import it.smartcommunitylab.playandgo.engine.manager.azienda.PgAziendaleManager;
import it.smartcommunitylab.playandgo.engine.manager.azienda.TrackData;
import it.smartcommunitylab.playandgo.engine.manager.azienda.TrackResult;
import it.smartcommunitylab.playandgo.engine.manager.ext.CampaignMsgManager;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.UpdateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Component
public class CompanyCampaignTripValidator implements ManageValidateCampaignTripRequest {
	private static Logger logger = LoggerFactory.getLogger(CompanyCampaignTripValidator.class);
	
	@Autowired
	MessageQueueManager queueManager;
	
	@Autowired
	PgAziendaleManager pgAziendaleManager;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	PlayerCampaignPlacingManager playerReportManager;
	
	@Autowired
	CampaignMsgManager campaignMsgManager;

	@PostConstruct
	public void init() {
		queueManager.setManageValidateCampaignTripRequest(this, Type.company);
	}

	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			TrackedInstance track = trackedInstanceRepository.findById(msg.getTrackedInstanceId()).orElse(null);
			TrackData trackData = getTrackData(track);
			if(trackData != null) {
				try {
					TrackResult trackResult = pgAziendaleManager.validateTrack(msg.getCampaignId(), msg.getPlayerId(), trackData);
					if(!trackResult.getValid()) {
						errorPlayerTrack(playerTrack, trackResult.getErrorCode());
					} else {
						LegResult legResult = trackResult.getLegs().get(0);
						populatePlayerTrack(track, playerTrack, legResult.getMean(), legResult.getValidDistance());
						playerReportManager.updatePlayerCampaignPlacings(playerTrack);
						sendWebhookRequest(playerTrack);
					}
				} catch (ServiceException e) {
					logger.warn("validateTripRequest error:" + e.getMessage());
					campaignMsgManager.addValidateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
				}
			}			
		}	
	}
	
	private void populatePlayerTrack(TrackedInstance track, CampaignPlayerTrack playerTrack, String modeType, double distance) {
		playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
		playerTrack.setValid(true);
		playerTrack.setModeType(modeType);
		playerTrack.setDistance(distance);
		playerTrack.setDuration(track.getValidationResult().getValidationStatus().getDuration());
		playerTrack.setCo2(Utils.getSavedCo2(modeType, distance));
		playerTrack.setStartTime(track.getStartTime());
		playerTrack.setEndTime(Utils.getEndTime(track));		
		campaignPlayerTrackRepository.save(playerTrack);
	}
	
	private void errorPlayerTrack(CampaignPlayerTrack playerTrack, String errorCode) {
		playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
		playerTrack.setValid(false);
		playerTrack.setErrorCode(errorCode);	
		campaignPlayerTrackRepository.save(playerTrack);
	}
	
	private TrackData getTrackData(TrackedInstance track) {
		if(track != null) {
			TrackData trackData = new TrackData();
			trackData.setStartTime(track.getStartTime().getTime());
			LegData legData = new LegData();
			legData.setId(track.getId());
			ValidationStatus validationStatus = track.getValidationResult().getValidationStatus();
			legData.setMean(validationStatus.getModeType().toString());
			if(validationStatus.getEffectiveDistances().containsKey(validationStatus.getModeType())) {
				legData.setDistance(validationStatus.getEffectiveDistances().get(validationStatus.getModeType()));
			} else {
				legData.setDistance(validationStatus.getDistance());
			}
			legData.getPoints().addAll(track.getGeolocationEvents());
			trackData.getLegs().add(legData);
			return trackData;
		}
		return null;
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
	
	@Override
	public void invalidateTripRequest(ValidateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			try {
				pgAziendaleManager.invalidateTrack(playerTrack.getCampaignId(), 
						playerTrack.getPlayerId(), playerTrack.getTrackedInstanceId());
			} catch (ServiceException e) {
				logger.warn("invalidateTripRequest error:" + e.getMessage());
				campaignMsgManager.addInvalidateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
			}
			TrackedInstance track = trackedInstanceRepository.findById(msg.getTrackedInstanceId()).orElse(null);
			errorPlayerTrack(playerTrack, track.getValidationResult().getValidationStatus().getError().toString());
			playerReportManager.removePlayerCampaignPlacings(playerTrack);							
		}
	}

	@Override
	public void updateTripRequest(UpdateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		TrackedInstance track = trackedInstanceRepository.findById(playerTrack.getTrackedInstanceId()).orElse(null);
		if((playerTrack != null) && (track != null)) {
			if(msg.getDeltaDistance() > 0) {
				try {
					TrackResult trackResult = pgAziendaleManager.updateTrack(playerTrack.getCampaignId(), 
							playerTrack.getPlayerId(), track.getId(), msg.getDeltaDistance());
					if(trackResult.getValid()) {
						LegResult legResult = trackResult.getLegs().get(0);
						double deltaDistance = legResult.getValidDistance() - playerTrack.getDistance();
						if(deltaDistance > 0) {
							double deltaCo2 = Utils.getSavedCo2(legResult.getMean(), deltaDistance); 
							playerTrack.setDistance(legResult.getValidDistance());
							playerTrack.setCo2(playerTrack.getCo2() + deltaCo2);
							campaignPlayerTrackRepository.save(playerTrack);
							playerReportManager.updatePlayerCampaignPlacings(playerTrack, deltaDistance, deltaCo2);							
						}
					}
				} catch (ServiceException e) {
					logger.warn("updateTripRequest error:" + e.getMessage());
					campaignMsgManager.addUpdateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
				}
			}
		}	
	}

	@Override
	public void revalidateTripRequest(UpdateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
			TrackedInstance track = trackedInstanceRepository.findById(playerTrack.getTrackedInstanceId()).orElse(null);
			TrackData trackData = getTrackData(track);
			if(trackData != null) {
				try {
					TrackResult trackResult = pgAziendaleManager.validateTrack(playerTrack.getCampaignId(), playerTrack.getPlayerId(), trackData);
					if(!trackResult.getValid()) {
						errorPlayerTrack(playerTrack, trackResult.getErrorCode());
					} else {
						LegResult legResult = trackResult.getLegs().get(0);
						double deltaDistance = legResult.getValidDistance() - playerTrack.getDistance();
						if(deltaDistance > 0) {
							double deltaCo2 = Utils.getSavedCo2(legResult.getMean(), deltaDistance); 
							playerTrack.setDistance(legResult.getValidDistance());
							playerTrack.setCo2(playerTrack.getCo2() + deltaCo2);
							campaignPlayerTrackRepository.save(playerTrack);
							playerReportManager.updatePlayerCampaignPlacings(playerTrack, deltaDistance, deltaCo2);							
						}
					}
				} catch (ServiceException e) {
					logger.warn("revalidateTripRequest error:" + e.getMessage());
					campaignMsgManager.addRevalidateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
				}
			}				
		}
	}

}
