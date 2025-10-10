package br.com.banco.servidor.dao;

import br.com.banco.model.Transacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TransacaoDAO {

    public boolean criarTransacao(Transacao transacao, Connection conn) throws SQLException {
        String sql = "INSERT INTO transacao(valor, cpf_remetente, cpf_destinatario, timestamp) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, transacao.getValor());
            pstmt.setString(2, transacao.getCpfRemetente());
            pstmt.setString(3, transacao.getCpfDestinatario());
            pstmt.setString(4, transacao.getDataHoraTransacao());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Transacao> lerTransacoesPorCpfEData(String cpf, String dataInicial, String dataFinal) {
        List<Transacao> transacoes = new ArrayList<>();
        String sql = "SELECT * FROM transacao WHERE (cpf_remetente = ? OR cpf_destinatario = ?) " +
                     "AND timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
        
        try (Connection conn = Database.getConnection(); // Use o nome da sua classe de DB
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, cpf);
            pstmt.setString(2, cpf);
            pstmt.setString(3, dataInicial);
            pstmt.setString(4, dataFinal);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transacao t = new Transacao();
                    t.setId(rs.getInt("id"));
                    t.setValor(rs.getBigDecimal("valor"));
                    t.setCpfRemetente(rs.getString("cpf_remetente"));
                    t.setCpfDestinatario(rs.getString("cpf_destinatario"));
                    t.setDataHoraTransacao(rs.getString("timestamp"));
                    transacoes.add(t);
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO Erro ao ler transações: " + e.getMessage());
        }
        return transacoes;
    }

    public List<Transacao> listarTodasTransacoes() {
        List<Transacao> transacoes = new ArrayList<>();
        String sql = "SELECT * FROM transacao ORDER BY timestamp DESC";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transacao transacao = new Transacao();
                transacao.setId(rs.getInt("id"));
                transacao.setValor(rs.getBigDecimal("valor"));
                transacao.setCpfRemetente(rs.getString("cpf_remetente"));
                transacao.setCpfDestinatario(rs.getString("cpf_destinatario"));
                transacao.setDataHoraTransacao(rs.getString("timestamp"));
                transacoes.add(transacao);
            }
        } catch (SQLException e) {
            System.err.println("DAO Erro ao listar transações: " + e.getMessage());
        }
        return transacoes;
    }
    
}