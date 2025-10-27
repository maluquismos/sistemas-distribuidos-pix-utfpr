package br.com.banco.servidor.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DATABASE_URL = "jdbc:sqlite:banco.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static void initializeDatabase() {
        String sqlUsuario = "CREATE TABLE IF NOT EXISTS usuario (\n"
                          + "    cpf TEXT PRIMARY KEY,\n"
                          + "    nome TEXT NOT NULL,\n"
                          + "    senha TEXT NOT NULL,\n"
                          + "    saldo REAL NOT NULL DEFAULT 0.0\n"
                          + ");";

        String sqlTransacao = "CREATE TABLE IF NOT EXISTS transacao (\n"
                            + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                            + "    valor REAL NOT NULL,\n"
                            + "    cpf_remetente TEXT NOT NULL,\n"
                            + "    cpf_destinatario TEXT NOT NULL,\n"
                            + "    timestamp TEXT NOT NULL,\n"
                            + "    FOREIGN KEY (cpf_remetente) REFERENCES usuario(cpf),\n"
                            + "    FOREIGN KEY (cpf_destinatario) REFERENCES usuario(cpf)\n"
                            + ");";
        
        String sqlLogError = "CREATE TABLE IF NOT EXISTS log_erros (\n"
                       + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                       + "    operacao_original TEXT NOT NULL,\n"
                       + "    detalhes_erro TEXT NOT NULL,\n"
                       + "    ip_cliente TEXT,\n"
                       + "    timestamp TEXT NOT NULL\n"
                       + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuario);
            stmt.execute(sqlTransacao);
            stmt.execute(sqlLogError);
            System.out.println("Banco de dados inicializado com sucesso.");
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
        }
    }
}