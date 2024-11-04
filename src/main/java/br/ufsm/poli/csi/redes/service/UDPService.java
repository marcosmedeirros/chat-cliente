package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Usuario;

public interface UDPService {

    /**
     * @param mensagem
     * @param destinatario
     * @param chatGeral
     */
    void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral);

    /**
     * @param usuario
     */
    void usuarioAlterado(Usuario usuario);

    /**
     * @param listener
     */
    void addListenerMensagem(UDPServiceMensagemListener listener);

    /**
     * @param listener
     */
    void addListenerUsuario(UDPServiceUsuarioListener listener);


}
