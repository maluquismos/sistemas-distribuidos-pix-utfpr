package br.com.banco.servidor.service;

import br.com.banco.model.Transacao;
import br.com.banco.model.Usuario;
import br.com.banco.servidor.dao.UsuarioDAO;
import br.com.banco.servidor.exception.AuthenticationException;
import br.com.banco.servidor.exception.BusinessException;
// import br.com.banco.servidor.exception.ProtocolException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import br.com.banco.validator.Validator;
import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.function.Consumer;
import br.com.banco.servidor.dao.TransacaoDAO;
import br.com.banco.servidor.util.PasswordUtil;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiConsumer;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Consumer<String> logger;
    private final String clientIp;
    private final ObjectMapper mapper = new ObjectMapper();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TransacaoService transacaoService = new TransacaoService();
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final Runnable onDisconnect;
    private final BiConsumer<String, String> onLoginSuccess;
//    private final BiConsumer<String, String> onUpdateUser;
    private final Consumer<String> onLogout;
    
//    public ClientHandler(Socket socket, Consumer<String> logger, Runnable onDisconnect, BiConsumer<String, String> onLoginSuccess, Consumer<String> onLogout, BiConsumer<String, String> onUpdateUser) {
//        this.clientSocket = socket;
//        this.logger = logger;
//        this.clientIp = socket.getInetAddress().getHostAddress();
//        this.onDisconnect = onDisconnect;
//        this.onLoginSuccess = onLoginSuccess;
//        this.onLogout = onLogout;
//        this.onUpdateUser = onUpdateUser;
//    }
    
    public ClientHandler(Socket socket, Consumer<String> logger, Runnable onDisconnect, BiConsumer<String, String> onLoginSuccess, Consumer<String> onLogout) {
        this.clientSocket = socket;
        this.logger = logger;
        this.clientIp = socket.getInetAddress().getHostAddress();
        this.onDisconnect = onDisconnect;
        this.onLoginSuccess = onLoginSuccess;
        this.onLogout = onLogout;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String handshakeJson = in.readLine();
            if (handshakeJson == null) {
                return; 
            }

            logger.accept(String.format("[%s] Recebido (Handshake): %s", clientIp, handshakeJson));
            JsonNode handshakeNode = mapper.readTree(handshakeJson);
            if (!handshakeNode.has("operacao") || !handshakeNode.get("operacao").asText().equals("conectar")) {
                String connectResponse = criarRespostaErro("conectar", "Protocolo violado: a primeira operação deve ser 'conectar'.");
                logger.accept(String.format("[ERRO] Protocolo violado por [%s]. Primeira operação não foi 'conectar'. Desconectando.", clientIp));
                logger.accept(String.format("[%s] Enviando (Handshake): %s", clientIp, connectResponse));
                out.println(connectResponse);
                return;
            }
            
            // Handshake bem-sucedido, envia resposta de sucesso
            String connectResponse = criarRespostaSucesso("conectar", "Servidor conectado com sucesso.");
            logger.accept(String.format("[%s] Enviando (Handshake): %s", clientIp, connectResponse));
            out.println(connectResponse);

            // --- INICIA O LOOP NORMAL DE OPERAÇÕES ---
            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                logger.accept(String.format("[%s] Recebido: %s", clientIp, jsonRequest));
                
                String jsonResponse;
                JsonNode requestNode = null;
                try {
                    Validator.validateClient(jsonRequest);
                    requestNode = mapper.readTree(jsonRequest);
                    jsonResponse = processarOperacao(requestNode);

                } catch (Exception e) {
                    String operacao = (requestNode != null && requestNode.has("operacao"))
                                      ? requestNode.get("operacao").asText() : "operacao_invalida";
                    logger.accept(String.format("[ERRO] %s: %s", operacao, e.getMessage()));
                    jsonResponse = criarRespostaErro(operacao, e.getMessage());
                }

                logger.accept(String.format("[%s] Enviando: %s", clientIp, jsonResponse));
                out.println(jsonResponse);
            }

        } catch (IOException e) {
            logger.accept("Conexão com cliente [" + clientIp + "] perdida ou fechada.");
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {}
            
            if (onDisconnect != null) {
                onDisconnect.run();
            }
        }
    }
    
    private String processarOperacao(JsonNode request) throws Exception {
        String operacao = request.get("operacao").asText();
        switch (operacao) {
            case "usuario_login": return handleLogin(request);
            case "usuario_criar": return handleCriarUsuario(request);
            case "conectar": return handleConectar();
        }

        String token = request.get("token").asText();
        String cpfUsuarioLogado = SessaoManager.validarToken(token, clientIp);
        
        switch (operacao) {
            case "usuario_logout": return handleLogout(request);
            case "usuario_ler": return handleLerUsuario(cpfUsuarioLogado);
            case "usuario_atualizar": return handleAtualizarUsuario(cpfUsuarioLogado, request);
            case "usuario_deletar": return handleDeletarUsuario(cpfUsuarioLogado);
            case "transacao_criar": return handleCriarTransacao(cpfUsuarioLogado, request);
            case "transacao_ler": return handleLerTransacoes(cpfUsuarioLogado, request);
            case "depositar": return handleDepositar(cpfUsuarioLogado, request);
            default:
                return handleUnknownOperation(operacao);
        }
    }

    private String handleConectar() throws Exception {
        Object response = criarRespostaBase(true, "conectar", "Já conectado anteriormente.");
        return mapper.writeValueAsString(response);
    }

    private String handleUnknownOperation(String operacao) throws Exception{
        Object response = criarRespostaBase(false, operacao, "Operação ainda não implementada.");
        return mapper.writeValueAsString(response);
    }

    private String handleLogin(JsonNode request) throws Exception {
        String cpf = request.get("cpf").asText();
        String senha = request.get("senha").asText();
        
        Usuario usuario = usuarioDAO.validarLogin(cpf, senha);
        if (usuario == null) {
            throw new AuthenticationException("CPF ou senha inválidos.");
        }
        
        String token = SessaoManager.criarSessao(usuario.getCpf(), clientIp);
        logger.accept("Usuário " + usuario.getNome() + " (" + cpf + ") logado com sucesso.");

        if (onLoginSuccess != null) {
            String ip = clientSocket.getInetAddress().getHostAddress();
            String porta = String.valueOf(clientSocket.getPort());
            String clientId = ip + ":" + porta;
            
            onLoginSuccess.accept(clientId, usuario.getNome());
        }

        ObjectNode response = criarRespostaBase(true, "usuario_login", "Login bem-sucedido.");
        response.put("token", token);
        return mapper.writeValueAsString(response);
    }

    private String handleCriarUsuario(JsonNode request) throws Exception {
        String cpf = request.get("cpf").asText();
        String nome = request.get("nome").asText();
        String senha = request.get("senha").asText();

        if (usuarioDAO.buscarUsuarioPorCpf(cpf) != null) {
            throw new BusinessException("CPF já cadastrado no sistema.");
        }
        
        String senhaHash = PasswordUtil.hashPassword(senha);
        Usuario novoUsuario = new Usuario(cpf, nome, senhaHash, BigDecimal.ZERO);
        boolean sucesso = usuarioDAO.criarUsuario(novoUsuario);
        if (!sucesso) {
            throw new Exception("Ocorreu um erro inesperado ao salvar o usuário no banco de dados.");
        }
        
        logger.accept("Novo usuário criado: " + cpf);
        return criarRespostaSucesso("usuario_criar", "Usuário criado com sucesso.");
    }

    private String handleLogout(JsonNode request) throws Exception {
        String token = request.get("token").asText();
        boolean sucesso = SessaoManager.encerrarSessao(token);
        if (sucesso) {
            logger.accept("Sessão encerrada para o token: " + token.substring(0, 8) + "...");
            
            if(onLogout != null) {
                String ip = clientSocket.getInetAddress().getHostAddress();
                String porta = String.valueOf(clientSocket.getPort());
                String clientId = ip + ":" + porta;
                
                onLogout.accept(clientId);
            }
            
            return criarRespostaSucesso("usuario_logout", "Logout realizado com sucesso.");
        } else {
            throw new AuthenticationException("Token inválido ao tentar fazer logout.");
        }
    }

    private String handleLerUsuario(String cpf) throws Exception {
        Usuario usuario = usuarioDAO.buscarUsuarioPorCpf(cpf);
        if (usuario == null) {
            throw new BusinessException("Usuário não encontrado, embora estivesse logado. Contate o suporte.");
        }

        ObjectNode response = criarRespostaBase(true, "usuario_ler", "Dados do usuário recuperados com sucesso.");
        ObjectNode usuarioNode = mapper.createObjectNode();
        usuarioNode.put("cpf", usuario.getCpf());
        usuarioNode.put("nome", usuario.getNome());
        usuarioNode.put("saldo", usuario.getSaldo());
        response.set("usuario", usuarioNode);
        
//        if (onUpdateUser != null) {
//        	String clientIdentifier = clientSocket.getRemoteSocketAddress().toString().substring(1);
//        	onUpdateUser.accept(clientIdentifier, usuario.getNome());
//        }
        
        return mapper.writeValueAsString(response);
    }
    
    private String handleAtualizarUsuario(String cpf, JsonNode request) throws Exception {
        JsonNode dadosParaAtualizar = request.get("usuario");
        if (dadosParaAtualizar.has("senha")) {
        	String senhaNovaHash = PasswordUtil.hashPassword(dadosParaAtualizar.get("senha").asText());
        	((ObjectNode)dadosParaAtualizar).put("senha", senhaNovaHash);
        }
        boolean sucesso = usuarioDAO.atualizarUsuario(cpf, dadosParaAtualizar);
        if (!sucesso) {
            throw new Exception("Falha ao atualizar dados no banco.");
        }
        logger.accept("Usuário " + cpf + " atualizou seus dados.");
        return criarRespostaSucesso("usuario_atualizar", "Usuário atualizado com sucesso.");
    }
    
    private String handleDeletarUsuario(String cpf) throws Exception {
        Usuario usuario = usuarioDAO.buscarUsuarioPorCpf(cpf);
        if (usuario == null) {
            return criarRespostaErro("usuario_deletar", "Usuário não encontrado no banco de dados");
        }
        
        boolean sucesso = usuarioDAO.deletarUsuario(cpf);
        if (!sucesso) {
            throw new Exception("Falha ao deletar usuário do banco.");
        }
        logger.accept("Usuário " + cpf + " deletou sua conta.");
        return criarRespostaSucesso("usuario_deletar", "Usuário deletado com sucesso.");
    }

    
    private ObjectNode criarRespostaBase(boolean status, String operacao, String info) {
        ObjectNode response = mapper.createObjectNode();
        response.put("status", status);
        response.put("operacao", operacao);
        response.put("info", info);
        return response;
    }

    private String criarRespostaSucesso(String operacao, String info) throws IOException {
        return mapper.writeValueAsString(criarRespostaBase(true, operacao, info));
    }
    
    private String criarRespostaErro(String operacao, String info) {
        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("operacao", operacao != null ? operacao : "operacao_desconhecida");
            response.put("status", false);
            response.put("info", info);
            return mapper.writeValueAsString(response);
        } catch (IOException e) {
            return String.format("{\"status\":false,\"operacao\":\"%s\",\"info\":\"Erro crítico ao gerar a resposta JSON.\"}",
            		operacao != null ? operacao : "operacao_desconhecida");
        }
    }
    
    private String handleDepositar(String cpf, JsonNode request) throws Exception {
        // CORREÇÃO: Lê o valor diretamente como BigDecimal
        BigDecimal valor = request.get("valor_enviado").decimalValue();
        
        // A chamada ao serviço agora funciona, pois estamos passando o tipo correto
        transacaoService.realizarDeposito(cpf, valor);

        logger.accept(String.format("Depósito de R$%.2f realizado para o CPF %s", valor, cpf));
        return criarRespostaSucesso("depositar", "Depósito realizado com sucesso.");
    }

    private String handleCriarTransacao(String cpfRemetente, JsonNode request) throws Exception {
        String cpfDestino = request.get("cpf_destino").asText();
        BigDecimal valor = request.get("valor").decimalValue();

        transacaoService.realizarTransferencia(cpfRemetente, cpfDestino, valor);

        logger.accept(String.format("Transferência de R$%.2f de %s para %s realizada com sucesso.", valor, cpfRemetente, cpfDestino));
        return criarRespostaSucesso("transacao_criar", "Transação realizada com sucesso.");
    }

    private String handleLerTransacoes(String cpf, JsonNode request) throws Exception {
        String dataInicialStr = request.get("data_inicial").asText();
        String dataFinalStr = request.get("data_final").asText();

        ZonedDateTime dataInicial = ZonedDateTime.parse(dataInicialStr);
        ZonedDateTime dataFinal = ZonedDateTime.parse(dataFinalStr);
        if (ChronoUnit.DAYS.between(dataInicial, dataFinal) > 31) {
            throw new BusinessException("O período de consulta não pode exceder 31 dias.");
        }

        List<Transacao> transacoes = transacaoDAO.lerTransacoesPorCpfEData(cpf, dataInicialStr, dataFinalStr);
        
        ObjectNode response = criarRespostaBase(true, "transacao_ler", "Transações recuperadas com sucesso.");
        ArrayNode transacoesNode = mapper.createArrayNode();

        for (Transacao t : transacoes) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", t.getId());
            tNode.put("valor_enviado", t.getValor());
            
            Usuario remetente = usuarioDAO.buscarUsuarioPorCpf(t.getCpfRemetente());
            ObjectNode remetenteNode = mapper.createObjectNode();
            remetenteNode.put("nome", remetente != null ? remetente.getNome() : "Desconhecido");
            remetenteNode.put("cpf", t.getCpfRemetente());
            tNode.set("usuario_enviador", remetenteNode);
            
            Usuario recebedor = usuarioDAO.buscarUsuarioPorCpf(t.getCpfDestinatario());
            ObjectNode recebedorNode = mapper.createObjectNode();
            recebedorNode.put("nome", recebedor != null ? recebedor.getNome() : "Desconhecido");
            recebedorNode.put("cpf", t.getCpfDestinatario());
            tNode.set("usuario_recebedor", recebedorNode);

            // --- CORREÇÃO DA DATA ANTES DO ENVIO ---
            // 1. Pega a string do banco (que pode ter nanossegundos)
            // Assumindo que você renomeou o método get para getDataHoraTransacao()
            String timestampDoBanco = t.getDataHoraTransacao(); 
            
            System.out.println("Timestamp original do banco: " + timestampDoBanco); // Log para depuração
            // 2. Converte para um objeto Instant, que entende o formato completo
            Instant instant = Instant.parse(timestampDoBanco);
            System.out.println("Instant parseado: " + instant); // Log para depuração

            // 3. Trunca para segundos (remove a parte fracionária) e converte para string
            String timestampCorreto = instant.truncatedTo(ChronoUnit.SECONDS).toString();
            System.out.println("Timestamp corrigido: " + timestampCorreto); // Log para depuração
            
            // 4. Usa a string formatada corretamente no JSON de resposta
            tNode.put("criado_em", timestampCorreto);
            tNode.put("atualizado_em", timestampCorreto);
            // --- FIM DA CORREÇÃO ---
            
            transacoesNode.add(tNode);
        }
        
        response.set("transacoes", transacoesNode);
        return mapper.writeValueAsString(response);
    }
}