package integration;

import integration.database.User;
import integration.database.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
//todo go back in history and restore the SecurityConfiguration that used to be here
// todo also go back into the pom.xml and restore the jwt-spring-boot-starter

@Configuration
class CorsConfig {

	@Bean
	UserDetailsService jdbcUserDetailsService(UserRepository repository) {
		return new JdbcUserDetailsService(repository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/*
	 * @Configuration
	 *
	 * @RequiredArgsConstructor public static class WebmvcSecurityConfiguration extends
	 * WebSecurityConfigurerAdapter {
	 *
	 * @Override protected void configure(HttpSecurity builder) throws Exception { String
	 * url = "/test-upload/**";
	 *
	 * builder// .requestMatchers(c -> { c.mvcMatchers(url); })//
	 * .csrf(AbstractHttpConfigurer::disable)// .cors(Customizer.withDefaults())//
	 * .authorizeRequests(ae -> ae.mvcMatchers(url).authenticated())//
	 * .httpBasic(Customizer.withDefaults())// ; } }
	 */

	@Bean
	WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedMethods("PUT", "OPTIONS", "GET", "POST", "DELETE");
			}
		};
	}

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