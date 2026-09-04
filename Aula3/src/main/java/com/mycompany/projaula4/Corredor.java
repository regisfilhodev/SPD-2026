/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projaula4;

import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Regis
 */
public class Corredor implements Runnable {
    private final Pista pista;
 
    public Corredor(Pista pista) {
        this.pista = pista;
    }
 
    @Override
    public void run() {
        int distancia = 0;
        while (distancia < 100) {
            distancia += ThreadLocalRandom.current().nextInt(3) + 1; // avança 1 a 3 metros
        }
        pista.registrarChegada(Thread.currentThread().getName());
    }
}