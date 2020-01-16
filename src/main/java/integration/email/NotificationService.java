package integration.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final Configuration configuration;

	private final SendGrid sendGrid;

	private final Locale locale = Locale.getDefault();

	@SneakyThrows
	public String render(Template template, Map<String, Object> dataModel) {
		try (var sw = new StringWriter()) {
			template.process(dataModel, sw);
			return sw.toString();
		}
	}

	@SneakyThrows
	public String render(String templatePathName, Map<String, Object> dataModel) {
		var template = this.configuration.getTemplate(templatePathName, this.locale);
		return this.render(template, dataModel);
	}

	@SneakyThrows
	public Response send(Email to, Email from, String subject, String html) {
		var content = new Content("text/html", html);
		var mail = new Mail(from, subject, to, content);
		var request = new Request();
		request.setMethod(Method.POST);
		request.setEndpoint("mail/send");
		request.setBody(mail.build());
		return sendGrid.api(request);
	}

}
