/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.engine.notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;

/**
 * @author raman
 *
 */
@Component
public class CommunicationHelper {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

	@Autowired
	private CommunicationManager notificationManager;

	/**
	 * Save notification entity and notify the user if needed. 
	 * @param n
	 * @param playerId, optional
	 * @param territoryId, optional
	 * @param campaignId, optional
	 * @param push, whether to send a push notification to user
	 * @throws PushException 
	 * @throws NotFoundException 
	 */
	public Notification notify(Notification n, String playerId, String territoryId, String campaignId, boolean push) throws Exception {
			long when = System.currentTimeMillis();
			n.setTimestamp(when);
			n.setTerritoryId(territoryId);
			n.setCampaignId(campaignId);
			n.setPlayerId(playerId);

			if (!StringUtils.hasText(playerId)) {
				if (StringUtils.hasText(campaignId)) {
					n.addChannelId(campaignId);
				} 
				else if (StringUtils.hasText(territoryId)) {
					n.addChannelId(territoryId);
				} 
			}
			return notificationManager.create(n, push);
	}
	
	public Notification notifyAnnouncement(Announcement announcement, String territoryId, String campaignId) throws Exception {
		Notification not = new Notification();
		
		not.setTitle(announcement.getTitle());
		not.setDescription(announcement.getDescription());
	
		long from = -1;
		long to = -1;
		
		try {
			from = sdf.parse(announcement.getFrom()).getTime();
		} catch (Exception e) {
		}
		try {
			to = sdf.parse(announcement.getTo()).getTime();
		} catch (Exception e) {
		}
		
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "announcement");
		if (announcement.getHtml() != null && !announcement.getHtml().isEmpty()) {
			content.put("_html", announcement.getHtml());
		}
		content.put("from", from);
		content.put("to", to);
		content.put("notification", Boolean.TRUE.equals(announcement.getNotification()));
		not.setContent(content);
		
		Notification stored = notify(not, null, territoryId, campaignId, Boolean.TRUE.equals(announcement.getNotification()));
		return stored;
	}
	
	public Announcement toAnnouncement(Notification n) {
		Announcement a = new Announcement();
		a.setDescription(n.getDescription());
		if (n.getContent().get("from") != null && !n.getContent().get("from").equals(-1l)) a.setFrom(sdf.format(new Date((long) n.getContent().get("from"))));
		if (n.getContent().get("to") != null && !n.getContent().get("to").equals(-1l)) a.setTo(sdf.format(new Date((long) n.getContent().get("to"))));
		a.setNotification((Boolean) n.getContent().get("notification"));
		a.setTimestamp(n.getTimestamp());
		a.setTitle(n.getTerritoryId());
		a.setHtml((String) n.getContent().get("_html"));
		return a;
	}
}
