package emailreader.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import emailreader.config.Prompts;
import okhttp3.OkHttpClient;
import org.apache.camel.Exchange;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;
import java.util.Arrays;

public class OpenAIResponder {

    private final OpenAiApi api;
    private final String model;

    public OpenAIResponder(String apiKey, String model) {
        this.model = model;
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .addInterceptor(chain -> chain.proceed(
                        chain.request()
                                .newBuilder()
                                .addHeader("Authorization", "Bearer " + apiKey)
                                .build()))
                .build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) 
                .build();

        this.api = retrofit.create(OpenAiApi.class);
    }

    public String generateResponse(Exchange exchange) {
        try {
            String emailBody = exchange.getIn().getBody(String.class);
            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), Prompts.HLN_SUPPORT_BOT_PROMPT);
            ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), emailBody);
            ChatCompletionRequest chatRequest = new ChatCompletionRequest();
            chatRequest.setModel(this.model); 
            chatRequest.setMessages(Arrays.asList(systemMessage, userMessage));
            chatRequest.setTemperature(0.7);
            chatRequest.setMaxTokens(1500);
            ChatCompletionResult result = api.createChatCompletion(chatRequest).blockingGet();
            if (result == null || result.getChoices().isEmpty()) {
                throw new RuntimeException("Nessuna risposta valida da OpenAI");
            }
            String aiResponse = result.getChoices().get(0).getMessage().getContent();
            exchange.getIn().setHeader("AiSuccessful", true);
            return aiResponse;
        } catch (Exception e) {
            exchange.getIn().setHeader("AiSuccessful", false);
            exchange.getIn().setHeader("Subject", "ATTENZIONE: Errore Risposta AI");
            return "Errore: impossibile generare la risposta automatica. " + e.getMessage();
        }
    }
}
