# Plano de Execução: foundry-chat

Projeto didático simples demonstrando a integração ponta a ponta:
HTML/JS -> Spring Boot -> Microsoft Foundry Agent -> Spring Boot -> HTML

## Fases do Desenvolvimento
- [ ] Fase 1: Inicialização do projeto Spring Boot e estrutura de pastas Maven
- [ ] Fase 2: Implementação dos DTOs (`ChatRequest`, `ChatResponse`), `ChatController` (`POST /api/chat`) e `ChatService` com resposta simulada
- [ ] Fase 3: Criação da interface simples (`index.html`, `style.css`, `script.js`) servida diretamente pelo Spring Boot (`src/main/resources/static`)
- [ ] Fase 4: Integração real no `ChatService` com a API REST do Microsoft Foundry (suporte a variáveis de ambiente e fallback para mock didático)
- [ ] Fase 5: Criação do `README.md` detalhado com diagrama de arquitetura e instruções de teste
- [ ] Fase 6: Validação e teste funcional do fluxo completo
