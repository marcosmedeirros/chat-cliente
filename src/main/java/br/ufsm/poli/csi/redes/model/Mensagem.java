package br.ufsm.poli.csi.redes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mensagem {
    private String tipoMensagem;
    private String usuario;
    private String status;
    private String msg;
}
