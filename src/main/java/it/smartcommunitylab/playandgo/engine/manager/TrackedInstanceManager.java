package it.smartcommunitylab.playandgo.engine.manager;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.geolocation.model.ValidationResult;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ManageValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.mq.MessageQueueManager;
import it.smartcommunitylab.playandgo.engine.mq.ValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.validation.GeolocationsProcessor;
import it.smartcommunitylab.playandgo.engine.validation.ValidationService;

@Component
public class TrackedInstanceManager implements ManageValidateTripRequest {
	private static Log logger = LogFactory.getLog(TrackedInstanceManager.class);
	
	private static final String TRACKEDINSTANCE = "trackedInstances";
	
	@Autowired
	private MessageQueueManager queueManager;

	@Autowired
	private TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	private GeolocationsProcessor geolocationsProcessor;
	
	@Autowired
	private ValidationService validationService;
	
	@PostConstruct
	public void init() {
		queueManager.setManageValidateTripRequest(this);
	}
	
	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String playerId, String territoryId) throws Exception {
		List<TrackedInstance> list = geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, playerId, territoryId);
		for (TrackedInstance ti : list) {
			ValidateTripRequest request = new ValidateTripRequest(playerId, territoryId, ti.getId());
			queueManager.sendValidateTripRequest(request);
		}
	}
	
	public List<TrackedInstance> getPlayerTrakedInstaces(String playerId) {
		return trackedInstanceRepository.findByUserId(playerId, Sort.by(Sort.Direction.DESC, "day"));
	}
	
	public TrackedInstance getTrackedInstance(String trackId) {
		return trackedInstanceRepository.findById(trackId).orElse(null);
	}
	
	public void updateValidationResult(TrackedInstance track, ValidationResult result) {
		track.setValidationResult(result);
		trackedInstanceRepository.save(track);
	}

	@Override
	public void validateTripRequest(ValidateTripRequest message) {
		TrackedInstance track = getTrackedInstance(message.getTrackedInstanceId());
		try {
			ValidationResult result = validationService.validateFreeTracking(track.getGeolocationEvents(), 
					track.getFreeTrackingTransport(), message.getTerritoryId());
			updateValidationResult(track, result);
		} catch (Exception e) {
			logger.warn("validateTripRequest error:" + e.getMessage());
		}
	}

}
