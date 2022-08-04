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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.smartcommunitylab.playandgo.engine.notification.Announcement;

/**
 * @author raman
 *
 */
public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

	@Query("{territoryId: ?0, campaignId: ?1, channels: {$in: ?2}}")
	Page<Announcement> searchAnnouncements(String territoryId, String campaignId, List<String> channels, Pageable pr);

	@Query("{territoryId: ?0, channels: {$in: ?1}}")
	Page<Announcement> searchAnnouncements(String territoryId, List<String> channels, Pageable pr);

}
