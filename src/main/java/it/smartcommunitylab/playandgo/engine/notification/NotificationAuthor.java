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

import java.io.Serializable;

public class NotificationAuthor implements Serializable {
	private static final long serialVersionUID = -1045073082737340872L;

	private String messagingAppId;
	private String playerId;
	
	public String getMessagingAppId() {
		return messagingAppId;
	}
	public void setMessagingAppId(String messagingAppId) {
		this.messagingAppId = messagingAppId;
	}
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

}
