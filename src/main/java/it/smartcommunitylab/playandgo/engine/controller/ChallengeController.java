package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.dto.ChallengeStatsInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeChoice;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo.ChallengeDataType;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeInvitation.Reward;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.Invitation;
import it.smartcommunitylab.playandgo.engine.model.Player;
import it.smartcommunitylab.playandgo.engine.model.PlayerStatChallenge;

@RestController
public class ChallengeController extends PlayAndGoController {
	private static Log logger = LogFactory.getLog(ChallengeController.class);
	
	@Autowired
	private ChallengeManager challengeManager;		
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	@GetMapping("/api/challenge/type")
	public @ResponseBody List<ChallengeChoice> getChallengesStatus(
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return challengeManager.getChallengeStatus(player.getPlayerId(), campaignId);
	}	
	
	@PutMapping("/api/challenge/unlock/{challengeName}")
	public @ResponseBody List<ChallengeChoice> activateChallengeType(
			@PathVariable String challengeName, 
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return challengeManager.activateChallengeType(player.getPlayerId(), campaignId, challengeName);
	}	
	
	@GetMapping("/api/challenge")
	public @ResponseBody ChallengeConceptInfo getChallenges(
			@RequestParam String campaignId,
			@RequestParam(required=false) ChallengeDataType filter, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return challengeManager.getChallenges(player.getPlayerId(), campaignId, filter);
	}	
	
	@PutMapping("/api/challenge/choose/{challengeId}")
	public void chooseChallenge(
			@PathVariable String challengeId,
			@RequestParam String campaignId,
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		challengeManager.chooseChallenge(player.getPlayerId(), campaignId, challengeId);
	}
	
	@PostMapping("/api/challenge/invitation")
	public void sendInvitation(
			@RequestParam String campaignId,
			@RequestBody Invitation invitation, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		challengeManager.sendInvitation(player.getPlayerId(), campaignId, invitation);
	}

	@PostMapping("/api/challenge/invitation/preview")
	public @ResponseBody Map<String, String> getGroupChallengePreview(
			@RequestParam String campaignId,
			@RequestBody Invitation invitation, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return challengeManager.getGroupChallengePreview(player.getPlayerId(), campaignId, invitation);
	}	
	
	@PostMapping("/api/challenge/invitation/status/{challengeName}/{status}")
	public void changeInvitationStatus(
			@RequestParam String campaignId,
			@PathVariable String challengeName, 
			@PathVariable String status, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		challengeManager.changeInvitationStatus(player.getPlayerId(), campaignId, challengeName, status);
	}	
	
	@GetMapping("/api/challenge/challengeables")
	public @ResponseBody List<Map<String, String>> getChallengeables(
			@RequestParam String campaignId,			
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return challengeManager.getChallengeables(player.getPlayerId(), campaignId);
	}	
	
	@GetMapping("/api/challenge/rewards")
	public @ResponseBody Map<String, Reward> getRewards(
			HttpServletResponse response) throws Exception {
		return challengeManager.getRewards();
	}	
	
	@GetMapping("/api/challenge/blacklist")
	public @ResponseBody List<Map<String, String>> getBlackList(
			@RequestParam String campaignId,						
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		return challengeManager.getBlackList(player.getPlayerId(), campaignId);
	}
	
	@PostMapping("/api/challenge/blacklist")
	public @ResponseBody void addToBlackList(
			@RequestParam String campaignId,
			@RequestParam String blockedPlayerId, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		challengeManager.addToBlackList(player.getPlayerId(), campaignId, blockedPlayerId);
	}	
	
	@DeleteMapping("/api/challenge/blacklist")
	public @ResponseBody void deleteFromBlackList(
			@RequestParam String campaignId,
			@RequestParam String blockedPlayerId, 
			HttpServletRequest request) throws Exception {
		Player player = getCurrentPlayer(request);
		challengeManager.deleteFromBlackList(player.getPlayerId(), campaignId, blockedPlayerId);
	}		
	
	@GetMapping("/api/challenge/stats")
	public @ResponseBody List<ChallengeStatsInfo> getChallengeStats(
			@RequestParam String campaignId,
			@RequestParam String playerId,
			@RequestParam long dateFrom,
			@RequestParam long dateTo,
			HttpServletRequest request) throws Exception {
		return challengeManager.getStats(playerId, campaignId, dateFrom, dateTo);
	}
}
