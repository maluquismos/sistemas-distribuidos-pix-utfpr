package br.com.banco.servidor.dao;

import br.com.banco.model.LogError;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LogErrorDAO {

    public boolean registrarErro(String operacaoOriginal, String detalhesErro, String ipCliente) {
        String sql = "INSERT INTO log_erros(operacao_original, detalhes_erro, ip_cliente, timestamp) VALUES(?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); // Use seu nome de classe
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operacaoOriginal);
            pstmt.setString(2, detalhesErro);
            pstmt.setString(3, ipCliente);
            pstmt.setString(4, Instant.now().toString()); // Timestamp atual
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO Erro ao registrar log de erro: " + e.getMessage());
            return false;
        }
    }

    public List<LogError> listarTodosErros() {
        List<LogError> logs = new ArrayList<>();
        String sql = "SELECT * FROM log_erros ORDER BY timestamp DESC"; // Mais recentes primeiro

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LogError log = new LogError(
                    rs.getInt("id"),
                    rs.getString("operacao_original"),
                    rs.getString("detalhes_erro"),
                    rs.getString("ip_cliente"),
                    rs.getString("timestamp")
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("DAO Erro ao listar logs de erro: " + e.getMessage());
        }
        return logs;
    }
}