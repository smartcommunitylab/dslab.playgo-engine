package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;

public interface CampaignSubscriptionRepository extends MongoRepository<CampaignSubscription, String>{

}
