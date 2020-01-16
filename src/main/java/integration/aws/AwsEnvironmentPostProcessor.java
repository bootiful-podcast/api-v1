package integration.aws;

import integration.utils.FileUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;

/**
 * Registers the configuration values in {@code $HOME/.aws/credentials} and
 * {@code $HOME/.aws/config} as keys in the Spring
 * {@link org.springframework.core.env.Environment}.
 */
@Log4j2
class AwsEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@SneakyThrows
	private static void readFileIntoEnvironment(File file, String psPrefix,
			ConfigurableEnvironment environment, Function<String, String> mapper) {

		FileUtils.assertFileExists(file);
		try (var reader = new FileReader(file)) {
			var propertyProperties = new Properties();
			propertyProperties.load(reader);

			var map = new HashMap<String, Object>();
			propertyProperties.keySet().forEach(oldKey -> {
				var newKey = mapper.apply((String) oldKey);
				map.put(newKey, propertyProperties.getProperty((String) oldKey).trim());
			});

			var mapPropertySource = new MapPropertySource(psPrefix, map);
			environment.getPropertySources().addLast(mapPropertySource);
		}
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {

		var awsRoot = new File(System.getProperty("user.home"), ".aws");
		readFileIntoEnvironment(new File(awsRoot, "credentials"), "aws-credentials",
				environment, (key) -> key);
		readFileIntoEnvironment(new File(awsRoot, "config"), "aws-config", environment,
				k -> {
					if (k.equalsIgnoreCase("region")) {
						return "aws_region";
					}
					return k;
				});
	}

}
