package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;

public class PixDialog extends JDialog {

    private final JFormattedTextField cpfDestinoField;
    private final JFormattedTextField valorField;
    private final ClienteService clienteService;
    private final Runnable onActionSuccess;

    public PixDialog(Frame owner, Runnable onActionSuccess) {
        super(owner, "Realizar Transferência PIX", true);
        this.clienteService = ClienteService.getInstance();
        this.onActionSuccess = onActionSuccess;

        // --- PAINEL PRINCIPAL E LAYOUT ---
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Font fontLabel = new Font("Georgia", Font.PLAIN, 16);
        Font fontInput = new Font("Arial", Font.PLAIN, 16);

        // --- CAMPO CPF DESTINO ---
        JLabel cpfLabel = new JLabel("CPF do Destinatário:");
        cpfLabel.setFont(fontLabel);
        cpfDestinoField = createCpfField();
        cpfDestinoField.setFont(fontInput);

        // --- CAMPO VALOR ---
        JLabel valorLabel = new JLabel("Valor a Transferir (R$):");
        valorLabel.setFont(fontLabel);
        valorField = createValorField();
        valorField.setFont(fontInput);

        // --- BOTÕES ---
        JButton transferirButton = new JButton("Confirmar Transferência");
        transferirButton.setFont(new Font("Georgia", Font.BOLD, 20));
        transferirButton.putClientProperty("JButton.buttonType", "default");

        JButton cancelarButton = new JButton("Cancelar");
        cancelarButton.setFont(new Font("Georgia", Font.BOLD, 20));

        // --- MONTAGEM ---
        panel.add(cpfLabel);
        panel.add(cpfDestinoField);
        panel.add(valorLabel);
        panel.add(valorField);
        panel.add(cancelarButton);
        panel.add(transferirButton);

        // --- AÇÕES ---
        transferirButton.addActionListener(e -> handleTransferencia());
        cancelarButton.addActionListener(e -> this.dispose());

        // --- CONFIGURAÇÕES DO DIÁLOGO ---
        setContentPane(panel);
        pack();
        getRootPane().setDefaultButton(transferirButton);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void handleTransferencia() {
        String cpfDestino = cpfDestinoField.getText();
        Object valorObj = valorField.getValue();

        if (cpfDestino.contains("_") || valorObj == null) {
            JOptionPane.showMessageDialog(this, "Por favor, preencha todos os campos.", "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal valor = BigDecimal.valueOf(((Number) valorObj).doubleValue());

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "O valor da transferência deve ser maior que zero.", "Valor Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirmação final
        int choice = JOptionPane.showConfirmDialog(this,
                String.format("Você confirma a transferência de R$ %.2f para o CPF %s?", valor, cpfDestino),
                "Confirmação de PIX",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                JsonNode response = clienteService.realizarTransferencia(cpfDestino, valor);
                String info = response.get("info").asText();

                if (response.get("status").asBoolean()) {
                    JOptionPane.showMessageDialog(this, info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    onActionSuccess.run(); // Atualiza o dashboard
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, info, "Erro na Transferência", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de comunicação: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- MÉTODOS AUXILIARES PARA CRIAR OS CAMPOS FORMATADOS ---
    private JFormattedTextField createCpfField() {
        try {
            MaskFormatter cpfFormatter = new MaskFormatter("###.###.###-##");
            return new JFormattedTextField(cpfFormatter);
        } catch (ParseException e) {
            return new JFormattedTextField(); // Fallback
        }
    }

    private JFormattedTextField createValorField() {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        NumberFormatter formatter = new NumberFormatter(decimalFormat);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setValue(0.00);
        return field;
    }
}