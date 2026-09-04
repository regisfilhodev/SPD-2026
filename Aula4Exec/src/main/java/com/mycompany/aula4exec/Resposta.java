package com.mycompany.aula4exec;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Resposta {

    private String status;    // "OK" ou "ERRO"
    private String timestamp; // momento em que a resposta foi gerada
    private String dados;     // os quatro resultados ou o texto do erro

    public Resposta(String status, String dados) {
        this.status = status;
        this.dados = dados;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.timestamp = LocalDateTime.now().format(fmt);
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDados() {
        return dados;
    }

    // Serializa este objeto em uma linha de texto JSON
    public String paraLinha() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Remonta o objeto a partir de uma linha de texto JSON
    public static Resposta fromLinha(String linha) {
        Gson gson = new Gson();
        return gson.fromJson(linha, Resposta.class);
    }
}