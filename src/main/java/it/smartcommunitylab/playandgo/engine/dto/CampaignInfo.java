package it.smartcommunitylab.playandgo.engine.dto;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.playandgo.engine.model.Campaign;
import it.smartcommunitylab.playandgo.engine.model.Campaign.Type;

public class CampaignInfo {
	private String campaignId;
	private Type type;
	private String territoryId;
	private Map<String, String> name = new HashMap<>();
	private Map<String, String> description = new HashMap<>();
	private Date dateFrom;
	private Date dateTo;
	private int startDayOfWeek = 1; //Monday is 1 and Sunday is 7
	
	public CampaignInfo() {}
	
	public CampaignInfo(Campaign campaign) {
		this.campaignId = campaign.getCampaignId();
		this.type = campaign.getType();
		this.territoryId = campaign.getTerritoryId();
		this.name = campaign.getName();
		this.description = campaign.getDescription();
		this.dateFrom = campaign.getDateFrom();
		this.dateTo = campaign.getDateTo();
		this.startDayOfWeek = campaign.getStartDayOfWeek();
	}
	
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
	public Map<String, String> getName() {
		return name;
	}
	public void setName(Map<String, String> name) {
		this.name = name;
	}
	public Map<String, String> getDescription() {
		return description;
	}
	public void setDescription(Map<String, String> description) {
		this.description = description;
	}
	public Date getDateFrom() {
		return dateFrom;
	}
	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}
	public Date getDateTo() {
		return dateTo;
	}
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}
	public int getStartDayOfWeek() {
		return startDayOfWeek;
	}
	public void setStartDayOfWeek(int startDayOfWeek) {
		this.startDayOfWeek = startDayOfWeek;
	}

}
