package integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
class SecurityConfiguration {

	// todo figure out how to authenticate with users/passwords in an encrypted database
	// todo figure out how to mask only parts of the API

	@Bean
	UserDetailsService authentication() {
		return new InMemoryUserDetailsManager(User.withDefaultPasswordEncoder()//
				.username("jlong")//
				.password("password")//
				.roles("USER")//
				.build()//
		);
	}

	@Configuration
	public static class MyConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.cors(Customizer.withDefaults()) //
					.authorizeRequests(ae -> ae.mvcMatchers("/podcasts/{uid}").authenticated().anyRequest().permitAll()) //
					.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
		}

	}

}
