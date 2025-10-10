package br.com.banco.cliente.gui;

import br.com.banco.cliente.service.ClienteService;
import br.com.banco.cliente.util.IconUtil;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class ExtratoDialog extends JDialog {

    private final JFormattedTextField dataInicialField;
    private final JFormattedTextField dataFinalField;
    private final DefaultTableModel tableModel;
    private final ClienteService clienteService;

    public ExtratoDialog(Frame owner) {
        super(owner, "Extrato de Transações", true);
        this.clienteService = ClienteService.getInstance();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- PAINEL DE FILTROS (TOPO) ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Selecione o Período"));

        Font fontLabel = new Font("Georgia", Font.PLAIN, 14);
        Font fontInput = new Font("Arial", Font.PLAIN, 14);

        dataInicialField = createDateField();
        dataInicialField.setFont(fontInput);
        dataFinalField = createDateField();
        dataFinalField.setFont(fontInput);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String hoje = sdf.format(new Date());
        dataInicialField.setText(hoje);
        dataFinalField.setText(hoje);

        JButton buscarButton = new JButton("Buscar");
        buscarButton.setFont(new Font("Georgia", Font.BOLD, 14));
        buscarButton.setIcon(IconUtil.loadIcon("search.png", 16, 16));
        // MUDANÇA: Garante que o botão de busca seja o padrão (para cor e tecla Enter)
        getRootPane().setDefaultButton(buscarButton);

        filterPanel.add(new JLabel("Data Inicial:")).setFont(fontLabel);
        filterPanel.add(dataInicialField);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(new JLabel("Data Final:")).setFont(fontLabel);
        filterPanel.add(dataFinalField);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(buscarButton);
        
        // --- TABELA DE RESULTADOS (CENTRO) ---
        String[] colunas = {"Data", "Descrição", "Valor (R$)", "Tipo"};
        tableModel = new DefaultTableModel(colunas, 0);
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Georgia", Font.PLAIN, 14));
        table.setRowHeight(25);
        
        // --- BOTÃO VOLTAR (SUL) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton voltarButton = new JButton("Voltar");
        voltarButton.setFont(new Font("Georgia", Font.BOLD, 14));
        voltarButton.setIcon(IconUtil.loadIcon("arrow-left.png", 16, 16));
        
        // Aplica o estilo vermelho, como no botão de exclusão
        voltarButton.putClientProperty("FlatLaf.style", "" +
            "background: rgb(200, 50, 50);" +
            "foreground: rgb(255, 255, 255);" +
            "hoverBackground: rgb(230, 70, 70);" +
            "pressedBackground: rgb(180, 40, 40);");
        
        bottomPanel.add(voltarButton);

        // --- MONTAGEM FINAL ---
        mainPanel.add(filterPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // --- AÇÕES ---
        buscarButton.addActionListener(e -> buscarExtrato());
        voltarButton.addActionListener(e -> this.dispose());

        // --- CONFIGURAÇÕES DO DIÁLOGO ---
        setContentPane(mainPanel);
        setSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(700, 400));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    // Busca as transações com base nas datas dos campos
    private void buscarExtrato() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date dataInicial, dataFinal;
        try {
            dataInicial = sdf.parse(dataInicialField.getText());
            dataFinal = sdf.parse(dataFinalField.getText());
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Formato de data inválido. Use dd/mm/aaaa.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // A validação de 31 dias agora é feita no serviço, mas podemos manter um aviso na GUI
        if (dataInicial.after(dataFinal)) {
            JOptionPane.showMessageDialog(this, "A data inicial não pode ser posterior à data final.", "Erro de Período", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        new SwingWorker<JsonNode, Void>() {
            @Override
            protected JsonNode doInBackground() throws Exception {
                // Chama o serviço, que contém a regra de negócio dos 31 dias
                return clienteService.lerTransacoes(dataInicial, dataFinal);
            }

            @Override
            protected void done() {
                try {
                    JsonNode response = get();
                    if (response.get("status").asBoolean()) {
                        preencherTabela(response.get("transacoes"));
                    } else {
                        tableModel.setRowCount(0);
                        JOptionPane.showMessageDialog(ExtratoDialog.this, response.get("info").asText(), "Informação", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    tableModel.setRowCount(0);
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(ExtratoDialog.this, cause.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Preenche a tabela com os dados recebidos do servidor
    private void preencherTabela(JsonNode transacoes) {
        tableModel.setRowCount(0);
        if (transacoes.isEmpty()){
            // Adiciona uma linha de aviso se não houver transações
            tableModel.addRow(new Object[]{"", "Nenhuma transação encontrada para o período.", "", ""});
            return;
        }

        String cpfUsuarioLogado = clienteService.getCpfLogado();
        DateTimeFormatter parser = DateTimeFormatter.ISO_DATE_TIME;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

        for (JsonNode t : transacoes) {
            String cpfRemetente = t.get("usuario_enviador").get("cpf").asText();
            String nomeRemetente = t.get("usuario_enviador").get("nome").asText();
            String cpfDestinatario = t.get("usuario_recebedor").get("cpf").asText();
            String nomeDestinatario = t.get("usuario_recebedor").get("nome").asText();
            double valor = t.get("valor_enviado").asDouble();
            ZonedDateTime data = ZonedDateTime.parse(t.get("criado_em").asText(), parser);
            String dataFormatada = data.format(formatter);

            String descricao, tipo;
            
            if (cpfRemetente.equals(cpfDestinatario) && cpfRemetente.equals(cpfUsuarioLogado)) {
                tipo = "Entrada";
                descricao = "Depósito recebido";
            } else if (cpfRemetente.equals(cpfUsuarioLogado)) {
                tipo = "Saída";
                descricao = "PIX enviado para " + nomeDestinatario;
            } else {
                tipo = "Entrada";
                descricao = "PIX recebido de " + nomeRemetente;
            }
            
            tableModel.addRow(new Object[]{dataFormatada, descricao, String.format("%.2f", valor), tipo});
        }
    }

    // Método auxiliar para criar campos de data formatados e com tamanho adequado
    private JFormattedTextField createDateField() {
        try {
            MaskFormatter dateFormatter = new MaskFormatter("##/##/####");
            dateFormatter.setPlaceholderCharacter('_');
            JFormattedTextField dateField = new JFormattedTextField(dateFormatter);
            dateField.setPreferredSize(new Dimension(120, 30)); // Define um comprimento maior
            return dateField;
        } catch (ParseException e) {
            JFormattedTextField dateField = new JFormattedTextField();
            dateField.setPreferredSize(new Dimension(120, 30));
            return dateField;
        }
    }
}