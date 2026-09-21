package protocolo;

import java.util.List;

public class Mensagem {
    private TipoMensagem tipo;
    private String origem;
    private String destino;
    private String mensagem;
    private List<String> usuarios; // usado somente em LISTA

    public Mensagem() {
    }

    public Mensagem(TipoMensagem tipo, String origem, String destino, String mensagem) {
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
        this.mensagem = mensagem;
    }

    public TipoMensagem getTipo() { return tipo; }
    public void setTipo(TipoMensagem tipo) { this.tipo = tipo; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public List<String> getUsuarios() { return usuarios; }
    public void setUsuarios(List<String> usuarios) { this.usuarios = usuarios; }
}
