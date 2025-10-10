package br.com.banco.servidor.gui;

import br.com.banco.model.ConexaoInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConexoesPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable connectionTable;
    private final Map<String, Integer> clientRowMap = new ConcurrentHashMap<>();

    public ConexoesPanel() {
        setLayout(new BorderLayout(10, 10));

        String[] columnNames = {"IP", "Porta", "Hostname", "Usuário Logado"};
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        connectionTable = new JTable(tableModel);
        connectionTable.setFillsViewportHeight(true);

        add(new JScrollPane(connectionTable), BorderLayout.CENTER);
    }

    public void adicionarConexao(String id, ConexaoInfo conexao) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addRow(new Object[]{
                conexao.getIp(),
                conexao.getPorta(),
                conexao.getHostname(),
                conexao.getNomeUsuario()
            });
            clientRowMap.put(id, tableModel.getRowCount() - 1);
        });
    }

    public void removerConexao(String id) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = clientRowMap.remove(id);
            if (rowIndex != null && rowIndex < tableModel.getRowCount()) {
                tableModel.removeRow(rowIndex);
                rebuildRowMap();
            }
        });
    }

    public void atualizarNomeUsuario(String id, String username) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = clientRowMap.get(id);
            if (rowIndex != null && rowIndex < tableModel.getRowCount()) {
                tableModel.setValueAt(username, rowIndex, 3);
            }
        });
    }

    private void rebuildRowMap() {
        clientRowMap.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String ip = (String) tableModel.getValueAt(i, 0);
            String porta = (String) tableModel.getValueAt(i, 1);
            clientRowMap.put(ip + ":" + porta, i);
        }
    }
}