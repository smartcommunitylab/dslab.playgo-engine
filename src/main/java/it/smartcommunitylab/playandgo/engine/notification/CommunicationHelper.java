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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.notification.Announcement.CHANNEL;
import it.smartcommunitylab.playandgo.engine.repository.TerritoryRepository;

/**
 * @author raman
 *
 */
@Component
public class CommunicationHelper {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private CommunicationManager notificationManager;
	@Autowired
	TerritoryRepository territoryRepository;

	@Autowired
	private EmailSender emailSender;

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
	
	public void notifyAnnouncement(Announcement announcement, String territoryId, String campaignId) throws Exception {
		Territory t = territoryRepository.findById(territoryId).orElse(null);
		ZoneId tz = t == null || t.getTimezone() == null ? ZoneId.systemDefault() : ZoneId.of(t.getTimezone());
		
		long from = -1;
		long to = -1;
		
		try {
			from = LocalDate.parse(announcement.getFrom()).atStartOfDay(tz).toInstant().toEpochMilli();
		} catch (Exception e) {
		}
		try {
			to = LocalDate.parse(announcement.getTo()).atStartOfDay(tz).toInstant().toEpochMilli();
		} catch (Exception e) {
		}
		
		Map<String, Object> content = new TreeMap<String, Object>();
		content.put("type", "announcement");
		if (announcement.getHtml() != null && !announcement.getHtml().isEmpty()) {
			content.put("_html", announcement.getHtml());
		}
		content.put("from", from);
		content.put("to", to);
		content.put("channels", announcement.getChannels());
		Notification not = new Notification();
		not.setTitle(announcement.getTitle());
		not.setDescription(announcement.getDescription());
		not.setContent(content);
		
		if (announcement.has(CHANNEL.email)) {
			sendEmail(not, territoryId, campaignId, announcement.getPlayers());
		}
		if (announcement.has(CHANNEL.news) || announcement.has(CHANNEL.push)) {
			if (announcement.getPlayers() != null && announcement.getPlayers().size() > 0) {
				for (String playerId: announcement.getPlayers()) {
					notify(not, playerId, territoryId, campaignId, announcement.has(CHANNEL.push));
				}
			} else {
				notify(not, null, territoryId, campaignId, announcement.has(CHANNEL.push));
			}
		}
	}
	
	/**
	 * @param not
	 * @param territoryId
	 * @param campaignId
	 * @param players
	 */
	private void sendEmail(Notification not, String territoryId, String campaignId, List<String> players) {
		if (players != null && players.size() > 0) {
			emailSender.sendGenericMailToUsers((String)not.getContent().get("_html"), not.getTitle(), territoryId, campaignId, null);
		} else {
			emailSender.sendGenericMailToAll((String)not.getContent().get("_html"), not.getTitle(), territoryId, campaignId);
		}
	}

	@SuppressWarnings("unchecked")
	public Announcement toAnnouncement(ZoneId tz, Notification n) {
		Announcement a = new Announcement();
		a.setDescription(n.getDescription());
		if (n.getContent().get("from") != null && !n.getContent().get("from").equals(-1l)) a.setFrom(new Date((long) n.getContent().get("from")).toInstant().atZone(tz).toLocalDate().toString());
		if (n.getContent().get("to") != null && !n.getContent().get("to").equals(-1l)) a.setTo(new Date((long) n.getContent().get("to")).toInstant().atZone(tz).toLocalDate().toString());
		a.setChannels((List<CHANNEL>) n.getContent().get("channels"));
		a.setTimestamp(n.getTimestamp());
		a.setTitle(n.getTerritoryId());
		a.setHtml((String) n.getContent().get("_html"));
		return a;
	}
}
