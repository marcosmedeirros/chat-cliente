package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Usuario;

public interface UDPServiceUsuarioListener {

    /**
     * @param usuario
     */
    void usuarioAdicionado(Usuario usuario);

    /**
     * @param usuario
     */
    void usuarioRemovido(Usuario usuario);

    /**
     * @param usuario
     */
    void usuarioAlterado(Usuario usuario);

}
