package br.com.banco.cliente.util;

import br.com.banco.cliente.exception.ClientProtocolException;
import br.com.banco.cliente.service.ClienteService;

import javax.swing.*;
import java.awt.Component;
import java.io.IOException;

public class ErrorHandlerUtil {

    /**
     * Centraliza o tratamento de exceções ocorridas durante a comunicação com o servidor,
     * exibindo mensagens apropriadas e oferecendo opções ao usuário.
     *
     * @param parentComponent O componente pai para centralizar o JOptionPane (pode ser JFrame ou JDialog).
     * @param error A exceção capturada.
     * @param operacaoOriginal A string da operação que estava sendo tentada (ex: "usuario_ler").
     * @param clienteService A instância do ClienteService para reportar erros.
     * @param voltarParaConexaoAction A ação a ser executada se precisar voltar para a tela de conexão.
     * @param voltarParaLoginAction A ação a ser executada se precisar voltar para a tela de login.
     */
    public static void handleError(Component parentComponent, Exception error, String operacaoOriginal,
                                ClienteService clienteService, Runnable voltarParaConexaoAction, Runnable voltarParaLoginAction) {

        String errorMessage = (error != null) ? error.getMessage().toLowerCase() : "Erro desconhecido";
        Throwable cause = error != null ? (error.getCause() != null ? error.getCause() : error) : error;
        boolean isValidationError = false;
        if (cause instanceof IllegalArgumentException && errorMessage != null &&
            (errorMessage.contains("campo obrigatório") || errorMessage.contains("chave inesperada") ||
            errorMessage.contains("deve estar no formato") || errorMessage.contains("deve ser do tipo")))
        {
            isValidationError = true;
        }

        if (error instanceof IOException) {
            JOptionPane.showMessageDialog(parentComponent, "Erro de comunicação com o servidor:\n" + errorMessage + "\nVerifique sua conexão e tente novamente.", "Erro de Rede", JOptionPane.ERROR_MESSAGE);
            if (voltarParaConexaoAction != null) SwingUtilities.invokeLater(voltarParaConexaoAction);

        } else if (isValidationError) {
            String detailedError = "O servidor enviou uma resposta inválida:\n" + errorMessage;
            Object[] options = {"Fechar janela", "Reportar ao Servidor"};
            int choice = JOptionPane.showOptionDialog(parentComponent,
                                detailedError + "\n\nDeseja reportar este erro ao servidor?",
                                "Erro de Protocolo",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.ERROR_MESSAGE,
                                null, options, options[0]);

            if (choice == JOptionPane.NO_OPTION) {
                boolean reported = clienteService.reportarErroEAguardarConfirmacao(operacaoOriginal, errorMessage);
                if (reported) JOptionPane.showMessageDialog(parentComponent, "Erro reportado com sucesso.", "Reporte Enviado", JOptionPane.INFORMATION_MESSAGE);
                else JOptionPane.showMessageDialog(parentComponent, "Falha ao confirmar reporte.", "Falha no Reporte", JOptionPane.WARNING_MESSAGE);
            }

        } else if (error instanceof ClientProtocolException) {
            JOptionPane.showMessageDialog(parentComponent, errorMessage, "Erro na Requisição", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parentComponent, "Ocorreu um erro na operação:\n" + errorMessage, "Erro na Operação", JOptionPane.WARNING_MESSAGE);
            if (errorMessage.contains("token inválido") || errorMessage.contains("sessão expirada")) {
                if (voltarParaLoginAction != null) SwingUtilities.invokeLater(voltarParaLoginAction);
            }
        }
    }
}