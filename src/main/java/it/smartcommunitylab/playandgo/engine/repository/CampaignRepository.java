package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.smartcommunitylab.playandgo.engine.model.Campaign;

public interface CampaignRepository extends MongoRepository<Campaign, String> {

	@Query ("{'defaultForTerritory': true, 'territoryId' : ?0}")
	public Campaign findDefaultByTerritoryId(String territoryId);
}
