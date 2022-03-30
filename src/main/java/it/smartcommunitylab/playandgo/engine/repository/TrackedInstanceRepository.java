package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

@Repository
public interface TrackedInstanceRepository extends MongoRepository<TrackedInstance, String> {
	public TrackedInstance findByDayAndUserIdAndClientId(String day, String userId, String clientId);
	
	@Query ("{'itinerary.userId': ?0, 'itinerary.clientId' : ?1}")
	public TrackedInstance findByItinerary(String userId, String clientId);
	
	@Query ("{'userId' : ?0}")
	public List<TrackedInstance> findByUserId(String userId, Sort sort);
	
	public List<TrackedInstance> findByMultimodalId(String multimodalId, Sort sort);
}
