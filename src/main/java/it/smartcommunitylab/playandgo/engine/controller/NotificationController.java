package it.smartcommunitylab.playandgo.engine.controller;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import it.smartcommunitylab.playandgo.engine.notification.Announcement;
import it.smartcommunitylab.playandgo.engine.notification.CommunicationHelper;
import it.smartcommunitylab.playandgo.engine.notification.CommunicationManager;

@Controller
@RequestMapping(value = "/console/notification")
public class NotificationController extends PlayAndGoController{

	@Autowired
	private CommunicationHelper notifier;		
	@Autowired
	private CommunicationManager notificationManager;
	
	@RequestMapping(method = RequestMethod.POST, value = "/{territoryId}/{campaignId}")
	public @ResponseBody Map<String, String> notifyCampaign(
			@PathVariable String territoryId, 
			@PathVariable String campaignId,
			@RequestBody(required=false) Announcement announcement) throws Exception {
		Map<String, String> result = Maps.newTreeMap();

		try {
			// TODO: check access rights
			notifier.notifyAnnouncement(announcement, territoryId, campaignId);

			if (announcement.getNotification() != null && announcement.getNotification().booleanValue()) {
			}
			result.put("message", "Message \"" + announcement.getTitle() + "\" sent @ " + new Date());
		} catch (Exception e) {
			result.put("error", "Exception @ " + new Date() + ": " + e.toString());
		}

		return result;
	}

	@GetMapping("/{territoryId}/{campaignId}")
	public @ResponseBody Page<Announcement> getNotifications(
			@PathVariable String territoryId, 
			@PathVariable String campaignId,
			@RequestParam(required = false, defaultValue = "0") Integer skip, 
			@RequestParam(required = false, defaultValue = "20") Integer limit, 
			HttpServletResponse response) throws Exception 
	{
		// TODO: check access rights
		
		Page<Announcement> result = notificationManager.getCampaignNotifications(territoryId, campaignId, skip, limit).map(n -> notifier.toAnnouncement(n));
		return result;
	}	
	

}