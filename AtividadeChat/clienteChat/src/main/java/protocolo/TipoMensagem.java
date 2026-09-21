package protocolo;

public enum TipoMensagem {
    ENTRAR,     // cliente -> servidor: pede login (origem = apelido)
    ENTRAR_OK,  // servidor -> cliente: login aceito
    ERRO,       // servidor -> cliente: apelido em uso, destino inexistente etc.
    TODOS,      // mensagem para todos os usuários
    PRIVADA,    // mensagem para um usuário específico (destino)
    LISTAR,     // cliente -> servidor: pede a lista de logados
    LISTA,      // servidor -> cliente: devolve os logados em "usuarios"
    SAIR,       // cliente -> servidor: quer sair do chat
    AVISO       // servidor -> todos: "fulano entrou/saiu"
}
