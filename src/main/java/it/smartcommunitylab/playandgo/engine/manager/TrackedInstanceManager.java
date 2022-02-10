package it.smartcommunitylab.playandgo.engine.manager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;
import it.smartcommunitylab.playandgo.engine.validation.GeolocationsProcessor;

@Component
public class TrackedInstanceManager {

	@Autowired
	private TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	private GeolocationsProcessor geolocationsProcessor;
	
	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String playerId) throws Exception {
		geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, playerId);
	}
	
	public List<TrackedInstance> getPlayerTrakedInstaces(String playerId) {
		return trackedInstanceRepository.findByUserId(playerId, Sort.by(Sort.Direction.DESC, "day"));
	}
}
