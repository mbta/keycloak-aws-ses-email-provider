package cz.integsoft.keycloak.provider.email.aws;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.utils.EmailValidationUtil;
import org.keycloak.utils.SMTPUtil;

import jakarta.mail.internet.InternetAddress;
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
 * @author Integsoft
 */
public class AwsSesEmailSenderProvider implements EmailSenderProvider {

	private final SesClient ses;

	AwsSesEmailSenderProvider(final SesClient ses) {
		this.ses = ses;
	}

	@Override
	public void send(final Map<String, String> config, final UserModel user, final String subject, final String textBody, final String htmlBody) throws EmailException {
		send(config, user.getEmail(), subject, textBody, htmlBody);
	}

	@Override
	public void send(final Map<String, String> config, final String address, final String subject, final String textBody, final String htmlBody) throws EmailException {
		final String from = config.get("from");
		final String fromDisplayName = config.get("fromDisplayName");
		final String replyTo = config.get("replyTo");
		final String replyToDisplayName = config.get("replyToDisplayName");

		try {
			if (from == null || from.isEmpty()) {
				throw new Exception("Missing 'from' email address.");
			}

			final SendEmailRequest.Builder sendEmailRequest = SendEmailRequest.builder().destination(Destination.builder().toAddresses(address).build())
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
			throw new EmailException(e.getMessage(), e);
		}
	}

	@Override
	public void validate(Map<String, String> config) throws EmailException {
		// just static configuration checking here, not really testing email
		checkFromAddress(config.get("from"), isAllowUTF8(config));
	}

	private static boolean isAllowUTF8(Map<String, String> config) {
		return "true".equals(config.get("allowutf8"));
	}

	private static String checkFromAddress(String from, boolean allowutf8) throws EmailException {
		final String covertedFrom = convertEmail(from, allowutf8);
		if (from == null) {
			throw new EmailException(String.format(
					"Invalid sender address '%s'. If the address contains UTF-8 characters in the local part please ensure the SMTP server supports the SMTPUTF8 extension and enable 'Allow UTF-8' in the email realm configuration.", from));
		}
		return covertedFrom;
	}

	private static String convertEmail(String email, boolean allowutf8) throws EmailException {
		if (!EmailValidationUtil.isValidEmail(email)) {
			return null;
		}

		if (allowutf8) {
			// if allowutf8 the extension will manage both parts
			return email;
		}

		// if no allowutf8, do the IDN conversion over the domain part
		final String convertedEmail = SMTPUtil.convertIDNEmailAddress(email);
		if (convertedEmail == null || !convertedEmail.chars().allMatch(c -> c < 128)) {
			// now if there are non-ascii characters, we should send an error
			return null;
		}

		return convertedEmail;
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
