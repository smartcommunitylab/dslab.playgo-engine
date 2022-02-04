package it.smartcommunitylab.playandgo.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlayGoEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlayGoEngineApplication.class, args);
	}
}
