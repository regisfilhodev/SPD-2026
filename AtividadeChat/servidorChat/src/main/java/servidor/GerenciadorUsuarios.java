package servidor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import protocolo.Mensagem;

/**
 * Guarda os usuários conectados. É a REGIÃO CRÍTICA do servidor: várias
 * threads (uma por cliente) leem e alteram este mapa ao mesmo tempo, então
 * todo acesso é feito dentro de blocos synchronized.
 */
public class GerenciadorUsuarios {

    // chave = apelido em minúsculas (apelido único, sem diferenciar maiúsculas)
    private final Map<String, ClienteHandler> usuarios = new LinkedHashMap<>();

    /**
     * Verifica se o apelido está livre E registra o usuário, tudo em uma
     * única operação atômica. Sem o synchronized, dois clientes poderiam
     * passar na verificação ao mesmo tempo e entrar com o mesmo apelido
     * (condição de corrida).
     */
    public synchronized boolean adicionar(String apelido, ClienteHandler handler) {
        String chave = apelido.toLowerCase();
        if (usuarios.containsKey(chave)) {
            return false;
        }
        handler.setApelido(apelido);
        usuarios.put(chave, handler);
        return true;
    }

    public synchronized void remover(String apelido) {
        if (apelido != null) {
            usuarios.remove(apelido.toLowerCase());
        }
    }

    public synchronized ClienteHandler buscar(String apelido) {
        if (apelido == null) {
            return null;
        }
        return usuarios.get(apelido.toLowerCase());
    }

    public synchronized List<String> listarApelidos() {
        List<String> nomes = new ArrayList<>();
        for (ClienteHandler h : usuarios.values()) {
            nomes.add(h.getApelido());
        }
        return nomes;
    }

    /**
     * Envia a mensagem para todos (menos para "excluir", se informado).
     * Copia a lista dentro do synchronized e envia fora dele, para que um
     * cliente lento não trave o acesso de todas as outras threads ao mapa.
     */
    public void enviarParaTodos(Mensagem msg, String excluir) {
        List<ClienteHandler> copia;
        synchronized (this) {
            copia = new ArrayList<>(usuarios.values());
        }
        for (ClienteHandler h : copia) {
            if (excluir != null && excluir.equalsIgnoreCase(h.getApelido())) {
                continue;
            }
            h.enviar(msg);
        }
    }
}
