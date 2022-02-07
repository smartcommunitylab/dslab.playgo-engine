package it.smartcommunitylab.playandgo.engine.model;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="campaign")
public class Campaign {
	public static enum Type {
		company, city, school
	};

	@Id
	private String id;
	
	private String playerId;	
	private Type type;
	private String campaignId;
	private String territory;
	private String name;
	private String description;
	private LocalDate dateFrom;
	private LocalDate dateTo;
	private String gameId;
	private Boolean active = Boolean.FALSE;
	private Boolean communications = Boolean.FALSE;
	
	private String webPageUrl;
	private String privacyUrl;
	private String rulesUrl;
	private String logoUrl;
	private String organization;
	private String registrationUrl;
	
	private Map<String, Object> validationData;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public String getTerritory() {
		return territory;
	}

	public void setTerritory(String territory) {
		this.territory = territory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(LocalDate dateFrom) {
		this.dateFrom = dateFrom;
	}

	public LocalDate getDateTo() {
		return dateTo;
	}

	public void setDateTo(LocalDate dateTo) {
		this.dateTo = dateTo;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean getCommunications() {
		return communications;
	}

	public void setCommunications(Boolean communications) {
		this.communications = communications;
	}

	public String getWebPageUrl() {
		return webPageUrl;
	}

	public void setWebPageUrl(String webPageUrl) {
		this.webPageUrl = webPageUrl;
	}

	public String getPrivacyUrl() {
		return privacyUrl;
	}

	public void setPrivacyUrl(String privacyUrl) {
		this.privacyUrl = privacyUrl;
	}

	public String getRulesUrl() {
		return rulesUrl;
	}

	public void setRulesUrl(String rulesUrl) {
		this.rulesUrl = rulesUrl;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getRegistrationUrl() {
		return registrationUrl;
	}

	public void setRegistrationUrl(String registrationUrl) {
		this.registrationUrl = registrationUrl;
	}

	public Map<String, Object> getValidationData() {
		return validationData;
	}

	public void setValidationData(Map<String, Object> validationData) {
		this.validationData = validationData;
	}
	
	
	
	

}
