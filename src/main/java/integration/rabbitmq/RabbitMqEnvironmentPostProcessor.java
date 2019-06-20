package integration.rabbitmq;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;

@Log4j2
class RabbitMqEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
																																				SpringApplication application) {

		var rmqAddress = System.getenv("RMQ_ADDRESS");
		log.debug("RMQ_ADDRESS: " + rmqAddress);
		if (!StringUtils.hasText(rmqAddress))
			return;

		log.info("detected RMQ_ADDRESS environment variable");

		var uri = URI.create(rmqAddress);
		var userInfo = uri.getUserInfo();
		var vhost = uri.getPath();
		var host = uri.getHost();
		var port = uri.getPort();
		var user = (String) null;
		var pw = (String) null;
		if (StringUtils.hasText(userInfo) && userInfo.contains(":")) {
			var parts = userInfo.split(":");
			user = parts[0];
			pw = parts[1];
		}

		var map = new HashMap<String, Object>();
		map.put("host", host);

		if (port != -1) {
			map.put("port", port);
		}

		if (StringUtils.hasText(vhost)) {
			vhost = vhost.trim();
			if (!vhost.equalsIgnoreCase("/")) {
				if (vhost.startsWith("/")) {
					vhost = vhost.substring(1);
				}
				map.put("virtual-host", vhost);
			}
		}

		if (StringUtils.hasText(pw)) {
			map.put("password", pw);
		}

		if (StringUtils.hasText(user)) {
			map.put("username", user);
		}

		var newMap = new HashMap<String, Object>();
		map.forEach((p, k) -> newMap.put("spring.rabbitmq." + p, k));

		var propertySource = new PropertySource<String>("rmq-environment") {

			@Override
			public Object getProperty(String name) {
				if (newMap.containsKey(name))
					return newMap.get(name);
				return null;
			}
		};

		environment.getPropertySources().addLast(propertySource);
	}

}
