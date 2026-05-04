package it.smartcommunitylab.playandgo.engine.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.FakeTrackManager;
import it.smartcommunitylab.playandgo.engine.model.FakeTrack;
import it.smartcommunitylab.playandgo.engine.model.Player;

@RestController
public class FakeTrackController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(FakeTrackController.class);
	
	@Autowired
	FakeTrackManager fakeTrackManager;

    @PostMapping("/api/faketrack/my")
    public List<FakeTrack> createMyFakeTrack(
			@RequestBody List<Long> timestamps,
            HttpServletRequest request) throws Exception {
        Player player = getCurrentPlayer(request);
		List<FakeTrack> fakeTracks = new ArrayList<>();
		for (Long timestamp : timestamps) {
	        FakeTrack fakeTrack = new FakeTrack();
    	    fakeTrack.setPlayerId(player.getPlayerId());
        	fakeTrack.setTerritoryId(player.getTerritoryId());
        	fakeTrack.setTimestamp(new Date(timestamp));        
        	logger.info("Creating new FakeTrack " + fakeTrack.toString());
			fakeTracks.add(fakeTrackManager.create(fakeTrack));
		}
        return fakeTracks;
    }
	
	@PostMapping("/api/faketrack")
	public FakeTrack create(
			@RequestBody FakeTrack fakeTrack,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Creating new FakeTrack: " + fakeTrack.toString());
		return fakeTrackManager.create(fakeTrack);
	}
	
	@GetMapping("/api/faketrack/{id}")
	public FakeTrack getById(
			@PathVariable String id,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting FakeTrack with id: " + id);
		FakeTrack fakeTrack = fakeTrackManager.getById(id);
		if (fakeTrack == null) {
			throw new BadRequestException("FakeTrack not found with id: " + id);
		}
		return fakeTrack;
	}
	
	@PutMapping("/api/faketrack/{id}")
	public FakeTrack update(
			@PathVariable String id,
			@RequestBody FakeTrack fakeTrack,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Updating FakeTrack with id: " + id);
		return fakeTrackManager.update(id, fakeTrack);
	}
	
	@DeleteMapping("/api/faketrack/{id}")
	public void delete(
			@PathVariable String id,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Deleting FakeTrack with id: " + id);
		fakeTrackManager.delete(id);
	}
	
	@GetMapping("/api/faketrack/by-player/{playerId}")
	public List<FakeTrack> getByPlayerId(
			@PathVariable String playerId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting FakeTracks for playerId: " + playerId);
		return fakeTrackManager.getByPlayerId(playerId);
	}
	
	@GetMapping("/api/faketrack/by-territory/{territoryId}")
	public List<FakeTrack> getByTerritoryId(
			@PathVariable String territoryId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting FakeTracks for territoryId: " + territoryId);
		return fakeTrackManager.getByTerritoryId(territoryId);
	}
	
	@GetMapping("/api/faketrack/by-player-territory")
	public List<FakeTrack> getByPlayerIdAndTerritoryId(
			@RequestParam String playerId,
			@RequestParam String territoryId,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting FakeTracks for playerId: " + playerId + " and territoryId: " + territoryId);
		return fakeTrackManager.getByPlayerIdAndTerritoryId(playerId, territoryId);
	}
	
	@GetMapping("/api/faketrack/by-date-range")
	public List<FakeTrack> getByDateRange(
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date startDate,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date endDate,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting FakeTracks in date range: " + startDate + " to " + endDate);
		return fakeTrackManager.getByDateRange(startDate, endDate);
	}
	
	@GetMapping("/api/faketrack/by-player-date-range")
	public List<FakeTrack> getByPlayerIdAndDateRange(
			@RequestParam String playerId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date startDate,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date endDate,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting FakeTracks for playerId: " + playerId + " in date range: " + startDate + " to " + endDate);
		return fakeTrackManager.getByPlayerIdAndDateRange(playerId, startDate, endDate);
	}
	
	@GetMapping("/api/faketrack")
	public List<FakeTrack> getAll(HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		logger.info("Getting all FakeTracks");
		return fakeTrackManager.getAll();
	}
}
