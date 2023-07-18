package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;

@Repository
public interface TrackedInstanceRepository extends MongoRepository<TrackedInstance, String> {
	public TrackedInstance findByUserIdAndClientId(String userId, String clientId);
	
	@Query ("{'itinerary.userId': ?0, 'itinerary.clientId' : ?1}")
	public TrackedInstance findByItinerary(String userId, String clientId);
	
	@Query ("{'userId' : ?0}")
	public List<TrackedInstance> findByUserId(String userId, Pageable pageRequest);
	
	public Long countByUserId(String userId);
	
	public List<TrackedInstance> findByMultimodalId(String multimodalId, Sort sort);
	
	public List<TrackedInstance> findByUserIdAndMultimodalId(String userId, String multimodalId);
	
	@Query("{territoryId: ?0, sharedTravelId: ?1, userId: {$ne: ?2}}")
	public List<TrackedInstance> findPassengerTrips(String territoryId, String sharedTravelId, String driverId);

	@Query("{territoryId: ?0, sharedTravelId: ?1, userId: {$ne: ?2}}")
	public TrackedInstance findDriverTrip(String territoryId, String sharedTravelId, String passengerId);

}
