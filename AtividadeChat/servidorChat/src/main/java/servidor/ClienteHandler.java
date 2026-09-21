package servidor;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import protocolo.Mensagem;
import protocolo.TipoMensagem;

/**
 * Atende UM cliente. Cada instância roda em uma thread do pool do servidor.
 * Protocolo: uma mensagem JSON por linha.
 */
public class ClienteHandler implements Runnable {

    private static final String SERVIDOR = "SERVIDOR";

    private final Socket socket;
    private final GerenciadorUsuarios gerenciador;
    private final Gson gson = new Gson();

    private BufferedReader entrada;
    private PrintWriter saida;
    private volatile String apelido; // lido por outras threads (lista, broadcast)

    public ClienteHandler(Socket socket, GerenciadorUsuarios gerenciador) {
        this.socket = socket;
        this.gerenciador = gerenciador;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            saida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            if (!fazerLogin()) {
                return;
            }

            String linha;
            while ((linha = entrada.readLine()) != null) {
                Mensagem msg = lerMensagem(linha);
                if (msg == null || msg.getTipo() == null) {
                    enviar(erro("Mensagem inválida."));
                    continue;
                }
                if (msg.getTipo() == TipoMensagem.SAIR) {
                    break;
                }
                processar(msg);
            }
        } catch (IOException e) {
            // cliente caiu ou fechou a conexão: só este handler termina
            System.out.println("Conexão encerrada: " + (apelido != null ? apelido : socket.getRemoteSocketAddress()));
        } finally {
            desconectar();
        }
    }

    /** Repete até o cliente enviar ENTRAR com um apelido válido e livre. */
    private boolean fazerLogin() throws IOException {
        String linha;
        while ((linha = entrada.readLine()) != null) {
            Mensagem msg = lerMensagem(linha);
            if (msg == null || msg.getTipo() != TipoMensagem.ENTRAR) {
                enviar(erro("Envie ENTRAR com um apelido para começar."));
                continue;
            }
            String nome = msg.getOrigem() == null ? "" : msg.getOrigem().trim();
            if (nome.isEmpty() || nome.equalsIgnoreCase(SERVIDOR)) {
                enviar(erro("Apelido inválido."));
                continue;
            }
            if (gerenciador.adicionar(nome, this)) {
                enviar(new Mensagem(TipoMensagem.ENTRAR_OK, SERVIDOR, nome, "Bem-vindo, " + nome + "!"));
                gerenciador.enviarParaTodos(aviso(nome + " entrou no chat."), nome);
                System.out.println(nome + " entrou.");
                return true;
            }
            enviar(erro("Apelido já em uso. Escolha outro."));
        }
        return false;
    }

    private void processar(Mensagem msg) {
        switch (msg.getTipo()) {
            case TODOS: {
                // origem é sempre definida pelo servidor (evita se passar por outro usuário)
                Mensagem m = new Mensagem(TipoMensagem.TODOS, apelido, null, msg.getMensagem());
                gerenciador.enviarParaTodos(m, null); // inclui o próprio remetente
                break;
            }
            case PRIVADA: {
                ClienteHandler dest = gerenciador.buscar(msg.getDestino());
                if (dest == null) {
                    enviar(erro("Usuário '" + msg.getDestino() + "' não está online."));
                    break;
                }
                Mensagem m = new Mensagem(TipoMensagem.PRIVADA, apelido, dest.getApelido(), msg.getMensagem());
                dest.enviar(m);
                if (dest != this) {
                    enviar(m); // confirmação para o remetente
                }
                break;
            }
            case LISTAR: {
                Mensagem m = new Mensagem(TipoMensagem.LISTA, SERVIDOR, apelido, null);
                m.setUsuarios(gerenciador.listarApelidos());
                enviar(m);
                break;
            }
            default:
                enviar(erro("Tipo de mensagem não permitido: " + msg.getTipo()));
        }
    }

    /** Vários handlers escrevem neste mesmo cliente, então o envio é synchronized. */
    public synchronized void enviar(Mensagem msg) {
        if (saida != null) {
            saida.println(gson.toJson(msg));
        }
    }

    private void desconectar() {
        if (apelido != null) {
            gerenciador.remover(apelido);
            gerenciador.enviarParaTodos(aviso(apelido + " saiu do chat."), null);
            System.out.println(apelido + " saiu.");
        }
        try {
            socket.close();
        } catch (IOException ignorada) {
        }
    }

    private Mensagem lerMensagem(String linha) {
        try {
            return gson.fromJson(linha, Mensagem.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private Mensagem erro(String texto) {
        return new Mensagem(TipoMensagem.ERRO, SERVIDOR, apelido, texto);
    }

    private Mensagem aviso(String texto) {
        return new Mensagem(TipoMensagem.AVISO, SERVIDOR, null, texto);
    }
}
