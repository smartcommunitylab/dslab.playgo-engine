package it.smartcommunitylab.playandgo.engine.model;

public class CampaignDetail {
	
	public static enum DescType {privacy, rules, faq, rewards, sponsor}
	
	private DescType type;
	private String name;
	private String extUrl;
	private String content;
	
	public DescType getType() {
		return type;
	}
	public void setType(DescType type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getExtUrl() {
		return extUrl;
	}
	public void setExtUrl(String extUrl) {
		this.extUrl = extUrl;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
}
