package integration;

import integration.database.User;
import integration.database.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
//todo go back in history and restore the SecurityConfiguration that used to be here
// todo also go back into the pom.xml and restore the jwt-spring-boot-starter

@Configuration
class CorsConfig {

	/*
	 *
	 * @Bean DefaultSecurityFilterChain authorization( HttpSecurity httpSecurity) throws
	 * Exception { DefaultSecurityFilterChain build = httpSecurity .authorizeRequests(ae
	 * -> ae .mvcMatchers("/test-upload/**").authenticated() .anyRequest().permitAll() )
	 * .cors(Customizer.withDefaults()) .oauth2ResourceServer(x -> x.jwt())
	 * .csrf(AbstractHttpConfigurer::disable) //
	 * .csrf(ServerHttpSecurity.CsrfSpec::disable) .build(); return build;
	 *
	 * }
	 */

	@Configuration
	public static class MyConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests(ae -> ae.mvcMatchers("/test-upload/**").authenticated().mvcMatchers("/podcasts/**")
					.authenticated().anyRequest().permitAll()).cors(Customizer.withDefaults())
					.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt).csrf(AbstractHttpConfigurer::disable);

		}

	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		var methods = List//
				.of(HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.GET)//
				.stream()//
				.map(Enum::name)//
				.collect(Collectors.toList());

		var configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowedOrigins(List.of("*"));
		configuration.setAllowedMethods(methods);
		return request -> configuration;
	}

	///
	@Bean
	UserDetailsService jdbcUserDetailsService(UserRepository repository) {
		return new JdbcUserDetailsService(repository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/*
	 * @Bean WebMvcConfigurer corsConfigurer() { return new WebMvcConfigurer() {
	 *
	 * @Override public void addCorsMappings(CorsRegistry registry) {
	 * registry.addMapping("/**").allowedMethods("PUT", "OPTIONS", "GET", "POST",
	 * "DELETE"); } }; }
	 */

}

@Log4j2
@RequiredArgsConstructor
class JdbcUserDetailsService implements UserDetailsService {

	@RequiredArgsConstructor
	private static class JpaUserDetails implements UserDetails {

		private final User user;

		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
			return Collections.singleton(new SimpleGrantedAuthority("USER"));
		}

		@Override
		public String getPassword() {
			return this.user.getPassword();
		}

		@Override
		public String getUsername() {
			return this.user.getUsername();
		}

		@Override
		public boolean isAccountNonExpired() {
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

	}

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		var byUsername = this.userRepository.findByUsernameIgnoreCase((username + "").toLowerCase()).stream()
				.map(JpaUserDetails::new).collect(Collectors.toList());

		if (byUsername.size() != 1) {
			throw new UsernameNotFoundException(
					"couldn't find one and only one instance of the user '" + username + "' ");
		}

		return byUsername.get(0);
	}

}