package it.smartcommunitylab.playandgo.engine.notification;

import java.util.List;

import org.springframework.data.annotation.Id;

public class Announcement implements Comparable<Announcement> {

	public enum CHANNEL {email, push, news};
	
	@Id
	private String id;
	private String territoryId;
	private String campaignId;
	
	private String title;
	private String description;
	private String html;
	private String from;
	private String to;
	private Long timestamp;
	
	private List<CHANNEL> channels;
	private List<String> players;
	
	public String getId() {
		return id;
	}
	
	public String getTerritoryId() {
		return territoryId;
	}

	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public List<CHANNEL> getChannels() {
		return channels;
	}

	public void setChannels(List<CHANNEL> channels) {
		this.channels = channels;
	}

	public List<String> getPlayers() {
		return players;
	}

	public void setPlayers(List<String> players) {
		this.players = players;
	}

	public boolean has(CHANNEL channel) {
		return getChannels() != null && getChannels().contains(channel);  
	}
	
	@Override
	public int compareTo(Announcement o) {
		return (int)(o.timestamp - timestamp);
	}

}
