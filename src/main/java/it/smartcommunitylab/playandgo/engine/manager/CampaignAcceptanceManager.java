package it.smartcommunitylab.playandgo.engine.manager;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.lock.UserCampaignLock;
import it.smartcommunitylab.playandgo.engine.model.CampaignAcceptance;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.repository.CampaignAcceptanceRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class CampaignAcceptanceManager {

    static Log logger = LogFactory.getLog(CampaignAcceptanceManager.class);

    @Autowired
    private CampaignAcceptanceRepository campaignAcceptanceRepository;

    @Autowired
    private CampaignSubscriptionRepository campaignSubscriptionRepository;

	@Autowired
	UserCampaignLock campaignLock;

    public CampaignAcceptance create(CampaignAcceptance acceptance) throws BadRequestException {
        if (acceptance == null) {
            throw new BadRequestException("CampaignAcceptance cannot be null", ErrorCode.INVALID_REQUEST);
        }
        if (acceptance.getAcceptanceDate() == null) {
            acceptance.setAcceptanceDate(new Date());
        }
        return campaignAcceptanceRepository.save(acceptance);
    }

    public CampaignAcceptance userAcceptance(String playerId, String campaignId, boolean privacyAcceptance,
            boolean rulesAcceptance) throws Exception {
        validateId(playerId);
        validateId(campaignId);

        CampaignSubscription subscription = campaignSubscriptionRepository
                .findByCampaignIdAndPlayerId(campaignId, playerId);
        if (subscription == null) {
            throw new BadRequestException("CampaignSubscription not found for playerId: " + playerId
                    + " and campaignId: " + campaignId, ErrorCode.INVALID_REQUEST);
        }

        Exception ex = null;
        CampaignAcceptance acceptance = null;
        try {
            campaignLock.lock(campaignLock.getKey(playerId, campaignId));

            subscription.setPrivacyAcceptance(privacyAcceptance);
            subscription.setRulesAcceptance(rulesAcceptance);
            campaignSubscriptionRepository.save(subscription);

            acceptance = new CampaignAcceptance();
            acceptance.setPlayerId(playerId);
            acceptance.setCampaignId(campaignId);
            acceptance.setPrivacyAcceptance(privacyAcceptance);
            acceptance.setRulesAcceptance(rulesAcceptance);
            acceptance.setAcceptanceDate(new Date());
            campaignAcceptanceRepository.save(acceptance);        
        } catch (Exception e) {
            ex = e;
        } finally {
            campaignLock.unlock(campaignLock.getKey(playerId, campaignId));
        }
		if (ex != null) {
			throw ex;
		}        
        return acceptance;
    }

    public CampaignSubscription updateSubscriptionFlags(String playerId, String campaignId,
            boolean privacyAcceptance, boolean rulesAcceptance) throws BadRequestException {
        validateId(playerId);
        validateId(campaignId);

        CampaignSubscription subscription = campaignSubscriptionRepository
                .findByCampaignIdAndPlayerId(campaignId, playerId);
        if (subscription == null) {
            throw new BadRequestException("CampaignSubscription not found for playerId: " + playerId
                    + " and campaignId: " + campaignId, ErrorCode.INVALID_REQUEST);
        }
         try {
            campaignLock.lock(campaignLock.getKey(playerId, campaignId));
            subscription.setPrivacyAcceptance(privacyAcceptance);
            subscription.setRulesAcceptance(rulesAcceptance);
            campaignSubscriptionRepository.save(subscription);
            logger.info("Successfully updated subscription flags for playerId: " + playerId + " and campaignId: " + campaignId + " with privacyAcceptance: " + privacyAcceptance + " and rulesAcceptance: " + rulesAcceptance);
        } catch (Exception e) {
            logger.warn("Failed to update subscription flags for playerId: " + playerId + " and campaignId: " + campaignId, e);
        } finally {
            campaignLock.unlock(campaignLock.getKey(playerId, campaignId));
        }
        return subscription;
    }

    public List<CampaignSubscription> updateCampaignSubscriptionFlags(String campaignId,
            boolean privacyAcceptance, boolean rulesAcceptance) throws BadRequestException {
        validateId(campaignId);

        List<CampaignSubscription> subscriptions = campaignSubscriptionRepository.findByCampaignId(campaignId);
        for (CampaignSubscription subscription : subscriptions) {
            try {
                campaignLock.lock(campaignLock.getKey(subscription.getPlayerId(), campaignId));
                subscription.setPrivacyAcceptance(privacyAcceptance);
                subscription.setRulesAcceptance(rulesAcceptance);
                campaignSubscriptionRepository.save(subscription);
                logger.info("Successfully updated subscription flags for playerId: " + subscription.getPlayerId() + " and campaignId: " + campaignId + " with privacyAcceptance: " + privacyAcceptance + " and rulesAcceptance: " + rulesAcceptance);
            } catch (Exception e) {
                logger.warn("Failed to update subscription flags for playerId: " + subscription.getPlayerId() + " and campaignId: " + campaignId, e);
            } finally {
                campaignLock.unlock(campaignLock.getKey(subscription.getPlayerId(), campaignId));
            }
        }
        return subscriptions;
    }

    public CampaignAcceptance getById(String id) throws BadRequestException {
        validateId(id);
        return campaignAcceptanceRepository.findById(id).orElse(null);
    }

    public List<CampaignAcceptance> getAll() {
        return campaignAcceptanceRepository.findAll();
    }

    public CampaignAcceptance update(String id, CampaignAcceptance acceptance) throws BadRequestException {
        validateId(id);
        if (acceptance == null) {
            throw new BadRequestException("CampaignAcceptance cannot be null", ErrorCode.INVALID_REQUEST);
        }

        CampaignAcceptance existing = campaignAcceptanceRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new BadRequestException("CampaignAcceptance not found with id: " + id, ErrorCode.INVALID_REQUEST);
        }

        existing.setPlayerId(acceptance.getPlayerId());
        existing.setCampaignId(acceptance.getCampaignId());
        existing.setPrivacyAcceptance(acceptance.isPrivacyAcceptance());
        existing.setRulesAcceptance(acceptance.isRulesAcceptance());
        existing.setAcceptanceDate(acceptance.getAcceptanceDate());
        return campaignAcceptanceRepository.save(existing);
    }

    public void delete(String id) throws BadRequestException {
        validateId(id);
        if (!campaignAcceptanceRepository.existsById(id)) {
            throw new BadRequestException("CampaignAcceptance not found with id: " + id, ErrorCode.INVALID_REQUEST);
        }
        campaignAcceptanceRepository.deleteById(id);
    }

    public List<CampaignAcceptance> getByCampaignId(String campaignId) throws BadRequestException {
        validateId(campaignId);
        return campaignAcceptanceRepository.findByCampaignId(campaignId);
    }

    public List<CampaignAcceptance> getByPlayerIdAndCampaignId(String playerId, String campaignId, String sortBy,
            Sort.Direction direction)
            throws BadRequestException {
        validateId(playerId);
        validateId(campaignId);
        String property = sortBy == null || sortBy.isEmpty() ? "acceptanceDate" : sortBy;
        Sort.Direction sortDirection = direction == null ? Sort.Direction.DESC : direction;
        return campaignAcceptanceRepository.findByPlayerIdAndCampaignId(playerId, campaignId,
                Sort.by(sortDirection, property));
    }

    private void validateId(String id) throws BadRequestException {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("Id cannot be null or empty", ErrorCode.INVALID_REQUEST);
        }
    }
}