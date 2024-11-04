package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Usuario;

public interface UDPServiceMensagemListener {

    /**
     * @param mensagem
     * @param remetente
     * @param chatGeral
     */


    void mensagemRecebida(String mensagem, Usuario remetente, boolean chatGeral);


}
