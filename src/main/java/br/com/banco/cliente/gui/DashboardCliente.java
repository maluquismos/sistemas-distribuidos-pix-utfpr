package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.IconUtil;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardCliente extends JFrame {

    private final ClienteService clienteService;
    private String nomeUsuario;
    private String cpfUsuario;
    private double saldoUsuario;

    private final JLabel saldoLabel;
    private final JLabel welcomeLabel;

    public DashboardCliente(String token) {
        super("Minha Conta");
        this.clienteService = ClienteService.getInstance();
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        
        saldoLabel = new JLabel();
        welcomeLabel = new JLabel();
        
        if (!carregarDadosUsuario()) {
            JOptionPane.showMessageDialog(null, "Não foi possível carregar os dados do usuário.", "Erro", JOptionPane.ERROR_MESSAGE);
            voltarParaConexao();
            return;
        }

        welcomeLabel.setText("Olá, " + nomeUsuario + "!");
        welcomeLabel.setFont(new Font("Georgia", Font.BOLD, 24));
        saldoLabel.setFont(new Font("Georgia", Font.PLAIN, 18));
        atualizarLabelSaldo();
        infoPanel.add(welcomeLabel);
        infoPanel.add(saldoLabel);

        JMenuBar menuBar = new JMenuBar();
        JMenu menuSistema = new JMenu("Sistema");
        JMenuItem itemVoltar = new JMenuItem("Voltar para Conexão");
        JMenuItem itemSair = new JMenuItem("Sair da aplicação");
        menuSistema.add(itemVoltar);
        menuSistema.add(itemSair);
        menuBar.add(menuSistema);
        setJMenuBar(menuBar);

        // --- PAINEL DE AÇÕES (VOLTANDO AO GRIDLAYOUT) ---
        JPanel actionsPanel = new JPanel(new GridLayout(5, 1, 10, 15));
        actionsPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        
        JButton btnPix = new JButton("Realizar Transferência (PIX)");
        JButton btnExtrato = new JButton("Visualizar Extrato");
        JButton btnDepositar = new JButton("Realizar Depósito");
        JButton btnGerenciarConta = new JButton("Gerenciar Minha Conta");
        JButton btnLogout = new JButton("Logout (Sair da Conta)");

        // Configuração dos botões
        Font buttonFont = new Font("Georgia", Font.BOLD, 14);
        Dimension buttonSize = new Dimension(200, 40);
        JButton[] buttons = {btnPix, btnExtrato, btnDepositar, btnGerenciarConta, btnLogout};
        String[] iconPaths = {"send.png", "file-text.png", "dollar-sign.png", "user.png", "log-out.png"};

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setFont(buttonFont);
            buttons[i].setIcon(IconUtil.loadIcon(iconPaths[i], 16, 16));
            buttons[i].setPreferredSize(buttonSize);
            if (i < buttons.length - 1) { // Mantém o botão de logout cinza
                buttons[i].putClientProperty("JButton.buttonType", "default");
            }
            actionsPanel.add(buttons[i]);
        }
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(actionsPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        // --- AÇÕES ---
        itemSair.addActionListener(e -> System.exit(0));
        itemVoltar.addActionListener(e -> voltarParaConexao());
        btnLogout.addActionListener(e -> handleLogout());
        btnGerenciarConta.addActionListener(e -> abrirGerenciadorConta());
        btnDepositar.addActionListener(e -> abrirDialogoDeposito());
        btnPix.addActionListener(e -> abrirDialogoPix());
        btnExtrato.addActionListener(e -> abrirDialogoExtrato()); // Adiciona a nova ação

        // --- CONFIGURAÇÕES FINAIS ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(550, 600));
        setMinimumSize(new Dimension(500, 550));
        setLocationRelativeTo(null);
    }
    
    // Abre o diálogo de extrato
    private void abrirDialogoExtrato() {
        ExtratoDialog dialog = new ExtratoDialog(this);
        dialog.setVisible(true);
    }
    
    // Carrega os dados do usuário logado via ClienteService
    private boolean carregarDadosUsuario() {
        try {
            JsonNode response = clienteService.getDadosUsuario();
            if (response.get("status").asBoolean()) {
                JsonNode userNode = response.get("usuario");
                this.nomeUsuario = userNode.get("nome").asText();
                this.cpfUsuario = userNode.get("cpf").asText();
                this.saldoUsuario = userNode.get("saldo").asDouble();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Atualiza o texto da label de saldo
    private void atualizarLabelSaldo() {
        saldoLabel.setText(String.format("Saldo disponível: R$ %.2f", this.saldoUsuario));
    }

    // Desconecta e volta para a tela de conexão
    private void voltarParaConexao() {
        clienteService.desconectar();
        this.dispose();
        new ClienteGUI().setVisible(true);
    }
    
    // Realiza o logout e volta para a tela de login
    private void handleLogout() {
        try {
            clienteService.logout();
        } catch (Exception e) {
            // Mesmo com erro, o logout local prossegue
        }
        clienteService.encerrarSessao();
        this.dispose();
        new LoginFrame().setVisible(true);
    }
    
    // Recarrega os dados do usuário e atualiza a UI (usado como callback)
    private void recarregarDadosEAtualizarUI() {
        if (carregarDadosUsuario()) {
            welcomeLabel.setText("Olá, " + nomeUsuario + "!");
            atualizarLabelSaldo();
            this.revalidate();
            this.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Sua sessão pode ter expirado. Por favor, faça login novamente.", "Erro", JOptionPane.ERROR_MESSAGE);
            voltarParaConexao();
        }
    }
    
    // Abre os diálogos para as ações financeiras e de gerenciamento
    private void abrirDialogoDeposito() {
        DepositoDialog dialog = new DepositoDialog(this, this::recarregarDadosEAtualizarUI);
        dialog.setVisible(true);
    }
    
    private void abrirDialogoPix() {
        PixDialog dialog = new PixDialog(this, this::recarregarDadosEAtualizarUI);
        dialog.setVisible(true);
    }
    
    private void abrirGerenciadorConta() {
        GerenciarContaDialog dialog = new GerenciarContaDialog(this, this::handleLogout, this::recarregarDadosEAtualizarUI);
        dialog.setVisible(true);
    }
}