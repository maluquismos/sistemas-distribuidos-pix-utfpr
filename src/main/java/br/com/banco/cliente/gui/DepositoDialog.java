package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.ErrorHandlerUtil; // Importa
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public class DepositoDialog extends JDialog {

    private final JFormattedTextField valorField;
    private final ClienteService clienteService;
    private final Runnable onActionSuccess;

    public DepositoDialog(Frame owner, Runnable onActionSuccess) {
        super(owner, "Realizar Depósito", true);
        this.clienteService = ClienteService.getInstance();
        this.onActionSuccess = onActionSuccess;

        // --- Interface Gráfica (sem alterações) ---
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        NumberFormatter formatter = new NumberFormatter(decimalFormat);
        formatter.setValueClass(Double.class); formatter.setAllowsInvalid(false); formatter.setCommitsOnValidEdit(true);
        valorField = new JFormattedTextField(formatter);
        valorField.setValue(0.00); valorField.setFont(new Font("Georgia", Font.BOLD, 24));
        valorField.setHorizontalAlignment(JTextField.RIGHT); valorField.setPreferredSize(new Dimension(200, 40));
        valorField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) { SwingUtilities.invokeLater(() -> valorField.selectAll()); }
        });
        JLabel label = new JLabel("Valor do Depósito (R$):"); label.setFont(new Font("Georgia", Font.PLAIN, 16));
        JPanel formPanel = new JPanel(new BorderLayout(10, 0));
        formPanel.add(label, BorderLayout.WEST); formPanel.add(valorField, BorderLayout.CENTER);
        JButton depositarButton = new JButton("Confirmar Depósito"); depositarButton.setFont(new Font("Georgia", Font.BOLD, 14));
        getRootPane().setDefaultButton(depositarButton);
        JButton cancelarButton = new JButton("Cancelar"); cancelarButton.setFont(new Font("Georgia", Font.BOLD, 14));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelarButton); buttonPanel.add(depositarButton);
        panel.add(formPanel, BorderLayout.CENTER); panel.add(buttonPanel, BorderLayout.SOUTH);
        depositarButton.addActionListener(e -> handleDeposito()); cancelarButton.addActionListener(e -> this.dispose());
        setContentPane(panel); setMinimumSize(new Dimension(450, 200)); pack(); setResizable(false); setLocationRelativeTo(owner);
    }

    private void handleDeposito() {
        Object value = valorField.getValue();
        if (value == null) return;
        BigDecimal valor = BigDecimal.valueOf(((Number) value).doubleValue());
        String operacaoOriginal = "depositar";

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "O valor do depósito deve ser maior que zero.", "Valor Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            JsonNode response = clienteService.depositar(valor);
            String info = response.path("info").asText("Operação concluída.");
            if (response.path("status").asBoolean(false)) {
                JOptionPane.showMessageDialog(this, info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                onActionSuccess.run();
                this.dispose();
            } else {
                // --- CORREÇÃO: Usa ErrorHandlerUtil para status:false ---
                ErrorHandlerUtil.handleError(this, new Exception(info), operacaoOriginal, clienteService, null, null);
            }
        } catch (Exception e) {
            // --- CORREÇÃO: Usa ErrorHandlerUtil para qualquer exceção ---
            ErrorHandlerUtil.handleError(this, e, operacaoOriginal, clienteService, null, null);
        }
    }
}