package it.smartcommunitylab.playandgo.engine.notification.pushservice;

import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Service
public class FCMInitializer {

    @Value("${gcm.firebase-config:}")
    private String firebaseConfigPath;
    Logger logger = LoggerFactory.getLogger(FCMInitializer.class);
    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(firebaseConfigPath))).build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase application initialized");
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}