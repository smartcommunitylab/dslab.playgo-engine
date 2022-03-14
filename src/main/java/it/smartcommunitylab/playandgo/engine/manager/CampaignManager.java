package it.smartcommunitylab.playandgo.engine.manager;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.dto.PlayerCampaignDTO;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;

@Component
public class CampaignManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignManager.class);
	
	private static final String CAMPAIGNSUB = "campaignSubscriptions";
	
	@Autowired
	MongoTemplate template;
	
	@Autowired
	private CampaignRepository campaignRepository;
	
	@Autowired
	private CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	public void saveTerritory(Campaign campaign) {
		campaignRepository.save(campaign);
	}
	
	public Campaign getCampaign(String campaignId) {
		return campaignRepository.findById(campaignId).orElse(null);
	}
	
	public List<Campaign> getCampaigns() {
		return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "dateFrom"));
	}
	
	public List<Campaign> getCampaignsByTerritory(String territoryId) {
		return campaignRepository.findByTerritoryId(territoryId, Sort.by(Sort.Direction.DESC, "dateFrom"));
	}
	
	public Campaign deleteCampaign(String campaignId) {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign != null) {
			campaignRepository.deleteById(campaignId);
		}
		return campaign;
	}
	
	public Campaign getDefaultCampaignByTerritory(String territoryId) {
		return campaignRepository.findByTerritoryIdAndType(territoryId, Type.personal);
	}
	
	public CampaignSubscription subscribePlayer(Player player, String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist");
		}
		if(!campaign.getTerritoryId().equals(player.getTerritoryId())) {
			throw new BadRequestException("territory doesn't match");
		}
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaignId);
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		//TODO campaign data?
		return campaignSubscriptionRepository.save(sub);
	}
	
	public void updateDefaultCampaignSuscription(Player player) {
		Campaign campaign = getDefaultCampaignByTerritory(player.getTerritoryId());
		if(campaign != null) {
			CampaignSubscription sub = campaignSubscriptionRepository.findByPlayerId(campaign.getCampaignId(), player.getPlayerId());
			if(sub != null) {
				Query query = new Query(new Criteria("id").is(sub.getId()));
				Update update = new Update();
				update.set("sendMail", player.getSendMail());
				update.set("mail", player.getMail());
				template.updateFirst(query, update, CAMPAIGNSUB);
			}
		}
	}
	
	public List<PlayerCampaignDTO> getPlayerCampaigns(String playerId) {
		List<PlayerCampaignDTO> result = new ArrayList<>();
		List<CampaignSubscription> campaigns = campaignSubscriptionRepository.findByPlayerId(playerId);
		for(CampaignSubscription sub : campaigns) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			if(campaign != null) {
				PlayerCampaignDTO dto = new PlayerCampaignDTO(campaign, sub);
				result.add(dto);
			}
		}
		return result;
	}
}
