package it.smartcommunitylab.playandgo.engine.controller;

import java.util.List;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.playandgo.engine.notification.Announcement;
import it.smartcommunitylab.playandgo.engine.notification.CommunicationHelper;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.security.SecurityHelper;

@Controller
@RequestMapping(value = "/api/console/notifications")
public class NotificationController extends PlayAndGoController{

	@Autowired
	private CommunicationHelper notifier;		
	@Autowired
	private SecurityHelper helper; 
	@Autowired
	TerritoryRepository territoryRepository;

	@PostMapping("/{territoryId}")
	public @ResponseBody Announcement notifyCampaign(
			@PathVariable String territoryId, 
			@RequestParam(required = false) String campaignId,
			@RequestBody Announcement announcement) throws Exception {
		helper.checkRole(territoryId, campaignId);
		return notifier.notifyAnnouncement(announcement, territoryId, campaignId);
	}

	@GetMapping("/{territoryId}")
	public @ResponseBody Page<Announcement> getNotifications(
			@PathVariable String territoryId, 
			@RequestParam(required = false) String campaignId,
			@RequestParam(required = false) List<String> channels,
			@ParameterObject Pageable pageRequest) throws Exception {
		helper.checkRole(territoryId, campaignId);
		Page<Announcement> result = notifier.getAnnouncements(territoryId, campaignId, channels, pageRequest);
		return result;
	}	
	

}