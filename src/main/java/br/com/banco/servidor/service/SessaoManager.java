package br.com.banco.servidor.service;

import br.com.banco.servidor.exception.AuthenticationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessaoManager {

    private static final Map<String, SessaoInfo> sessoesAtivas = new ConcurrentHashMap<>();

    private static class SessaoInfo {
        String cpf;
        String ip;

        SessaoInfo(String cpf, String ip) {
            this.cpf = cpf;
            this.ip = ip;
        }
    }

    public static String criarSessao(String cpf, String ip) {
        sessoesAtivas.values().removeIf(sessao -> sessao.cpf.equals(cpf));

        String token = UUID.randomUUID().toString();
        sessoesAtivas.put(token, new SessaoInfo(cpf, ip));
        return token;
    }

    public static String validarToken(String token, String ip) throws AuthenticationException {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Token não fornecido.");
        }
        
        SessaoInfo sessao = sessoesAtivas.get(token);
        if (sessao == null) {
            throw new AuthenticationException("Token inválido ou sessão expirada.");
        }

         if (!sessao.ip.equals(ip)) {
             throw new AuthenticationException("O endereço IP da requisição não corresponde ao da sessão.");
         }

        return sessao.cpf;
    }


    public static boolean encerrarSessao(String token) {
        return sessoesAtivas.remove(token) != null;
    }
}