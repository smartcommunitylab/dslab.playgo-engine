package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignAcceptanceManager;
import it.smartcommunitylab.playandgo.engine.model.CampaignAcceptance;
import it.smartcommunitylab.playandgo.engine.model.CampaignSubscription;
import it.smartcommunitylab.playandgo.engine.model.Player;

@RestController
public class CampaignAcceptanceController extends PlayAndGoController {

    @Autowired
    private CampaignAcceptanceManager campaignAcceptanceManager;

    @PostMapping("/api/campaign-acceptance/user")
    public CampaignAcceptance userAcceptance(
            @RequestParam String campaignId,
            @RequestParam boolean privacyAcceptance, 
            @RequestParam boolean rulesAcceptance,
            HttpServletRequest request) throws Exception {
        Player player = getCurrentPlayer(request);
        return campaignAcceptanceManager.userAcceptance(player.getPlayerId(), campaignId, privacyAcceptance,
                rulesAcceptance);
    }

    @PostMapping("/api/campaign-acceptance")
    public CampaignAcceptance create(@RequestBody CampaignAcceptance acceptance, HttpServletRequest request)
            throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.create(acceptance);
    }

    @GetMapping("/api/campaign-acceptance/{id}")
    public CampaignAcceptance getById(@PathVariable String id, HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        CampaignAcceptance acceptance = campaignAcceptanceManager.getById(id);
        if (acceptance == null) {
            throw new BadRequestException("CampaignAcceptance not found with id: " + id);
        }
        return acceptance;
    }

    @GetMapping("/api/campaign-acceptance")
    public List<CampaignAcceptance> getAll(HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.getAll();
    }

    @PutMapping("/api/campaign-acceptance/{id}")
    public CampaignAcceptance update(@PathVariable String id, @RequestBody CampaignAcceptance acceptance,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.update(id, acceptance);
    }

    @DeleteMapping("/api/campaign-acceptance/{id}")
    public void delete(@PathVariable String id, HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        campaignAcceptanceManager.delete(id);
    }

    @GetMapping("/api/campaign-acceptance/by-campaign/{campaignId}")
    public List<CampaignAcceptance> getByCampaignId(@PathVariable String campaignId, HttpServletRequest request)
            throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.getByCampaignId(campaignId);
    }

    @GetMapping("/api/campaign-acceptance/by-player-campaign")
    public List<CampaignAcceptance> getByPlayerIdAndCampaignId(
            @RequestParam String playerId,
            @RequestParam String campaignId, 
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Sort.Direction direction, 
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.getByPlayerIdAndCampaignId(playerId, campaignId, sortBy, direction);
    }

    @PutMapping("/api/campaign-acceptance/subscription/{playerId}/{campaignId}")
    public CampaignSubscription updateSubscriptionFlags(
            @PathVariable String playerId,
            @PathVariable String campaignId,
            @RequestParam boolean privacyAcceptance,
            @RequestParam boolean rulesAcceptance,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.updateSubscriptionFlags(playerId, campaignId, privacyAcceptance,
                rulesAcceptance);
    }

    @PutMapping("/api/campaign-acceptance/subscriptions/{campaignId}")
    public List<CampaignSubscription> updateCampaignSubscriptionFlags(
            @PathVariable String campaignId,
            @RequestParam boolean privacyAcceptance,
            @RequestParam boolean rulesAcceptance,
            HttpServletRequest request) throws Exception {
        checkAdminRole(request);
        return campaignAcceptanceManager.updateCampaignSubscriptionFlags(campaignId, privacyAcceptance,
                rulesAcceptance);
    }
}