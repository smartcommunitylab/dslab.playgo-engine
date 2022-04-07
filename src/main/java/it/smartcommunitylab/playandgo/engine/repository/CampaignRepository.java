package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String> {

	public Campaign findByGameId(String gameId);
	
	public List<Campaign> findByTerritoryId(String territoryId, Sort sort); 
	
	public List<Campaign> findByType(Type type, Sort sort);
	
	public Campaign findByTerritoryIdAndType(String territoryId, Type type);
	
	public Long countByTerritoryId(String territoryId);
}
