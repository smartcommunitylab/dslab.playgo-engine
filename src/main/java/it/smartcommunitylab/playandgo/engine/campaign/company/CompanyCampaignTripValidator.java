package it.smartcommunitylab.playandgo.engine.campaign.company;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.ServiceException;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;
import it.smartcommunitylab.playandgo.engine.lock.UserCampaignLock;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.manager.PlayerCampaignPlacingManager;
import it.smartcommunitylab.playandgo.engine.manager.azienda.LegData;
import it.smartcommunitylab.playandgo.engine.manager.azienda.LegResult;
import it.smartcommunitylab.playandgo.engine.manager.azienda.PgAziendaleManager;
import it.smartcommunitylab.playandgo.engine.manager.azienda.TrackData;
import it.smartcommunitylab.playandgo.engine.manager.azienda.TrackResult;
import it.smartcommunitylab.playandgo.engine.manager.ext.CampaignMsgManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.CampaignWebhook.EventType;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.UpdateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.WebhookRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
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
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignMsgManager campaignMsgManager;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;

	@Autowired
	TerritoryRepository territoryRepository;

	@Autowired
	UserCampaignLock campaignLock;

	@PostConstruct
	public void init() {
		queueManager.setManageValidateCampaignTripRequest(this, Type.company);
	}

	@Override
	public void validateTripRequest(ValidateCampaignTripRequest msg) {
	    Campaign campaign = campaignRepository.findById(msg.getCampaignId()).orElse(null);
		if(campaign == null) return;
		TrackData trackData = new TrackData();
	    boolean sendValidation = fillTrackData(msg.getPlayerId(), msg.getMultimodalId(), 
	            msg.getCampaignId(), trackData);
        if(sendValidation) {
			ZonedDateTime startingDay = getTrackDay(campaign, new Date(trackData.getStartTime()));		
            try {
				campaignLock.lock(campaignLock.getKey(msg.getPlayerId(), msg.getCampaignId()));
                TrackResult trackResult = pgAziendaleManager.validateTrack(msg.getCampaignId(), msg.getPlayerId(), trackData);
                if(!trackResult.getValid()) {
                    for(LegData legData : trackData.getLegs()) {
                        CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(msg.getPlayerId(), 
                                msg.getCampaignId(), legData.getId());
                        if(playerTrack != null) {
                            errorPlayerTrack(playerTrack, trackResult.getErrorCode());                               
                        }
                    }                
                } else {
					List<CampaignPlayerTrack> playerTracks = new ArrayList<>();
                    for(LegResult legResult : trackResult.getLegs()) {
                        CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(msg.getPlayerId(), 
                                msg.getCampaignId(), legResult.getId());
                        if(playerTrack != null) {
							TrackedInstance track = trackedInstanceRepository.findById(legResult.getId()).orElse(null);
							populatePlayerTrack(track, playerTrack, legResult, getCompanyId(playerTrack), startingDay, 
									trackResult.isVirtualTrack(), trackData.getFirstTrackId());
							playerTracks.add(playerTrack);
                            sendWebhookRequest(playerTrack);                                                                
                        }
                    }
					updatePlayerCampaignPlacings(playerTracks, startingDay);
                }
            } catch (ServiceException e) {
                logger.error("validateTripRequest error:" + e.getMessage());
                campaignMsgManager.addValidateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
            } finally {
				campaignLock.unlock(campaignLock.getKey(msg.getPlayerId(), msg.getCampaignId()));
			}  
        }	    
	}

	private void updatePlayerCampaignPlacings(List<CampaignPlayerTrack> playerTracks, ZonedDateTime firstTrackStartTime) throws ServiceException {
	    if(playerTracks == null || playerTracks.isEmpty()) return;
		try {
			// get groupId from first track
			CampaignPlayerTrack pt = playerTracks.get(0);
			String groupId = getCompanyId(pt);	
			// get set of modeTypes
			Set<String> modeTypes = Utils.getModeTypesFromPlayerTracks(playerTracks);
			// for each modeType update placings
			for (String modeType : modeTypes) {
				playerReportManager.updatePlayerCampaignPlacings(pt.getPlayerId(), pt.getCampaignId(), 
						modeType, groupId, firstTrackStartTime);
			}
		} catch (Exception e) {
			throw new ServiceException("updatePlayerCampaignPlacings error:" + e.getMessage());
		}
	}
	
	private String getCompanyId(CampaignPlayerTrack pt) {
	    CampaignSubscription cs = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(pt.getCampaignId(), pt.getPlayerId());
	    if(cs != null) {
	        return (String) cs.getCampaignData().get(CompanyCampaignSubscription.companyKey);
	    }
	    return null;
	}
	
	private boolean fillTrackData(String playerId, String multimodalId, String campaignId, TrackData trackData) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if(campaign != null) {
            boolean sendValidation = false;            
            List<TrackedInstance> trackList = getTrackedInstance(playerId, multimodalId);
            trackData.setMultimodalId(multimodalId);
            Date startTime = new Date(4102358400000L); //2099-12-31
            for(TrackedInstance track : trackList) {
                if(track.getStartTime().before(startTime)) {
                    startTime = track.getStartTime();
                    trackData.setFirstTrackId(track.getId());
                }
                LegData legData = null;
                if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
                    legData = getLegData(track);
                } else {
                    legData = getLegDataForInvalid(track);
                }
                if(legData != null) {
                    trackData.getLegs().add(legData);
                }                   
                if(Utils.checkMean(campaign, track.getFreeTrackingTransport())) {
                    if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
						CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, 
							campaignId, track.getId());
						if(playerTrack != null) {
							//send if at least one track is valid
							sendValidation = true;                         							
						}
                    }
                }
            }
            trackData.setStartTime(startTime.getTime());
            logger.info("filltTrackData:" + trackData.toString());
            return sendValidation;
        } else {
            return false;
        }
	}

    private List<TrackedInstance> getTrackedInstance(String userId, String multimodalId) {
        return trackedInstanceRepository.findByUserIdAndMultimodalId(userId, multimodalId, Sort.by(Direction.ASC, "startTime"));
    }
	
	private void populatePlayerTrack(TrackedInstance track, CampaignPlayerTrack playerTrack, 
	        LegResult legResult, String groupId, ZonedDateTime firstTrackStartTime, boolean isVirtualTrack, String firstTrackId) {
	    playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
	    playerTrack.setVirtualScore(legResult.getVirtualScore());
		playerTrack.setVirtualTrack(false);
		if(isVirtualTrack && firstTrackId.equals(track.getId())) {
	    	playerTrack.setVirtualTrack(true);
		}	
		playerTrack.setValid(true);
        playerTrack.setErrorCode(null);
		playerTrack.setModeType(legResult.getMean());
		playerTrack.setDistance(legResult.getDistance());
		playerTrack.setCo2(Utils.getSavedCo2(legResult.getMean(), legResult.getDistance()));
		if(track != null) {
			playerTrack.setDuration(track.getValidationResult().getValidationStatus().getDuration());
			playerTrack.setStartTime(track.getStartTime());
			playerTrack.setEndTime(Utils.getEndTime(track));

		}
		if(Utils.isNotEmpty(groupId)) playerTrack.setGroupId(groupId);
		playerTrack.setStartingDay(firstTrackStartTime.format(Utils.dtfDay));
		campaignPlayerTrackRepository.save(playerTrack);
	}
	
    private void errorPlayerTrack(CampaignPlayerTrack playerTrack, String errorCode) {
        playerTrack.setScoreStatus(ScoreStatus.COMPUTED);
        playerTrack.setValid(false);
        playerTrack.setErrorCode(errorCode);
        campaignPlayerTrackRepository.save(playerTrack);
    }
    
	private LegData getLegData(TrackedInstance track) {
		if(track != null) {
			LegData legData = new LegData();
			legData.setId(track.getId());
			ValidationStatus validationStatus = track.getValidationResult().getValidationStatus();
			legData.setMean(validationStatus.getModeType().toString());
			legData.setDistance(Utils.getTrackDistance(track));
			legData.setDuration(validationStatus.getDuration());
			legData.setCo2(Utils.getSavedCo2(validationStatus.getModeType().toString(), Utils.getTrackDistance(track)));
			legData.getPoints().addAll(track.getGeolocationEvents());
			legData.setValid(true);
			return legData;
		}
		return null;
	}
	
    private LegData getLegDataForInvalid(TrackedInstance track) {
        if(track != null) {
            LegData legData = new LegData();
            legData.setId(track.getId());
            legData.setMean(track.getFreeTrackingTransport());
            legData.getPoints().addAll(track.getGeolocationEvents());
            legData.setValid(false);
            return legData;
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

	private ZonedDateTime getTrackDay(Campaign campaign, Date date) {		
		ZoneId zoneId = null;
		Territory territory = territoryRepository.findById(campaign.getTerritoryId()).orElse(null);
		if(territory == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = ZoneId.of(territory.getTimezone());
		}
		return ZonedDateTime.ofInstant(date.toInstant(), zoneId);
	}

	
	@Override
	public void invalidateTripRequest(UpdateCampaignTripRequest msg) {
		CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(playerTrack != null) {
		    TrackedInstance track = trackedInstanceRepository.findById(playerTrack.getTrackedInstanceId()).orElse(null);
		    if(track != null) {
	            try {
					campaignLock.lock(campaignLock.getKey(playerTrack.getPlayerId(), playerTrack.getCampaignId()));					
	                pgAziendaleManager.invalidateTrack(playerTrack.getCampaignId(), 
	                        playerTrack.getPlayerId(), playerTrack.getTrackedInstanceId());
	                List<TrackedInstance> trackList = getTrackedInstance(playerTrack.getPlayerId(), track.getMultimodalId());
	                // sort trackList by startTime
	                trackList.sort((t1, t2) -> t1.getStartTime().compareTo(t2.getStartTime()));
					for(TrackedInstance ti : trackList) {
                        CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerTrack.getPlayerId(), 
                                playerTrack.getCampaignId(), ti.getId());
                        if((pTrack != null) && (pTrack.isValid())) {
                            errorPlayerTrack(pTrack, track.getValidationResult().getValidationStatus().getError().toString());
                            //playerReportManager.updatePlayerCampaignPlacings(null, null, null, null, null);                                              
                        }
	                }
					Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
					ZonedDateTime startingDay = getTrackDay(campaign, trackList.get(0).getStartTime());
					String groupId = getCompanyId(playerTrack);
					// finally update placings
					playerReportManager.updatePlayerCampaignPlacings(playerTrack.getPlayerId(), playerTrack.getCampaignId(), 
							playerTrack.getModeType(), groupId, startingDay);
	            } catch (ServiceException e) {
	                logger.error("invalidateTripRequest error:" + e.getMessage());
	                campaignMsgManager.addInvalidateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
	            } finally {
					campaignLock.unlock(campaignLock.getKey(playerTrack.getPlayerId(), playerTrack.getCampaignId()));
				}
		    }
		}
	}

	@Override
	public void updateTripRequest(UpdateCampaignTripRequest msg) {
	    logger.info(String.format("updateTripRequest[%s]:%s", msg.getCampaignPlayerTrackId(), msg.getCampaignType()));
	    revalidateTripRequest(msg);
	}

	@Override
	public void revalidateTripRequest(UpdateCampaignTripRequest msg) {
		CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findById(msg.getCampaignPlayerTrackId()).orElse(null);
		if(pTrack != null) {
		    Campaign campaign = campaignRepository.findById(pTrack.getCampaignId()).orElse(null);
		    TrackedInstance uTrack = trackedInstanceRepository.findById(pTrack.getTrackedInstanceId()).orElse(null);
		    if((campaign != null) && (uTrack != null)) {
		        TrackData trackData = new TrackData();
		        boolean sendValidation = fillTrackData(pTrack.getPlayerId(), uTrack.getMultimodalId(), 
		                pTrack.getCampaignId(), trackData);
		        if(sendValidation) {
					ZonedDateTime startingDay = getTrackDay(campaign, new Date(trackData.getStartTime()));
		            try {
						campaignLock.lock(campaignLock.getKey(pTrack.getPlayerId(), pTrack.getCampaignId()));
						List<CampaignPlayerTrack> playerTracks = new ArrayList<>();
		                TrackResult trackResult = pgAziendaleManager.validateTrack(pTrack.getCampaignId(), pTrack.getPlayerId(), trackData);
		                if(!trackResult.getValid()) {
		                    for(LegData legData : trackData.getLegs()) {
		                        CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(pTrack.getPlayerId(), 
		                                pTrack.getCampaignId(), legData.getId());
		                        if(playerTrack != null) {
		                            errorPlayerTrack(playerTrack, trackResult.getErrorCode()); 
									playerTracks.add(playerTrack);     
		                        }
		                    }                
		                } else {
		                    for(LegResult legResult : trackResult.getLegs()) {
		                        CampaignPlayerTrack playerTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(pTrack.getPlayerId(), 
		                                pTrack.getCampaignId(), legResult.getId());
		                        if(playerTrack != null) {
		                            TrackedInstance track = trackedInstanceRepository.findById(legResult.getId()).orElse(null);
									populatePlayerTrack(track, playerTrack, legResult, getCompanyId(playerTrack), 
										startingDay, trackResult.isVirtualTrack(), trackData.getFirstTrackId());
									playerTracks.add(playerTrack);                        
		                        }
		                    }
		                }
						updatePlayerCampaignPlacings(playerTracks, startingDay);						
		            } catch (ServiceException e) {
		                logger.error("revalidateTripRequest error:" + e.getMessage());
		                campaignMsgManager.addRevalidateTripRequest(msg, Type.company, e.getMessage(), e.getCode());
		            } finally {
						campaignLock.unlock(campaignLock.getKey(pTrack.getPlayerId(), pTrack.getCampaignId()));}                          
		        }   		        
		    }				
		}
	}

}
