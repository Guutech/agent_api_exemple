package com.guutech.foundrychat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Ponto de entrada da aplicação Spring Boot
@SpringBootApplication
public class FoundryChatApplication {

    public static void main(String[] args) {
        // Inicializa o servidor web embutido (Tomcat) e carrega o contexto Spring
        SpringApplication.run(FoundryChatApplication.class, args);
    }
}
