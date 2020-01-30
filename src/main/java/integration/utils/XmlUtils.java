package integration.utils;

import org.springframework.util.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Objects;

public abstract class XmlUtils {

	public static String getAttribute(Node map, String attr) {
		return getAttribute(Objects.requireNonNull(map).getAttributes(), attr);
	}

	private static String getAttribute(NamedNodeMap map, String attr) {
		if (map == null || map.getLength() == 0) {
			return null;
		}
		var namedItem = map.getNamedItem(attr);
		if (namedItem != null) {
			var textContent = namedItem.getTextContent();
			if (StringUtils.hasText(textContent)) {
				return textContent;
			}
		}
		return null;
	}

}
