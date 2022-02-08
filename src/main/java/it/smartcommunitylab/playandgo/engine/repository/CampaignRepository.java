package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.smartcommunitylab.playandgo.engine.model.Campaign;

public interface CampaignRepository extends MongoRepository<Campaign, String> {

}
