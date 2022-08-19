package it.smartcommunitylab.playandgo.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
@EnableRetry
public class PlayGoEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlayGoEngineApplication.class, args);
	}
}
