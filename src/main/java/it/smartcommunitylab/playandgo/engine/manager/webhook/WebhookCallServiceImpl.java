package it.smartcommunitylab.playandgo.engine.manager.webhook;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import it.smartcommunitylab.playandgo.engine.exception.ConnectorException;
import it.smartcommunitylab.playandgo.engine.util.ErrorCode;

@Service
public class WebhookCallServiceImpl implements WebhookCallService {

	@Override
	public void doPost(String url, String content) throws Exception {
		RestTemplate restTemplate = buildRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Object>(content, headers), String.class);
		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), ErrorCode.HTTP_ERROR);
		}		
	}
	
	private RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(15000);
		return new RestTemplate(factory);
	}

}
