package it.smartcommunitylab.playandgo.engine.model;

import java.util.HashMap;
import java.util.Map;

public class CampaignReward {
	private Map<String, String> desc = new HashMap<>();
	private Map<String, String> rewardNote = new HashMap<>();
	private Map<String, String> sponsorDesc = new HashMap<>();
	private String sponsor;
	private String sponsorWebsite;
	private String winner;
	private int position;
	
	public Map<String, String> getDesc() {
		return desc;
	}
	public void setDesc(Map<String, String> desc) {
		this.desc = desc;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
    public Map<String, String> getRewardNote() {
        return rewardNote;
    }
    public void setRewardNote(Map<String, String> rewardNote) {
        this.rewardNote = rewardNote;
    }
    public Map<String, String> getSponsorDesc() {
        return sponsorDesc;
    }
    public void setSponsorDesc(Map<String, String> sponsorDesc) {
        this.sponsorDesc = sponsorDesc;
    }
    public String getSponsor() {
        return sponsor;
    }
    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }
    public String getSponsorWebsite() {
        return sponsorWebsite;
    }
    public void setSponsorWebsite(String sponsorWebsite) {
        this.sponsorWebsite = sponsorWebsite;
    }
    public String getWinner() {
        return winner;
    }
    public void setWinner(String winner) {
        this.winner = winner;
    }
}
