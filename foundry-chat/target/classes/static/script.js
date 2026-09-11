// JavaScript didático para o Foundry Chat
// Referências aos elementos da interface
const chatForm = document.getElementById("chat-form");
const userInput = document.getElementById("user-input");
const sendButton = document.getElementById("send-button");
const chatMessages = document.getElementById("chat-messages");

// Função auxiliar para adicionar mensagens na tela
function appendMessage(sender, text, cssClass) {
    const messageElement = document.createElement("div");
    messageElement.classList.add("message", cssClass);
    messageElement.innerHTML = `<strong>${sender}:</strong> ${text}`;
    chatMessages.appendChild(messageElement);
    
    // Rolagem automática para a mensagem mais recente
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return messageElement;
}

// Evento disparado quando o usuário envia o formulário (clique em Enviar ou tecla Enter)
chatForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const message = userInput.value.trim();
    if (!message) return;

    // 1. Mostra a mensagem digitada pelo usuário na tela
    appendMessage("Você", message, "user-message");
    userInput.value = "";

    // Desabilita campo e botão enquanto aguarda a resposta
    userInput.disabled = true;
    sendButton.disabled = true;

    // 2. Indicador visual de processamento
    const loadingMessage = appendMessage("Agente", "Pensando...", "system-status");

    try {
        // 3. Comunicação via fetch() com a API Spring Boot
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                message: message
            })
        });

        // Remove o indicador visual de processamento
        chatMessages.removeChild(loadingMessage);

        if (!response.ok) {
            throw new Error(`Erro na requisição: ${response.status} ${response.statusText}`);
        }

        // 4. Converte a resposta recebida em JSON
        const data = await response.json();

        // 5. Exibe a resposta do Agente na tela
        appendMessage("Agente", data.response, "agent-message");

    } catch (error) {
        // Remove o indicador caso ainda exista
        if (loadingMessage.parentNode) {
            chatMessages.removeChild(loadingMessage);
        }
        // Exibe mensagem de erro didática
        appendMessage("Sistema", `Falha ao obter resposta: ${error.message}`, "system-status");
        console.error("Erro ao chamar /api/chat:", error);
    } finally {
        // Reabilita o campo de entrada e botão
        userInput.disabled = false;
        sendButton.disabled = false;
        userInput.focus();
    }
});
