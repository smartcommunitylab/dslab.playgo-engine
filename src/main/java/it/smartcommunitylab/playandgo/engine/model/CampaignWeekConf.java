package it.smartcommunitylab.playandgo.engine.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampaignWeekConf {
	private String campaignId;
	private Date dateFrom;
	private Date dateTo;
	private int weekNumber;
	private Map<String, String> desc = new HashMap<>();
	private List<CampaignReward> rewards = new ArrayList<>();
	private String groupId;
	
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
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
	public int getWeekNumber() {
		return weekNumber;
	}
	public void setWeekNumber(int weekNumber) {
		this.weekNumber = weekNumber;
	}
	public List<CampaignReward> getRewards() {
		return rewards;
	}
	public void setRewards(List<CampaignReward> rewards) {
		this.rewards = rewards;
	}
    public Map<String, String> getDesc() {
        return desc;
    }
    public void setDesc(Map<String, String> desc) {
        this.desc = desc;
    }
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
}
