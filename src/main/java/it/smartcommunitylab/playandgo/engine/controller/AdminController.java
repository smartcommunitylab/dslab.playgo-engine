package it.smartcommunitylab.playandgo.engine.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import it.smartcommunitylab.playandgo.engine.campaign.company.CompanyCampaignSubscription;
import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.PlayerManager;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.repository.CampaignRepository;
import it.smartcommunitylab.playandgo.engine.repository.CampaignSubscriptionRepository;
import it.smartcommunitylab.playandgo.engine.repository.PlayerRepository;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@RestController
public class AdminController extends PlayAndGoController {
	private static transient final Logger logger = LoggerFactory.getLogger(AdminController.class);
	
	@Autowired
	PlayerManager playerManager;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@Autowired
	CampaignRepository campaignRepository;
	
	@Autowired
	CampaignSubscriptionRepository campaignSubscriptionRepository;
	
	@Autowired
	CompanyCampaignSubscription companyCampaignSubscription;
	
	@PostMapping(value = "/api/admin/player/upload")
	public List<String> uploadPlayers(
			@RequestParam String territoryId,
			@RequestParam String lang,
			@RequestParam("data") MultipartFile data,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		List<String> result = new ArrayList<>();
		MappingIterator<Map<String, String>> iterator = null;
		try {
			iterator = readCsv(data);
		} catch (Exception e) {
			result.add(e.getMessage());
		}
		int lineCount = 1;
		while(iterator.hasNextValue()) {
			Map<String, String> map = iterator.next();
			String playerId = map.get("id").trim();
			String nickname = map.get("nickname").trim();
			String email =  map.get("email").trim();
			String name = map.get("name").trim();
			String surname = map.get("surname").trim();
			Player p = new Player();
			p.setPlayerId(playerId);
			p.setNickname(nickname);
			p.setTerritoryId(territoryId);
			p.setLanguage(lang);
			p.setMail(email);
			p.setGivenName(name);
			p.setFamilyName(surname);
			p.setSendMail(true);
			try {
				playerManager.registerPlayer(p);
			} catch (Exception e) {
				result.add(String.format("error on player [%s] %s : %s", lineCount, playerId, e.getMessage()));
			}
			lineCount++;
		}
		return result;
	}

	@PostMapping(value = "/api/admin/company/subscribe/upload")
	public List<String> uploadCompanyCampaignSubscription(
			@RequestParam String territoryId,
			@RequestParam String campaignId,
			@RequestParam("data") MultipartFile data,
			HttpServletRequest request) throws Exception {
		checkAdminRole(request);
		List<String> result = new ArrayList<>();
		MappingIterator<Map<String, String>> iterator = null;
		try {
			iterator = readCsv(data);
		} catch (Exception e) {
			result.add(e.getMessage());
		}
		int lineCount = 1;
		while(iterator.hasNextValue()) {
			Map<String, String> map = iterator.next();
			String playerId = map.get("playerId").trim();
			String companyKey = map.get(CompanyCampaignSubscription.companyKey).trim();
			String employeeCode =  map.get(CompanyCampaignSubscription.employeeCode).trim();
			try {
				Player player = playerRepository.findById(playerId).orElse(null);
				if(player == null) {
					throw new BadRequestException("player doesn't exist", ErrorCode.PLAYER_NOT_FOUND);
				}
				Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
				if(campaign == null) {
					throw new BadRequestException("campaign doesn't exist", ErrorCode.CAMPAIGN_NOT_FOUND);
				}
				CampaignSubscription sub = campaignSubscriptionRepository.findByCampaignIdAndPlayerId(campaignId, player.getPlayerId());
				if(sub != null) {
					throw new BadRequestException("player already joined", ErrorCode.CAMPAIGN_ALREADY_JOINED);
				}
				Map<String, Object> campaignData = new HashMap<>();
				campaignData.put(CompanyCampaignSubscription.companyKey, companyKey);
				campaignData.put(CompanyCampaignSubscription.employeeCode, employeeCode);
				companyCampaignSubscription.subscribeCampaign(player, campaign, campaignData);
			} catch (Exception e) {
				result.add(String.format("error on player [%s] %s : %s", lineCount, playerId, e.getMessage()));
			}
			lineCount++;
		}
		return result;
	}
	
	private MappingIterator<Map<String, String>> readCsv(MultipartFile data)
			throws UnsupportedEncodingException, IOException {
		InputStreamReader contentReader = new InputStreamReader(data.getInputStream(), "UTF-8");
		CsvMapper csvMapper = new CsvMapper();
		CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
		MappingIterator<Map<String, String>> iterator = csvMapper.readerFor(Map.class)
				.with(csvSchema).readValues(contentReader);
		return iterator;
	}
	
			
}
