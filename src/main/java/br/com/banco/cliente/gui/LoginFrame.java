package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.ErrorHandlerUtil; // Importa
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

        // --- Menu ---
        JMenuBar menuBar = new JMenuBar();
        JMenu menuConexao = new JMenu("Opções de conexão");
        JMenuItem itemTrocarServidor = new JMenuItem("Sair do servidor atual");
        menuConexao.add(itemTrocarServidor);
        menuBar.add(menuConexao);
        setJMenuBar(menuBar);
        itemTrocarServidor.addActionListener(e -> {
            clienteService.desconectar();
            this.dispose();
            new ClienteGUI().setVisible(true);
        });

        // --- Layout Principal ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // --- Fontes ---
        Font fontTitulo = new Font("Georgia", Font.BOLD, 28);
        Font fontLabel = new Font("Georgia", Font.PLAIN, 14);
        Font fontInput = new Font("Arial", Font.PLAIN, 14);
        Font fontButton = new Font("Georgia", Font.BOLD, 14);

        // --- Título ---
        JLabel titleLabel = new JLabel("Bem-vindo ao Pix UTFPR");
        titleLabel.setFont(fontTitulo);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Formulário ---
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setMaximumSize(new Dimension(400, 150));
        JLabel cpfLabel = new JLabel("CPF (login/cadastro):");
        JLabel nomeLabel = new JLabel("Nome (só cadastro):");
        JLabel senhaLabel = new JLabel("Senha:");
        cpfLabel.setFont(fontLabel); nomeLabel.setFont(fontLabel); senhaLabel.setFont(fontLabel);
        cpfField = createCpfField(); nomeField = new JTextField(); senhaField = new JPasswordField();
        cpfField.setFont(fontInput); nomeField.setFont(fontInput); senhaField.setFont(fontInput);
        formPanel.add(cpfLabel); formPanel.add(cpfField);
        formPanel.add(nomeLabel); formPanel.add(nomeField);
        formPanel.add(senhaLabel); formPanel.add(senhaField);

        // --- Botões ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton loginButton = new JButton("Login");
        JButton cadastrarButton = new JButton("Cadastrar");
        loginButton.setFont(fontButton); cadastrarButton.setFont(fontButton);
        loginButton.setIcon(IconUtil.loadIcon("log-in.png", 16, 16));
        cadastrarButton.setIcon(IconUtil.loadIcon("user-plus.png", 16, 16));
        Dimension buttonSize = new Dimension(150, 35);
        loginButton.setPreferredSize(buttonSize); cadastrarButton.setPreferredSize(buttonSize);
        //loginButton.putClientProperty("JButton.buttonType", "default"); // Definido via setDefaultButton
        cadastrarButton.putClientProperty("JButton.buttonType", "default"); // Força cor azul
        buttonPanel.add(loginButton); buttonPanel.add(cadastrarButton);

        // --- Montagem ---
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);

        // --- Ações ---
        cadastrarButton.addActionListener(e -> handleCadastro());
        loginButton.addActionListener(e -> handleLogin());

        // --- Finalização ---
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton); // Define Login como botão padrão
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(500, 400));
        pack();
        setLocationRelativeTo(null);
    }

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
        String operacaoOriginal = "usuario_criar";

        // Validação simples de campos (pode ser melhorada)
        if (cpf.replace(".", "").replace("-", "").trim().isEmpty() || nome.trim().isEmpty() || senha.isEmpty()) {
             JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios para cadastro.", "Erro", JOptionPane.WARNING_MESSAGE);
             return;
        }

        try {
            JsonNode response = clienteService.criarUsuario(nome, cpf, senha);
            String info = response.path("info").asText("Operação concluída.");
            if (response.path("status").asBoolean(false)) {
                JOptionPane.showMessageDialog(this, info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Trata status:false como erro de operação
                ErrorHandlerUtil.handleError(this, new Exception(info), operacaoOriginal,
                                             clienteService, null, null);
            }
        } catch (Exception e) {
            // Delega qualquer exceção (rede, validação de requisição/resposta)
            ErrorHandlerUtil.handleError(this, e, operacaoOriginal,
                                         clienteService, null, null);
        }
    }

    private void handleLogin() {
        String cpf = cpfField.getText();
        String senha = new String(senhaField.getPassword());
        String operacaoOriginal = "usuario_login";

         if (cpf.replace(".", "").replace("-", "").trim().isEmpty() || senha.isEmpty()) {
             JOptionPane.showMessageDialog(this, "CPF e Senha são obrigatórios para login.", "Erro", JOptionPane.WARNING_MESSAGE);
             return;
        }

        try {
            JsonNode response = clienteService.login(cpf, senha);
            if (response.path("status").asBoolean(false)) {
                String token = response.path("token").asText();
                clienteService.iniciarSessao(token, cpf);
                SwingUtilities.invokeLater(() -> {
                    new DashboardCliente(token).setVisible(true);
                    this.dispose();
                });
            } else {
                String info = response.path("info").asText("Login falhou.");
                ErrorHandlerUtil.handleError(this, new Exception(info), operacaoOriginal,
                                             clienteService, null, null);
            }
        } catch(IllegalArgumentException iae){
            ErrorHandlerUtil.handleError(this, iae, operacaoOriginal,
                                         clienteService, null, null);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao comunicar com o servidor:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
