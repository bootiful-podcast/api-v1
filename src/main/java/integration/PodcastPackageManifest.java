package integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

@Data
@AllArgsConstructor
@Log4j2
@NoArgsConstructor
public class PodcastPackageManifest {

	protected String title, description, uid;

	protected Photo photo = new Photo();

	@Data
	public static class Interview {

		private String src = "";

	}

	@Data
	public static class Introduction {

		private String src = "";

	}

	@Data
	public static class Photo {

		private String src = "";

	}

	@Data
	public static class ProducedAudio {

		private String src = "";

	}

}