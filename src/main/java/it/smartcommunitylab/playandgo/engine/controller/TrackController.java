package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.geolocation.model.GeolocationsEvent;
import it.smartcommunitylab.playandgo.engine.manager.TrackedInstanceManager;
import it.smartcommunitylab.playandgo.engine.model.Player;

@RestController
public class TrackController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(TrackController.class);
	
	@Autowired
	TrackedInstanceManager trackedInstanceManager;
	
	@PostMapping("/api/track/player/geolocations")
	public void storeGeolocationEvent(
			@RequestBody(required = false) GeolocationsEvent geolocationsEvent,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		trackedInstanceManager.storeGeolocationEvents(geolocationsEvent, player.getPlayerId(), player.getTerritoryId());
	}
	
	@GetMapping("/api/track/player")
	public List<TrackedInstanceInfo> getTrackedInstanceInfoList(
			Pageable pageRequest,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return trackedInstanceManager.getTrackedInstanceInfoList(player.getPlayerId(), pageRequest);
	}
	
	@GetMapping("/api/track/player/{trackedInstanceId}")
	public TrackedInstanceInfo getTrackedInstanceInfo(
			@PathVariable String trackedInstanceId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return trackedInstanceManager.getTrackedInstanceInfo(player.getPlayerId(), trackedInstanceId);
	}
	
	
	
	
}
