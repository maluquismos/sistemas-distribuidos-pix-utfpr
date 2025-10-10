package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.IconUtil;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GerenciarContaDialog extends JDialog {

    private final JTextField nomeField;
    private final JPasswordField senhaField;
    private final ClienteService clienteService;
    private final Runnable onAccountDeleted;
    private final Runnable onDataUpdated;

    public GerenciarContaDialog(Frame owner, Runnable onAccountDeleted, Runnable onDataUpdated) {
        super(owner, "Gerenciar Minha Conta", true);
        this.clienteService = ClienteService.getInstance();
        this.onAccountDeleted = onAccountDeleted;
        this.onDataUpdated = onDataUpdated;

        JPanel mainPanel = new JPanel(new BorderLayout(50, 40));
        mainPanel.setBorder(new EmptyBorder(40, 30, 40, 30));
        
        // --- MUDANÇA: Substituindo GridLayout por GridBagLayout ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Alterar Dados (preencha apenas o que deseja mudar)"),
            new EmptyBorder(30, 25, 30, 25)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        JLabel nomeLabel = new JLabel("Novo Nome:");
        nomeLabel.setFont(new Font("Georgia", Font.PLAIN, 14));
        formPanel.add(nomeLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        nomeField = new JTextField();
        nomeField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(nomeField, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel senhaLabel = new JLabel("Nova Senha:");
        senhaLabel.setFont(new Font("Georgia", Font.PLAIN, 14));
        formPanel.add(senhaLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        senhaField = new JPasswordField();
        senhaField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(senhaField, gbc);
        
        // --- O RESTO DA JANELA PERMANECE IGUAL ---
        JButton salvarButton = new JButton("Salvar Alterações");
        salvarButton.setFont(new Font("Georgia", Font.BOLD, 13));
        salvarButton.setIcon(IconUtil.loadIcon("save.png", 16, 16));
        getRootPane().setDefaultButton(salvarButton);
        
        JButton voltarButton = new JButton("Voltar");
        voltarButton.setFont(new Font("Georgia", Font.BOLD, 13));
        voltarButton.setIcon(IconUtil.loadIcon("arrow-left.png", 16, 16));
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        salvarButton.setPreferredSize(new Dimension(180, 45));
        voltarButton.setPreferredSize(new Dimension(120, 45));
        actionPanel.add(salvarButton);
        actionPanel.add(voltarButton);
        
        JPanel dangerZonePanel = new JPanel(new BorderLayout(10,10));
        dangerZonePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Zona de Perigo"),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JButton excluirButton = new JButton("Excluir minha Conta Permanentemente");
        excluirButton.putClientProperty("FlatLaf.style", "" +
                "background: rgb(200, 50, 50);" +
                "foreground: rgb(255, 255, 255);" +
                "hoverBackground: rgb(230, 70, 70);" +
                "pressedBackground: rgb(180, 40, 40);");
        excluirButton.setIcon(IconUtil.loadIcon("trash-2.png", 16, 16));
        dangerZonePanel.add(excluirButton, BorderLayout.CENTER);
        
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(actionPanel, BorderLayout.CENTER);
        mainPanel.add(dangerZonePanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);

        // --- AÇÕES ---
        salvarButton.addActionListener(e -> handleUpdate());
        excluirButton.addActionListener(e -> handleDelete());
        voltarButton.addActionListener(e -> this.dispose());
    }

    private void handleUpdate() {
        String novoNome = nomeField.getText();
        String novaSenha = new String(senhaField.getPassword());

        try {
            // A GUI apenas chama o método de serviço
            JsonNode response = clienteService.atualizarUsuario(novoNome, novaSenha);
            String info = response.get("info").asText();

            if (response.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this, info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                onDataUpdated.run(); // Executa o callback para atualizar o dashboard
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, info, "Erro na Atualização", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException e) { // Captura o erro local se nenhum campo for preenchido
            JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro de comunicação: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "ATENÇÃO!\nEsta ação é irreversível e todos os seus dados serão apagados.\n\nDeseja continuar com a exclusão da sua conta?",
            "Confirmação de Exclusão de Conta",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            try {
                JsonNode response = clienteService.deletarConta();
                String info = response.get("info").asText();

                if (response.get("status").asBoolean()) {
                    JOptionPane.showMessageDialog(this, info, "Conta Excluída", JOptionPane.INFORMATION_MESSAGE);
                    this.dispose();
                    onAccountDeleted.run();
                } else {
                    JOptionPane.showMessageDialog(this, info, "Erro ao Excluir", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de comunicação: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}