package it.smartcommunitylab.playandgo.engine.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="players")
public class Player {
	
	@Id
	private String playerId;
	@Indexed(unique=true)
	private String nickname;
	private String language;
	private String mail;
	private Boolean sendMail = Boolean.FALSE;
	@Indexed
	private String territoryId;
	private String givenName;
	private String familyName;
	private Boolean deleted = Boolean.FALSE;
	private Boolean group = Boolean.FALSE;
	private Map<String, Object> personalData = new HashMap<>();
//	private boolean checkedRecommendation;
//	private List<Event> eventsCheckIn;
	
	public Player() {
		super();
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getTerritoryId() {
		return territoryId;
	}

	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	public Boolean getSendMail() {
		return sendMail;
	}

	public void setSendMail(Boolean sendMail) {
		this.sendMail = sendMail;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

    public Boolean getGroup() {
        return group;
    }

    public void setGroup(Boolean group) {
        this.group = group;
    }

    public Map<String, Object> getPersonalData() {
        return personalData;
    }

    public void setPersonalData(Map<String, Object> personalData) {
        this.personalData = personalData;
    }

	
}
