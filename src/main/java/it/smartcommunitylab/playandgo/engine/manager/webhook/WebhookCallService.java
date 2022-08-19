package it.smartcommunitylab.playandgo.engine.manager.webhook;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public interface WebhookCallService {
	@Retryable(value = Exception.class, 
		      maxAttempts = 3, backoff = @Backoff(delay = 60000, multiplier = 5))
	public void doPost(String url, String content) throws Exception;
}
