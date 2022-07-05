package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import it.smartcommunitylab.playandgo.engine.exception.BadRequestException;
import it.smartcommunitylab.playandgo.engine.manager.CampaignManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyInfo;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyManager;
import it.smartcommunitylab.playandgo.engine.manager.survey.SurveyRequest;
import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;
import it.smartcommunitylab.playandgo.engine.util.Utils;

@Controller
public class SurveyController extends PlayAndGoController {
	
	@Autowired
	CampaignManager campaignManager;
	
	@Autowired
	SurveyManager surveyManager;
	
	@PostMapping("/api/survey/assign")
	public void assignSurveyChallenges(
			@RequestParam String campaignId,
			@RequestParam(required=false) List<String> playerIds,
			@RequestBody SurveyRequest sr,
			HttpServletRequest request) throws Exception {
		Campaign campaign = campaignManager.getCampaign(campaignId);
		if(campaign == null) {
			throw new BadRequestException("campaign not found", ErrorCode.CAMPAIGN_NOT_FOUND);
		}	
		checkRole(request, campaign.getTerritoryId(), campaign.getCampaignId());
		surveyManager.assignSurveyChallenges(campaignId, playerIds, sr);
	}
	
	@PostMapping("/survey/compile/{surveyName}")
	public void compileSurvey(
			@PathVariable String surveyName, 
			@RequestBody Map<String,Object> formData,
			HttpServletRequest request) throws Exception {
		surveyManager.compileSurvey(surveyName, formData);
	}
	
	@GetMapping("/survey/{lang}/{surveyName}/{id}")
	public ModelAndView redirectSurvey(
			@PathVariable String lang, 
			@PathVariable String surveyName, 
			@PathVariable String id,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		//RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(lang));
		ModelAndView model = null;
		SurveyInfo info = surveyManager.getSurveyUrl(id, surveyName);
		if(info.isCompleted()) {
			model = new ModelAndView("web/survey_complete");
			model.addObject("surveyComplete", true);
		} else if(Utils.isEmpty(info.getUrl())) {
			model = new ModelAndView("web/survey_complete");
			model.addObject("surveyComplete", false);				
		} else {
			model = new ModelAndView("redirect:" + info.getUrl());
		}
		return model;
	}
	
}
