package com.mycompany.aula4exec;

import com.google.gson.Gson;

public class Requisicao {

    private double num1;
    private double num2;

    public Requisicao(double num1, double num2) {
        this.num1 = num1;
        this.num2 = num2;
    }

    public double getNum1() {
        return num1;
    }

    public double getNum2() {
        return num2;
    }

    // Serializa este objeto em uma linha de texto JSON
    public String paraLinha() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Remonta o objeto a partir de uma linha de texto JSON
    public static Requisicao fromLinha(String linha) {
        Gson gson = new Gson();
        return gson.fromJson(linha, Requisicao.class);
    }
}