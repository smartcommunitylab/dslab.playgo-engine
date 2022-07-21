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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.playandgo.engine.exception.NoUserAccount;
import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;
import it.smartcommunitylab.playandgo.engine.notification.pushservice.GoogleCloudMessengerManager;
import it.smartcommunitylab.playandgo.engine.repository.NotificationRepository;

@Component
public class CommunicationManager {
	
	private static final Logger logger = LoggerFactory.getLogger(CommunicationManager.class);

	@Autowired
	private GoogleCloudMessengerManager googleManager;
	@Autowired
	private NotificationRepository notificationRepository;
	

	public Notification create(Notification notification, boolean push) throws NotFoundException, PushException {
		notification.setUpdateTime(System.currentTimeMillis());
		notification.setTimestamp(System.currentTimeMillis());
		notification = notificationRepository.save(notification);
		if (push) {
			try {
				googleManager.sendToCloud(notification);
			} catch (NoUserAccount e) {
				logger.error(e.getMessage(), e);
			}
		}
		return notification;
	}

	public Page<Notification> get(String playerId, String territoryId, Collection<String> campaigns, Long since, Pageable pageRequest) {
		return notificationRepository.searchPlayerNotifications(playerId, territoryId, campaigns, since, pageRequest);
	}

	public Notification getById(String id)  {
		return notificationRepository.findById(id).orElse(null);
	}

}
