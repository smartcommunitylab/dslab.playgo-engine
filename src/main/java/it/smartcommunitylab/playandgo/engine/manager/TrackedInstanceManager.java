package it.smartcommunitylab.playandgo.engine.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
				info.setScoreStatus(playerTrack.getScoreStatus());
				info.setScore(playerTrack.getScore());
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
	
	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String playerId, String territoryId) throws Exception {
		List<TrackedInstance> list = geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, playerId, territoryId);
		for (TrackedInstance ti : list) {
			ValidateTripRequest request = new ValidateTripRequest(playerId, territoryId, ti.getId());
			queueManager.sendValidateTripRequest(request);
		}
	}
	
	public List<TrackedInstance> getPlayerTrakedInstaces(String playerId, Pageable pageRequest) {
		PageRequest pageRequestNew = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "day"));
		return trackedInstanceRepository.findByUserId(playerId, pageRequestNew);
	}
	
	public TrackedInstance getTrackedInstance(String trackId) {
		return trackedInstanceRepository.findById(trackId).orElse(null);
	}
	
	private void updateValidationResult(TrackedInstance track, ValidationResult result) {
		track.setValidationResult(result);
		trackedInstanceRepository.save(track);
	}

	private void updateValidationResultAsError(TrackedInstance track) {
		ValidationResult vr = new ValidationResult();
		ValidationStatus status = new ValidationStatus();
		status.setValidationOutcome(TravelValidity.INVALID);
		vr.setValidationStatus(status);
		track.setValidationResult(vr);
		trackedInstanceRepository.save(track);
	}
	
	@Override
	public void validateTripRequest(ValidateTripRequest msg) {
		TrackedInstance track = getTrackedInstance(msg.getTrackedInstanceId());
		if(track != null) {
			if (!StringUtils.hasText(track.getSharedTravelId())) {
				// free tracking
				validateFreeTrackingTripRequest(msg, track);
			} else {
				// carsharing
				validateSharedTravelRequest(msg, track);
			}
		}
	}

	/**
	 * Validate free tracking: validate on territory, and in case of validity validate on campaigns
	 * @param msg
	 * @param track
	 */
	private void validateFreeTrackingTripRequest(ValidateTripRequest msg, TrackedInstance track) {
		try {
			ValidationResult validationResult = validationService.validateFreeTracking(track.getGeolocationEvents(), 
					track.getFreeTrackingTransport(), msg.getTerritoryId());
			updateValidationResult(track, validationResult);
			if(TravelValidity.VALID.equals(validationResult.getTravelValidity())) {
				storeAndValidateCampaigns(msg);
			}
		} catch (Exception e) {
			logger.warn("validateTripRequest error:" + e.getMessage());
			updateValidationResultAsError(track);
		}			
	}

	/**
	 * For each campaign subscribed and matching validate for campaign and store CampaignPlayerTrack
	 * @param msg
	 * @param track
	 * @throws Exception
	 */
	private void storeAndValidateCampaigns(ValidateTripRequest msg) throws Exception {
		List<CampaignSubscription> list = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(msg.getPlayerId(), msg.getTerritoryId());
		for(CampaignSubscription sub : list) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			// skip non-active campaigns
			if (!campaign.currentlyActive()) continue;
			// keep existing track
			CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(msg.getPlayerId(), campaign.getCampaignId(), msg.getTrackedInstanceId());
			if (pTrack == null) {
				pTrack = new CampaignPlayerTrack();
				pTrack.setPlayerId(msg.getPlayerId());
				pTrack.setCampaignId(sub.getCampaignId());
				pTrack.setCampaignSubscriptionId(sub.getId());
				pTrack.setTrackedInstanceId(msg.getTrackedInstanceId());
				pTrack.setTerritoryId(msg.getTerritoryId());
				pTrack.setScoreStatus(ScoreStatus.UNASSIGNED);
				pTrack = campaignPlayerTrackRepository.save(pTrack);
			}
			ValidateCampaignTripRequest request = new ValidateCampaignTripRequest(msg.getPlayerId(), 
					msg.getTerritoryId(), msg.getTrackedInstanceId(), sub.getCampaignId(), sub.getId(), pTrack.getId(), campaign.getType().toString());
			queueManager.sendValidateCampaignTripRequest(request);
		}
	}
	
	/**
	 * For each campaign subscribed send a invalidate message for the specific track
	 */
	private void invalidateCampaigns(ValidateTripRequest msg) throws Exception {
		List<CampaignSubscription> list = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(msg.getPlayerId(), msg.getTerritoryId());
		for(CampaignSubscription sub : list) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(msg.getPlayerId(), campaign.getCampaignId(), msg.getTrackedInstanceId());
			if(pTrack != null) {
				ValidateCampaignTripRequest request = new ValidateCampaignTripRequest(msg.getPlayerId(), 
						msg.getTerritoryId(), msg.getTrackedInstanceId(), sub.getCampaignId(), sub.getId(), pTrack.getId(), campaign.getType().toString());
				queueManager.sendInvalidateCampaignTripRequest(request);
			}
		}
	}

	/**
	 * For each campaign subscribed send an update distance message for the specific track
	 */	
	private void updateCampaigns(ValidateTripRequest msg, double deltaDistance) throws Exception {
		List<CampaignSubscription> list = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(msg.getPlayerId(), msg.getTerritoryId());
		for(CampaignSubscription sub : list) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			CampaignPlayerTrack pTrack = campaignPlayerTrackRepository.findByPlayerIdAndCampaignIdAndTrackedInstanceId(msg.getPlayerId(), campaign.getCampaignId(), msg.getTrackedInstanceId());
			if(pTrack != null) {
				UpdateCampaignTripRequest request = new UpdateCampaignTripRequest(campaign.getType().toString(), pTrack.getId(), deltaDistance);
				queueManager.sendUpdateCampaignTripRequest(request);
			}
		}
	}
	
	private void validateSharedTravelRequest(ValidateTripRequest msg, TrackedInstance track) {
		String sharedId = track.getSharedTravelId();
		try {
			if (ValidationConstants.isDriver(sharedId)) {
				String passengerTravelId = ValidationConstants.getPassengerTravelId(sharedId);
				List<TrackedInstance> list = trackedInstanceRepository.findPassengerTrips(msg.getTerritoryId(), passengerTravelId, msg.getPlayerId());
				if (!list.isEmpty()) {
					for (TrackedInstance passengerTravel: list) {
						validateSharedTripPair(passengerTravel, passengerTravel.getUserId(), track.getClientId(), msg.getTerritoryId(), track);
					}
				}
			} else {
				String driverTravelId = ValidationConstants.getDriverTravelId(sharedId);
				TrackedInstance driverTravel = trackedInstanceRepository.findDriverTrip(msg.getTerritoryId(), driverTravelId, msg.getPlayerId());
				if (driverTravel != null) {
					validateSharedTripPair(track, msg.getPlayerId(), track.getClientId(), msg.getTerritoryId(), driverTravel);
				}
			}
		} catch (Exception e) {
			logger.warn("validateTripRequest error:" + e.getMessage());
			updateValidationResultAsError(track);
		}	
	}

	private void validateSharedTripPair(TrackedInstance passengerTravel, String passengerId, String passengerTravelId, String territoryId, TrackedInstance driverTravel) throws Exception {
		// validate passenger trip
		ValidationResult vr = validationService.validateSharedTripPassenger(passengerTravel.getGeolocationEvents(), driverTravel.getGeolocationEvents(), territoryId);
		passengerTravel.setValidationResult(vr);
		updateValidationResult(passengerTravel, vr);
		
		// validated driver trip if not yet done
		if (driverTravel.getValidationResult() == null || driverTravel.getValidationResult().getValidationStatus() == null || TravelValidity.PENDING.equals(driverTravel.getValidationResult().getValidationStatus().getValidationOutcome())) {
			ValidationResult driverVr = validationService.validateSharedTripDriver(driverTravel.getGeolocationEvents(), territoryId);
			driverTravel.setValidationResult(driverVr);
			updateValidationResult(driverTravel, driverVr);
		}
		
		// passenger trip is valid: points are assigned to both
		if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
			ValidateTripRequest driverRequest = new ValidateTripRequest(driverTravel.getUserId(), territoryId, driverTravel.getId());
			storeAndValidateCampaigns(driverRequest);
			
			ValidateTripRequest passRequest = new ValidateTripRequest(passengerId, territoryId, passengerTravel.getId());
			storeAndValidateCampaigns(passRequest);
		} else {
			logger.warn("Validation result null");
			updateValidationResultAsError(passengerTravel);
		}
	}
	
	public Page<TrackedInstanceConsole> searchTrackedInstance(String territoryId, String trackedInstanceId, String playerId, String modeType, 
			String campaignId, String validationStatus, Date dateFrom, Date dateTo, Pageable pageRequest) {
		List<AggregationOperation> operations = new ArrayList<>();
		Criteria criteria = new Criteria("territoryId").is(territoryId);
		if(Utils.isNotEmpty(trackedInstanceId)) {
			criteria = criteria.and("id").is(trackedInstanceId);
		}
		if(Utils.isNotEmpty(playerId)) {
			criteria = criteria.and("userId").is(playerId);
		}
		if(Utils.isNotEmpty(modeType)) {
			criteria = criteria.and("validationResult.validationStatus.modeType").is(modeType);
		}
		if(Utils.isNotEmpty(validationStatus)) {
			criteria = criteria.and("validationResult.validationStatus.validationOutcome").is(validationStatus);	
		}
		if((dateFrom != null) && (dateTo != null)) {
			criteria = criteria.andOperator(Criteria.where("startTime").gte(dateFrom), Criteria.where("startTime").lte(dateTo));
		}
		if(Utils.isNotEmpty(campaignId)) {
			AddFieldsOperation addFields = AddFieldsOperation.addField("trackId").withValueOfExpression("{ \"$toString\": \"$_id\" }").build();
			LookupOperation lookup = Aggregation.lookup("campaignPlayerTracks", "trackId", "trackedInstanceId", "campaignPlayerTracks");
			criteria = criteria.and("campaignPlayerTracks.campaignId").is(campaignId);
			operations.add(addFields);
			operations.add(lookup);
		}
		MatchOperation match = Aggregation.match(criteria);
		operations.add(match);
		ProjectionOperation project = Aggregation.project().andExclude("geolocationEvents");
		operations.add(project);
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
		Aggregation aggregation = Aggregation.newAggregation(operations);
		AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, 
				collectionName, Document.class);
		return aggregationResults.getMappedResults().size();
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
				track.getValidationResult().getValidationStatus().setDuration(duration);
				track.setFreeTrackingTransport(modeType);
				track.getValidationResult().setValid(true);
				trackedInstanceRepository.save(track);
				ValidateTripRequest msg = new ValidateTripRequest();
				msg.setTerritoryId(track.getTerritoryId());
				msg.setPlayerId(track.getUserId());
				msg.setTrackedInstanceId(track.getId());
				storeAndValidateCampaigns(msg);
			} else if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
				//update distance for a already validated track
				double delta = distance - track.getValidationResult().getValidationStatus().getDistance();
				track.getValidationResult().getValidationStatus().setDistance(distance);
				trackedInstanceRepository.save(track);
				ValidateTripRequest msg = new ValidateTripRequest();
				msg.setTerritoryId(track.getTerritoryId());
				msg.setPlayerId(track.getUserId());
				msg.setTrackedInstanceId(track.getId());
				updateCampaigns(msg, delta);
			}
		} else if(TravelValidity.INVALID.equals(changedValidity)) {
			if(TravelValidity.VALID.equals(track.getValidationResult().getTravelValidity())) {
				// VALID -> INVALID
				track.getValidationResult().getValidationStatus().setValidationOutcome(TravelValidity.INVALID);
				track.getValidationResult().setValid(false);
				track.getValidationResult().getValidationStatus().setError(ERROR_TYPE.valueOf(errorType));
				track.setNote(note);
				trackedInstanceRepository.save(track);
				ValidateTripRequest msg = new ValidateTripRequest();
				msg.setTerritoryId(track.getTerritoryId());
				msg.setPlayerId(track.getUserId());
				msg.setTrackedInstanceId(track.getId());
				invalidateCampaigns(msg);
			}
		}
	}
	
}
