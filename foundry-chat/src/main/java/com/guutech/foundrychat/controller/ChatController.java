package com.guutech.foundrychat.controller;

import com.guutech.foundrychat.dto.ChatRequest;
import com.guutech.foundrychat.dto.ChatResponse;
import com.guutech.foundrychat.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST responsável por receber as requisições HTTP do frontend
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    // Injeção de dependência do serviço de chat
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Endpoint: POST /api/chat
    // Recebe a mensagem enviada pelo navegador no corpo da requisição JSON
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        // Encaminha a mensagem para o serviço responsável pela comunicação
        String response = chatService.getAgentResponse(request.message());

        // Retorna a resposta empacotada em JSON para o navegador
        return new ChatResponse(response);
    }
}
