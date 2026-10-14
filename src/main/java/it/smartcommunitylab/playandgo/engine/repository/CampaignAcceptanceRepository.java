package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.CampaignAcceptance;

@Repository
public interface CampaignAcceptanceRepository extends MongoRepository<CampaignAcceptance, String> {

    List<CampaignAcceptance> findByCampaignId(String campaignId);

    List<CampaignAcceptance> findByPlayerIdAndCampaignId(String playerId, String campaignId, Sort sort);
}