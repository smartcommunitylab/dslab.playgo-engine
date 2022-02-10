package it.smartcommunitylab.playandgo.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/*
 * extend WebMvcConfigurerAdapter and not use annotation @EnableMvc to permit correct static
 * resources publishing and restController functionalities
 */
@Configuration
@EnableWebMvc
@EnableSwagger2
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
		return new Docket(DocumentationType.SWAGGER_2).groupName("api").select()
				.apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.playandgo.engine.controller"))
				.paths(PathSelectors.ant("/api/**"))
				.build()
				.apiInfo(apiInfo());
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
	
}
