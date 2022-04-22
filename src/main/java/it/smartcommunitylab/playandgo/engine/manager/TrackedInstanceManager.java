package it.smartcommunitylab.playandgo.engine.manager;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

import it.smartcommunitylab.playandgo.engine.dto.CampaignTripInfo;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.dto.TripInfo;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationStatus;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;
import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack.ScoreStatus;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ValidateCampaignTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.ValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;
import it.smartcommunitylab.playandgo.engine.validation.GeolocationsProcessor;
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
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	CampaignRepository campaignRepository;

	@Autowired
	GeolocationsProcessor geolocationsProcessor;
	
	@Autowired
	ValidationService validationService;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
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
			TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(track, playerId);		
			result.add(trackInfo);
		}
		return new PageImpl<>(result, pageRequestNew, trackedInstanceRepository.countByUserId(playerId));
	}
	
	public TrackedInstanceInfo getTrackedInstanceInfo(String playerId, String trackedInstanceId) throws Exception {
		TrackedInstance track = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(track == null) {
			throw new BadRequestException("track not found", ErrorCode.TRACK_NOT_FOUND);
		}
		TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(track, playerId);
		List<Geolocation> geo = Lists.newArrayList(track.getGeolocationEvents());
		geo = GamificationHelper.optimize(geo);
		Collections.sort(geo);
		String poly = GamificationHelper.encodePoly(geo);
		trackInfo.setPolyline(poly);
		return trackInfo;
	}
	
	private TrackedInstanceInfo getTrackedInstanceInfoFromTrack(TrackedInstance track, String playerId) {
		TrackedInstanceInfo trackInfo = new TrackedInstanceInfo();
		trackInfo.setTrackedInstanceId(track.getId());
		trackInfo.setMultimodalId(track.getMultimodalId());
		trackInfo.setStartTime(track.getStartTime());
		trackInfo.setEndTime(getEndTime(track));
		trackInfo.setValidity(track.getValidationResult().getTravelValidity());
		trackInfo.setDistance(track.getValidationResult().getValidationStatus().getDistance());
		if(track.getValidationResult().getValidationStatus().getModeType() != null) {
			trackInfo.setModeType(track.getValidationResult().getValidationStatus().getModeType().toString());
		}
		//campaigns info
		Map<String, CampaignTripInfo> campaignInfoMap = new HashMap<>();
		List<CampaignPlayerTrack> playerTrackList = campaignPlayerTrackRepository.findByPlayerIdAndTrackedInstanceId(playerId, track.getId());
		for(CampaignPlayerTrack playerTrack : playerTrackList) {
			CampaignTripInfo info = campaignInfoMap.get(playerTrack.getCampaignId());
			if(info == null) {
				info = new CampaignTripInfo();
				Campaign campaign = campaignRepository.findById(playerTrack.getCampaignId()).orElse(null);
				info.setCampaignId(campaign.getCampaignId());
				info.setCampaignName(campaign.getName());
				info.setType(campaign.getType());
				info.setValid(playerTrack.isValid());
				info.setScoreStatus(playerTrack.getScoreStatus());
				info.setScore(playerTrack.getScore());
				campaignInfoMap.put(campaign.getCampaignId(), info);
			}
		}
		trackInfo.getCampaigns().addAll(campaignInfoMap.values());
		return trackInfo;
	}	
	
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
	}
	
	private Date getEndTime(TrackedInstance track) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(track.getStartTime());
		calendar.add(Calendar.SECOND, (int) track.getValidationResult().getValidationStatus().getDuration());
		Date endTime = calendar.getTime();
		return endTime;
	}
	
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
	
	public void updateValidationResult(TrackedInstance track, ValidationResult result) {
		track.setValidationResult(result);
		trackedInstanceRepository.save(track);
	}

	public void updateValidationResultAsError(TrackedInstance track) {
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
			if(!TravelValidity.INVALID.equals(validationResult.getTravelValidity())) {
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
		if (vr != null && !TravelValidity.INVALID.equals(vr.getTravelValidity())) {
			ValidateTripRequest driverRequest = new ValidateTripRequest(driverTravel.getUserId(), territoryId, driverTravel.getId());
			storeAndValidateCampaigns(driverRequest);
			
			ValidateTripRequest passRequest = new ValidateTripRequest(passengerId, territoryId, passengerTravel.getId());
			storeAndValidateCampaigns(passRequest);
		} else {
			logger.warn("Validation result null");
			updateValidationResultAsError(passengerTravel);
		}
	}

}
