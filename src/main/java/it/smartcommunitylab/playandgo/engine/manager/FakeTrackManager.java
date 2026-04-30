package it.smartcommunitylab.playandgo.engine.manager;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.model.FakeTrack;
import it.smartcommunitylab.playandgo.engine.repository.FakeTrackRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class FakeTrackManager {
	private static transient final Logger logger = LoggerFactory.getLogger(FakeTrackManager.class);
	
	@Autowired
	FakeTrackRepository fakeTrackRepository;
	
	public FakeTrack create(FakeTrack fakeTrack) throws BadRequestException {
		if (fakeTrack == null) {
			throw new BadRequestException("FakeTrack cannot be null", ErrorCode.INVALID_REQUEST);
		}
		
		if (fakeTrack.getPlayerId() == null || fakeTrack.getPlayerId().isEmpty()) {
			throw new BadRequestException("PlayerId is required", ErrorCode.INVALID_REQUEST);
		}
		
		if (fakeTrack.getTerritoryId() == null || fakeTrack.getTerritoryId().isEmpty()) {
			throw new BadRequestException("TerritoryId is required", ErrorCode.INVALID_REQUEST);
		}
		
		if (fakeTrack.getTimestamp() == null) {
			fakeTrack.setTimestamp(new Date());
		}
		
		return fakeTrackRepository.save(fakeTrack);
	}
	
	public FakeTrack getById(String id) throws BadRequestException {
		if (id == null || id.isEmpty()) {
			throw new BadRequestException("Id cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		return fakeTrackRepository.findById(id).orElse(null);
	}
	
	public FakeTrack update(String id, FakeTrack fakeTrack) throws BadRequestException {
		if (id == null || id.isEmpty()) {
			throw new BadRequestException("Id cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		FakeTrack existing = fakeTrackRepository.findById(id).orElse(null);
		if (existing == null) {
			throw new BadRequestException("FakeTrack not found with id: " + id, ErrorCode.INVALID_REQUEST);
		}
		
		if (fakeTrack.getPlayerId() != null && !fakeTrack.getPlayerId().isEmpty()) {
			existing.setPlayerId(fakeTrack.getPlayerId());
		}
		
		if (fakeTrack.getTerritoryId() != null && !fakeTrack.getTerritoryId().isEmpty()) {
			existing.setTerritoryId(fakeTrack.getTerritoryId());
		}
		
		if (fakeTrack.getTimestamp() != null) {
			existing.setTimestamp(fakeTrack.getTimestamp());
		}
		
		return fakeTrackRepository.save(existing);
	}
	
	public void delete(String id) throws BadRequestException {
		if (id == null || id.isEmpty()) {
			throw new BadRequestException("Id cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		FakeTrack existing = fakeTrackRepository.findById(id).orElse(null);
		if (existing == null) {
			throw new BadRequestException("FakeTrack not found with id: " + id, ErrorCode.INVALID_REQUEST);
		}
		
		fakeTrackRepository.deleteById(id);
	}
	
	public List<FakeTrack> getByPlayerId(String playerId) throws BadRequestException {
		if (playerId == null || playerId.isEmpty()) {
			throw new BadRequestException("PlayerId cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		return fakeTrackRepository.findByPlayerId(playerId);
	}
	
	public List<FakeTrack> getByTerritoryId(String territoryId) throws BadRequestException {
		if (territoryId == null || territoryId.isEmpty()) {
			throw new BadRequestException("TerritoryId cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		return fakeTrackRepository.findByTerritoryId(territoryId);
	}
	
	public List<FakeTrack> getByPlayerIdAndTerritoryId(String playerId, String territoryId) throws BadRequestException {
		if (playerId == null || playerId.isEmpty()) {
			throw new BadRequestException("PlayerId cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		if (territoryId == null || territoryId.isEmpty()) {
			throw new BadRequestException("TerritoryId cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		return fakeTrackRepository.findByPlayerIdAndTerritoryId(playerId, territoryId);
	}
	
	public List<FakeTrack> getByDateRange(Date startDate, Date endDate) throws BadRequestException {
		if (startDate == null || endDate == null) {
			throw new BadRequestException("Start date and end date are required", ErrorCode.INVALID_REQUEST);
		}
		
		if (startDate.after(endDate)) {
			throw new BadRequestException("Start date cannot be after end date", ErrorCode.INVALID_REQUEST);
		}
		
		return fakeTrackRepository.findByDateRange(startDate, endDate);
	}
	
	public List<FakeTrack> getByPlayerIdAndDateRange(String playerId, Date startDate, Date endDate) throws BadRequestException {
		if (playerId == null || playerId.isEmpty()) {
			throw new BadRequestException("PlayerId cannot be null or empty", ErrorCode.INVALID_REQUEST);
		}
		
		if (startDate == null || endDate == null) {
			throw new BadRequestException("Start date and end date are required", ErrorCode.INVALID_REQUEST);
		}
		
		if (startDate.after(endDate)) {
			throw new BadRequestException("Start date cannot be after end date", ErrorCode.INVALID_REQUEST);
		}
		
		return fakeTrackRepository.findByPlayerIdAndDateRange(playerId, startDate, endDate);
	}
	
	public List<FakeTrack> getAll() {
		return fakeTrackRepository.findAll();
	}
}
