package it.smartcommunitylab.playandgo.engine.controller;

import java.util.Arrays;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.campaign.company.CompanyCampaignSurveyManager;
import it.smartcommunitylab.playandgo.engine.dto.TrackedInstanceInfo;
import it.smartcommunitylab.playandgo.engine.manager.GameManager;
import it.smartcommunitylab.playandgo.engine.manager.TrackedInstanceManager;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeConceptInfo;
import it.smartcommunitylab.playandgo.engine.manager.challenge.ChallengeManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.PlayerGameStatus;
import it.smartcommunitylab.playandgo.engine.model.TrackedInstance;
import it.smartcommunitylab.playandgo.engine.mq.ValidateTripRequest;
import it.smartcommunitylab.playandgo.engine.notification.EmailService;

@RestController
public class TestController extends PlayAndGoController {
    @Autowired
    TrackedInstanceManager trackedInstanceManager;
    
    @Autowired
    GameManager gameManager;
    
    @Autowired
    private ChallengeManager challengeManager;      

    @Autowired
    EmailService emailService;

    @Autowired
    CompanyCampaignSurveyManager companySurveyManager;

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
    

    @GetMapping("/api/test/email/survey")
    public void sendSurveyInviteByMail(
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String template,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        emailService.sendSurveyInvite("http://localhost/web", "Campagna test", email, "it", subject, template);
    }    

    @PostMapping("/api/test/notification/survey")
    public void sendSurveyInviteByNotification(
            @RequestParam String campaignId,
            @RequestParam String playerId,
            @RequestBody SurveyRequest sr,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        companySurveyManager.assignSurvey(campaignId, Arrays.asList(new String[]{playerId}), sr);
    }    

}
