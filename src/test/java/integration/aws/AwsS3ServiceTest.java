package integration.aws;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Log4j2
class AwsS3ServiceTest {

	@Autowired
	private AwsS3Service s3Service;

	@Test
	void getURI() {
		var fqn = s3Service.createS3Uri("podcast-input-bucket",
				"1143a526-3d70-41ea-8ec5-974a1a0b319f", "manifest.xml".trim());
		log.info("FQN: " + fqn.toString());
	}

}