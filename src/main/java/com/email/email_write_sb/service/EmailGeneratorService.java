package com.email.email_write_sb.service;

import com.email.email_write_sb.dto.RequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = WebClient.builder().build();
    }

    public String generateEmailReply(RequestDTO requestDTO) {
        //Build the Prompt

        String prompt = buildPrompt(requestDTO);
        //Craft a request
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        //Do request And get Resposne
        String response = webClient.post().uri(geminiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //Extract Response And return response
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper objectMapper=new ObjectMapper();
            JsonNode rootNode=objectMapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }catch (Exception e){
            return "Error progress request :"+e.getMessage();
        }
    }

    private String buildPrompt(RequestDTO requestDTO) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Generate a professinal mail reply for the following email content" +
                ", Please don't generate Subject of email");
        if(requestDTO.getTone() !=null  && !requestDTO.getTone().isEmpty()){
                prompt.append("Use a ").append(requestDTO.getTone()).append("tone");
        }
        prompt.append("\noriginal Email :\n").append(requestDTO.getEmailContent());
        return prompt.toString();
    }
}
