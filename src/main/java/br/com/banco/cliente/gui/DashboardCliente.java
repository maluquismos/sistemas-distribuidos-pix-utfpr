package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.ErrorHandlerUtil;
import br.com.banco.cliente.util.IconUtil;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardCliente extends JFrame {

    private final ClienteService clienteService;
    // Dados do usuário (inicializados com valores padrão)
    private String nomeUsuario = "Usuário";
    private String cpfUsuario = "---.---.---.--";
    private double saldoUsuario = 0.0;

    // Componentes da UI
    private final JLabel saldoLabel;
    private final JLabel welcomeLabel;
    private final JLabel cpfLabel;
    private final JButton refreshButton;

    public DashboardCliente(String token) {
        super("Minha Conta");
        this.clienteService = ClienteService.getInstance();
        // Token é guardado no ClienteService, não precisamos mais dele aqui diretamente

        // Inicializa componentes com texto padrão/carregando
        saldoLabel = new JLabel("Saldo disponível: Carregando...");
        welcomeLabel = new JLabel("Olá, Carregando...");
        cpfLabel = new JLabel("CPF: Carregando...");
        refreshButton = new JButton("Atualizar Dados");
        refreshButton.setIcon(IconUtil.loadIcon("refresh-cw.png", 16, 16));

        // --- Constrói a UI ---
        // A UI é montada PRIMEIRO, independentemente da carga de dados
        setupUI();

        // --- Adiciona ação ao botão Refresh ---
        refreshButton.addActionListener(e -> recarregarDadosEAtualizarUI());

        // --- Configurações finais da janela ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(550, 600));
        setMinimumSize(new Dimension(500, 550));
        setLocationRelativeTo(null);
        // setVisible(true) é chamado pelo LoginFrame

        // --- TENTA A CARGA INICIAL DE DADOS (APÓS A UI ESTAR PRONTA) ---
        // Chama o método de refresh, que já tem a lógica de erro correta
        // Usa invokeLater para garantir que isso aconteça depois que a janela estiver visível
        SwingUtilities.invokeLater(this::recarregarDadosEAtualizarUI);
    }

    // Monta a interface gráfica
    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Painel de Informações (Norte)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomeLabel.setFont(new Font("Georgia", Font.BOLD, 24));
        cpfLabel.setFont(new Font("Georgia", Font.PLAIN, 16));
        saldoLabel.setFont(new Font("Georgia", Font.PLAIN, 18));
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(welcomeLabel);
        infoPanel.add(cpfLabel);
        infoPanel.add(saldoLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(refreshButton);

        // Menu Superior
        JMenuBar menuBar = new JMenuBar();
        JMenu menuSistema = new JMenu("Sistema");
        JMenuItem itemVoltar = new JMenuItem("Voltar para Conexão");
        JMenuItem itemSair = new JMenuItem("Sair da aplicação");
        menuSistema.add(itemVoltar);
        menuSistema.add(itemSair);
        menuBar.add(menuSistema);
        setJMenuBar(menuBar);

        // Painel de Ações (Centro)
        JPanel actionsPanel = new JPanel(new GridLayout(5, 1, 10, 15));
        actionsPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        JButton btnPix = new JButton("Realizar Transferência (PIX)");
        JButton btnExtrato = new JButton("Visualizar Extrato");
        JButton btnDepositar = new JButton("Realizar Depósito");
        JButton btnGerenciarConta = new JButton("Gerenciar Minha Conta");
        JButton btnLogout = new JButton("Logout (Sair da Conta)");
        Font buttonFont = new Font("Georgia", Font.BOLD, 14);
        Dimension buttonSize = new Dimension(200, 40);
        JButton[] buttons = {btnPix, btnExtrato, btnDepositar, btnGerenciarConta, btnLogout};
        String[] iconPaths = {"send.png", "file-text.png", "dollar-sign.png", "user.png", "log-out.png"};
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setFont(buttonFont);
            buttons[i].setIcon(IconUtil.loadIcon(iconPaths[i], 16, 16));
            buttons[i].setPreferredSize(buttonSize);
            if (i < buttons.length - 1) { buttons[i].putClientProperty("JButton.buttonType", "default"); }
            actionsPanel.add(buttons[i]);
        }

        // Montagem final
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(actionsPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        // Ações dos Componentes
        itemSair.addActionListener(e -> System.exit(0));
        itemVoltar.addActionListener(e -> voltarParaConexao());
        btnLogout.addActionListener(e -> handleLogout());
        btnGerenciarConta.addActionListener(e -> abrirGerenciadorConta());
        btnDepositar.addActionListener(e -> abrirDialogoDeposito());
        btnPix.addActionListener(e -> abrirDialogoPix());
        btnExtrato.addActionListener(e -> abrirDialogoExtrato());
    }

    // Atualiza as labels com os dados atuais
    private void atualizarLabelsUI() {
        welcomeLabel.setText("Olá, " + nomeUsuario + "!");
        cpfLabel.setText("CPF: " + cpfUsuario);
        atualizarLabelSaldo();
        this.revalidate();
        this.repaint();
    }

private void recarregarDadosEAtualizarUI() {
        refreshButton.setEnabled(false);
        refreshButton.setText("Atualizando...");

        new SwingWorker<Boolean, Void>() {
            Exception error = null;

            @Override protected Boolean doInBackground() {
                try {
                    // Tenta carregar os dados (pode lançar Exception)
                    carregarDadosUsuario();
                    return true;
                } catch (Exception e) {
                    error = e; // Guarda a exceção
                    return false;
                }
            }

            @Override protected void done() {
                try {
                    // Verifica se doInBackground() retornou true (sucesso)
                    if (Boolean.TRUE.equals(get())) {
                        atualizarLabelsUI(); // Atualiza a UI com os novos dados
                    } else {
                        // Se falhou, chama o ErrorHandlerUtil
                        ErrorHandlerUtil.handleError(DashboardCliente.this, error, "usuario_ler",
                                                     clienteService, DashboardCliente.this::voltarParaConexao, DashboardCliente.this::voltarParaLogin);
                        // NÃO fecha a janela aqui. ErrorHandler decide.
                    }
                } catch (Exception e) {
                    // Captura erro do próprio get() (menos provável, mas por segurança)
                    ErrorHandlerUtil.handleError(DashboardCliente.this, e, "usuario_ler",
                                                 clienteService, DashboardCliente.this::voltarParaConexao, DashboardCliente.this::voltarParaLogin);
                } finally {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Atualizar Dados");
                }
            }
        }.execute();
    }

    // Carrega os dados do usuário do servidor, lança Exception em QUALQUER falha
    private JsonNode carregarDadosUsuario() throws Exception {
        JsonNode response = clienteService.getDadosUsuario();
        if (response.get("status").asBoolean()) {
            JsonNode userNode = response.get("usuario");
            if (userNode == null || userNode.isNull()) {
                 throw new IllegalArgumentException("Dados do usuário ('usuario') recebidos do servidor estão nulos.");
            }
            // Atualiza os atributos da classe com os dados recebidos
            this.nomeUsuario = userNode.path("nome").asText("Nome Indisponível");
            this.cpfUsuario = userNode.path("cpf").asText("CPF Indisponível");
            this.saldoUsuario = userNode.path("saldo").asDouble(0.0);
            return userNode;
        } else {
            throw new Exception(response.path("info").asText("Erro desconhecido do servidor."));
        }
    }

    // Atualiza a label de saldo
    private void atualizarLabelSaldo() {
        saldoLabel.setText(String.format("Saldo disponível: R$ %.2f", this.saldoUsuario));
    }

    // Desconecta e volta para Conexão
    private void voltarParaConexao() {
        clienteService.desconectar();
        this.dispose();
        new ClienteGUI().setVisible(true);
    }

    // Volta para Login
    private void voltarParaLogin() {
         clienteService.encerrarSessao();
         this.dispose();
         new LoginFrame().setVisible(true);
    }

    // Trata a ação de logout
    private void handleLogout() {
        try {
            JsonNode response = clienteService.logout();
            if (response.get("status").asBoolean()) {
                voltarParaLogin(); // Só volta se o servidor confirmar
            } else {
                ErrorHandlerUtil.handleError(this, new Exception(response.path("info").asText("Falha no logout.")), "usuario_logout",
                                             clienteService, this::voltarParaConexao, this::voltarParaLogin);
            }
        } catch (Exception e) {
             ErrorHandlerUtil.handleError(this, e, "usuario_logout",
                                          clienteService, this::voltarParaConexao, this::voltarParaLogin);
        }
    }

    // Métodos para abrir os diálogos
    private void abrirDialogoDeposito() { DepositoDialog dialog = new DepositoDialog(this, this::recarregarDadosEAtualizarUI); dialog.setVisible(true); }
    private void abrirDialogoPix() { PixDialog dialog = new PixDialog(this, this::recarregarDadosEAtualizarUI); dialog.setVisible(true); }
    private void abrirGerenciadorConta() { GerenciarContaDialog dialog = new GerenciarContaDialog(this, this::handleLogout, this::recarregarDadosEAtualizarUI); dialog.setVisible(true); }
    private void abrirDialogoExtrato() { ExtratoDialog dialog = new ExtratoDialog(this); dialog.setVisible(true); }
}