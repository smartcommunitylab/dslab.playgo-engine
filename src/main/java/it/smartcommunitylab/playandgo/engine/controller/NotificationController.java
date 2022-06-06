package it.smartcommunitylab.playandgo.engine.controller;

import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.notification.Announcement;
import it.smartcommunitylab.playandgo.engine.notification.CommunicationHelper;
import it.smartcommunitylab.playandgo.engine.notification.CommunicationManager;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;
import it.smartcommunitylab.playandgo.engine.security.SecurityHelper;

@Controller
@RequestMapping(value = "/api/console/notifications")
public class NotificationController extends PlayAndGoController{

	@Autowired
	private CommunicationHelper notifier;		
	@Autowired
	private CommunicationManager notificationManager;
	@Autowired
	private SecurityHelper helper; 
	@Autowired
	TerritoryRepository territoryRepository;

	@PostMapping("/{territoryId}")
	public @ResponseBody Map<String, String> notifyCampaign(
			@PathVariable String territoryId, 
			@RequestParam(required = false) String campaignId,
			@RequestBody Announcement announcement) throws Exception {
		Map<String, String> result = Maps.newTreeMap();

		try {
			helper.checkRole(territoryId, campaignId);
			notifier.notifyAnnouncement(announcement, territoryId, campaignId);
			result.put("message", "Message \"" + announcement.getTitle() + "\" sent @ " + new Date());
		} catch (Exception e) {
			result.put("error", "Exception @ " + new Date() + ": " + e.toString());
		}

		return result;
	}

	@GetMapping("/{territoryId}")
	public @ResponseBody Page<Announcement> getNotifications(
			@PathVariable String territoryId, 
			@RequestParam(required = false) String campaignId,
			@RequestParam(required = false, defaultValue = "0") Integer skip, 
			@RequestParam(required = false, defaultValue = "20") Integer limit, 
			HttpServletResponse response) throws Exception 
	{
		helper.checkRole(territoryId, campaignId);
		Territory t = territoryRepository.findById(territoryId).orElse(null);
		ZoneId tz = t == null || t.getTimezone() == null ? ZoneId.systemDefault() : ZoneId.of(t.getTimezone());
		
		Page<Announcement> result = notificationManager.getCampaignNotifications(territoryId, campaignId, skip, limit).map(n -> notifier.toAnnouncement(tz, n));
		return result;
	}	
	

}