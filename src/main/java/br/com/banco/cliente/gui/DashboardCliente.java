package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.IconUtil;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class DashboardCliente extends JFrame {

    private final ClienteService clienteService;
    // Dados do usuário (podem começar nulos/padrão se a carga inicial falhar)
    private String nomeUsuario = "Usuário";
    private String cpfUsuario = "---.---.---.--";
    private double saldoUsuario = 0.0;

    // Componentes da UI que precisam ser atualizados
    private final JLabel saldoLabel;
    private final JLabel welcomeLabel;
    private final JLabel cpfLabel;
    private final JButton refreshButton;

    public DashboardCliente(String token) {
        super("Minha Conta");
        this.clienteService = ClienteService.getInstance();

        saldoLabel = new JLabel("Saldo disponível: Carregando...");
        welcomeLabel = new JLabel("Olá, Carregando...");
        cpfLabel = new JLabel("CPF: Carregando...");
        refreshButton = new JButton("Atualizar Dados");
        refreshButton.setIcon(IconUtil.loadIcon("refresh-cw.png", 16, 16));

        try {
            carregarDadosUsuario();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro crítico de comunicação ao carregar dados iniciais:\n" + e.getMessage() + "\nVoltando para a tela de conexão.", "Erro Crítico", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                voltarParaConexao();
                this.dispose();
            });
            return;
        } catch (Exception e) {
            final String errorMessage = e.getMessage();
             SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Não foi possível carregar os dados iniciais:\n" + errorMessage + "\nUse o botão 'Atualizar Dados'.", "Aviso", JOptionPane.WARNING_MESSAGE);
             });
        }
        setupUI();
        atualizarLabelsUI();

        refreshButton.addActionListener(e -> recarregarDadosEAtualizarUI());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(550, 600));
        setMinimumSize(new Dimension(500, 550));
        setLocationRelativeTo(null);
    }

    // Método que monta a interface gráfica
    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

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

        JMenuBar menuBar = new JMenuBar();
        JMenu menuSistema = new JMenu("Sistema");
        JMenuItem itemVoltar = new JMenuItem("Voltar para Conexão");
        JMenuItem itemSair = new JMenuItem("Sair da aplicação");
        menuSistema.add(itemVoltar);
        menuSistema.add(itemSair);
        menuBar.add(menuSistema);
        setJMenuBar(menuBar);

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
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(actionsPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

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

    // Recarrega os dados do usuário (chamado pelo botão Refresh)
    private void recarregarDadosEAtualizarUI() {
        refreshButton.setEnabled(false);
        refreshButton.setText("Atualizando...");

        new SwingWorker<Boolean, Void>() {
            Exception error = null;

            @Override protected Boolean doInBackground() {
                try { carregarDadosUsuario(); return true; }
                catch (Exception e) { error = e; return false; }
            }

            @Override protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) { // Sucesso
                        atualizarLabelsUI();
                    } else { // Falha
                        handleUpdateError(error); // Chama método auxiliar para tratar o erro
                    }
                } catch (Exception e) { // Captura erro do próprio get()
                    handleUpdateError(e);
                } finally {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Atualizar Dados");
                }
            }
        }.execute();
    }
    
    // Método auxiliar para tratar erros ocorridos durante a atualização (refresh)
    private void handleUpdateError(Exception error) {
        if (error instanceof IOException) {
            JOptionPane.showMessageDialog(this, "Erro de comunicação ao atualizar:\n" + error.getMessage() + "\nVoltando para a tela de conexão.", "Erro Crítico", JOptionPane.ERROR_MESSAGE);
            voltarParaConexao();
        } else if (error instanceof IllegalArgumentException || error.getMessage().contains("deve estar no formato")) { // Erro de validação da resposta
             JOptionPane.showMessageDialog(this, "O servidor enviou uma resposta inválida:\n" + error.getMessage(), "Erro de Protocolo", JOptionPane.ERROR_MESSAGE);
             clienteService.reportarErroServidor("usuario_ler", error.getMessage());
             // NÃO desloga nem fecha a janela
        } else { // Outros erros (geralmente status:false do servidor)
             JOptionPane.showMessageDialog(this, "Não foi possível atualizar os dados:\n" + error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
             // NÃO desloga nem fecha a janela
             // Aqui você pode adicionar lógica: se error.getMessage().contains("Token inválido"), chamar voltarParaLogin();
        }
    }

    // Carrega os dados do usuário do servidor
    private JsonNode carregarDadosUsuario() throws Exception {
        JsonNode response = clienteService.getDadosUsuario();
        if (response.get("status").asBoolean()) {
            JsonNode userNode = response.get("usuario");
            this.nomeUsuario = userNode.get("nome").asText();
            this.cpfUsuario = userNode.get("cpf").asText();
            this.saldoUsuario = userNode.hasNonNull("saldo") ? userNode.get("saldo").asDouble() : 0.0;
            return userNode;
        } else {
            throw new Exception(response.get("info").asText());
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
    
    // Volta para Login (chamado em caso de erro na carga inicial não-rede)
    private static void voltarParaLoginEstatico() {
        ClienteService.getInstance().encerrarSessao();
        new LoginFrame().setVisible(true);
    }

    // Volta para Login (chamado pelo botão de logout)
    private void voltarParaLogin() {
         clienteService.encerrarSessao();
         this.dispose();
         new LoginFrame().setVisible(true);
    }


    // Realiza o logout
    private void handleLogout() {
        try {
            JsonNode response = clienteService.logout();
            if (response.get("status").asBoolean()) {
                voltarParaLogin(); // Só volta se o servidor confirmar
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao fazer logout no servidor:\n" + response.get("info").asText(), "Erro de Logout", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) { // Trata erros de rede, validação da resposta, etc.
             handleUpdateError(e); // Reutiliza o tratamento de erro do refresh
        }
    }

    // Abre os diálogos
    private void abrirDialogoDeposito() { DepositoDialog dialog = new DepositoDialog(this, this::recarregarDadosEAtualizarUI); dialog.setVisible(true); }
    private void abrirDialogoPix() { PixDialog dialog = new PixDialog(this, this::recarregarDadosEAtualizarUI); dialog.setVisible(true); }
    private void abrirGerenciadorConta() { GerenciarContaDialog dialog = new GerenciarContaDialog(this, this::handleLogout, this::recarregarDadosEAtualizarUI); dialog.setVisible(true); }
    private void abrirDialogoExtrato() { ExtratoDialog dialog = new ExtratoDialog(this); dialog.setVisible(true); }
}