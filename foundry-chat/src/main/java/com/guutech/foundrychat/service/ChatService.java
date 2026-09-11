package com.guutech.foundrychat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * ChatService: Responsável pela integração com o Microsoft Foundry (Azure AI Agent Service).
 *
 * Aqui fica centralizada toda a comunicação externa com a IA.
 * Se as variáveis de ambiente/propriedades não estiverem configuradas,
 * o serviço opera automaticamente em modo simulado (Mock didático).
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Value("${foundry.endpoint:}")
    private String endpoint;

    @Value("${foundry.api-key:}")
    private String apiKey;

    @Value("${foundry.agent-id:}")
    private String agentId;

    @Value("${foundry.api-version:2024-05-01-preview}")
    private String apiVersion;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ChatService() {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Ponto de entrada chamado pelo ChatController.
     */
    public String getAgentResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Por favor, digite uma mensagem válida.";
        }

        // Verifica se as credenciais do Microsoft Foundry foram configuradas
        if (!isFoundryConfigured()) {
            logger.info("Credenciais do Microsoft Foundry não configuradas. Retornando resposta simulada.");
            return "[Resposta Simulada] Olá! Para conversar com o seu Agent real no Microsoft Foundry, " +
                   "configure as variáveis FOUNDRY_ENDPOINT, FOUNDRY_API_KEY e FOUNDRY_AGENT_ID. " +
                   "Sua mensagem recebida foi: \"" + userMessage.trim() + "\"";
        }

        // Realiza a chamada real ao Microsoft Foundry
        try {
            return callMicrosoftFoundryAgent(userMessage.trim());
        } catch (Exception e) {
            logger.error("Erro na comunicação com o Microsoft Foundry: {}", e.getMessage(), e);
            return "Erro ao comunicar com o Microsoft Foundry: " + e.getMessage();
        }
    }

    /**
     * Executa o fluxo de 5 etapas do Azure AI Agent Service (OpenAI Assistants API):
     * 1. Criar Thread (sessão da conversa)
     * 2. Enviar mensagem do usuário para a Thread
     * 3. Executar o Agent na Thread (Criar Run)
     * 4. Aguardar o término do processamento (Polling do Run)
     * 5. Buscar e retornar a resposta gerada pelo Agent
     */
    private String callMicrosoftFoundryAgent(String userMessage) throws Exception {
        String baseUrl = normalizeBaseUrl(endpoint);

        // -------------------------------------------------------------
        // ETAPA 1: Cria uma nova Thread (conversa temporária)
        // POST {baseUrl}/threads?api-version={apiVersion}
        // -------------------------------------------------------------
        logger.info("// 1. Criando nova Thread no Microsoft Foundry...");
        String createThreadUrl = baseUrl + "/threads?api-version=" + apiVersion;

        String threadResponseJson = restClient.post()
                .uri(createThreadUrl)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .body(String.class);

        JsonNode threadNode = objectMapper.readTree(threadResponseJson);
        String threadId = threadNode.get("id").asText();
        logger.info("Thread criada com ID: {}", threadId);

        // -------------------------------------------------------------
        // ETAPA 2: Adiciona a mensagem do usuário à Thread criada
        // POST {baseUrl}/threads/{threadId}/messages?api-version={apiVersion}
        // -------------------------------------------------------------
        logger.info("// 2. Enviando mensagem do usuário para a Thread...");
        String addMessageUrl = baseUrl + "/threads/" + threadId + "/messages?api-version=" + apiVersion;
        Map<String, String> messageBody = Map.of(
                "role", "user",
                "content", userMessage
        );

        restClient.post()
                .uri(addMessageUrl)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(messageBody))
                .retrieve()
                .toBodilessEntity();

        // -------------------------------------------------------------
        // ETAPA 3: Executa o Agent associado na Thread
        // POST {baseUrl}/threads/{threadId}/runs?api-version={apiVersion}
        // -------------------------------------------------------------
        logger.info("// 3. Iniciando a execução (Run) do Agent: {}", agentId);
        String createRunUrl = baseUrl + "/threads/" + threadId + "/runs?api-version=" + apiVersion;
        Map<String, String> runBody = Map.of(
                "assistant_id", agentId
        );

        String runResponseJson = restClient.post()
                .uri(createRunUrl)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(runBody))
                .retrieve()
                .body(String.class);

        JsonNode runNode = objectMapper.readTree(runResponseJson);
        String runId = runNode.get("id").asText();

        // -------------------------------------------------------------
        // ETAPA 4: Aguarda o Agent processar a resposta (Polling do status)
        // GET {baseUrl}/threads/{threadId}/runs/{runId}?api-version={apiVersion}
        // -------------------------------------------------------------
        logger.info("// 4. Aguardando o Agent concluir a resposta...");
        String getRunUrl = baseUrl + "/threads/" + threadId + "/runs/" + runId + "?api-version=" + apiVersion;

        String runStatus = "queued";
        int attempts = 0;
        int maxAttempts = 30; // Aguarda até ~30 segundos

        while (!runStatus.equals("completed") && attempts < maxAttempts) {
            Thread.sleep(1000); // Aguarda 1 segundo entre as consultas

            String checkRunJson = restClient.get()
                    .uri(getRunUrl)
                    .header("api-key", apiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode statusNode = objectMapper.readTree(checkRunJson);
            runStatus = statusNode.get("status").asText();
            logger.debug("Status da execução do Agent: {}", runStatus);

            if (runStatus.equals("failed") || runStatus.equals("cancelled") || runStatus.equals("expired")) {
                throw new RuntimeException("O processamento do Agent falhou com status: " + runStatus);
            }

            attempts++;
        }

        if (!runStatus.equals("completed")) {
            throw new RuntimeException("Tempo limite excedido aguardando resposta do Agent.");
        }

        // -------------------------------------------------------------
        // ETAPA 5: Obtém as mensagens da Thread e extrai a resposta do Agent
        // GET {baseUrl}/threads/{threadId}/messages?api-version={apiVersion}
        // -------------------------------------------------------------
        logger.info("// 5. Recuperando a resposta gerada pelo Agent...");
        String listMessagesUrl = baseUrl + "/threads/" + threadId + "/messages?api-version=" + apiVersion;

        String listMessagesJson = restClient.get()
                .uri(listMessagesUrl)
                .header("api-key", apiKey)
                .retrieve()
                .body(String.class);

        JsonNode messagesRoot = objectMapper.readTree(listMessagesJson);
        JsonNode messagesArray = messagesRoot.get("data");

        if (messagesArray != null && messagesArray.isArray()) {
            for (JsonNode messageItem : messagesArray) {
                String role = messageItem.get("role").asText();
                if ("assistant".equalsIgnoreCase(role)) {
                    // Extrai o texto do primeiro bloco de conteúdo
                    JsonNode contentArray = messageItem.get("content");
                    if (contentArray != null && contentArray.isArray() && !contentArray.isEmpty()) {
                        JsonNode textNode = contentArray.get(0).get("text");
                        if (textNode != null && textNode.has("value")) {
                            return textNode.get("value").asText();
                        }
                    }
                }
            }
        }

        return "O agente concluiu o processamento, mas não retornou conteúdo textual.";
    }

    /**
     * Verifica se os parâmetros mínimos para chamada ao Microsoft Foundry estão preenchidos.
     */
    private boolean isFoundryConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && agentId != null && !agentId.isBlank();
    }

    /**
     * Normaliza o endpoint fornecido pelo usuário.
     * Trata variações comuns (com ou sem barra final, com ou sem /openai no caminho).
     */
    private String normalizeBaseUrl(String rawEndpoint) {
        String trimmed = rawEndpoint.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // Se o endpoint não contém /openai no final e for um recurso Azure OpenAI padrão, adiciona
        if (!trimmed.endsWith("/openai") && trimmed.contains(".openai.azure.com")) {
            trimmed = trimmed + "/openai";
        }
        return trimmed;
    }
}
