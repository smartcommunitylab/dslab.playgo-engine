package it.smartcommunitylab.playandgo.engine.config;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;

/*
 * extend WebMvcConfigurerAdapter and not use annotation @EnableMvc to permit correct static
 * resources publishing and restController functionalities
 */
@Configuration
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {

	@Value("${mail.host}")
	private String host;
	@Value("${mail.port}")
	private String port;
	@Value("${mail.user}")
	private String username;
	@Value("${mail.password}")
	private String password;
    @Value("${mail.protocol}")
    private String protocol;
    @Value("${mail.localhost}")
    private String localhost;
	
	@Autowired
	MongoTemplate mongoTemplate;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("*");
	}

	@Bean
	public JavaMailSender getJavaMailSender() throws IOException {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(Integer.parseInt(port));
		sender.setUsername(username);
		sender.setPassword(password);
		sender.setProtocol(protocol);
		
		Properties props = new Properties();
		//props.setProperty("mail.smtp.ssl.enable", "true");
		props.setProperty("mail." + protocol + ".localhost", localhost);
		props.setProperty("mail." + protocol + ".connectiontimeout", "5000");
		sender.setJavaMailProperties(props);
		return sender;
	}
	
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(25000);
        return new RestTemplate(factory);        
    }

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
//		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
//		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}
	
	@Bean
    public LockProvider lockProvider() {
        return new MongoLockProvider(mongoTemplate.getCollection("shedlock"));
    }
	
	@Bean
	public SessionLocaleResolver localeResolver() {
		SessionLocaleResolver slr = new SessionLocaleResolver();
		slr.setDefaultLocale(Locale.ITALIAN);
		return slr;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang");
		return lci;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

	@Bean("messageSource")
	public MessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasenames("language/messages");
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}
	
    @Bean
    public OpenAPI springOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                    new Components()
                        .addSecuritySchemes(securitySchemeName,
                            new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                )                
                .info(new Info().title("Play&Go Project")
                .version("2.0.1")
                .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                .description("SmartCommunityLab")
                .url("https://http://www.smartcommunitylab.it/"));
    }

}
