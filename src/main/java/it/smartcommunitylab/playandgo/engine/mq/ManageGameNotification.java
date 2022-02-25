package it.smartcommunitylab.playandgo.engine.mq;

public interface ManageGameNotification {
	public void manageGameNotification(String msg, String routingKey);
}
