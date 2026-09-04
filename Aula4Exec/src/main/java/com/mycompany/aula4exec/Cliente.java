package com.mycompany.aula4exec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Cliente extends JFrame {

    private JTextField tfNum1;
    private JTextField tfNum2;
    private JTextArea taResultado;
    private JButton btnEnviar;

    public Cliente() {
        super("Cliente UDP - Calculadora");
        montarInterface();
    }

    private void montarInterface() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 350);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel painelTopo = new JPanel(new GridLayout(3, 2, 5, 5));
        painelTopo.add(new JLabel("Número 1:"));
        tfNum1 = new JTextField();
        painelTopo.add(tfNum1);

        painelTopo.add(new JLabel("Número 2:"));
        tfNum2 = new JTextField();
        painelTopo.add(tfNum2);

        btnEnviar = new JButton("Enviar Requisição");
        painelTopo.add(new JLabel());
        painelTopo.add(btnEnviar);

        taResultado = new JTextArea();
        taResultado.setEditable(false);
        JScrollPane scroll = new JScrollPane(taResultado);

        add(painelTopo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        btnEnviar.addActionListener(this::enviarRequisicao);
    }

    private void enviarRequisicao(ActionEvent e) {
        try {
            double num1 = Double.parseDouble(tfNum1.getText());
            double num2 = Double.parseDouble(tfNum2.getText());

            Requisicao req = new Requisicao(num1, num2);
            String linha = req.paraLinha();

            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress servidor = InetAddress.getByName("localhost");
                byte[] dadosEnvio = linha.getBytes();
                DatagramPacket pacoteEnvio = new DatagramPacket(dadosEnvio, dadosEnvio.length, servidor, 9999);
                socket.send(pacoteEnvio);

                byte[] buffer = new byte[1024];
                DatagramPacket pacoteResp = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(3000); // evita travar para sempre no receive()
                socket.receive(pacoteResp);

                String linhaResp = new String(pacoteResp.getData(), 0, pacoteResp.getLength());
                Resposta resp = Resposta.fromLinha(linhaResp);

                taResultado.append("Status: " + resp.getStatus() + "\n");
                taResultado.append("Horário: " + resp.getTimestamp() + "\n");
                taResultado.append("Resultado: " + resp.getDados() + "\n");
                taResultado.append("----------------------------\n");
            }
        } catch (NumberFormatException ex) {
            taResultado.append("Erro: digite números válidos.\n");
        } catch (SocketTimeoutException ex) {
            taResultado.append("Erro: servidor não respondeu (timeout). Ele está rodando?\n");
        } catch (Exception ex) {
            taResultado.append("Erro: " + ex.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Cliente cliente = new Cliente();
            cliente.setVisible(true);
        });
    }
}