package com.guutech.foundrychat.dto;

// DTO que representa a resposta enviada de volta para a interface web via JSON: { "response": "Olá! Como posso ajudar?" }
public record ChatResponse(String response) {
}
