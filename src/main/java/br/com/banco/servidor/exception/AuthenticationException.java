package br.com.banco.servidor.exception;

public class AuthenticationException extends ProtocolException {
    public AuthenticationException(String message) {
        super(message);
    }
}