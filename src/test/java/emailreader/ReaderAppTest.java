package emailreader;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import emailreader.config.AiProperties;
import emailreader.config.EmailProperties;
import emailreader.service.OpenAIResponder;
import emailreader.service.EmailFilterService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class ReaderAppTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @MockitoBean
    private EmailProperties emailProperties;

    @MockitoBean
    private AiProperties aiProperties;

    @MockitoBean
    private OpenAIResponder openAIResponder;

    @MockitoBean
    private EmailFilterService emailFilterService;

    private static final String CUSTOMER_EMAIL = "tommasoz2005@gmail.com";
    private static final String EXPERT_EMAIL = "expert@hln.it";
    private static final String SUPPORT_EMAIL = "support@hln.it";
    private static final String UNAUTHORIZED_EMAIL = "unauthorized@test.com";
    private static final String ESCALATION_TOKEN = "[HLN_ESCALATION]";

    @BeforeEach
    void setup() throws Exception {
        when(emailProperties.getUsername()).thenReturn(SUPPORT_EMAIL);
        when(emailProperties.getExpertEmail()).thenReturn(EXPERT_EMAIL);
        when(emailProperties.getImapHost()).thenReturn("localhost");
        when(emailProperties.getSmtpHost()).thenReturn("localhost");
        when(emailProperties.getPassword()).thenReturn("test-password-123");
        when(aiProperties.getKey()).thenReturn("sk-test-fake-api-key-for-testing-only");
        when(aiProperties.getModel()).thenReturn("gpt-4");
    }

    @Test
    @DisplayName("Email da mittente non autorizzato viene ignorata")
    void testUnauthorizedSenderIsIgnored() throws Exception {
        when(emailFilterService.isAllowedSender(UNAUTHORIZED_EMAIL)).thenReturn(false);
        AdviceWith.adviceWith(camelContext, "OpenAIEmailResponder", a -> {
            a.replaceFromWith("direct:start-unauthorized");
            a.interceptSendToEndpoint("smtp:*")
                    .skipSendToOriginalEndpoint()
                    .to("mock:smtp-result");
        });
        camelContext.start();
        MockEndpoint mockSmtp = camelContext.getEndpoint("mock:smtp-result", MockEndpoint.class);
        mockSmtp.expectedMessageCount(0);
        Map<String, Object> headers = new HashMap<>();
        headers.put("from", UNAUTHORIZED_EMAIL);
        headers.put("subject", "Test Unauthorized");
        producerTemplate.sendBodyAndHeaders("direct:start-unauthorized", "Test body", headers);
        mockSmtp.assertIsSatisfied();
        verify(emailFilterService, times(1)).isAllowedSender(UNAUTHORIZED_EMAIL);
    }

    @Test
    @DisplayName("Email valida riceve risposta standard dall'AI")
    void testAllowedSenderGetsStandardReply() throws Exception {
        final String originalSubject = "Aiuto con il mio ordine";
        final String originalBody = "Ho un problema con il mio ordine #12345";
        final String aiResponse = "Grazie per averci contattato. Il tuo ordine è in elaborazione.";
        when(emailFilterService.isAllowedSender(CUSTOMER_EMAIL)).thenReturn(true);
        AdviceWith.adviceWith(camelContext, "OpenAIEmailResponder", a -> {
            a.replaceFromWith("direct:start-standard");
            a.weaveById("call-ai-responder")
                    .replace()
                    .setBody(a.constant(aiResponse));

            a.interceptSendToEndpoint("smtp:*")
                    .skipSendToOriginalEndpoint()
                    .to("mock:smtp-result");
        });
        camelContext.start();
        MockEndpoint mockSmtp = camelContext.getEndpoint("mock:smtp-result", MockEndpoint.class);
        mockSmtp.expectedMessageCount(1);
        mockSmtp.expectedHeaderReceived("To", CUSTOMER_EMAIL);
        mockSmtp.expectedHeaderReceived("Subject", "Re: " + originalSubject);
        mockSmtp.expectedHeaderReceived("From", SUPPORT_EMAIL);
        Map<String, Object> headers = new HashMap<>();
        headers.put("from", CUSTOMER_EMAIL);
        headers.put("subject", originalSubject);
        producerTemplate.sendBodyAndHeaders("direct:start-standard", originalBody, headers);
        mockSmtp.assertIsSatisfied();
        Exchange receivedExchange = mockSmtp.getReceivedExchanges().get(0);
        String receivedBody = receivedExchange.getIn().getBody(String.class);
        assertThat(receivedBody).isEqualTo(aiResponse);
        assertThat(receivedExchange.getIn().getHeader("To")).isEqualTo(CUSTOMER_EMAIL);
        assertThat(receivedExchange.getIn().getHeader("Subject")).isEqualTo("Re: " + originalSubject);
        verify(emailFilterService, times(1)).isAllowedSender(CUSTOMER_EMAIL);
    }

    @Test
    @DisplayName("Email complessa innesca escalation all'esperto e notifica al cliente")
    void testComplexEmailTriggersEscalation() throws Exception {
        final String originalSubject = "Problema tecnico complesso";
        final String originalBody = "Ho un problema molto complesso con il sistema che non riesco a risolvere.";
        final String aiResponseWithToken = ESCALATION_TOKEN + " Questa richiesta richiede supporto specializzato.";
        final String cleanAiResponse = "Questa richiesta richiede supporto specializzato.";
        when(emailFilterService.isAllowedSender(CUSTOMER_EMAIL)).thenReturn(true);
        AdviceWith.adviceWith(camelContext, "OpenAIEmailResponder", a -> {
            a.replaceFromWith("direct:start-escalation");
            a.weaveById("call-ai-responder")
                    .replace()
                    .setBody(a.constant(aiResponseWithToken));
            a.interceptSendToEndpoint("smtp:*")
                    .skipSendToOriginalEndpoint()
                    .to("mock:smtp-result");
        });
        camelContext.start();
        MockEndpoint mockSmtp = camelContext.getEndpoint("mock:smtp-result", MockEndpoint.class);
        mockSmtp.expectedMessageCount(2);
        Map<String, Object> headers = new HashMap<>();
        headers.put("from", CUSTOMER_EMAIL);
        headers.put("subject", originalSubject);
        producerTemplate.sendBodyAndHeaders("direct:start-escalation", originalBody, headers);
        mockSmtp.assertIsSatisfied();
        Exchange expertExchange = mockSmtp.getReceivedExchanges().get(0);
        assertThat(expertExchange.getIn().getHeader("To")).isEqualTo(EXPERT_EMAIL);
        assertThat(expertExchange.getIn().getHeader("Subject")).isEqualTo("Escalation: " + originalSubject);
        assertThat(expertExchange.getIn().getHeader("From")).isEqualTo(SUPPORT_EMAIL);
        String expertBody = expertExchange.getIn().getBody(String.class);
        assertThat(expertBody)
                .contains("Il SupportBot ha identificato un problema complesso")
                .contains("Mittente originale: " + CUSTOMER_EMAIL)
                .contains("Oggetto: " + originalSubject)
                .contains(originalBody)
                .contains(cleanAiResponse);
        assertThat(expertBody).doesNotContain(ESCALATION_TOKEN);
        Exchange customerExchange = mockSmtp.getReceivedExchanges().get(1);
        assertThat(customerExchange.getIn().getHeader("To")).isEqualTo(CUSTOMER_EMAIL);
        assertThat(customerExchange.getIn().getHeader("Subject")).isEqualTo("Re: " + originalSubject);
        assertThat(customerExchange.getIn().getHeader("From")).isEqualTo(SUPPORT_EMAIL);
        String customerBody = customerExchange.getIn().getBody(String.class);
        assertThat(customerBody).isEqualTo(cleanAiResponse);
        assertThat(customerBody).doesNotContain(ESCALATION_TOKEN);
        verify(emailFilterService, times(1)).isAllowedSender(CUSTOMER_EMAIL);
    }
}