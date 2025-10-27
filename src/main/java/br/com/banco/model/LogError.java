package br.com.banco.model;

public class LogError {
    private int id;
    private String operacaoOriginal;
    private String detalhesErro;
    private String ipCliente;
    private String timestamp;

    public LogError(int id, String operacaoOriginal, String detalhesErro, String ipCliente, String timestamp) {
        this.id = id;
        this.operacaoOriginal = operacaoOriginal;
        this.detalhesErro = detalhesErro;
        this.ipCliente = ipCliente;
        this.timestamp = timestamp;
    }

    // Getters
    public int getId() { return id; }
    public String getOperacaoOriginal() { return operacaoOriginal; }
    public String getDetalhesErro() { return detalhesErro; }
    public String getIpCliente() { return ipCliente; }
    public String getTimestamp() { return timestamp; }
}