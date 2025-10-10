package br.com.banco.model;

import java.math.BigDecimal;


public class Usuario {
    private String cpf;
    private String nome;
    private String senha;
    private BigDecimal saldo;

    
    public Usuario() {
    	//construtor vazio
    }

    //construtor completo
    public Usuario(String cpf, String nome, String senha, BigDecimal saldo) {
        this.cpf = cpf;
        this.nome = nome;
        this.senha = senha;
        this.saldo = saldo;
    }

    //getters e setters
    public String getCpf() {
    	return this.cpf;
    }
    public void setCpf(String cpf) {
    	this.cpf = cpf;
    }
    public String getNome() {
    	return this.nome;
    }
    public void setNome(String nome) {
    	this.nome = nome;
    }
    public String getSenha() {
    	return this.senha;
    }
    public void setSenha(String senha) {
    	this.senha = senha;
    }
    public BigDecimal getSaldo() { 
    	return this.saldo;
    }
    public void setSaldo(BigDecimal saldo) {
    	this.saldo = saldo;
    }
}