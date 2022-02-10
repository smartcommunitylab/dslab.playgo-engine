package it.smartcommunitylab.playandgo.engine.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.gamification.GeolocationsProcessor;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.repository.TrackedInstanceRepository;

@Component
public class TrackedInstanceManager {

	@Autowired
	private TrackedInstanceRepository trackedInstanceRepository;
	
	@Autowired
	private GeolocationsProcessor geolocationsProcessor;
	
	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String playerId) throws Exception {
		geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, playerId);
	}
}
