package it.smartcommunitylab.playandgo.engine.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="faketracks")
public class FakeTrack {
	@Id
	private String id;
    private String playerId;
    private String territoryId;
    private Date timestamp;

    // Constructors, getters, and setters
    public FakeTrack() {
    }   

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

    public String getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(String territoryId) {
        this.territoryId = territoryId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String toString() {
        return "FakeTrack{" +
                "id='" + id + '\'' +
                ", playerId='" + playerId + '\'' +
                ", territoryId='" + territoryId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
