package br.com.banco.cliente.exception;

// Exceção específica para erros detectados pelo cliente ANTES de enviar a mensagem
public class ClientProtocolException extends Exception {
    public ClientProtocolException(String message) {
        super(message);
    }
}