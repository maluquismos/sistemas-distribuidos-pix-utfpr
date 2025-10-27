package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.IconUtil;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.*;

public class ClienteGUI extends JFrame {

    private final JTextField hostField;
    private final JTextField portField;
    private final JButton conectarButton;
    private final ClienteService clienteService;

    public ClienteGUI() {
        super("Conectar ao Servidor");
        this.clienteService = ClienteService.getInstance();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Host do Servidor:"), gbc);
        gbc.gridx = 1; hostField = new JTextField("localhost", 15); add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Porta:"), gbc);
        gbc.gridx = 1; portField = new JTextField("20000", 15); add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        conectarButton = new JButton("Conectar");
        conectarButton.setFont(new Font("Georgia", Font.BOLD, 14));
        conectarButton.setIcon(IconUtil.loadIcon("log-in.png", 16, 16));
        add(conectarButton, gbc);

        conectarButton.addActionListener(e -> conectarAoServidor());

        getRootPane().setDefaultButton(conectarButton);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void conectarAoServidor() {
        String host = hostField.getText();
        String portStr = portField.getText();
        try {
            int port = Integer.parseInt(portStr);
            clienteService.conectar(host, port);
            new LoginFrame().setVisible(true);
            this.dispose();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "A porta deve ser um número válido.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor:\n" + ex.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
            // --- TEMA ---
            UIManager.put("Component.arc", 10); UIManager.put("Button.arc", 10); UIManager.put("TextComponent.arc", 10);
            Color azulPrincipal = new Color(40, 75, 130); Color azulHover = new Color(55, 90, 145);
            UIManager.put("Component.focusColor", azulPrincipal); UIManager.put("ProgressBar.foreground", azulPrincipal);
            UIManager.put("Button.default.background", azulPrincipal); UIManager.put("Button.default.hoverBackground", azulHover);
            UIManager.put("Button.background", new Color(75, 75, 75)); UIManager.put("Button.foreground", new Color(220, 220, 220));
            UIManager.put("Button.hoverBackground", new Color(90, 90, 90));
            // --- TRADUÇÃO ---
            UIManager.put("OptionPane.yesButtonText", "Sim"); UIManager.put("OptionPane.noButtonText", "Não");
            UIManager.put("OptionPane.cancelButtonText", "Cancelar"); UIManager.put("OptionPane.okButtonText", "OK");
        } catch (UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        LogFrame logFrame = new LogFrame();
        logFrame.setVisible(true);
        ClienteService.getInstance().setLogFrame(logFrame);

        SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
    }
}