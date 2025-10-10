package br.com.banco.servidor.service;

import br.com.banco.servidor.gui.ConexoesPanel;
import br.com.banco.model.ConexaoInfo; // Importa o novo modelo

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Servidor implements Runnable {

    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final Consumer<String> logger;
    private final ConexoesPanel conexoesPanel;
    private final Map<String, ClientHandler> clientesConectados = new ConcurrentHashMap<>();

    public Servidor(int port, Consumer<String> logger, ConexoesPanel conexoesPanel) {
        this.port = port;
        this.logger = logger;
        this.conexoesPanel = conexoesPanel;
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        clientPool.shutdownNow();
        logger.accept("Servidor parado.");
    }

    @Override
    public void run() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            String serverIp = InetAddress.getLocalHost().getHostAddress();
            logger.accept(String.format("Servidor iniciado. Escutando em %s:%d", serverIp, port));
            logger.accept("Aguardando conexões de clientes...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    InetAddress inetAddress = clientSocket.getInetAddress();
                    String ip = inetAddress.getHostAddress();
                    String hostname = inetAddress.getHostName();
                    String porta = String.valueOf(clientSocket.getPort());
                    String id = ip + ":" + porta; 
                    
                    ConexaoInfo novaConexao = new ConexaoInfo(ip, porta, hostname, "Aguardando login...");
                    
                    Runnable onDisconnect = () -> {
                        clientesConectados.remove(id);
                        conexoesPanel.removerConexao(id);
                        logger.accept("Cliente desconectado: " + id);
                    };

                    BiConsumer<String, String> onLogin = (clientId, username) -> {
                        String cleanId = clientId.startsWith("/") ? clientId.substring(1) : clientId;
                        conexoesPanel.atualizarNomeUsuario(cleanId, username);
                    };

                    Consumer<String> onLogout = (clientId) -> {
                        conexoesPanel.atualizarNomeUsuario(clientId, "Aguardando login...");
                    };

                    ClientHandler clientHandler = new ClientHandler(clientSocket, logger, onDisconnect, onLogin, onLogout);
                    
                    clientesConectados.put(id, clientHandler);
                    conexoesPanel.adicionarConexao(id, novaConexao);
                    clientPool.submit(clientHandler);
                    
                    logger.accept("Novo cliente conectado: " + id + " | Total: " + clientesConectados.size());

                } catch (IOException e) {
                    if (!running) break;
                    logger.accept("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.accept("Erro fatal no servidor: " + e.getMessage());
        } finally {
            logger.accept("Thread do servidor encerrada.");
            running = false;
        }
    }
}