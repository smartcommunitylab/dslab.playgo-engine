package it.smartcommunitylab.playandgo.engine.manager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.playandgo.engine.campaign.CityCampaignGameNotification;
import it.smartcommunitylab.playandgo.engine.campaign.CityCampaignSubscription;
import it.smartcommunitylab.playandgo.engine.campaign.CompanyCampaignSubscription;
import it.smartcommunitylab.playandgo.engine.campaign.PersonalCampaignGameNotification;
import it.smartcommunitylab.playandgo.engine.campaign.PersonalCampaignSubscription;
import it.smartcommunitylab.playandgo.engine.campaign.SchoolCampaignGameNotification;
import it.smartcommunitylab.playandgo.engine.dto.PlayerCampaign;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.exception.StorageException;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Image;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.CampaignPlayerTrackRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Component
public class CampaignManager {
	private static transient final Logger logger = LoggerFactory.getLogger(CampaignManager.class);
	
	private static final String CAMPAIGNSUB = "campaignSubscriptions";
	
	@Autowired
	MongoTemplate template;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	CampaignPlayerTrackRepository campaignPlayerTrackRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	PersonalCampaignSubscription personalCampaignSubscription;
	
	@Autowired
	CityCampaignSubscription cityCampaignSubscription;
	
	@Autowired
	CompanyCampaignSubscription companyCampaignSubscription;
	
	@Autowired
	PersonalCampaignGameNotification personalCampaignGameNotification;
	
	@Autowired
	CityCampaignGameNotification cityCampaignGameNotification;
	
	@Autowired
	SchoolCampaignGameNotification schoolCampaignGameNotification;
	
	public void addCampaign(Campaign campaign) throws Exception {
		try {
			campaignRepository.save(campaign);
			switch (campaign.getType()) {
				case personal:
					personalCampaignGameNotification.subcribeCampaing(campaign);
					break;
				case city:
					cityCampaignGameNotification.subcribeCampaing(campaign);
					break;
				case school:
					schoolCampaignGameNotification.subcribeCampaing(campaign);
					break;
				case company:
			}			
		} catch (Exception e) {
			throw new StorageException("territory save error", ErrorCode.ENTITY_SAVE_ERROR);
		}
	}
	
	public void updateCampaign(Campaign campaign) throws Exception {
		Campaign campaignDb = getCampaign(campaign.getCampaignId());
		if(campaignDb == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		campaignDb.setName(campaign.getName());
		campaignDb.setDescription(campaign.getDescription());
		campaignDb.setDateFrom(campaign.getDateFrom());
		campaignDb.setDateTo(campaign.getDateTo());
		campaignDb.setActive(campaign.getActive());
		campaignDb.setStartDayOfWeek(campaign.getStartDayOfWeek());
		campaignDb.setPrivacy(campaign.getPrivacy());
		campaignDb.setRules(campaign.getRules());
		campaignDb.setGameId(campaign.getGameId());
		campaignDb.setValidationData(campaign.getValidationData());
		campaignRepository.save(campaignDb);
		switch (campaign.getType()) {
			case personal:
				personalCampaignGameNotification.subcribeCampaing(campaignDb);
				break;
			case city:
				cityCampaignGameNotification.subcribeCampaing(campaignDb);
				break;
			case school:
				schoolCampaignGameNotification.subcribeCampaing(campaignDb);
				break;
			case company:
		}							
	}
	
	public Campaign getCampaign(String campaignId) {
		return campaignRepository.findById(campaignId).orElse(null);
	}
	
	public List<Campaign> getCampaigns() {
		return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "dateFrom"));
	}
	
	public List<Campaign> getCampaignsByTerritory(String territoryId, Type type) {
		if(type == null) {
			return campaignRepository.findByTerritoryId(territoryId, Sort.by(Sort.Direction.DESC, "dateFrom"));
		}
		return campaignRepository.findByTerritoryIdAndType(territoryId, type, Sort.by(Sort.Direction.DESC, "dateFrom"));
	}
	
	public Campaign deleteCampaign(String campaignId) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		Long count = campaignSubscriptionRepository.countByCampaignId(campaignId);
		if(count > 0) {
			throw new BadRequestException("campaign in use", ErrorCode.CAMPAIGN_IN_USE);
		}
		count = campaignPlayerTrackRepository.countByCampaignId(campaignId);
		if(count > 0) {
			throw new BadRequestException("campaign in use", ErrorCode.CAMPAIGN_IN_USE);
		}
		campaignRepository.deleteById(campaignId);		
		return campaign;
	}
	
	public Campaign getDefaultCampaignByTerritory(String territoryId) {
		return campaignRepository.findByTerritoryIdAndType(territoryId, Type.personal);
	}
	
	public CampaignSubscription subscribePlayer(Player player, String campaignId, Map<String, Object> campaignData) throws Exception {
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if(campaign == null) {
			throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		if(!campaign.getTerritoryId().equals(player.getTerritoryId())) {
			throw new BadRequestException("territory doesn't match", ErrorCode.TERRITORY_NOT_ALLOWED);
		}
		CampaignSubscription sub = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaignId, player.getPlayerId());
		if(sub != null) {
			throw new BadRequestException("player already joined", ErrorCode.CAMPAIGN_ALREADY_JOINED);
		}
		switch (campaign.getType()) {
			case personal:
				sub = personalCampaignSubscription.subscribeCampaign(player, campaign, campaignData);
				break;
			case company:
				sub = companyCampaignSubscription.subscribeCampaign(player, campaign, campaignData); 
				break;
			case city:
				sub = cityCampaignSubscription.subscribeCampaign(player, campaign, campaignData);
				break;
			case school:
		}
		return campaignSubscriptionRepository.save(sub);
	}
	
	public CampaignSubscription unsubscribePlayer(Player player, String campaignId) throws Exception {
		CampaignSubscription subscription = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaignId, player.getPlayerId());
		if(subscription != null) {
			campaignSubscriptionRepository.deleteById(subscription.getId());
		}
		return subscription;
	}
	
	public void updateDefaultCampaignSuscription(Player player) {
		Campaign campaign = getDefaultCampaignByTerritory(player.getTerritoryId());
		if(campaign != null) {
			CampaignSubscription sub = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), player.getPlayerId());
			if(sub != null) {
				Query query = new Query(new Criteria("id").is(sub.getId()));
				Update update = new Update();
				update.set("sendMail", player.getSendMail());
				update.set("mail", player.getMail());
				template.updateFirst(query, update, CAMPAIGNSUB);
			}
		}
	}
	
	public List<PlayerCampaign> getPlayerCampaigns(String playerId) {
		List<PlayerCampaign> result = new ArrayList<>();
		List<CampaignSubscription> campaigns = campaignSubscriptionRepository.findByPlayerId(playerId);
		for(CampaignSubscription sub : campaigns) {
			Campaign campaign = campaignRepository.findById(sub.getCampaignId()).orElse(null);
			if(campaign != null) {
				PlayerCampaign dto = new PlayerCampaign(campaign, sub);
				result.add(dto);
			}
		}
		return result;
	}

	public CampaignSubscription subscribePlayerByTerritory(String nickname, Campaign campaign,
			Map<String, Object> campaignData) throws Exception {
		Player player = playerRepository.findByNicknameIgnoreCase(nickname);
		if(player == null) {
			throw new BadRequestException("nickname doesn't exist", ErrorCode.PLAYER_NICK_NOT_FOUND);
		}
		if(!campaign.getTerritoryId().equals(player.getTerritoryId())) {
			throw new BadRequestException("territory doesn't match", ErrorCode.TERRITORY_NOT_ALLOWED);
		}
		CampaignSubscription sub = new CampaignSubscription();
		sub.setPlayerId(player.getPlayerId());
		sub.setCampaignId(campaign.getCampaignId());
		sub.setTerritoryId(player.getTerritoryId());
		sub.setMail(player.getMail());
		sub.setSendMail(player.getSendMail());
		sub.setRegistrationDate(LocalDate.now());
		if(campaignData != null) {
			sub.setCampaignData(campaignData);
		}
		return campaignSubscriptionRepository.save(sub);
	}
	
	public CampaignSubscription unsubscribePlayerByTerritory(String nickname, Campaign campaign) throws Exception {
		Player player = playerRepository.findByNicknameIgnoreCase(nickname);
		if(player == null) {
			throw new BadRequestException("nickname doesn't exist", ErrorCode.PLAYER_NICK_NOT_FOUND);
		}
		if(!campaign.getTerritoryId().equals(player.getTerritoryId())) {
			throw new BadRequestException("territory doesn't match", ErrorCode.TERRITORY_NOT_ALLOWED);
		}
		CampaignSubscription subscription = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaign.getCampaignId(), 
				player.getPlayerId());
		if(subscription != null) {
			campaignSubscriptionRepository.deleteById(subscription.getId());
		}
		return subscription;
	}
	
	public Image uploadCampaignLogo(String campaignId, MultipartFile data) throws Exception {
		Campaign campaign = getCampaign(campaignId);
		if(campaign == null) {
			logger.warn("campaign not found");
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);			
		}
		MediaType mediaType = MediaType.parseMediaType(data.getContentType());
		if (!mediaType.isCompatibleWith(MediaType.IMAGE_GIF) && !mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) && !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
			logger.warn("Image format not supported");
			throw new BadRequestException("Image format not supported", ErrorCode.IMAGE_WRONG_FORMAT);
		}
		Image logo = new Image();
		logo.setContentType(data.getContentType());
		byte[] targetArray = new byte[data.getInputStream().available()];
		data.getInputStream().read(targetArray);
		logo.setImage(targetArray);
		campaign.setLogo(logo);
		campaignRepository.save(campaign);
		return logo;
	}
}
