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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.playandgo.engine.exception.NoUserAccount;
import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;
import it.smartcommunitylab.playandgo.engine.notification.pushservice.GoogleCloudMessengerManager;
import it.smartcommunitylab.playandgo.engine.repository.NotificationRepository;

@Component
public class CommunicationManager {
	
	/**
	 * 
	 */
	private static final int MAX_CACHE_SIZE = 1000;

	private static final Logger logger = LoggerFactory.getLogger(CommunicationManager.class);

	@Autowired
	private GoogleCloudMessengerManager googleManager;
	@Autowired
	private NotificationRepository notificationRepository;
	
	/**
	 * Take last week of notifications
	 */
	private CacheLoader<String, NotificationCache> loader = new CacheLoader<String, NotificationCache>() {

		@Override
		public NotificationCache load(String key) throws Exception {
			Long since = 0l;
			Pageable pr = PageRequest.of(0, MAX_CACHE_SIZE);
			List<Notification> list = notificationRepository.searchNotifications(key, since, pr).getContent();
			if (list == null) list = Collections.emptyList();
			if (list.size() > 0) since = list.get(list.size()-1).getTimestamp();
			return new NotificationCache(list, since);
		}
		
	};

	private LoadingCache<String, NotificationCache> notificationCache =  CacheBuilder.newBuilder()
		       .maximumSize(100)
		       .expireAfterWrite(1, TimeUnit.DAYS)
		       .build(loader);
	

	public void create(Notification notification) throws NotFoundException, PushException {
		notification.setUpdateTime(System.currentTimeMillis());
		notification.setTimestamp(System.currentTimeMillis());
		notificationRepository.save(notification);
		
		if(notification.getAuthor().getMessagingAppId() != null){
			// app notification, refresh the cache
			if (notification.getUser() == null) {
				notificationCache.refresh(notification.getAuthor().getMessagingAppId());
			}
			try {
				googleManager.sendToCloud(notification);
			} catch (NoUserAccount e) {
				logger.error(e.getMessage(), e);
			}
		}
		
	}

	public List<Notification> get(String capp, Long since, Integer position, Integer count) {
		try {
			if (since == null) since = System.currentTimeMillis();
			NotificationCache cached = notificationCache.get(capp);
			int pos = position != null ? position : 0;
			int c = count != null ? count : 100;
			
			int sublistSize = 0;
			for (Notification n : cached.notifications) {
				if (n.getTimestamp() >= since) sublistSize++;
				else break;
			}
			if (pos >= sublistSize) return Collections.emptyList();

			int max = (pos + c) > sublistSize ? sublistSize : (pos + c); 
			return cached.notifications.subList(pos, max);
		} catch (ExecutionException e) {
			logger.error("Error reading cached data: "+ e.getMessage(), e);
		}
		int pos = position != null ? position : 0;
		int c = count != null ? count : 100;
		Pageable pr = PageRequest.of(pos / c, count);
		return notificationRepository.searchNotifications(capp, since, pr).getContent();
	}

	public Notification getById(String id)  {
		return notificationRepository.findById(id).orElse(null);
	}

	private static class NotificationCache {
		/**
		 * @param list
		 * @param since2
		 */
		public NotificationCache(List<Notification> list, Long since) {
			this.notifications = list; 
			this.since = since;
		}
		private List<Notification> notifications;
		@SuppressWarnings("unused")
		private Long since;
		
	}

}
