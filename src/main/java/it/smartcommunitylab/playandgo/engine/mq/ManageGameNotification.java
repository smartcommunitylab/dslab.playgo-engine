package it.smartcommunitylab.playandgo.engine.mq;

import java.util.Map;

public interface ManageGameNotification {
	public void manageGameNotification(Map<String, Object> msg, String routingKey);
}
