package br.com.banco.servidor.service;

import br.com.banco.model.Transacao;
import br.com.banco.model.Usuario;
import br.com.banco.servidor.dao.Database; // Use o nome correto da sua classe
import br.com.banco.servidor.dao.TransacaoDAO;
import br.com.banco.servidor.dao.UsuarioDAO;
import br.com.banco.servidor.exception.BusinessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit; // Importa ChronoUnit

public class TransacaoService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();

    public void realizarTransferencia(String cpfRemetente, String cpfDestinatario, BigDecimal valor) throws Exception {
        if (cpfRemetente.equals(cpfDestinatario)) {
            throw new BusinessException("Não é possível transferir para si mesmo. Realize um depósito.");
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da transferência deve ser positivo.");
        }

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); 

            Usuario remetente = usuarioDAO.buscarUsuarioPorCpf(cpfRemetente);
            Usuario destinatario = usuarioDAO.buscarUsuarioPorCpf(cpfDestinatario);

            if (destinatario == null) {
                throw new BusinessException("CPF de destino não encontrado.");
            }
            if (remetente.getSaldo().compareTo(valor) < 0) {
                throw new BusinessException("Saldo insuficiente para realizar a transferência.");
            }
            
            
            BigDecimal novoSaldoRemetente = remetente.getSaldo().subtract(valor);
            BigDecimal novoSaldoDestinatario = destinatario.getSaldo().add(valor);

            usuarioDAO.atualizarSaldo(remetente.getCpf(), novoSaldoRemetente, conn);
            usuarioDAO.atualizarSaldo(destinatario.getCpf(), novoSaldoDestinatario, conn);

            Transacao transacao = new Transacao();
            transacao.setValor(valor);
            transacao.setCpfRemetente(remetente.getCpf());
            transacao.setCpfDestinatario(destinatario.getCpf());
            transacao.setDataHoraTransacao(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
            transacaoDAO.criarTransacao(transacao, conn);
            
            conn.commit();

        } catch (SQLException | BusinessException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void realizarDeposito(String cpf, BigDecimal valor) throws Exception {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor do depósito deve ser positivo.");
        }
        
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            Usuario usuario = usuarioDAO.buscarUsuarioPorCpf(cpf);
            if (usuario == null) {
                throw new BusinessException("Usuário não encontrado para depósito.");
            }
            
            Transacao deposito = new Transacao();
            deposito.setValor(valor);
            deposito.setCpfRemetente(cpf);
            deposito.setCpfDestinatario(cpf);

            deposito.setDataHoraTransacao(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

            transacaoDAO.criarTransacao(deposito, conn);
            
            BigDecimal novoSaldo = usuario.getSaldo().add(valor);
            usuarioDAO.atualizarSaldo(cpf, novoSaldo, conn);
            conn.commit();

        } catch (SQLException | BusinessException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}