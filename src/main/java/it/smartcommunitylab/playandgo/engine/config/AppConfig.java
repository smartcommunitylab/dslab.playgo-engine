package it.smartcommunitylab.playandgo.engine.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.annotations.ApiParam;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.HttpAuthenticationScheme;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

/*
 * extend WebMvcConfigurerAdapter and not use annotation @EnableMvc to permit correct static
 * resources publishing and restController functionalities
 */
@Configuration
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {
	
	@Override
  public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/**").allowedMethods("*");
  }

	@Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/");
		registry
    	.addResourceHandler("swagger-ui.html")
      .addResourceLocations("classpath:/META-INF/resources/");
    registry
    	.addResourceHandler("/webjars/**")
      .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
	
	@Bean
	public Docket api() {
		return new Docket(DocumentationType.OAS_30).select()
				.apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.playandgo.engine.controller"))
				.paths(PathSelectors.ant("/**/api/**"))
				.build()
				.directModelSubstitute(Pageable.class, SwaggerPageable.class)
				.apiInfo(apiInfo())
				.securitySchemes(Arrays.asList(securitySchema()))
				.securityContexts(Arrays.asList(securityContext()));
	}
	
	private SecurityScheme securitySchema() {
		return HttpAuthenticationScheme.JWT_BEARER_BUILDER
	            .name("JWT")
	            .build();		
	}
	
    private SecurityContext securityContext() {
        return SecurityContext.builder()
            .securityReferences(defaultAuth())
            .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
            = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Arrays.asList(
            new SecurityReference("JWT", authorizationScopes));
    }
	
	private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
    		.title("Play&Go Project")
    		.version("2.0")
    		.license("Apache License Version 2.0")
    		.licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
    		.contact(new Contact("SmartCommunityLab", "https://http://www.smartcommunitylab.it/", "info@smartcommunitylab.it"))
    		.build();
	}
	
	private static class SwaggerPageable {

	    @ApiParam(required = true, value = "Number of records per page", example = "0")
	    public int size;

	    @ApiParam(required = true, value = "Results page you want to retrieve (0..N)", example = "0")
	    public int page;
	    
	    @ApiParam(required = false, value = "Sorting option: field,[asc,desc]", example = "nickname,desc")
	    public String sort;

	}	
	
}
