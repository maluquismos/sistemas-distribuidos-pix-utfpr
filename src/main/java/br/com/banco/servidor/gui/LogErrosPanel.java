package br.com.banco.servidor.gui;

import br.com.banco.servidor.dao.LogErrorDAO;
import br.com.banco.model.LogError;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class LogErrosPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final LogErrorDAO logErrorDAO;
    private final Timer refreshTimer;
    private final DateTimeFormatter formatter;

    public LogErrosPanel() {
        this.logErrorDAO = new LogErrorDAO();
        this.formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.of("pt", "BR"))
                                          .withZone(ZoneId.systemDefault());
        setLayout(new BorderLayout(10, 10));

        String[] columnNames = {"Data/Hora", "IP Cliente", "Operação Original", "Detalhes do Erro"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTimer = new Timer(5000, e -> refreshTable());
        refreshTimer.setInitialDelay(1000); // Começa um pouco depois dos outros
        refreshTimer.start();
    }

    private void refreshTable() {
        new SwingWorker<List<LogError>, Void>() {
            @Override
            protected List<LogError> doInBackground() throws Exception {
                return logErrorDAO.listarTodosErros();
            }

            @Override
            protected void done() {
                try {
                    List<LogError> logs = get();
                    tableModel.setRowCount(0);
                    for (LogError log : logs) {
                        Instant instant = Instant.parse(log.getTimestamp());
                        String dataFormatada = formatter.format(instant);
                        tableModel.addRow(new Object[]{
                            dataFormatada,
                            log.getIpCliente(),
                            log.getOperacaoOriginal(),
                            log.getDetalhesErro()
                        });
                    }
                } catch (Exception e) {
                    refreshTimer.stop();
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}