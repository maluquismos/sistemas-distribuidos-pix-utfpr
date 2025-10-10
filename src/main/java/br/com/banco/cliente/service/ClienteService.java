package br.com.banco.cliente.service;

import br.com.banco.cliente.gui.LogFrame;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import br.com.banco.validator.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ClienteService {

    private static ClienteService instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private LogFrame logFrame;
    private final ObjectMapper mapper = new ObjectMapper();

    private String tokenSessao;
    private String cpfLogado;

    private ClienteService() {}

    public static synchronized ClienteService getInstance() {
        if (instance == null) instance = new ClienteService();
        return instance;
    }


    public void setLogFrame(LogFrame logFrame) { this.logFrame = logFrame; }
    
    public void conectar(String host, int port) throws Exception {
        if (isConectado()) desconectar();
        log("Tentando conectar em " + host + ":" + port);
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        ObjectNode request = mapper.createObjectNode().put("operacao", "conectar");
        JsonNode response = enviarMensagem(mapper.writeValueAsString(request));
        if (!response.get("status").asBoolean()) {
            throw new IOException("Falha no handshake com o servidor: " + response.get("info").asText());
        }
        log("Conectado com sucesso ao servidor em " + host + ":" + port);
    }

    public void desconectar() {
        encerrarSessao();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                log("Conexão com o servidor fechada.");
            }
        } catch (IOException e) { log("Erro ao desconectar: " + e.getMessage()); }
        finally { socket = null; out = null; in = null; }
    }
    
    public void iniciarSessao(String token, String cpf) {
        this.tokenSessao = token;
        this.cpfLogado = cpf;
        log("Sessão local iniciada para o CPF: " + cpf);
    }

    public void encerrarSessao() {
        this.tokenSessao = null;
        this.cpfLogado = null;
        log("Sessão local encerrada.");
    }
    
    public String getToken() { return this.tokenSessao; }
    public boolean isConectado() { return socket != null && !socket.isClosed(); }
    
    private void log(String message) {
        System.out.println("[C] " + message);
        if (logFrame != null) logFrame.addLog("[C] " + message);
    }
    
    private JsonNode enviarMensagem(String jsonRequest) throws Exception {
        if (!isConectado()) throw new IOException("Não conectado ao servidor.");
        
        Validator.validateClient(jsonRequest);

        log("Enviando: " + jsonRequest);
        out.println(jsonRequest);
        
        String jsonResponse = in.readLine();
        if (jsonResponse == null) {
            desconectar();
            throw new IOException("O servidor encerrou a conexão inesperadamente.");
        }

        log("Recebido: " + jsonResponse);
        Validator.validateServer(jsonResponse); // Valida ao receber
        return mapper.readTree(jsonResponse);
    }

    // --- API DE OPERAÇÕES DE NEGÓCIO ---

    public JsonNode login(String cpf, String senha) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_login");
        request.put("cpf", cpf);
        request.put("senha", senha);
        return enviarMensagem(mapper.writeValueAsString(request));
    }
    
    public JsonNode criarUsuario(String nome, String cpf, String senha) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_criar");
        request.put("nome", nome);
        request.put("cpf", cpf);
        request.put("senha", senha);
        return enviarMensagem(mapper.writeValueAsString(request));
    }

    public JsonNode logout() throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_logout");
        request.put("token", this.tokenSessao);
        return enviarMensagem(mapper.writeValueAsString(request));
    }

    public JsonNode getDadosUsuario() throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_ler");
        request.put("token", this.tokenSessao);
        return enviarMensagem(mapper.writeValueAsString(request));
    }

    public JsonNode depositar(BigDecimal valor) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "depositar");
        request.put("token", this.tokenSessao);
        request.put("valor_enviado", valor);
        return enviarMensagem(mapper.writeValueAsString(request));
    }
    
    public JsonNode realizarTransferencia(String cpfDestino, BigDecimal valor) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "transacao_criar");
        request.put("token", this.tokenSessao);
        request.put("cpf_destino", cpfDestino);
        request.put("valor", valor);
        return enviarMensagem(mapper.writeValueAsString(request));
    }
    
    public JsonNode lerTransacoes(Date dataInicial, Date dataFinal) throws Exception {
        if (dataInicial.after(dataFinal)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        long dias = ChronoUnit.DAYS.between(dataInicial.toInstant(), dataFinal.toInstant());
        if (dias > 31) {
            throw new IllegalArgumentException("O período de consulta não pode exceder 31 dias.");
        }
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_INSTANT;
        String dataInicialStr = isoFormatter.format(dataInicial.toInstant());
        String dataFinalStr = isoFormatter.format(dataFinal.toInstant());

        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "transacao_ler");
        request.put("token", this.tokenSessao);
        request.put("data_inicial", dataInicialStr);
        request.put("data_final", dataFinalStr);
        
        return enviarMensagem(mapper.writeValueAsString(request));
    }
    
    public JsonNode atualizarUsuario(String novoNome, String novaSenha) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_atualizar");
        request.put("token", this.tokenSessao);
        
        ObjectNode userNode = mapper.createObjectNode();
        boolean hasUpdate = false;
        if (novoNome != null && !novoNome.trim().isEmpty()) {
            userNode.put("nome", novoNome);
            hasUpdate = true;
        }
        if (novaSenha != null && !novaSenha.trim().isEmpty()) {
            userNode.put("senha", novaSenha);
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new IllegalArgumentException("Nenhum dado fornecido para atualização.");
        }

        request.set("usuario", userNode);
        return enviarMensagem(mapper.writeValueAsString(request));
    }

    public JsonNode deletarConta() throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_deletar");
        request.put("token", this.tokenSessao);
        return enviarMensagem(mapper.writeValueAsString(request));
    }
    
    public String getCpfLogado() {
        return this.cpfLogado;
    }
}