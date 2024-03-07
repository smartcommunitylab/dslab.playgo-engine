package it.smartcommunitylab.playandgo.engine.notification;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;

@Component 
public class EmailSender {

	private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

	private static final String ITA_LANG = "it";

	@Autowired
	private CampaignSubscriptionRepository subscriptionRepo;
	@Autowired
	private PlayerRepository playerRepository;
	
	@Autowired
	private EmailService emailService;
	
	/**
	 * Send a generic mail to all the subscribed users
	 * @param body
	 * @param subject
	 * @param territoryId
	 * @param campaignId
	 */
	public void sendGenericMailToAll(String body, String subject, String territoryId, String campaignId) {
		logger.info("Sending generic mail to all");
		Iterable<CampaignSubscription> iter = 
				  campaignId != null 
				? subscriptionRepo.findByTerritoryIdAndCampaignId(territoryId, campaignId)
				: subscriptionRepo.findByTerritoryId(territoryId);

		for (CampaignSubscription s : iter) {

			if (!Boolean.FALSE.equals(s.getSendMail())) {
				Player player = playerRepository.findById(s.getPlayerId()).orElse(null);
				if (player != null && !Boolean.FALSE.equals(player.getSendMail())) {
					try {
						emailService.sendGenericMail(body, subject, player.getNickname(), player.getMail(), Locale.forLanguageTag(getPlayerLang(player)));
					} catch (Exception e) {
						logger.error("Failed to send message to "+player.getMail(), e);
					}
				}
			}
		}	
	}

	/**
	 * Send a generic mail to the specific list of users. If one of the users does not exist or is not subscribed, error is returned
	 * @param body
	 * @param subject
	 * @param territoryId
	 * @param campaignId
	 * @param ids
	 */
	public void sendGenericMailToUsers(String body, String subject, String territoryId, String campaignId, Set<String> ids) {
		logger.debug("Sending generic mail to users: "+ids);
		
		List<Player> players = playerRepository.findByTerritoryIdAndPlayerIdIn(territoryId, ids);
		
		for (Player player: players) {
			if (!Boolean.FALSE.equals(player.getSendMail())) {
				if (campaignId != null) {
					CampaignSubscription s = subscriptionRepo.findByCampaignIdAndPlayerId(campaignId, player.getPlayerId());
					if (s == null) {
						logger.error("Unsubscribed player " + player.getPlayerId());
						continue;
					}
				}
				try {
					emailService.sendGenericMail(body, subject, player.getNickname(), player.getMail(), Locale.forLanguageTag(getPlayerLang(player)));
				} catch (Exception e) {
					logger.error("Failed to send message to "+player.getMail(), e);
				}

			}
		}	
	}

	public String getPlayerLang(Player p) {
		return p.getLanguage() != null ? p.getLanguage() : ITA_LANG;
	}
}
