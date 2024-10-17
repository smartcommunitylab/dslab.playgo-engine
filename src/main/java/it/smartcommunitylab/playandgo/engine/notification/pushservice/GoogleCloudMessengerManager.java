package it.smartcommunitylab.playandgo.engine.notification.pushservice;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

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
		if (notification.getPlayerId() != null) {
			sendToCloudUser(notification);
		} else if (notification.getChannelIds() != null && !notification.getChannelIds().isEmpty()) {
			sendToCloudTopics(notification);
		}
	}


	private AndroidConfig getAndroidConfig(String topic) {
        return AndroidConfig.builder()
                .setCollapseKey("")
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();
    }
    private ApnsConfig getApnsConfig(String topic) {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setCategory(topic).setThreadId(topic).build()).build();
    }
	
	private void sendToCloudUser(Notification notification) throws NotFoundException, NoUserAccount, PushException {


		UserAccount listUserAccount = userAccountRepository.findByPlayerId(notification.getPlayerId());

		if (listUserAccount != null) {

			UserAccount userAccountSelected = listUserAccount;

			List<Configuration> listConfUser = userAccountSelected.getConfigurations();
			if (listConfUser != null && !listConfUser.isEmpty()) {
				MulticastMessage.Builder msgBuilder = MulticastMessage.builder();
				List<String> regIds = listConfUser.stream().map(c -> c.getRegistrationId()).collect(Collectors.toList());
  				msgBuilder.addAllTokens(regIds)
				.putData("title", notification.getTitle())
				.putData("description", notification.getDescription());
				
				// Message.Builder message = new Message.Builder().collapseKey("").delayWhileIdle(true).addData("title", notification.getTitle()).addData("description", notification.getDescription());
				if (notification.getContent() != null) {
					for (String key : notification.getContent().keySet()) {
						if (key.startsWith("_")) {
							continue;
						}						
						if (notification.getContent().get(key) != null) {
							msgBuilder.putData("content." + key, notification.getContent().get(key).toString());
						}
					}
				}
				
				msgBuilder.putData("content-available", "1");
				msgBuilder.putData("body", notification.getDescription());
				msgBuilder.putData("title", notification.getTitle());
				

				AndroidConfig androidConfig = getAndroidConfig("");
        		ApnsConfig apnsConfig = getApnsConfig("");
        		com.google.firebase.messaging.Notification n = com.google.firebase.messaging.Notification.builder()
                                        .setTitle(notification.getTitle())
                                        .setBody(notification.getDescription())
                                        .build();

				msgBuilder.setAndroidConfig(androidConfig).setApnsConfig(apnsConfig).setNotification(n);
				try {
					MulticastMessage message = msgBuilder.build();
					BatchResponse res = FirebaseMessaging.getInstance().sendEachForMulticast(message);
					cleanRegistrations(userAccountSelected, regIds, res.getResponses());
				} catch (Exception e) {
					e.printStackTrace();
					throw new PushException(e);
				}
			}

		} else {
			throw new NoUserAccount("The user "+notification.getPlayerId()+" is not register for receive push notification");
		}
	}

	/**
	 * @param userAccountSelected 
	 * @param regIds
	 * @param results
	 * @throws AlreadyExistException 
	 */
	private void cleanRegistrations(UserAccount account, List<String> regIds, List<SendResponse> results)  {
		Set<String> toRemove = new HashSet<String>();
		for (int i = 0; i < results.size(); i++) {
			SendResponse res = results.get(i);
			if (!StringUtils.hasText(res.getMessageId())) {
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

		for (String topic : notification.getChannelIds()) {
			Message.Builder msgBuilder = Message.builder();				

			if (notification.getContent() != null) {
				for (String key : notification.getContent().keySet()) {
					if (key.startsWith("_")) {
						continue;
					}
					if (notification.getContent().get(key) != null) {
						msgBuilder.putData("content." + key, notification.getContent().get(key).toString());
					}
				}
			}
			
			msgBuilder.putData("content-available", "1");
			msgBuilder.putData("body", notification.getDescription());
			msgBuilder.putData("description", notification.getDescription());
			msgBuilder.putData("title", notification.getTitle());

			AndroidConfig androidConfig = getAndroidConfig(topic);
			ApnsConfig apnsConfig = getApnsConfig(topic);
			com.google.firebase.messaging.Notification n = com.google.firebase.messaging.Notification.builder()
									.setTitle(notification.getTitle())
									.setBody(notification.getDescription())
									.build();

			msgBuilder.setAndroidConfig(androidConfig).setApnsConfig(apnsConfig).setNotification(n);
			msgBuilder.setTopic(topic);
			
			try {
				logger.info("SENDING ANDROID " + msgBuilder);
				msgBuilder.setTopic(topic + ".android");
				Message forAndroid = msgBuilder.build();
				FirebaseMessaging.getInstance().send(forAndroid);

				logger.info("SENDING IOS " + msgBuilder);
				msgBuilder.setTopic(topic + ".ios");
				Message forIos = msgBuilder.build();
				FirebaseMessaging.getInstance().send(forIos);

				logger.info("SENDING LEGACY " + msgBuilder);
				msgBuilder.setTopic(topic);
				Message forLegacy = msgBuilder.build();
				FirebaseMessaging.getInstance().send(forLegacy);
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new PushException(e);
			}
		}
	}
	
}
