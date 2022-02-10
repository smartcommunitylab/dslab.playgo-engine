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
	
	public Map<String, Object> territoryData = new HashMap<>();

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
}
