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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;
import it.smartcommunitylab.playandgo.engine.model.Territory;
import it.smartcommunitylab.playandgo.engine.notification.Announcement.CHANNEL;
import it.smartcommunitylab.playandgo.engine.repository.AnnouncementRepository;
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
	private TerritoryRepository territoryRepository;
	@Autowired
	private AnnouncementRepository announcementRepository;
	
	@Autowired
	private EmailSender emailSender;
    
	@Autowired 
    private TemplateEngine templateEngine;
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	

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
				n.addChannelId(territoryId + "-" + campaignId);
			} 
			else if (StringUtils.hasText(territoryId)) {
				n.addChannelId(territoryId);
			} 
		}
		return notificationManager.create(n, push);
	}
	
	public Announcement notifyAnnouncement(Announcement announcement, String territoryId, String campaignId) throws Exception {
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
		announcement.setTerritoryId(territoryId);
		announcement.setCampaignId(campaignId);
		return announcementRepository.save(announcement);
		
	}
	
	/**
	 * @param not
	 * @param territoryId
	 * @param campaignId
	 * @param players
	 */
	private void sendEmail(Notification not, String territoryId, String campaignId, List<String> players) {
		if (players != null && players.size() > 0) {
			emailSender.sendGenericMailToUsers((String)not.getContent().get("_html"), not.getTitle(), territoryId, campaignId, new HashSet<>(players));
		} else {
			emailSender.sendGenericMailToAll((String)not.getContent().get("_html"), not.getTitle(), territoryId, campaignId);
		}
	}

	public Page<Announcement> getAnnouncements(String territoryId, String campaignId, List<String> channels, Pageable pageRequest) {
		if (channels == null || channels.isEmpty()) channels = Stream.of(CHANNEL.values()).map(v -> v.name()).collect(Collectors.toList());
		if (StringUtils.hasText(campaignId)) return announcementRepository.searchAnnouncements(territoryId, campaignId, channels, pageRequest);
		else return announcementRepository.searchAnnouncements(territoryId, channels, pageRequest);
	} 

    public String getNotificationByTemplate(String template, String lang, Map<String, Object> vars) throws Exception {
        final Context ctx = new Context();
		ctx.setVariables(vars);

        String htmlContent = (lang.equals("it")) ? this.templateEngine.process("notification/" + template + "-it", ctx) 
                : this.templateEngine.process("notification/" + template + "-en", ctx);
		return htmlContent;
    }
	
}
