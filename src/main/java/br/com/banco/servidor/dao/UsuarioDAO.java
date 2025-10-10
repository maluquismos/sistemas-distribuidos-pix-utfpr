package br.com.banco.servidor.dao;

import br.com.banco.model.Usuario;
import br.com.banco.servidor.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import br.com.banco.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;



public class UsuarioDAO {
    public boolean criarUsuario(Usuario usuario) {
        String sql = "INSERT INTO usuario(cpf, nome, senha, saldo) VALUES(?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario.getCpf());
            pstmt.setString(2, usuario.getNome());
            pstmt.setString(3, usuario.getSenha());
            pstmt.setDouble(4, 0.0);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO Erro ao criar usuário: " + e.getMessage());
            return false;
        }
    }

    public Usuario buscarUsuarioPorCpf(String cpf) {
        String sql = "SELECT * FROM usuario WHERE cpf = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                        rs.getString("cpf"),
                        rs.getString("nome"),
                        rs.getString("senha"),
                        rs.getBigDecimal("saldo")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("DAO Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public Usuario validarLogin(String cpf, String senha) {
    	
        Usuario usuario = buscarUsuarioPorCpf(cpf);
        
        if (usuario != null) {
        	String senhaHash = usuario.getSenha();
        	if (PasswordUtil.checkPassword(senha, senhaHash)) {
        		return usuario;
        	}
        }
        
        return null;
    }
    
    public List<Usuario> listarTodosUsuarios() {
        String sql = "SELECT * FROM usuario";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Usuario usuario = new Usuario(
                    rs.getString("cpf"),
                    rs.getString("nome"),
                    rs.getString("senha"),
                    rs.getBigDecimal("saldo")
                );
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("DAO Erro ao listar usuários: " + e.getMessage());
        }
        return usuarios;
    }
    
    public boolean atualizarUsuario(String cpf, JsonNode dadosAtualizacao) {
        StringBuilder sql = new StringBuilder("UPDATE usuario SET ");
        if (dadosAtualizacao.has("nome")) {
            sql.append("nome = ?, ");
        }
        if (dadosAtualizacao.has("senha")) {
            sql.append("senha = ?, ");
        }
        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE cpf = ?");

        try (Connection conn = Database.getConnection(); // Use o nome correto da sua classe
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (dadosAtualizacao.has("nome")) {
                pstmt.setString(paramIndex++, dadosAtualizacao.get("nome").asText());
            }
            if (dadosAtualizacao.has("senha")) {
                pstmt.setString(paramIndex++, dadosAtualizacao.get("senha").asText());
            }
            pstmt.setString(paramIndex, cpf);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO Erro ao atualizar usuário: " + e.getMessage());
            return false;
        }
    }

    public boolean deletarUsuario(String cpf) {
        String sql = "DELETE FROM usuario WHERE cpf = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DAO Erro ao deletar usuário: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarSaldo(String cpf, BigDecimal novoSaldo, Connection conn) throws SQLException {
        String sql = "UPDATE usuario SET saldo = ? WHERE cpf = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, novoSaldo);
            pstmt.setString(2, cpf);
            return pstmt.executeUpdate() > 0;
        }
    }
}