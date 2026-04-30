package it.smartcommunitylab.playandgo.engine.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.FakeTrack;

@Repository
public interface FakeTrackRepository extends MongoRepository<FakeTrack, String> {
	
	public List<FakeTrack> findByPlayerId(String playerId);
	
	public List<FakeTrack> findByTerritoryId(String territoryId);
	
	public List<FakeTrack> findByPlayerIdAndTerritoryId(String playerId, String territoryId);
	
	@Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
	public List<FakeTrack> findByDateRange(Date startDate, Date endDate);
	
	@Query("{'playerId': ?0, 'timestamp': {$gte: ?1, $lte: ?2}}")
	public List<FakeTrack> findByPlayerIdAndDateRange(String playerId, Date startDate, Date endDate);
}
