package it.smartcommunitylab.playandgo.engine.manager.highschool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PgHighSchoolConfig {

    @Value("${hsc.endpoint}")
    private String endpoint;

	@Bean(name = "hscClient")
	WebClient hscWebClient(
	    ClientRegistrationRepository clientRegistrationRepository,
	    OAuth2AuthorizedClientService authorizedClientService
	) {
			AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
		        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
		            clientRegistrationRepository, authorizedClientService);

		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
				authorizedClientManager
	    );
		oauth.setDefaultClientRegistrationId("oauthprovider");
	    return WebClient.builder()
	      .baseUrl(endpoint)
	      .apply(oauth.oauth2Configuration())
	      .build();
	}
}
