package it.smartcommunitylab.playandgo.engine.notification.pushservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Message.Priority;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;

import it.smartcommunitylab.playandgo.engine.exception.NoUserAccount;
import it.smartcommunitylab.playandgo.engine.exception.NotFoundException;
import it.smartcommunitylab.playandgo.engine.exception.PushException;
import it.smartcommunitylab.playandgo.engine.model.Configuration;
import it.smartcommunitylab.playandgo.engine.model.UserAccount;
import it.smartcommunitylab.playandgo.engine.notification.Notification;
import it.smartcommunitylab.playandgo.engine.repository.UserAccountRepository;

@Component
public class GoogleCloudMessengerManager implements PushServiceCloud {
	private static transient final Logger logger = LoggerFactory.getLogger(GoogleCloudMessengerManager.class);
	
	@Value("${gcm.gcmSenderApiKey}")
	private String gcmSenderApiKey;	

	@Value("${gcm.gcmSenderId}")
	private String gcmSenderId;
	
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	@Autowired
	private  UserAccountRepository userAccountRepository;
	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.trentorise.smartcampus.vas.communicator.manager.pushservice.
	 * PushServiceCloud
	 * #sendToCloud(eu.trentorise.smartcampus.communicator.model.Notification)
	 */
	@Override
	public void sendToCloud(Notification notification) throws NotFoundException, NoUserAccount, PushException {
		if (notification.getUser() != null) {
			sendToCloudUser(notification);
		} else if (notification.getChannelIds() != null && !notification.getChannelIds().isEmpty()) {
			sendToCloudTopics(notification);
		}
	}
	
	private void sendToCloudUser(Notification notification) throws NotFoundException, NoUserAccount, PushException {

		
		// in default case is the system messenger that send
		FCMSender sender = null;
		String senderAppName = notification.getAuthor().getMessagingAppId();

		sender = new FCMSender(gcmSenderApiKey);

		String registrationId = "";

		logger.info("Sending message to user: "+senderAppName+" -> "+notification.getUser());

		UserAccount listUserAccount = userAccountRepository.findByPlayerId(notification.getUser());

		if (listUserAccount != null && sender != null) {

			UserAccount userAccountSelected = listUserAccount;

			List<Configuration> listConfUser = userAccountSelected.getConfigurations();
			if (listConfUser != null && !listConfUser.isEmpty()) {
				Message.Builder message = new Message.Builder().collapseKey("").delayWhileIdle(true).addData("title", notification.getTitle()).addData("description", notification.getDescription());
				if (notification.getContent() != null) {
					for (String key : notification.getContent().keySet()) {
						if (key.startsWith("_")) {
							continue;
						}						
						if (notification.getContent().get(key) != null) {
							message.addData("content." + key, notification.getContent().get(key).toString());
						}
					}
				}
				if (notification.getEntities() != null && !notification.getEntities().isEmpty()) {
					try {
						message.addData("entities", mapper.writeValueAsString(notification.getEntities()));
					} catch (Exception e) {
						logger.warn("Failed to convert entities: " + e.getMessage());
					}
				}
				
				message.addData("content-available", "1");
				message.addData("body", notification.getDescription());
				message.addData("title", notification.getTitle());
				
				message.priority(Priority.HIGH);
				
				List<String> regIds = new ArrayList<String>();
				List<String> iosRegIds = new ArrayList<String>();
				
				for (Configuration index : listConfUser) {
					registrationId = index.getRegistrationId();
					if (registrationId != null) {
						String platform = index.getPlatform();
						if ("android".equalsIgnoreCase(platform)) {
							regIds.add(registrationId);
						} else {
							iosRegIds.add(registrationId);
						}
					}
				}


				try {
					if (regIds.size() > 0) {
						logger.info("Sending android push to "+regIds);
						com.google.android.gcm.server.Notification.Builder builder = new com.google.android.gcm.server.Notification.Builder("");
						builder.title(notification.getTitle()).body(notification.getDescription());
						message.notification(builder.build());
						Message n = message.build();
						MulticastResult result = sender.send(n, regIds, 1);
						cleanRegistrations(userAccountSelected, regIds, result.getResults());
						logger.info("Android push result "+result);
					}
					if (iosRegIds.size() > 0) {
						com.google.android.gcm.server.Notification.Builder builder = new com.google.android.gcm.server.Notification.Builder("");
						builder.title(notification.getTitle()).body(notification.getDescription());
						message.notification(builder.build());

						logger.info("Sending iOS push to "+iosRegIds);
						MulticastResult result = sender.send(message.build(), iosRegIds, 1);
						cleanRegistrations(userAccountSelected, iosRegIds, result.getResults());
						logger.info("iOS push result "+result);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					throw new PushException(e);
				}
			}

		} else {
			throw new NoUserAccount("The user "+notification.getUser()+" is not register for receive push notification");
		}
	}

	/**
	 * @param userAccountSelected 
	 * @param regIds
	 * @param results
	 * @throws AlreadyExistException 
	 */
	private void cleanRegistrations(UserAccount account, List<String> regIds, List<Result> results)  {
		Set<String> toRemove = new HashSet<String>();
		for (int i = 0; i < results.size(); i++) {
			Result res = results.get(i);
			if (StringUtils.isEmpty(res.getMessageId())) {
				toRemove.add(regIds.get(i));
			}
		}
		List<Configuration> configs = new LinkedList<Configuration>();
		for (Configuration c : account.getConfigurations()) {
			if (c.getRegistrationId() != null && !toRemove.contains(c.getRegistrationId())) {
				configs.add(c);
			}
		}
		logger.info("cleaning user ("+account.getPlayerId()+") configs: "+toRemove);
		account.setConfigurations(configs);
		userAccountRepository.save(account);
	}

	private void sendToCloudTopics(Notification notification) throws PushException, NotFoundException {
		FCMSender sender = new FCMSender(gcmSenderApiKey);

		for (String topic : notification.getChannelIds()) {

			Message.Builder message = new Message.Builder().collapseKey("").delayWhileIdle(true).addData("title", notification.getTitle()).addData("description", notification.getDescription());
			if (notification.getContent() != null) {
				for (String key : notification.getContent().keySet()) {
					if (key.startsWith("_")) {
						continue;
					}
					if (notification.getContent().get(key) != null) {
						message.addData("content." + key, notification.getContent().get(key).toString());
					}
				}
			}
			if (notification.getEntities() != null && !notification.getEntities().isEmpty()) {
				try {
					message.addData("entities", mapper.writeValueAsString(notification.getEntities()));
				} catch (Exception e) {
					logger.warn("Failed to convert entities: " + e.getMessage());
				}
			}
			
			message.addData("content-available", "1");
			message.addData("body", notification.getDescription());
			message.addData("title", notification.getTitle());
			message.priority(Priority.HIGH);

			// REQUIRED ON IOS TO WORK
			com.google.android.gcm.server.Notification.Builder builder = new com.google.android.gcm.server.Notification.Builder("");
			builder.title(notification.getTitle()).body(notification.getDescription());
			message.notification(builder.build());
			
			try {
				logger.info("SENDING ANDROID " + message);
				Result sendresult = sender.send(message.build(), Constants.TOPIC_PREFIX + topic +".android", 1);
				logger.info("SENDING ANDROID RESULT " + sendresult);
				
				// REQUIRED ON IOS TO WORK
				logger.info("SENDING IOS " + message);
				sendresult = sender.send(message.build(), Constants.TOPIC_PREFIX + topic+".ios", 1);
				logger.info("SENDING IOS RESULT " + sendresult);
				logger.info("SENDING LEGACY " + message);
				sendresult = sender.send(message.build(), Constants.TOPIC_PREFIX + topic, 1);
				logger.info("SENDING LEGACY RESULT " + sendresult);
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new PushException(e);
			}
		}
	}
	
}
