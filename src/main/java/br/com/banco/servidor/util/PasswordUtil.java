package br.com.banco.servidor.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Gera um hash BCrypt a partir de uma senha de texto puro.
     * O "salt" é gerado e embutido no próprio hash.
     * @param plainPassword A senha original.
     * @return O hash da senha.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Compara uma senha de texto puro com um hash salvo no banco de dados.
     * @param plainPassword A senha que o usuário digitou no login.
     * @param hashedPassword O hash que está salvo no banco.
     * @return true se a senha corresponder ao hash, false caso contrário.
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}