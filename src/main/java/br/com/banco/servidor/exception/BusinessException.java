package br.com.banco.servidor.exception;

public class BusinessException extends ProtocolException {
    public BusinessException(String message) {
        super(message);
    }
}