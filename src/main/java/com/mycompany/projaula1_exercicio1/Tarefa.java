package com.mycompany.projaula1_exercicio1;

public class Tarefa implements Runnable {

    private final int[] vetor;
    private final int inicio;
    private final int fim;
    private long somaParcial;
    private long tempo;

    public Tarefa(int[] vetor, int inicio, int fim) {
        this.vetor = vetor;
        this.inicio = inicio;
        this.fim = fim;
    }

    @Override
    public void run() {

        long inicioTempo = System.nanoTime();

        somaParcial = 0;
        
        for (int i = inicio; i < fim; i++) {
            somaParcial += (long) vetor[i]*vetor[i]*vetor[i]; //soma do cubo do de cada elemento do vetor 
        }

        // Calcula o tempo decorrido e converte de ns para µs
        tempo = (System.nanoTime() - inicioTempo) / 1000;
    }

    public long getSomaParcial() {
        return somaParcial;
    }

    public long getTempo() {
        return tempo;
    }

    public int getInicio() {
        return inicio;
    }

    public int getFim() {
        return fim;
    }
}