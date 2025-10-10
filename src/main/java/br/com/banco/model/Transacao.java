package br.com.banco.model;
import java.math.BigDecimal;

public class Transacao {
    private int id;
    private BigDecimal valor;
    private String cpfRemetente;
    private String cpfDestinatario;
    private String dataHoraTransacao; //formato ISO 8601: "yyyy-MM-dd'T'HH:mm:ss'Z'"

    public int getId() {
    	return this.id;
    }
    
    public void setId(int id) {
    	this.id = id;
    }
    
    public BigDecimal getValor()
    {
    	return this.valor;
    }
    
    public void setValor(BigDecimal valor) {
    	this.valor = valor;
    }
    public String getCpfRemetente() { 
    	return this.cpfRemetente;
    }
    
    public void setCpfRemetente(String cpfRemetente) { 
    	this.cpfRemetente = cpfRemetente;
    }
    public String getCpfDestinatario() {
    	return this.cpfDestinatario;
    }
    
    public void setCpfDestinatario(String cpfDestinatario) {
    	this.cpfDestinatario = cpfDestinatario;
    }
    
    public String getDataHoraTransacao() {
    	return this.dataHoraTransacao;
    }
    public void setDataHoraTransacao(String timestamp) {
    	this.dataHoraTransacao = timestamp;
    }
}