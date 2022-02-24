package it.smartcommunitylab.playandgo.engine.notification.pushservice;

import it.smartcommunitylab.playandgo.engine.exception.NoUserAccount;
import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;
import it.smartcommunitylab.playandgo.engine.notification.Notification;

public interface PushServiceCloud {

	public abstract void sendToCloud(Notification notification) throws NotFoundException, NoUserAccount, PushException;

}