package pl;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
class RabbitMqEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

		var rmqAddress = System.getenv("RMQ_ADDRESS");
		log.info("RMQ_ADDRESS: " + rmqAddress);
		if (!StringUtils.hasText(rmqAddress))
			return;

		log.info("detected RMQ_ADDRESS environment variable");

		var uri = URI.create(rmqAddress);
		var userInfo = uri.getUserInfo();
		var vhost = uri.getPath();
		var host = uri.getHost();
		var port = uri.getPort();

		String user = null, pw = null;
		if (StringUtils.hasText(userInfo) && userInfo.contains(":")) {
			String[] parts = userInfo.split(":");
			user = parts[0];
			pw = parts[1];
		}

		var map = new HashMap<String, Object>();
		map.put("host", host);

		if (port != -1) {
			map.put("port", port);
		}

		if (StringUtils.hasText(vhost)) {
			map.put("virtual-host", vhost);
		}

		if (StringUtils.hasText(pw)) {
			map.put("password", pw);
		}

		if (StringUtils.hasText(user)) {
			map.put("username", user);
		}

		var propertySource = new PropertySource<String>("rmq-environment") {

			@Override
			public Object getProperty(String name) {
				var matchingKeys = map
					.entrySet()
					.stream()
					.filter(entry -> name.equalsIgnoreCase("spring.rabbitmq." + entry.getKey()));
				var toCollect = matchingKeys
					.map(Map.Entry::getValue)
					.collect(Collectors.toList());
				if (toCollect.size() > 0) {
					var valueThatMatches = toCollect.iterator().next();
					log.debug("for key '" + name + "' we found value '" + valueThatMatches + "'");
					return valueThatMatches;
				}
				return null;
			}
		};

		environment.getPropertySources().addLast(propertySource);
	}
}
