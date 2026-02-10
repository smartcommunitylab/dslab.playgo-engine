package it.smartcommunitylab.playandgo.engine.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

import it.smartcommunitylab.playandgo.engine.dto.CampaignTripInfo;
import it.smartcommunitylab.playandgo.engine.dto.PlayerInfo;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceConsole;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstancePoly;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus.ERROR_TYPE;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus.MODE_TYPE;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.UpdateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;
import it.smartcommunitylab.playandgo.engine.util.Utils;
import it.smartcommunitylab.playandgo.engine.validation.GeolocationsProcessor;
import it.smartcommunitylab.playandgo.engine.validation.PTDataHelper;
import it.smartcommunitylab.playandgo.engine.validation.ValidationConstants;
import it.smartcommunitylab.playandgo.engine.validation.ValidationService;

@Component
public class TrackedInstanceManager implements ManageValidateTripRequest {
	private static Log logger = LogFactory.getLog(TrackedInstanceManager.class);
	
	public static final String meansKey = "means";
	
	@Autowired
	MongoTemplate mongoTemplate;
	
	@Autowired
	MessageQueueManager queueManager;

	@Autowired
	TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	CampaignRepository campaignRepository;

	@Autowired
	GeolocationsProcessor geolocationsProcessor;
	
	@Autowired
	ValidationService validationService;
	
	@PostConstruct
	public void init() {
		queueManager.setManageValidateTripRequest(this);
	}
	
	public Page<TrackedInstanceInfo> getTrackedInstanceInfoList(String playerId, Date dateFrom, Date dateTo, Pageable pageRequest) {
		PageRequest pageRequestNew = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "startTime"));
		Criteria criteria = new Criteria("userId").is(playerId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.andOperator(Criteria.where("startTime").gte(dateFrom), Criteria.where("startTime").lte(dateTo));
		} 
		Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "startTime")).with(pageRequestNew);
		List<TrackedInstance> trackList = mongoTemplate.find(query, TrackedInstance.class);
		List<TrackedInstanceInfo> result = new ArrayList<>();
		for(TrackedInstance track : trackList) {
			TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(track, playerId, null);		
			result.add(trackInfo);
		}
		return new PageImpl<>(result, pageRequestNew, trackedInstanceRepository.countByUserId(playerId));
	}

	public Page<TrackedInstanceInfo> getTrackedInstanceInfoList(String playerId, String campaignId, 
			Date dateFrom, Date dateTo, Pageable pageRequest) {
		PageRequest pageRequestNew = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "startTime"));
		Criteria criteria = new Criteria("playerId").is(playerId).and("campaignId").is(campaignId);
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.andOperator(Criteria.where("startTime").gte(dateFrom), Criteria.where("startTime").lte(dateTo));
		} 
		Query query = new Query(criteria).with(pageRequestNew);
		List<CampaignPlayerTrack> trackList = mongoTemplate.find(query, CampaignPlayerTrack.class);
		List<TrackedInstanceInfo> result = new ArrayList<>();
		for(CampaignPlayerTrack track : trackList) {
			TrackedInstance trackedInstance = trackedInstanceRepository.findById(track.getTrackedInstanceId()).orElse(null);
			TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(trackedInstance, playerId, campaignId);
			result.add(trackInfo);
		}
		return new PageImpl<>(result, pageRequestNew, trackedInstanceRepository.countByUserId(playerId));
	}

	public TrackedInstanceInfo getTrackedInstanceInfo(String playerId, String trackedInstanceId) throws Exception {
		TrackedInstance track = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(track == null) {
			throw new BadRequestException("track not found", ErrorCode.TRACK_NOT_FOUND);
		}
		TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(track, playerId, null);
		trackInfo.setPolyline(getPolyline(Lists.newArrayList(track.getGeolocationEvents())));
		return trackInfo;
	}
	
	public TrackedInstanceInfo getTrackedInstanceInfo(String playerId, String trackedInstanceId, String campaignId) throws Exception {
		TrackedInstance track = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(track == null) {
			throw new BadRequestException("track not found", ErrorCode.TRACK_NOT_FOUND);
		}
		TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(track, playerId, campaignId);
		trackInfo.setPolyline(getPolyline(Lists.newArrayList(track.getGeolocationEvents())));
		return trackInfo;
	}
	
	private String getPolyline(List<Geolocation> geo) {
		geo = GamificationHelper.optimize(geo);
		Collections.sort(geo);
		String poly = GamificationHelper.encodePoly(geo);
		return poly;
	}
	
	private TrackedInstanceInfo getTrackedInstanceInfoFromTrack(TrackedInstance track, String playerId, String campaignId) {
		TrackedInstanceInfo trackInfo = new TrackedInstanceInfo();
		trackInfo.setTrackedInstanceId(track.getId());
		trackInfo.setClientId(track.getClientId());
		trackInfo.setMultimodalId(track.getMultimodalId());
		trackInfo.setStartTime(track.getStartTime());
		trackInfo.setEndTime(Utils.getEndTime(track));
		trackInfo.setValidity(track.getValidationResult().getTravelValidity());
		trackInfo.setDistance(track.getValidationResult().getValidationStatus().getDistance());
		if(track.getValidationResult().getValidationStatus().getModeType() != null) {
			trackInfo.setModeType(track.getValidationResult().getValidationStatus().getModeType().toString());
		} else {
			trackInfo.setModeType(track.getFreeTrackingTransport());
		}
		//campaigns info
		Map<String, CampaignTripInfo> campaignInfoMap = new HashMap<>();
		List<CampaignPlayerTrack> playerTrackList = campaignPlayerTrackRepository.findByPlayerIdAndTrackedInstanceId(playerId, track.getId());
		for(CampaignPlayerTrack playerTrack : playerTrackList) {
			if((campaignId != null) && !playerTrack.getCampaignId().equals(campaignId)) {
				continue;
			}
			CampaignTripInfo info = campaignInfoMap.get(playerTrack.getCampaignId());
			if(info == null) {
				info = new CampaignTripInfo();
				Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
				info.setCampaignId(campaign.getCampaignId());
				info.setCampaignName(campaign.getName());
				info.setType(campaign.getType());
				info.setValid(playerTrack.isValid());
				if(!info.isValid()) {
					info.setErrorCode(playerTrack.getErrorCode());	
				}
				info.setDistance(playerTrack.getDistance());
				info.setScoreStatus(playerTrack.getScoreStatus());
				info.setScore(playerTrack.getScore());
				info.setVirtualScore(playerTrack.getVirtualScore());
				campaignInfoMap.put(campaign.getCampaignId(), info);
			}
		}
		trackInfo.getCampaigns().addAll(campaignInfoMap.values());
		return trackInfo;
	}	
	
	/**
	public List<TripInfo> getTripInfoList(Player player, Pageable pageRequest) {
		List<TripInfo> result = new ArrayList<>();
		
		//get trips
		MatchOperation matchOperation = Aggregation.match(new Criteria("playerId").is(player.getPlayerId()));
		GroupOperation groupOperation = Aggregation.group("multimodalId").min("startTime").as("minStartTime");
		SortOperation sortOperation = Aggregation.sort(Sort.by(Direction.DESC, "minStartTime"));
		SkipOperation skipOperation = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limitOperation = Aggregation.limit(pageRequest.getPageSize());
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, sortOperation, skipOperation, limitOperation);
		AggregationResults<Document> trips = mongoTemplate.aggregate(aggregation, TrackedInstance.class, Document.class);
		for(Document doc : trips.getMappedResults()) {
			String multimodalId = doc.getString("_id");
			TripInfo tripInfo = new TripInfo();
			
			//tracks info
			List<TrackedInstance> trackList = trackedInstanceRepository.findByMultimodalId(multimodalId, Sort.by(Sort.Direction.ASC, "startTime"));
			double distance = 0.0;
			Date startTime = doc.getDate("minStartTime");
			Date endTime = null;
			for(TrackedInstance track : trackList) {
				TrackedInstanceInfo info = new TrackedInstanceInfo();
				info.setStartTime(track.getStartTime());
				info.setEndTime(getEndTime(track));
				info.setValidity(track.getValidationResult().getTravelValidity());
				tripInfo.getTracks().add(info);
				endTime = info.getEndTime();
				distance += track.getValidationResult().getValidationStatus().getDistance();
			}
			tripInfo.setStartTime(startTime);
			tripInfo.setEndTime(endTime);
			tripInfo.setDistance(distance);
			
			//campaigns info
			Map<String, CampaignTripInfo> campaignInfoMap = new HashMap<>();
			for(TrackedInstance track : trackList) {
				List<CampaignPlayerTrack> playerTrackList = campaignPlayerTrackRepository.findByPlayerIdAndTrackedInstanceId(player.getPlayerId(), track.getId());
				for(CampaignPlayerTrack playerTrack : playerTrackList) {
					CampaignTripInfo info = campaignInfoMap.get(playerTrack.getCampaignId());
					if(info == null) {
						info = new CampaignTripInfo();
						Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
						info.setCampaignId(campaign.getCampaignId());
						info.setCampaignName(campaign.getName());
						campaignInfoMap.put(campaign.getCampaignId(), info);
					}
					info.setScore(info.getScore() + playerTrack.getScore());
				}
			}
			tripInfo.getCampaigns().addAll(campaignInfoMap.values());
			
			//(campaignSubscriptionRepository
			result.add(tripInfo);
		}
		return result;
	}**/
	
	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, Player player) throws Exception {
		List<TrackedInstance> list = geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, player);
		List<String> multimodalIds = new ArrayList<>();
		for(TrackedInstance ti : list) {
		    if(!multimodalIds.contains(ti.getMultimodalId())) {
		        multimodalIds.add(ti.getMultimodalId());
		    }
		}
		for(String multimodalId : multimodalIds) {
	        ValidateTripRequest request = new ValidateTripRequest(player.getPlayerId(), player.getTerritoryId(), multimodalId, false);
	        queueManager.sendValidateTripRequest(request);
			//add delay of 100 ms
			Thread.sleep(100);		    
		}
	}
	
	public List<TrackedInstance> getPlayerTrakedInstaces(String playerId, Pageable pageRequest) {
		PageRequest pageRequestNew = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "day"));
		return trackedInstanceRepository.findByUserId(playerId, pageRequestNew);
	}
	
	public List<TrackedInstance> getTrackedInstance(String userId, String multimodalId) {
		return trackedInstanceRepository.findByUserIdAndMultimodalId(userId, multimodalId, Sort.by(Direction.ASC, "startTime"));
	}
	
	public TrackedInstance getTrackedInstance(String trackedInstanceId) {
	    return trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
	}
	
	private void updateValidationResult(TrackedInstance track, ValidationResult result) {
		track.setValidationResult(result);
        if(TravelValidity.VALID.equals(result.getTravelValidity())) {
            track.setToCheck(Boolean.FALSE);
        }
        if(TravelValidity.INVALID.equals(result.getTravelValidity())) {

            if(ValidationStatus.ERROR_TYPE.NO_DATA.equals(result.getValidationStatus().getError()) ||
                    ValidationStatus.ERROR_TYPE.TOO_SHORT.equals(result.getValidationStatus().getError()) ||
					ValidationStatus.ERROR_TYPE.OUT_OF_AREA.equals(result.getValidationStatus().getError())) {
                track.setToCheck(Boolean.FALSE);
            } else {
                track.setToCheck(Boolean.TRUE);
            }
        }
		trackedInstanceRepository.save(track);
	}

	private void updateValidationResultAsError(TrackedInstance track) {
		ValidationResult vr = new ValidationResult();
		ValidationStatus status = new ValidationStatus();
		status.setValidationOutcome(TravelValidity.INVALID);
		vr.setValidationStatus(status);
		track.setValidationResult(vr);
		track.setToCheck(Boolean.TRUE);
		trackedInstanceRepository.save(track);
	}
	
	@Override
	public void validateTripRequest(ValidateTripRequest msg) {
	    try {
	        List<Pair<String, String>> validatePairList = new ArrayList<>();
	        validatePairList.add(Pair.of(msg.getPlayerId(), msg.getMultimodalId())); 
	        List<TrackedInstance> list = getTrackedInstance(msg.getPlayerId(), msg.getMultimodalId());
	        for(TrackedInstance track : list) {
	            if (!StringUtils.hasText(track.getSharedTravelId())) {
	                // free tracking
	                validateFreeTrackingTripRequest(track, msg.isForceValidation());
	            } else {
	                // carsharing
	                List<Pair<String, String>> validateSharedTravelRequest = validateSharedTravelRequest(track, msg.isForceValidation());
	                mergePair(validatePairList, validateSharedTravelRequest);
	            }           
	        }
	        logger.info(String.format("validateTripRequest: %s", validatePairList.toString()));
	        for(Pair<String, String> pair : validatePairList) {
	            ValidateTripRequest newMsg = new ValidateTripRequest(pair.getLeft(), msg.getTerritoryId(), pair.getRight(), msg.isForceValidation());
	            sendValidateCampaignRequest(newMsg);
	        }            
        } catch (Exception e) {
            logger.error(String.format("validateTripRequest:%s", e.getMessage()));
        }
	}
	
	private void mergePair(List<Pair<String, String>> validatePairList, List<Pair<String, String>> newList) {
	    for(Pair<String, String> newPair : newList) {
	        boolean found = false;
	        for(Pair<String, String> pair : validatePairList) {
	            if(newPair.getLeft().equals(pair.getLeft()) && newPair.getRight().equals(pair.getRight())) {
	                found = true;
	                break;
	            }
	        }
	        if(!found) {
	            validatePairList.add(newPair);
	        }
	    }
	}
	
	private void sendValidateCampaignRequest(ValidateTripRequest msg) {
	    List<TrackedInstance> list = getTrackedInstance(msg.getPlayerId(), msg.getMultimodalId());
        boolean pendingTrip = false;
        boolean validTrip = false;
        for(TrackedInstance track : list) {
            if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
                validTrip = true;
            }
            if(TravelValidity.PENDING.equals(track.getValidationResult().getTravelValidity())) {
                pendingTrip = true;
            }
        }
        logger.info(String.format("sendValidateCampaignRequest: %s [valid:%s, pending:%s]", msg.getMultimodalId(), validTrip, pendingTrip));
        if(validTrip && !pendingTrip) {
            List<CampaignSubscription> listSub = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(msg.getPlayerId(), msg.getTerritoryId());
            for(CampaignSubscription sub : listSub) {
                Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
                for(TrackedInstance track : list) {
                    if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
	        	        // skip non-active campaigns
						if((campaign.validTrack(track.getStartTime()) && campaign.currentlyActive()) || 
								(campaign.validTrack(track.getStartTime()) && msg.isForceValidation())) {
							storeCampaignPlayerTrack(msg.getTerritoryId(), msg.getPlayerId(), track.getId(), campaign, sub.getId());
						} 
                    }
                }
                ValidateCampaignTripRequest request = new ValidateCampaignTripRequest(msg.getPlayerId(), 
                        msg.getTerritoryId(), msg.getMultimodalId(), sub.getCampaignId(), sub.getId(), campaign.getType().toString());
                try {
                    queueManager.sendValidateCampaignTripRequest(request);
                } catch (Exception e) {
                    logger.error("sendValidateCampaignRequest error:" + e.getMessage());
                }                                               
            }
        }	    
	}

	/**
	 * Validate free tracking: validate on territory
	 * @param msg
	 * @param track
	 */
	private void validateFreeTrackingTripRequest(TrackedInstance track, boolean forceValidation) {
		if(!forceValidation) {
	        try {
	            ValidationResult validationResult = validationService.validateFreeTracking(track.getGeolocationEvents(), 
	                    track.getFreeTrackingTransport(), track.getTerritoryId());
	            updateValidationResult(track, validationResult);
	        } catch (Exception e) {
	            logger.error("validateFreeTrackingTripRequest error:" + e.getMessage());
	            updateValidationResultAsError(track);
	        }           		    
		}
	}

	private void storeCampaignPlayerTrack(String territoryId, String playerId, String trackedInstanceId,
	        Campaign campaign, String campaignSubscriptionId) {
		TrackedInstance trackedInstance = getTrackedInstance(trackedInstanceId);
		if(trackedInstance != null) {
			ValidationStatus validationStatus = trackedInstance.getValidationResult().getValidationStatus();
			if(Utils.checkMean(campaign, validationStatus.getModeType().toString())) {
				// keep existing track
				CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, campaign.getCampaignId(), trackedInstanceId);
				if (pTrack == null) {
					pTrack = new CampaignPlayerTrack();
					pTrack.setPlayerId(playerId);
					pTrack.setCampaignId(campaign.getCampaignId());
					pTrack.setCampaignSubscriptionId(campaignSubscriptionId);
					pTrack.setTrackedInstanceId(trackedInstanceId);
					pTrack.setTerritoryId(territoryId);
					pTrack.setScoreStatus(ScoreStatus.UNASSIGNED);
					pTrack.setDuration(trackedInstance.getValidationResult().getValidationStatus().getDuration());
					pTrack.setStartTime(trackedInstance.getStartTime());
					pTrack.setEndTime(Utils.getEndTime(trackedInstance));     
					pTrack.setModeType(validationStatus.getModeType().toString());
					pTrack = campaignPlayerTrackRepository.save(pTrack);
				}
			}
		}
	}
	
	/**
	 * For each campaign subscribed send a invalidate message for the specific track
	 */
	private void invalidateCampaigns(String playerId, String territoryId, String trackedInstanceId) throws Exception {
		List<CampaignSubscription> list = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(playerId, territoryId);
		for(CampaignSubscription sub : list) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, 
			        campaign.getCampaignId(), trackedInstanceId);
			if(pTrack != null) {
			    UpdateCampaignTripRequest request = new UpdateCampaignTripRequest(campaign.getType().toString(), pTrack.getId(), 0.0);
				queueManager.sendInvalidateCampaignTripRequest(request);
			}
		}
	}

	/**
	 * For each campaign subscribed send an update distance message for the specific track
	 */	
	private void updateCampaigns(String playerId, String territoryId, String trackedInstanceId, double deltaDistance) throws Exception {
		List<CampaignSubscription> list = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(playerId, territoryId);
		for(CampaignSubscription sub : list) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(playerId, 
			        campaign.getCampaignId(), trackedInstanceId);
			if(pTrack != null) {
				UpdateCampaignTripRequest request = new UpdateCampaignTripRequest(campaign.getType().toString(), pTrack.getId(), deltaDistance);
				queueManager.sendUpdateCampaignTripRequest(request);
			}
		}
	}
	
	private List<Pair<String, String>> validateSharedTravelRequest(TrackedInstance track, boolean forceValidation) {
	    List<Pair<String, String>> travelToValidateList = new ArrayList<>(); 
		String sharedId = track.getSharedTravelId();
		try {
			if (ValidationConstants.isDriver(sharedId)) {
				String passengerTravelId = ValidationConstants.getPassengerTravelId(sharedId);
				List<TrackedInstance> list = trackedInstanceRepository.findPassengerTrips(track.getTerritoryId(), passengerTravelId, track.getUserId());
				if (!list.isEmpty()) {
				    //validate driver travel
		            ValidationResult driverVr = validationService.validateSharedTripDriver(track, forceValidation);
		            updateValidationResult(track, driverVr);				    
					for(TrackedInstance passengerTravel: list) {
				        // validate passenger trip
					    if(TravelValidity.PENDING.equals(passengerTravel.getValidationResult().getTravelValidity())) {
	                        ValidationResult vr = validationService.validateSharedTripPassenger(passengerTravel, track, false);
	                        updateValidationResult(passengerTravel, vr);
	                        travelToValidateList.add(Pair.of(passengerTravel.getUserId(), passengerTravel.getMultimodalId()));					        
					    }
					}
				} else {
				    setValidationStatusPending(track);
				    trackedInstanceRepository.save(track);
				}
			} else {
				String driverTravelId = ValidationConstants.getDriverTravelId(sharedId);
				TrackedInstance driverTravel = trackedInstanceRepository.findDriverTrip(track.getTerritoryId(), driverTravelId, track.getUserId());
				if (driverTravel != null) {
				    //validate passenger travel
			        ValidationResult vr = validationService.validateSharedTripPassenger(track, driverTravel, forceValidation);
			        updateValidationResult(track, vr);
			        //validate also driver travel
		            if(TravelValidity.PENDING.equals(driverTravel.getValidationResult().getTravelValidity())) {
		                ValidationResult driverVr = validationService.validateSharedTripDriver(driverTravel, false);
		                updateValidationResult(driverTravel, driverVr);
		                travelToValidateList.add(Pair.of(driverTravel.getUserId(), driverTravel.getMultimodalId()));
		            }
				} else {
                    setValidationStatusPending(track);
                    trackedInstanceRepository.save(track);
				}
			}
		} catch (Exception e) {
			logger.error("validateSharedTravelRequest error" + e.getMessage(), e);
			updateValidationResultAsError(track);
		}
		return travelToValidateList;
	}
	
	private void setValidationStatusPending(TrackedInstance track) {
	    if(track.getValidationResult() == null) {
	        track.setValidationResult(new ValidationResult());
	    }
	    if(track.getValidationResult().getValidationStatus() == null) {
	        track.getValidationResult().setValidationStatus(new ValidationStatus()); 
	    }
	    track.getValidationResult().getValidationStatus().setValidationOutcome(TravelValidity.PENDING);
	}
	
	public Page<TrackedInstanceConsole> searchTrackedInstance(String territoryId, String trackedInstanceId, String multimodalId, String playerId, String modeType, 
			String campaignId, String validationStatus, String scoreStatus, Boolean campaignValidity, Boolean toCheck, Date dateFrom, Date dateTo, Pageable pageRequest) {
		List<AggregationOperation> operations = new ArrayList<>();
        
		Criteria criteria = new Criteria("territoryId").is(territoryId);
		if(Utils.isNotEmpty(trackedInstanceId)) {
			criteria = criteria.and("id").is(trackedInstanceId);
		}
        if(Utils.isNotEmpty(multimodalId)) {
            criteria = criteria.and("multimodalId").is(multimodalId);
        }		
		if(Utils.isNotEmpty(playerId)) {
			//criteria = criteria.and("userId").is(playerId);
			criteria = criteria.orOperator(Criteria.where("userId").is(playerId), Criteria.where("nickname").is(playerId));
		}
		if(Utils.isNotEmpty(modeType)) {
			criteria = criteria.and("freeTrackingTransport").is(modeType);
		}
		if(Utils.isNotEmpty(validationStatus)) {
			criteria = criteria.and("validationResult.validationStatus.validationOutcome").is(validationStatus);	
		}
		if(toCheck != null) {
		    if(toCheck) {
		        criteria = criteria.and("toCheck").ne(false);
		    } else {
		        criteria = criteria.and("toCheck").is(false);
		    }
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.andOperator(Criteria.where("startTime").gte(dateFrom), Criteria.where("startTime").lte(dateTo));
		}
        MatchOperation match = Aggregation.match(criteria);
        operations.add(match);
        
        ProjectionOperation project = Aggregation.project().andExclude("geolocationEvents");
        operations.add(project);        
        
        
		if(Utils.isNotEmpty(campaignId)) {
	        AddFieldsOperation addFields = AddFieldsOperation.addField("trackId").withValueOfExpression("{ \"$toString\": \"$_id\" }").build();
	        operations.add(addFields);

            LookupOperation lookup = Aggregation.lookup("campaignPlayerTracks", "trackId", "trackedInstanceId", "campaignPlayerTracks");
            operations.add(lookup);
            
            Criteria criteriaLookup = new Criteria("campaignPlayerTracks.campaignId").is(campaignId);
			if(Utils.isNotEmpty(scoreStatus)) {
			    criteriaLookup = criteriaLookup.and("campaignPlayerTracks.scoreStatus").is(scoreStatus);
			}
			if (campaignValidity != null) {
			    criteriaLookup = criteriaLookup.and("campaignPlayerTracks.valid").is(campaignValidity);
			}
			operations.add(Aggregation.match(criteriaLookup));
		}
		
		SortOperation sort = Aggregation.sort(pageRequest.getSort());
		SkipOperation skip = Aggregation.skip((long) (pageRequest.getPageNumber() * pageRequest.getPageSize()));
		LimitOperation limit = Aggregation.limit(pageRequest.getPageSize());
		List<AggregationOperation> operationsPaged = new ArrayList<>();
		operationsPaged.addAll(operations);
		if(!pageRequest.getSort().equals(Sort.unsorted())) {
			operationsPaged.add(sort);
		}
		operationsPaged.add(skip);
		operationsPaged.add(limit);
		
		Aggregation aggregation = Aggregation.newAggregation(operationsPaged);
		AggregationResults<TrackedInstance> trips = mongoTemplate.aggregate(aggregation, TrackedInstance.class, TrackedInstance.class);
		List<TrackedInstanceConsole> result = new ArrayList<>();
		for(TrackedInstance t : trips.getMappedResults()) {
			TrackedInstanceConsole tc = new TrackedInstanceConsole();
			tc.setTrackedInstance(t);
			tc.setPlayerInfo(getPlayerInfo(t.getUserId()));
			result.add(tc);
		}
		return new PageImpl<>(result, pageRequest, countRecords(operations, "trackedInstances"));
	}
	
	private long countRecords(List<AggregationOperation> operations, String collectionName) {
	    operations.add(Aggregation.count().as("totalNum"));
	    operations.add(Aggregation.project("totalNum"));
		Aggregation aggregation = Aggregation.newAggregation(operations);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				collectionName, Document.class);
		List<Document> list = aggregationResults.getMappedResults();
		if(list.size() == 1) {
		    return list.get(0).getInteger("totalNum");
		}
		return 0;
	}
	
	private PlayerInfo getPlayerInfo(String playerId) {
		PlayerInfo pi = new PlayerInfo();
		Player p = playerRepository.findById(playerId).orElse(null);
		if(p != null) {
			pi.setNickname(p.getNickname());
			pi.setPlayerId(p.getPlayerId());
			pi.setEmail(p.getMail());
		}
		return pi;
	}
	
	public TrackedInstancePoly getTrackPolylines(String territoryId, String trackedInstanceId) throws Exception {
		TrackedInstance trackedInstance = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(trackedInstance == null) {
			throw new BadRequestException("track not found", ErrorCode.TRACK_NOT_FOUND);
		}
		if(!trackedInstance.getTerritoryId().equals(territoryId)) {
			throw new BadRequestException("territory not corrisponding", ErrorCode.TERRITORY_NOT_ALLOWED);
		}
		TrackedInstancePoly ti = new TrackedInstancePoly();
		ti.setTrackedInstance(trackedInstance);
		ti.setTrackPolyline(getPolyline(Lists.newArrayList(trackedInstance.getGeolocationEvents())));
		ti.setRoutesPolylines(PTDataHelper.getPolylines(trackedInstance, trackedInstance.getTerritoryId()));
		ti.setPlayerInfo(getPlayerInfo(trackedInstance.getUserId()));
		ti.setCampaigns(getTrackedInstanceInfoFromTrack(trackedInstance, trackedInstance.getUserId(), null).getCampaigns());
		return ti;
	}
	
	public void updateValidationResult(String trackedInstanceId, TravelValidity changedValidity, 
			String modeType, Double distance, Long duration, String errorType, String note) throws Exception {
		TrackedInstance track = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(track == null) {
			throw new BadRequestException("track not found", ErrorCode.TRACK_NOT_FOUND);
		}
		if((modeType != null) && (distance != null)) {
			Map<String, Double> overriddenDistances = new HashMap<>();
			overriddenDistances.put(modeType, distance);
			track.setOverriddenDistances(overriddenDistances);
		}
		track.setChangedValidity(changedValidity);
		trackedInstanceRepository.save(track);			
		if(TravelValidity.VALID.equals(changedValidity)) {
			if(TravelValidity.INVALID.equals(track.getValidationResult().getTravelValidity())) {
				// INVALID -> VALID
				track.getValidationResult().getValidationStatus().setValidationOutcome(TravelValidity.VALID);
				track.getValidationResult().getValidationStatus().setModeType(MODE_TYPE.valueOf(modeType));
				track.getValidationResult().getValidationStatus().setDistance(distance);
				track.getValidationResult().getValidationStatus().getEffectiveDistances().put(MODE_TYPE.valueOf(modeType), distance);
				track.getValidationResult().getValidationStatus().setDuration(duration);
				track.setFreeTrackingTransport(modeType);
				track.getValidationResult().setValid(true);
				trackedInstanceRepository.save(track);
				ValidateTripRequest msg = new ValidateTripRequest();
				msg.setTerritoryId(track.getTerritoryId());
				msg.setPlayerId(track.getUserId());
				msg.setMultimodalId(track.getMultimodalId());
				msg.setForceValidation(true);
				validateTripRequest(msg);
			} else if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
				//update distance for a already validated track
				double delta = distance - Utils.getTrackDistance(track);
				track.getValidationResult().getValidationStatus().setDistance(distance);
				track.getValidationResult().getValidationStatus().getEffectiveDistances().put(MODE_TYPE.valueOf(track.getFreeTrackingTransport()), distance);
				trackedInstanceRepository.save(track);
				updateCampaigns(track.getUserId(), track.getTerritoryId(), track.getId(), delta);
			}
		} else if(TravelValidity.INVALID.equals(changedValidity)) {
			if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
				// VALID -> INVALID
				track.getValidationResult().getValidationStatus().setValidationOutcome(TravelValidity.INVALID);
				track.getValidationResult().setValid(false);
				track.getValidationResult().getValidationStatus().setError(ERROR_TYPE.valueOf(errorType));
				track.setNote(note);
				trackedInstanceRepository.save(track);
				invalidateCampaigns(track.getUserId(), track.getTerritoryId(), track.getId());
			}
		}
	}

	public void revalidateTrack(String territoryId, String campaignId, String trackedInstanceId) throws Exception {
		TrackedInstance trackedInstance = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(trackedInstance == null) {
			throw new BadRequestException("track not found", ErrorCode.TRACK_NOT_FOUND);
		}
		if(!trackedInstance.getTerritoryId().equals(territoryId)) {
			throw new BadRequestException("territory not corrisponding", ErrorCode.TERRITORY_NOT_ALLOWED);
		}
		if(!trackedInstance.getValidationResult().isValid()) {
			return;
		}
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
        CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(trackedInstance.getUserId(), 
                campaignId, trackedInstanceId);
        if(pTrack != null) {
            UpdateCampaignTripRequest request = new UpdateCampaignTripRequest(campaign.getType().toString(), pTrack.getId(), 0);
            queueManager.sendRevalidateCampaignTripRequest(request);
        }
	}
	
	public void revalidateTracks(String territoryId, String campaignId, Date dateFrom, Date dateTo) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Criteria criteria = new Criteria("territoryId").is(territoryId).and("campaignId").is(campaignId);
		criteria = criteria.andOperator(Criteria.where("startTime").gte(dateFrom), Criteria.where("startTime").lte(dateTo));
		Query query = new Query(criteria);
		List<CampaignPlayerTrack> list = mongoTemplate.find(query, CampaignPlayerTrack.class);
		for(CampaignPlayerTrack pTrack : list) {
            UpdateCampaignTripRequest request = new UpdateCampaignTripRequest(campaign.getType().toString(), pTrack.getId(), 0);
            queueManager.sendRevalidateCampaignTripRequest(request);
		}			
	}
	
	public void modifyToCheck(String trackId, boolean toCheck) {
        Query query = new Query(Criteria.where("id").is(trackId));
        Update update = new Update();
        update.set("toCheck", toCheck);
        mongoTemplate.updateFirst(query, update, TrackedInstance.class);
	}
	
}
