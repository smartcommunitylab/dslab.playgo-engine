package it.smartcommunitylab.playandgo.engine.manager;

import java.text.SimpleDateFormat;
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
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import it.smartcommunitylab.playandgo.engine.dto.CampaignTripInfo;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.dto.TripInfo;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.geolocation.model.Geolocation;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult.TravelValidity;
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
import it.smartcommunitylab.playandgo.engine.util.GamificationHelper;
import it.smartcommunitylab.playandgo.engine.validation.GeolocationsProcessor;
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
	
	public List<TrackedInstanceInfo> getTrackedInstanceInfoList(String playerId, Pageable pageRequest) {
		PageRequest pageRequestNew = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "startTime"));
		List<TrackedInstanceInfo> result = new ArrayList<>();
		List<TrackedInstance> trackList = trackedInstanceRepository.findByUserId(playerId, pageRequestNew);
		for(TrackedInstance track : trackList) {
			TrackedInstanceInfo trackInfo = getTrackedInstanceInfoFromTrack(track, playerId);		
			result.add(trackInfo);
		}
		return result;
	}
	
	public TrackedInstanceInfo getTrackedInstanceInfo(String playerId, String trackedInstanceId) throws Exception {
		TrackedInstance track = trackedInstanceRepository.findById(trackedInstanceId).orElse(null);
		if(track == null) {
			throw new BadRequestException("track not found");
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
		trackInfo.setModeType(track.getValidationResult().getValidationStatus().getModeType().toString());
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
				info.setValid(playerTrack.getValid());
				campaignInfoMap.put(campaign.getCampaignId(), info);
			}
			info.setScore(info.getScore() + playerTrack.getScore());
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

	@Override
	public void validateTripRequest(ValidateTripRequest msg) {
		TrackedInstance track = getTrackedInstance(msg.getTrackedInstanceId());
		if(track != null) {
			try {
				ValidationResult validationResult = validationService.validateFreeTracking(track.getGeolocationEvents(), 
						track.getFreeTrackingTransport(), msg.getTerritoryId());
				updateValidationResult(track, validationResult);
				if(!TravelValidity.INVALID.equals(validationResult.getTravelValidity())) {
					List<CampaignSubscription> list = campaignSubscriptionRepository.findByPlayerIdAndTerritoryId(msg.getPlayerId(), msg.getTerritoryId());
					for(CampaignSubscription sub : list) {
						Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
						CampaignPlayerTrack pTrack = new CampaignPlayerTrack();
						pTrack.setPlayerId(msg.getPlayerId());
						pTrack.setCampaignId(sub.getCampaignId());
						pTrack.setCampaignSubscriptionId(sub.getId());
						pTrack.setTrackedInstanceId(track.getId());
						pTrack.setTerritoryId(msg.getTerritoryId());
						pTrack.setScoreStatus(ScoreStatus.UNASSIGNED);
						campaignPlayerTrackRepository.save(pTrack);
						ValidateCampaignTripRequest request = new ValidateCampaignTripRequest(msg.getPlayerId(), 
								msg.getTerritoryId(), track.getId(), sub.getCampaignId(), sub.getId(), pTrack.getId(), campaign.getType().toString());
						queueManager.sendValidateCampaignTripRequest(request);
					}
				}
			} catch (Exception e) {
				logger.warn("validateTripRequest error:" + e.getMessage());
			}			
		}
	}

}
