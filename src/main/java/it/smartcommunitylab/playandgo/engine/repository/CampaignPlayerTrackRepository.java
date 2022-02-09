package it.smartcommunitylab.playandgo.engine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.CampaignPlayerTrack;

@Repository
public interface CampaignPlayerTrackRepository extends MongoRepository<CampaignPlayerTrack, String> {

}
