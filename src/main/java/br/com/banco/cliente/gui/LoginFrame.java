package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.IconUtil;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;

public class LoginFrame extends JFrame {

    private final JFormattedTextField cpfField;
    private final JTextField nomeField;
    private final JPasswordField senhaField;
    private final ClienteService clienteService;

    public LoginFrame() {
        super("Acesso ao Sistema");
        this.clienteService = ClienteService.getInstance();
        
        JMenuBar menuBar = new JMenuBar();
        JMenu menuConexao = new JMenu("Opções de conexão");
        JMenuItem itemTrocarServidor = new JMenuItem("Sair do servidor atual");
        menuConexao.add(itemTrocarServidor);
        menuBar.add(menuConexao);
        setJMenuBar(menuBar);
        
        itemTrocarServidor.addActionListener(e -> {
            clienteService.desconectar();
            this.dispose(); // Fecha a janela de login
            new ClienteGUI().setVisible(true); // Abre a de conexão
        });
        
        // --- PAINEL PRINCIPAL ---
        // Usa BoxLayout para empilhar os painéis filhos verticalmente
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 40, 20, 40)); // Margens generosas

        // --- FONTES ---
        Font fontTitulo = new Font("Georgia", Font.BOLD, 28);
        Font fontLabel = new Font("Georgia", Font.PLAIN, 14);
        Font fontInput = new Font("Arial", Font.PLAIN, 14);
        Font fontButton = new Font("Georgia", Font.BOLD, 14);

        // --- PAINEL DO TÍTULO ---
        JLabel titleLabel = new JLabel("Bem-vindo ao Pix UTFPR");
        titleLabel.setFont(fontTitulo);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Centraliza o título

        // --- PAINEL DO FORMULÁRIO (com GridLayout) ---
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10)); // 3 linhas, 2 colunas, com espaçamento
        formPanel.setMaximumSize(new Dimension(400, 150)); // Define um tamanho máximo para não esticar demais

        // Labels
        JLabel cpfLabel = new JLabel("CPF (login/cadastro):");
        JLabel nomeLabel = new JLabel("Nome (só cadastro):");
        JLabel senhaLabel = new JLabel("Senha:");
        cpfLabel.setFont(fontLabel);
        nomeLabel.setFont(fontLabel);
        senhaLabel.setFont(fontLabel);

        // Campos de Input
        cpfField = createCpfField();
        nomeField = new JTextField();
        senhaField = new JPasswordField();
        cpfField.setFont(fontInput);
        nomeField.setFont(fontInput);
        senhaField.setFont(fontInput);
        
        // Adiciona os componentes ao painel do formulário
        formPanel.add(cpfLabel);
        formPanel.add(cpfField);
        formPanel.add(nomeLabel);
        formPanel.add(nomeField);
        formPanel.add(senhaLabel);
        formPanel.add(senhaField);

        // --- PAINEL DOS BOTÕES ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton loginButton = new JButton("Login");
        JButton cadastrarButton = new JButton("Cadastrar");
        loginButton.setFont(fontButton);
        cadastrarButton.setFont(fontButton);
        loginButton.setIcon(IconUtil.loadIcon("log-in.png", 16, 16));
        cadastrarButton.setIcon(IconUtil.loadIcon("user-plus.png", 16, 16));
        Dimension buttonSize = new Dimension(150, 35);
        loginButton.setPreferredSize(buttonSize);
        cadastrarButton.setPreferredSize(buttonSize);
        loginButton.putClientProperty("JButton.buttonType", "default");
        cadastrarButton.putClientProperty("JButton.buttonType", "default");

        buttonPanel.add(loginButton);
        buttonPanel.add(cadastrarButton);
        
        // --- MONTAGEM DO PAINEL PRINCIPAL ---
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(30)); // Espaço vertical rígido
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);

        // --- AÇÕES DOS BOTÕES ---
        cadastrarButton.addActionListener(e -> handleCadastro());
        loginButton.addActionListener(e -> handleLogin());

        // --- CONFIGURAÇÕES FINAIS ---
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton);

        // 2. Configurações restantes
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(500, 400));
        pack();
        
        // 3. Centraliza
        setLocationRelativeTo(null);
    }
    
    // Método auxiliar para criar o campo de CPF
    private JFormattedTextField createCpfField() {
        try {
            MaskFormatter cpfFormatter = new MaskFormatter("###.###.###-##");
            return new JFormattedTextField(cpfFormatter);
        } catch (ParseException e) {
            return new JFormattedTextField();
        }
    }
    
    private void handleCadastro() {
        String cpf = cpfField.getText();
        String nome = nomeField.getText();
        String senha = new String(senhaField.getPassword());
        if (cpf.isBlank()) {
        	JOptionPane.showMessageDialog(this, "CPF é obrigatório", "Erro", JOptionPane.INFORMATION_MESSAGE);
        }
        try {
            JsonNode response = clienteService.criarUsuario(nome, cpf, senha);
            String info = response.get("info").asText();
            if (response.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this, info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, info, "Erro no Cadastro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage(), "Erro de Comunicação", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogin() {
        String cpf = cpfField.getText();
        String senha = new String(senhaField.getPassword());
        
        try {
            JsonNode response = clienteService.login(cpf, senha);
            if (response.get("status").asBoolean()) {
                String token = response.get("token").asText();
                clienteService.iniciarSessao(token, cpf);
                
                new DashboardCliente(token).setVisible(true);
                this.dispose();
            } else {
                String info = response.get("info").asText();
                JOptionPane.showMessageDialog(this, info, "Erro no Login", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage(), "Erro de Comunicação", JOptionPane.ERROR_MESSAGE);
        }
    }
}