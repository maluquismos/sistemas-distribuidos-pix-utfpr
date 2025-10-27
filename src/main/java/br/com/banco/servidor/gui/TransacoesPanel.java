package br.com.banco.servidor.gui;

import br.com.banco.model.Transacao;
import br.com.banco.servidor.dao.TransacaoDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TransacoesPanel extends JPanel {

    private final JTable transacoesTable;
    private final DefaultTableModel tableModel;
    private final TransacaoDAO transacaoDAO;
    private final Timer refreshTimer;
    private final DateTimeFormatter formatter; // Formatter para a data

    public TransacoesPanel() {
        this.transacaoDAO = new TransacaoDAO();
        // Define o formato de data/hora desejado
        this.formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.of("pt", "BR"))
                                          .withZone(ZoneId.systemDefault());
        
        setLayout(new BorderLayout(10, 10));

        // -- Modelo da Tabela --
        String[] columnNames = {"Data/Hora", "Valor (R$)", "CPF Remetente", "CPF Destinatário"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        transacoesTable = new JTable(tableModel);
        transacoesTable.setFillsViewportHeight(true);
        
        add(new JScrollPane(transacoesTable), BorderLayout.CENTER);

        // -- Timer para atualização automática --
        refreshTimer = new Timer(5000, e -> refreshTable());
        refreshTimer.setInitialDelay(0); // Inicia a primeira atualização imediatamente
        refreshTimer.start();
    }

    private void refreshTable() {
        new SwingWorker<List<Transacao>, Void>() {
            @Override
            protected List<Transacao> doInBackground() throws Exception {
                // Busca todas as transações do banco
                return transacaoDAO.listarTodasTransacoes();
            }

            @Override
            protected void done() {
                try {
                    List<Transacao> transacoes = get();
                    
                    int selectedRow = transacoesTable.getSelectedRow();
                    tableModel.setRowCount(0); // Limpa a tabela
                    
                    for (Transacao t : transacoes) {
                        // Converte a data do formato ISO para o formato local
                        Instant instant = Instant.parse(t.getDataHoraTransacao());
                        String dataFormatada = formatter.format(instant);
                        
                        tableModel.addRow(new Object[]{
                            dataFormatada,
                            String.format("%.2f", t.getValor()),
                            t.getCpfRemetente(),
                            t.getCpfDestinatario()
                        });
                    }

                    if (selectedRow != -1 && selectedRow < tableModel.getRowCount()) {
                        transacoesTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }
                } catch (Exception e) {
                    refreshTimer.stop();
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(TransacoesPanel.this,
                            "Erro ao carregar dados das transações: " + e.getMessage(),
                            "Erro de Banco de Dados",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}