package com.guutech.foundrychat.dto;

// DTO que representa a mensagem recebida da interface web via JSON: { "message": "Olá" }
public record ChatRequest(String message) {
}
