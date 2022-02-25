package it.smartcommunitylab.playandgo.engine.mq;

public interface ManageGameStatus {
	public void manageGameStatus(String msg, String routingKey);
}
