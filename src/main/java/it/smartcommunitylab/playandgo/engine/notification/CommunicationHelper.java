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
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author raman
 *
 */
@Component
public class CommunicationHelper {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private CommunicationManager notificationManager;

	public void notify(Notification n, String playerId, String messagingAppId) {
			long when = System.currentTimeMillis();
			n.setTimestamp(when);
			try {
				NotificationAuthor author = new NotificationAuthor();
				author.setPlayerId(playerId);

				n.setMessagingAppId(messagingAppId);
				n.setAuthor(author);

				if (StringUtils.isEmpty(playerId)) {
					n.setId(null);
					n.setUser(null);
					n.addChannelId(messagingAppId);
				} else {
					n.setUser(playerId);
				}
				notificationManager.create(n);
			} catch (Exception e) {
				e.printStackTrace();
				logger .error("Failed to send notifications: "+e.getMessage(), e);
			}
	}
	
	public void notifyAnnouncement(Announcement announcement, String messagingAppId) {
		Notification not = new Notification();
		
		not.setTitle(announcement.getTitle());
		not.setDescription(announcement.getDescription());
	
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
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
		not.setContent(content);
		
		notify(not, null, messagingAppId);
	}
}
