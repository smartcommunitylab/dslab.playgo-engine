package it.smartcommunitylab.playandgo.engine.manager.highschool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

@Configuration
public class PgHighSchoolConfig {

    @Value("${hsc.endpoint}")
    private String endpoint;

	@Bean(name = "hscClient")
	RestTemplate hscRestTemplate(
	    RestTemplateBuilder builder,
	    ClientRegistrationRepository clientRegistrationRepository,
	    OAuth2AuthorizedClientService authorizedClientService
	) {
		AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
	        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
	            clientRegistrationRepository, authorizedClientService);

		// Interceptor per aggiungere il token OAuth2 alle richieste
		ClientHttpRequestInterceptor oauth2Interceptor = (request, body, execution) -> {
			var clientRegistration = clientRegistrationRepository.findByRegistrationId("oauthprovider");
			if (clientRegistration != null) {
				var authorizedClient = authorizedClientManager.authorize(
					org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
						.withClientRegistrationId("oauthprovider")
						.principal("oauthprovider")
						.build()
				);
				if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
					OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
					request.getHeaders().setBearerAuth(accessToken.getTokenValue());
				}
			}
			return execution.execute(request, body);
		};

		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(endpoint);
		factory.setEncodingMode(EncodingMode.NONE);

	    return builder
	      .rootUri(endpoint)
	      .uriTemplateHandler(factory)
	      .interceptors(oauth2Interceptor)
	      .build();
	}
}
