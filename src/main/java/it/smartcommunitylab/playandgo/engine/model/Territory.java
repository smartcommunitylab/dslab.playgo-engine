package it.smartcommunitylab.playandgo.engine.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="territories")
public class Territory {
	@Id
	private String territoryId;
	private String name;
	private String description;
	
	private Map<String, Object> territoryData = new HashMap<>();
	
	private String messagingAppId;	

	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<String, Object> getTerritoryData() {
		return territoryData;
	}
	public void setTerritoryData(Map<String, Object> territoryData) {
		this.territoryData = territoryData;
	}
	public String getMessagingAppId() {
		return messagingAppId;
	}
	public void setMessagingAppId(String messagingAppId) {
		this.messagingAppId = messagingAppId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
