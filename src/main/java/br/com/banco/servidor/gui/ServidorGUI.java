package br.com.banco.servidor.gui;

import br.com.banco.servidor.service.Servidor;
import br.com.banco.servidor.dao.Database;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.InetAddress;
import java.util.function.Consumer;

public class ServidorGUI extends JFrame {

    private final JTextField portField;
    private final JButton startButton, stopButton;
    private final JLabel statusLabel, ipLabel;
    private final JTextArea logArea;
    private Thread serverThread;
    private Servidor servidor;
    private ConexoesPanel conexoesPanel;
    private UsuariosPanel usuariosPanel;
    private TransacoesPanel transacoesPanel;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());

            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            
            Color azulPrincipal = new Color(40, 75, 130);
            Color azulHover = new Color(55, 90, 145);

            UIManager.put("Component.focusColor", azulPrincipal);
            UIManager.put("Button.default.background", azulPrincipal);
            UIManager.put("Button.default.hoverBackground", azulHover);

            UIManager.put("Button.background", new Color(75, 75, 75));
            UIManager.put("Button.foreground", new Color(220, 220, 220));
            UIManager.put("Button.hoverBackground", new Color(90, 90, 90));
            
            UIManager.put("defaultFont", new Font("Dubai", Font.PLAIN, 14));


        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Falha ao carregar o Look and Feel FlatLaf.");
        }

        SwingUtilities.invokeLater(ServidorGUI::new);
    }
    
    public ServidorGUI() {
        super("Painel de Controle do Servidor PIX");
        Database.initializeDatabase(); 

        // --- PAINEL DE CONTROLE (NORTE) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        portField = new JTextField("20000", 5);
        startButton = new JButton("Iniciar Servidor");
        stopButton = new JButton("Parar Servidor");
        stopButton.setEnabled(false);
        
        // Estilo do botão de parar (vermelho)
        stopButton.putClientProperty("FlatLaf.style", "" +
            "background: rgb(170, 50, 50);" +
            "foreground: rgb(255, 255, 255);" +
            "hoverBackground: rgb(200, 70, 70);");

        statusLabel = new JLabel("Status: Parado");
        statusLabel.setForeground(Color.RED);
        ipLabel = new JLabel("IP: Carregando...");
        loadServerIp();
        
        controlPanel.add(new JLabel("Porta:"));
        controlPanel.add(portField);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);
        controlPanel.add(ipLabel);

        // --- PAINEL DE ABAS (CENTRO) ---
        JTabbedPane tabbedPane = new JTabbedPane();
        
        conexoesPanel = new ConexoesPanel();
        usuariosPanel = new UsuariosPanel();
        transacoesPanel = new TransacoesPanel();
        

        tabbedPane.putClientProperty("JTabbedPane.tabAlignment", "center");
        tabbedPane.putClientProperty("JTabbedPane.showTabSeparators", true);
        tabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(5, 15, 5, 15));
        tabbedPane.addTab("Conexões", conexoesPanel);
        tabbedPane.addTab("Usuários", usuariosPanel);
        tabbedPane.addTab("Transações", transacoesPanel);
        tabbedPane.addTab("Sobre", new JPanel());
        
        // --- PAINEL DE LOG (SUL) ---
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Fonte monoespaçada é melhor para logs
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Logs do Servidor"));
        
        // --- MONTAGEM DA JANELA ---
        JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.add(controlPanel, BorderLayout.NORTH);
        contentPane.add(tabbedPane, BorderLayout.CENTER);
        contentPane.add(logScrollPane, BorderLayout.SOUTH);

        // --- AÇÕES ---
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        // --- CONFIGURAÇÕES FINAIS ---
        getRootPane().setDefaultButton(startButton);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setVisible(true); 
    }
    
    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText());
            Consumer<String> logger = this::logMessage;
            servidor = new Servidor(port, logger, conexoesPanel); 
            serverThread = new Thread(servidor);
            serverThread.start();
            statusLabel.setText("Status: Rodando");
            statusLabel.setForeground(new Color(0, 200, 0));
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            portField.setEnabled(false);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopServer() {
        if (servidor != null) {
            try {
                servidor.stop();
                if (serverThread != null) {
                    serverThread.interrupt();
                }
            } catch (Exception ex) {
                logMessage("Erro ao parar servidor: " + ex.getMessage());
            }
        }
        statusLabel.setText("Status: Parado");
        statusLabel.setForeground(Color.RED);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        portField.setEnabled(true);
    }

    private void logMessage(String message) {
        final String logEntry = "[S] " + message;
        System.out.println(logEntry);
        SwingUtilities.invokeLater(() -> {
            logArea.append(logEntry + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void loadServerIp() {
        try {
            ipLabel.setText("IP: " + InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            ipLabel.setText("IP: Desconhecido");
        }
    }
}