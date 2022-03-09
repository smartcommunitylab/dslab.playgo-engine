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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="notifications")
public class Notification {
	
	@Id
	private String id;
	
	private String gameId;
	private String playerId;
	@Indexed
	private String campaignId;
	
	private long version;
	private long updateTime;
	private String title;
	private String description;
	private String messagingAppId;
	private String user;
	private Map<String, Object> content;
	private long timestamp;
	private boolean starred;
	private List<String> labelIds;
	private List<String> channelIds;
	private List<EntityObject> entities;

	private NotificationAuthor author;

	private boolean readed;

	private transient boolean markDeleted;

	public Notification() {
		super();
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Map<String, Object> getContent() {
		return content;
	}

	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isStarred() {
		return starred;
	}

	public void setStarred(boolean starred) {
		this.starred = starred;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getChannelIds() {
		return channelIds;
	}

	public void setChannelIds(List<String> channelIds) {
		this.channelIds = channelIds;
	}

	public void addChannelId(String channelId) {
		if (channelIds == null)
			channelIds = new ArrayList<String>();
		channelIds.add(channelId);
	}

	public boolean isReaded() {
		return readed;
	}

	public void setReaded(boolean readed) {
		this.readed = readed;
	}

	public List<String> getLabelIds() {
		return labelIds;
	}

	public void setLabelIds(List<String> labelIds) {
		this.labelIds = labelIds;
	}

	public List<EntityObject> getEntities() {
		return entities;
	}

	public void setEntities(List<EntityObject> entities) {
		this.entities = entities;
	}

	public NotificationAuthor getAuthor() {
		return author;
	}

	public void setAuthor(NotificationAuthor author) {
		this.author = author;
	}

	public static String userCopyId(String id, String userId) {
		return id + "_" + userId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public Notification copy(String userId) {
		Notification notification = new Notification();
		notification.setAuthor(author);
		notification.setChannelIds(channelIds);
		notification.setContent(content);
		notification.setDescription(description);
		notification.setEntities(entities);
		notification.setLabelIds(labelIds);
		notification.setReaded(readed);
		notification.setStarred(starred);
		notification.setTimestamp(timestamp);
		notification.setUpdateTime(updateTime);
		notification.setTitle(title);
		notification.setMessagingAppId(messagingAppId);
		notification.setUser(user);
		notification.setId(userCopyId(getId(), userId));
		return notification;
	}

	public void markAsDeleted() {
		markDeleted = true;
	}

	public boolean markedDeleted() {
		return markDeleted;
	}

	public String getMessagingAppId() {
		return messagingAppId;
	}

	public void setMessagingAppId(String messagingAppId) {
		this.messagingAppId = messagingAppId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
}
