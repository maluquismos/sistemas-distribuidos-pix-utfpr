package br.com.banco.servidor.gui;

import br.com.banco.model.Usuario;
import br.com.banco.servidor.dao.UsuarioDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UsuariosPanel extends JPanel {

    private final JTable userTable;
    private final DefaultTableModel tableModel;
    private final UsuarioDAO usuarioDAO;
    private final Timer refreshTimer;

    public UsuariosPanel() {
        this.usuarioDAO = new UsuarioDAO();
        setLayout(new BorderLayout(10, 10));

        // MUDANÇA 1: Adiciona a coluna "Senha" ao modelo da tabela
        tableModel = new DefaultTableModel(new String[]{"CPF", "Nome", "Saldo", "Senha (Hash)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        userTable = new JTable(tableModel);
        userTable.setFillsViewportHeight(true);
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        
        add(scrollPane, BorderLayout.CENTER);

        refreshTimer = new Timer(5000, e -> refreshTable());
        refreshTimer.setInitialDelay(0);
        refreshTimer.start();
    }

    private void refreshTable() {
        new SwingWorker<List<Usuario>, Void>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                return usuarioDAO.listarTodosUsuarios();
            }

            @Override
            protected void done() {
                try {
                    List<Usuario> usuarios = get();
                    
                    int selectedRow = userTable.getSelectedRow();
                    tableModel.setRowCount(0);
                    
                    for (Usuario u : usuarios) {
                        // MUDANÇA 2: Adiciona o getSenha() ao criar a linha
                        tableModel.addRow(new Object[]{
                            u.getCpf(),
                            u.getNome(),
                            String.format("R$ %.2f", u.getSaldo().doubleValue()), // Usa doubleValue() para formatar
                            u.getSenha()
                        });
                    }

                    if (selectedRow != -1 && selectedRow < tableModel.getRowCount()) {
                        userTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }

                } catch (Exception e) {
                    refreshTimer.stop(); 
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            "Erro ao carregar dados dos usuários. A atualização automática foi pausada.\n" + e.getMessage(),
                            "Erro de Banco de Dados",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}