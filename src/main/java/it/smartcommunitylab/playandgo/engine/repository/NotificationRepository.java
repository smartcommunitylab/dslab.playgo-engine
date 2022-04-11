/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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

package it.smartcommunitylab.playandgo.engine.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.smartcommunitylab.playandgo.engine.notification.Notification;

/**
 * @author raman
 *
 */
public interface NotificationRepository extends MongoRepository<Notification, String>{

	@Query("{playerId: {$in:[?0, null]}, territoryId: ?1,campaignId: {$in: ?2}, 'timestamp': {$gte: ?3}}")
	Page<Notification> searchPlayerNotifications(String playerId, String territoryId, Collection<String> campaigns, Long since, Pageable pageRequest);

	@Query("{playerId: null, territoryId: ?0,campaignId: ?1}")
	Page<Notification> searchCampaignNotifications(String territoryId, String campaignId, Pageable pr);

}
