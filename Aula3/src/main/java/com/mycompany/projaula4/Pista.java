/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projaula4;

/**
 *
 * @author Regis
 */
public class Pista {
    String[] podio = new String[6];
    int proxima = 0;
 
    // Sem sincronização (versão inicial do exercício)
    public void registrarChegada(String nome) {
        int pos = proxima;                 // lê o lugar
        
        long duracaoNs = java.util.concurrent.ThreadLocalRandom.current().nextLong(50000, 150000); // ~50μs a 150μs de atraso real
        long fim = System.nanoTime() + duracaoNs;
        while (System.nanoTime() < fim) {
            // espera ocupada, para gerar imprvisibilidade na execução
        }
        
        podio[pos] = nome;                 // grava
        proxima = pos + 1;                 // avança
    }
}
