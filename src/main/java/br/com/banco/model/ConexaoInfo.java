package br.com.banco.model;

public class ConexaoInfo {
    private String ip;
    private String porta;
    private String hostname;
    private String nomeUsuario;

    public ConexaoInfo(String ip, String porta, String hostname, String nomeUsuario) {
        this.ip = ip;
        this.porta = porta;
        this.hostname = hostname;
        this.nomeUsuario = nomeUsuario;
    }

    public String getIp() { return ip; }
    public String getPorta() { return porta; }
    public String getHostname() { return hostname; }
    public String getNomeUsuario() { return nomeUsuario; }
}