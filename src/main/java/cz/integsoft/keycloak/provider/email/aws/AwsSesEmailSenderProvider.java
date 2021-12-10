package cz.integsoft.keycloak.provider.email.aws;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

/**
 * Provider for sending emails via AWS SES.
 *
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class AwsSesEmailSenderProvider implements EmailSenderProvider {

	private final SesClient ses;

	AwsSesEmailSenderProvider(final SesClient ses) {
		this.ses = ses;
	}

	@Override
	public void send(final Map<String, String> config, final UserModel user, final String subject, final String textBody, final String htmlBody) throws EmailException {

		final String from = config.get("from");
		final String fromDisplayName = config.get("fromDisplayName");
		final String replyTo = config.get("replyTo");
		final String replyToDisplayName = config.get("replyToDisplayName");

		try {
			if (from == null || from.isEmpty()) {
				throw new Exception("Missing 'from' email address.");
			}

			final SendEmailRequest.Builder sendEmailRequest = SendEmailRequest.builder().destination(Destination.builder().toAddresses(user.getEmail()).build())
					.message(Message.builder().subject(Content.builder().charset(StandardCharsets.UTF_8.toString()).data(subject).build())
							.body(Body.builder().html(Content.builder().charset(StandardCharsets.UTF_8.toString()).data(htmlBody).build()).text(Content.builder().charset(StandardCharsets.UTF_8.toString()).data(textBody).build()).build())
							.build())
					.source(toInternetAddress(from, fromDisplayName).toString());

			if (replyTo != null && !replyTo.isEmpty()) {
				sendEmailRequest.replyToAddresses(Collections.singletonList(toInternetAddress(replyTo, replyToDisplayName).toString()));
			}

			ses.sendEmail(sendEmailRequest.build());

		} catch (final Exception e) {
			ServicesLogger.LOGGER.failedToSendEmail(e);
			throw new EmailException(e);
		}
	}

	private InternetAddress toInternetAddress(final String email, final String displayName) throws Exception {
		if (email == null || "".equals(email.trim())) {
			throw new EmailException("Please provide a valid address", null);
		}
		if (displayName == null || "".equals(displayName.trim())) {
			return new InternetAddress(email);
		}
		return new InternetAddress(email, displayName, StandardCharsets.UTF_8.toString());
	}

	@Override
	public void close() {
	}
}
