package it.smartcommunitylab.playandgo.engine.controller;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.manager.GameManager;
import it.smartcommunitylab.playandgo.engine.manager.TrackedInstanceManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeManager;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ValidateTripRequest;

@RestController
public class TestController extends PlayAndGoController {
    @Autowired
    TrackedInstanceManager trackedInstanceManager;
    
    @Autowired
    GameManager gameManager;
    
    @Autowired
    private ChallengeManager challengeManager;      
  
    static final Random RANDOM = new Random();
  
    @GetMapping("/api/test/track/player")
    public Page<TrackedInstanceInfo> getTrackedInstanceInfoList(
            @RequestParam String playerId,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        PageRequest pageRequest = PageRequest.of(RANDOM.nextInt(5), 20);
        return trackedInstanceManager.getTrackedInstanceInfoList(playerId, null, null, pageRequest);  
    }
    
    @GetMapping("/api/test/game/campaign")
    public PlayerGameStatus getCampaignGameStatus(
            @RequestParam String playerId,
            @RequestParam String campaignId,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return gameManager.getCampaignGameStatus(playerId, campaignId);
    }

    @GetMapping("/api/test/challenge")
    public @ResponseBody ChallengeConceptInfo getChallenges(
            @RequestParam String playerId,
            @RequestParam String campaignId,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return challengeManager.getChallenges(playerId, campaignId, null);
    }
    
    @GetMapping("/api/test/track/validate")
    public void validateTrack(
            @RequestParam(required = false) String trackedInstanceId,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        TrackedInstance ti = trackedInstanceManager.getTrackedInstance(trackedInstanceId);
        if(ti != null) {
            ValidateTripRequest msg = new ValidateTripRequest(ti.getUserId(), ti.getTerritoryId(), ti.getMultimodalId(), false);
            trackedInstanceManager.validateTripRequest(msg);
        }
    }

}
