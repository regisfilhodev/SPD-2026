package com.mycompany.aula4exec;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Servidor {

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(9999)) {
            System.out.println("Servidor UDP aguardando na porta 9999...");
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote); // bloqueia até chegar um datagrama

                String linha = new String(pacote.getData(), 0, pacote.getLength());
                InetAddress origem = pacote.getAddress();
                int porta = pacote.getPort();

                System.out.println("Recebido de " + origem + ":" + porta + " -> " + linha);

                Requisicao req = Requisicao.fromLinha(linha);
                Resposta resposta = calcular(req);

                byte[] dadosEnvio = resposta.paraLinha().getBytes();
                DatagramPacket pacoteResp = new DatagramPacket(dadosEnvio, dadosEnvio.length, origem, porta);
                socket.send(pacoteResp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Resposta calcular(Requisicao req) {
        double n1 = req.getNum1();
        double n2 = req.getNum2();

        double soma = n1 + n2;
        double sub = n1 - n2;
        double mul = n1 * n2;
        String div = (n2 == 0) ? "indefinido" : String.valueOf(n1 / n2);

        String dados = "\nSOMA = " + soma + "\nSUB = " + sub + "\nMUL = " + mul + "\nDIV = " + div;
        return new Resposta("\nOK", dados);
    }
}