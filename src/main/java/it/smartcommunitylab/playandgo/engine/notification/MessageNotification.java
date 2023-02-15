package it.smartcommunitylab.playandgo.engine.notification;

import java.util.HashMap;
import java.util.Map;

public class MessageNotification extends NotificationGe {
    private String key;
    private Map<String, Object> data = new HashMap<String, Object>();
    
	@Override
	public String toString() {
		return String.format("[gameId=%s, playerId=%s, key=%s, data=%s]", getGameId(), getPlayerId(), getKey(), getData().toString());
	}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


}
