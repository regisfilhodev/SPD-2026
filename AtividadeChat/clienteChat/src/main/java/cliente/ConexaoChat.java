package cliente;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import protocolo.Mensagem;

/**
 * Cuida só da rede: conecta, envia mensagens e recebe mensagens em uma
 * THREAD SEPARADA. Assim o recebimento nunca trava a digitação do usuário.
 * Protocolo: uma mensagem JSON por linha (igual ao do servidor).
 */
public class ConexaoChat {

    /** Callbacks chamados pela thread de recebimento (não pela thread da tela). */
    public interface Ouvinte {
        void aoReceber(Mensagem msg);

        void aoDesconectar(String motivo);
    }

    private final Gson gson = new Gson();
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private volatile boolean ativo;

    public void conectar(String host, int porta) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, porta), 5000); // timeout de 5 s
        entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        saida = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        ativo = true;
    }

    /** Inicia a thread que fica lendo do servidor o tempo todo. */
    public void iniciarRecebimento(Ouvinte ouvinte) {
        Thread receptor = new Thread(() -> {
            String motivo = "O servidor encerrou a conexão.";
            try {
                String linha;
                while (ativo && (linha = entrada.readLine()) != null) {
                    Mensagem msg = converter(linha);
                    if (msg != null && msg.getTipo() != null && ativo) {
                        ouvinte.aoReceber(msg);
                    }
                }
            } catch (IOException e) {
                motivo = "Conexão perdida: " + e.getMessage();
            } finally {
                // só avisa se a queda NÃO foi provocada pelo próprio usuário (fechar())
                if (ativo) {
                    ativo = false;
                    ouvinte.aoDesconectar(motivo);
                }
            }
        }, "Receptor");
        receptor.setDaemon(true);
        receptor.start();
    }

    public synchronized void enviar(Mensagem msg) {
        if (saida != null) {
            saida.println(gson.toJson(msg));
        }
    }

    /** Fechamento iniciado pelo usuário (botão Sair ou fechar a janela). */
    public void fechar() {
        ativo = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignorada) {
        }
    }

    private Mensagem converter(String linha) {
        try {
            return gson.fromJson(linha, Mensagem.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
