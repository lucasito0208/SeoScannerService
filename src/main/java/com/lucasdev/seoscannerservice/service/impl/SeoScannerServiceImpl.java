package com.lucasdev.seoscannerservice.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucasdev.seoscannerservice.model.SeoReportDto;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeoScannerService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SeoScannerService(WebClient webClient) {
        this.webClient = webClient;
    }

    public SeoScannerService(WebClient.Builder webClientBuilder, Dotenv dotenv) {
        String apiURL = dotenv.get("WORDPRESS_API_URL");
        this.webClient = webClientBuilder
                .baseUrl(apiURL)
                .build();
    }

    public List<SeoReportDto> analizePost() {
        String rawJson = webClient.get()
                .uri("/wp-json/wp/v2/posts?per_page=10")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<SeoReportDto> resultados = new ArrayList<>();

        try {
            JsonNode posts = objectMapper.readTree(rawJson);

            for(JsonNode post : posts) {
                Long id = post.get("id").asLong();
                String titulo = post.get("title").get("rendered").asText();
                String extracto = post.get("excerpt").get("rendered").asText();
                String contenido = post.get("content").get("rendered").asText();

                boolean tieneTitulo = titulo != null && !titulo.isBlank();
                boolean tieneExtracto = extracto != null && extracto.length() > 30;
                boolean tieneContenido = contenido != null && contenido.length() > 200;

                resultados.add(new SeoReportDto(id, titulo, tieneTitulo, tieneExtracto, tieneContenido));
            }
        }catch(Exception e) {
            throw new RuntimeException("Error al analizar el JSON: "+ e.getMessage(),e);
        }

        return resultados;
    }




}
