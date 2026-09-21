package cliente;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import protocolo.Mensagem;
import protocolo.TipoMensagem;

public class TelaCliente extends javax.swing.JFrame {

    private ConexaoChat conexao;
    private boolean conectado = false;
    private boolean logado = false;
    private String meuApelido;

    private final DefaultListModel<String> modeloUsuarios = new DefaultListModel<>();
    private final Map<String, StringBuilder> conversas = new HashMap<>();
    private final Map<String, Integer> naoLidas = new HashMap<>();

    private final ConexaoChat.Ouvinte ouvinte = new ConexaoChat.Ouvinte() {
        @Override
        public void aoReceber(Mensagem msg) {
            SwingUtilities.invokeLater(() -> tratarMensagem(msg));
        }

        @Override
        public void aoDesconectar(String motivo) {
            SwingUtilities.invokeLater(() -> {
                conectado = false;
                logado = false;
                JOptionPane.showMessageDialog(TelaCliente.this, motivo);
            });
        }
    };

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(TelaCliente.class.getName());

    public TelaCliente() {
        initComponents();

        listaUsuarios.setModel(modeloUsuarios);
        taSaida.setEditable(false);

        listaUsuarios.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                String usuario = String.valueOf(value);
                int quantidade = naoLidas.getOrDefault(usuario, 0);

                String texto = quantidade > 0
                        ? usuario + " (" + quantidade + ")"
                        : usuario;

                return super.getListCellRendererComponent(
                        list, texto, index, isSelected, cellHasFocus
                );
            }
        });

        setLocationRelativeTo(null);
    }

    private void entrar() {
        String host = tfIpServidor.getText().trim();
        String apelido = jTextField1.getText().trim();

        if (apelido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite um apelido.");
            return;
        }

        int porta;

        try {
            porta = Integer.parseInt(tfPortaServidor.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida.");
            return;
        }

        btnEntrar.setEnabled(false);

        new Thread(() -> {
            try {
                ConexaoChat novaConexao = new ConexaoChat();
                novaConexao.conectar(host, porta);

                SwingUtilities.invokeLater(() -> {
                    conexao = novaConexao;
                    conectado = true;

                    conexao.iniciarRecebimento(ouvinte);

                    conexao.enviar(new Mensagem(
                            TipoMensagem.ENTRAR,
                            apelido,
                            null,
                            null
                    ));
                });

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    btnEntrar.setEnabled(true);
                    JOptionPane.showMessageDialog(
                            this,
                            "Erro ao conectar: " + e.getMessage()
                    );
                });
            }
        }).start();
    }

    private void sair() {
        if (conexao != null) {
            if (logado) {
                conexao.enviar(new Mensagem(
                        TipoMensagem.SAIR,
                        meuApelido,
                        null,
                        null
                ));
            }

            conexao.fechar();
            conexao = null;
        }

        conectado = false;
        logado = false;
        meuApelido = null;

        modeloUsuarios.clear();
        conversas.clear();
        naoLidas.clear();

        taSaida.setText("");
        btnEntrar.setEnabled(true);
    }

    private void enviarMensagemPrivada() {
        if (!logado || conexao == null) {
            JOptionPane.showMessageDialog(this, "Você não está conectado.");
            return;
        }

        String destino = listaUsuarios.getSelectedValue();

        if (destino == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma conversa.");
            return;
        }

        String texto = tfMensagem.getText().trim();

        if (texto.isEmpty()) {
            return;
        }

        Mensagem mensagem;

        if (destino.equals("Todos")) {
            mensagem = new Mensagem(
                    TipoMensagem.TODOS,
                    meuApelido,
                    null,
                    texto
            );
        } else {
            mensagem = new Mensagem(
                    TipoMensagem.PRIVADA,
                    meuApelido,
                    destino,
                    texto
            );
        }

        conexao.enviar(mensagem);

        tfMensagem.setText("");
        tfMensagem.requestFocusInWindow();
    }

    private void adicionarMensagem(
            String usuario,
            String mensagem,
            boolean contarComoNova) {

        conversas.computeIfAbsent(usuario, k -> new StringBuilder())
                .append(mensagem)
                .append("\n");

        String selecionado = listaUsuarios.getSelectedValue();

        if (usuario.equals(selecionado)) {
            taSaida.setText(conversas.get(usuario).toString());
            taSaida.setCaretPosition(taSaida.getDocument().getLength());

        } else if (contarComoNova) {
            naoLidas.merge(usuario, 1, Integer::sum);
            listaUsuarios.repaint();
        }
    }

    private void tratarMensagem(Mensagem msg) {
        switch (msg.getTipo()) {

            case ENTRAR_OK:
                logado = true;
                meuApelido = msg.getDestino();
                btnEntrar.setEnabled(false);

                conexao.enviar(new Mensagem(
                        TipoMensagem.LISTAR,
                        meuApelido,
                        null,
                        null
                ));
                break;

            case LISTA:
                modeloUsuarios.clear();
                modeloUsuarios.addElement("Todos");

                if (msg.getUsuarios() != null) {
                    for (String usuario : msg.getUsuarios()) {
                        if (!usuario.equalsIgnoreCase(meuApelido)) {
                            modeloUsuarios.addElement(usuario);
                        }
                    }
                }
                break;

            case TODOS:
                boolean mensagemMinha
                        = meuApelido != null
                        && meuApelido.equalsIgnoreCase(msg.getOrigem());

                String textoGeral;

                if (mensagemMinha) {
                    textoGeral = "Você: " + msg.getMensagem();
                } else {
                    textoGeral = msg.getOrigem() + ": " + msg.getMensagem();
                }

                adicionarMensagem("Todos", textoGeral, !mensagemMinha);
                break;

            case PRIVADA:
                boolean fuiEu
                        = meuApelido != null
                        && meuApelido.equalsIgnoreCase(msg.getOrigem());

                String contato;

                if (fuiEu) {
                    contato = msg.getDestino();
                } else {
                    contato = msg.getOrigem();
                }

                String texto;

                if (fuiEu) {
                    texto = "Você: " + msg.getMensagem();
                } else {
                    texto = msg.getOrigem() + ": " + msg.getMensagem();
                }

                adicionarMensagem(contato, texto, !fuiEu);
                break;

            case AVISO:
                adicionarMensagem(
                        "Todos",
                        "*** " + msg.getMensagem(),
                        false
                );

                if (logado) {
                    conexao.enviar(new Mensagem(
                            TipoMensagem.LISTAR,
                            meuApelido,
                            null,
                            null
                    ));
                }

                break;

            case ERRO:
                JOptionPane.showMessageDialog(this, msg.getMensagem());

                if (!logado) {
                    btnEntrar.setEnabled(true);
                }

                break;

            default:
                break;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        tfIpServidor = new javax.swing.JTextField();
        tfPortaServidor = new javax.swing.JTextField();
        jTextField1 = new javax.swing.JTextField();
        btnEntrar = new javax.swing.JButton();
        btnSair = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        taSaida = new javax.swing.JTextArea();
        btnListarUsuarios = new javax.swing.JButton();
        scrollPaneConversas = new javax.swing.JPanel();
        scrollLista = new javax.swing.JScrollPane();
        listaUsuarios = new javax.swing.JList<>();
        tfMensagem = new javax.swing.JTextField();
        btnEnviarMensagem = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Servidor:");

        jLabel2.setText("Porta:");

        jLabel3.setText("Apelido:");

        tfIpServidor.setText("localhost");

        tfPortaServidor.setText("5000");

        jTextField1.setColumns(10);
        jTextField1.addActionListener(this::jTextField1ActionPerformed);

        btnEntrar.setText("Entrar");
        btnEntrar.addActionListener(this::btnEntrarActionPerformed);

        btnSair.setText("Sair");
        btnSair.addActionListener(this::btnSairActionPerformed);

        taSaida.setColumns(20);
        taSaida.setRows(5);
        taSaida.setBorder(javax.swing.BorderFactory.createTitledBorder("Respostas"));
        jScrollPane1.setViewportView(taSaida);

        btnListarUsuarios.setText("Listar Usúarios");
        btnListarUsuarios.addActionListener(this::btnListarUsuariosActionPerformed);

        scrollPaneConversas.setBorder(javax.swing.BorderFactory.createTitledBorder("Conversas"));

        listaUsuarios.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listaUsuarios.addListSelectionListener(this::listaUsuariosValueChanged);
        scrollLista.setViewportView(listaUsuarios);

        javax.swing.GroupLayout scrollPaneConversasLayout = new javax.swing.GroupLayout(scrollPaneConversas);
        scrollPaneConversas.setLayout(scrollPaneConversasLayout);
        scrollPaneConversasLayout.setHorizontalGroup(
            scrollPaneConversasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scrollPaneConversasLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollLista, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                .addContainerGap())
        );
        scrollPaneConversasLayout.setVerticalGroup(
            scrollPaneConversasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollPaneConversasLayout.createSequentialGroup()
                .addComponent(scrollLista, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                .addContainerGap())
        );

        btnEnviarMensagem.setText("Enviar");
        btnEnviarMensagem.addActionListener(this::btnEnviarMensagemActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(tfMensagem)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tfIpServidor, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tfPortaServidor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(39, 39, 39)
                        .addComponent(btnEntrar)
                        .addGap(18, 18, 18)
                        .addComponent(btnSair, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(scrollPaneConversas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(btnEnviarMensagem)
                        .addComponent(btnListarUsuarios, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(tfIpServidor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tfPortaServidor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEntrar)
                    .addComponent(btnSair))
                .addGap(31, 31, 31)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(scrollPaneConversas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnListarUsuarios))
                    .addComponent(jScrollPane1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfMensagem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEnviarMensagem))
                .addContainerGap(39, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void listaUsuariosValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listaUsuariosValueChanged
        if (evt.getValueIsAdjusting()) {
            return;
        }

        String usuario
                = listaUsuarios.getSelectedValue();

        if (usuario == null) {
            return;
        }

        // Ao abrir a conversa, considera as mensagens como lidas
        naoLidas.remove(usuario);

        listaUsuarios.repaint();

        StringBuilder historico
                = conversas.get(usuario);

        if (historico == null) {

            taSaida.setText("");

        } else {

            taSaida.setText(
                    historico.toString()
            );
        }

        taSaida.setCaretPosition(
                taSaida.getDocument().getLength()
        );

    }//GEN-LAST:event_listaUsuariosValueChanged

    private void btnEntrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEntrarActionPerformed
        entrar();
    }//GEN-LAST:event_btnEntrarActionPerformed

    private void btnSairActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSairActionPerformed
        sair();
    }//GEN-LAST:event_btnSairActionPerformed

    private void btnListarUsuariosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnListarUsuariosActionPerformed

        if (!logado || conexao == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Entre no chat primeiro."
            );
            return;
        }

        conexao.enviar(
                new Mensagem(
                        TipoMensagem.LISTAR,
                        meuApelido,
                        null,
                        null
                )
        );
    }//GEN-LAST:event_btnListarUsuariosActionPerformed

    private void btnEnviarMensagemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarMensagemActionPerformed
        enviarMensagemPrivada();
    }//GEN-LAST:event_btnEnviarMensagemActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new TelaCliente().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEntrar;
    private javax.swing.JButton btnEnviarMensagem;
    private javax.swing.JButton btnListarUsuarios;
    private javax.swing.JButton btnSair;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JList<String> listaUsuarios;
    private javax.swing.JScrollPane scrollLista;
    private javax.swing.JPanel scrollPaneConversas;
    private javax.swing.JTextArea taSaida;
    private javax.swing.JTextField tfIpServidor;
    private javax.swing.JTextField tfMensagem;
    private javax.swing.JTextField tfPortaServidor;
    // End of variables declaration//GEN-END:variables
}
