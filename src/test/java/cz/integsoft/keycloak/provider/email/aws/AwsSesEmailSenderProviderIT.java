package cz.integsoft.keycloak.provider.email.aws;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.email.EmailException;
import org.keycloak.models.UserModel;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
class AwsSesEmailSenderProviderIT {

	@Test
	@Disabled("Run only manually")
	void testSend() throws EmailException {
		final SesClient ses = SesClient.builder().region(Region.EU_WEST_1).build();

		final Map<String, String> config = new HashMap<>();
		config.put("from", "john@example.com");
		config.put("fromDisplayName", "Keycloak Test");

		final UserModel user = mock(UserModel.class);
		when(user.getEmail()).thenReturn("john@example.com");

		final AwsSesEmailSenderProvider provider = new AwsSesEmailSenderProvider(ses);
		provider.send(config, user, "Test", "Hello", "<h1>Hello</h1>");
	}
}
