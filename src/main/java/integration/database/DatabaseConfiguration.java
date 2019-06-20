package integration.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
class DatabaseConfiguration {

	@Bean
	TransactionTemplate template(PlatformTransactionManager txm) {
		return new TransactionTemplate(txm);
	}

}
