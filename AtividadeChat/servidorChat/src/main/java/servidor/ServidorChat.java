package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorChat {

    public static final int PORTA = 5000;
    private static final int MAX_CLIENTES = 50; // tamanho do pool de threads

    public static void main(String[] args) {
        GerenciadorUsuarios gerenciador = new GerenciadorUsuarios();
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTES);

        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("Servidor de chat rodando na porta " + PORTA);
            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Nova conexão: " + socket.getRemoteSocketAddress());
                pool.execute(new ClienteHandler(socket, gerenciador));
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
