package it.smartcommunitylab.playandgo.engine.mq;

public interface ManageWebhookRequest {
	public void sendMessage(WebhookRequest msg) throws Exception;
}
