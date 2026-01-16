package it.smartcommunitylab.playandgo.engine.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.engine.model.SurveyTask;

@Repository
public interface SurveyTaskRepository extends MongoRepository<SurveyTask, String> {
	
	/**
	 * Trova tutti i task per una campagna
	 */
	public List<SurveyTask> findByCampaignId(String campaignId);
	
	/**
	 * Trova tutti i task per un player
	 */
	public List<SurveyTask> findByPlayerId(String playerId);
	
	/**
	 * Trova un task specifico per campagna e player e survey
	 */
	public SurveyTask findByCampaignIdAndPlayerIdAndSurveyName(String campaignId, String playerId, String surveyName);
	
	/**
	 * Elimina tutti i task di una campagna
	 */
	public void deleteByCampaignId(String campaignId);
	
	/**
	 * Elimina tutti i task di un player
	 */
	public void deleteByPlayerId(String playerId);
	
	/**
	 * Elimina un task specifico
	 */
	public void deleteByCampaignIdAndPlayerId(String campaignId, String playerId);
	
	/**
	 * Conta i task per una campagna
	 */
	@Query("{ 'campaignId': ?0 }")
	public long countByCampaignId(String campaignId);
	
}
