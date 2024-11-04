package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Mensagem;
import br.ufsm.poli.csi.redes.model.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;

public class UDPServiceImpl implements UDPService {

    private Usuario meuUsuario;
    private Map<Usuario, Usuario> usuarios = new HashMap<>();

    private class RecebeUDP implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket socket = new DatagramSocket(8080);
            while (true) {
                DatagramPacket pacoteUDP = new DatagramPacket(new byte[1024], 1024);
                socket.receive(pacoteUDP);
                String strPacote = new String(pacoteUDP.getData(), 0, pacoteUDP.getLength(), "UTF-8");
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Mensagem mensagem = mapper.readValue(strPacote, Mensagem.class);

                    if (meuUsuario == null) {
                        continue;
                    }
                    if (mensagem.getUsuario().equals(meuUsuario.getNome()) && "sonda".equals(mensagem.getTipoMensagem())) {
                        continue;
                    }

                    Usuario usuario = new Usuario(mensagem.getUsuario(), pacoteUDP.getAddress(),
                            Usuario.StatusUsuario.valueOf(mensagem.getStatus()), new Date());

                    usuario.setUltimaSondaRecebida(new Date());

                    if (usuarios.containsKey(usuario)) {
                        Usuario usuarioExistente = usuarios.get(usuario);
                        usuarioExistente.setStatus(usuario.getStatus());
                    } else {
                        if (usuarioListener != null) {
                            usuarioListener.usuarioAdicionado(usuario);
                        }
                        usuarios.put(usuario, usuario);
                    }

                    switch (mensagem.getTipoMensagem()) {
                        case "msg_individual":
                            if (mensagemListener != null) {
                                mensagemListener.mensagemRecebida(mensagem.getMsg(), usuario, false);
                            }
                            break;
                        case "msg_grupo":
                            if (mensagemListener != null) {
                                mensagemListener.mensagemRecebida(mensagem.getMsg(), usuario, true);
                            }
                            break;
                        case "fim_chat":
                            if (mensagemListener != null) {
                                mensagemListener.mensagemRecebida("Chat encerrado pelo outro usuário.", usuario, false);
                            }
                            break;
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    private class EnviaMensagemUDP implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                if (meuUsuario != null && meuUsuario.getNome() != null) {
                    String ip = "192.168.0.";
                    for (int i = 1; i < 255; i++) {
                        DatagramSocket socketUDP = new DatagramSocket();
                        try {
                            Mensagem mensagem = new Mensagem("sonda", meuUsuario.getNome(),
                                    meuUsuario.getStatus().toString(), null);
                            String strPacote = new ObjectMapper().writeValueAsString(mensagem);
                            byte[] bytes = strPacote.getBytes();
                            DatagramPacket pacoteUDP = new DatagramPacket(bytes, bytes.length,
                                    InetAddress.getByName(ip + i), 8080);
                            socketUDP.send(pacoteUDP);
                        } finally {
                            socketUDP.close();
                        }
                    }
                }
                Thread.sleep(5000);
            }
        }
    }


    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) {
        try {
            Mensagem msg = new Mensagem(chatGeral ? "msg_grupo" : "msg_individual",
                    meuUsuario.getNome(), meuUsuario.getStatus().toString(), mensagem);

            if ("fim_chat".equals(mensagem)) {
                msg.setTipoMensagem("fim_chat");
            }

            String strPacote = new ObjectMapper().writeValueAsString(msg);
            byte[] bytes = strPacote.getBytes();

            if (chatGeral) {
                Set<InetAddress> enviados = new HashSet<>();
                for (Usuario usuario : usuarios.values()) {
                    if (!enviados.contains(usuario.getEndereco())) {
                        DatagramPacket pacoteUDP = new DatagramPacket(bytes, bytes.length, usuario.getEndereco(), 8080);
                        try (DatagramSocket socketUDP = new DatagramSocket()) {
                            socketUDP.send(pacoteUDP);
                        }
                        enviados.add(usuario.getEndereco());
                        System.out.println("[ENVIA MENSAGEM GERAL] " + mensagem + " --> " + usuario.getNome() + "/" +
                                usuario.getEndereco().getHostAddress());
                    }
                }
            } else {
                DatagramPacket pacoteUDP = new DatagramPacket(bytes, bytes.length, destinatario.getEndereco(), 8080);
                try (DatagramSocket socketUDP = new DatagramSocket()) {
                    socketUDP.send(pacoteUDP);
                }
                System.out.println("[ENVIA MENSAGEM INDIVIDUAL] " + mensagem + " --> " + destinatario.getNome() + "/" +
                        destinatario.getEndereco().getHostAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removerUsuariosInativos() {
        long agora = System.currentTimeMillis();
        long timeout = 30000;

        usuarios.entrySet().removeIf(entry -> {
            Usuario usuario = entry.getKey();
            boolean inativo = (agora - usuario.getUltimaSondaRecebida().getTime()) > timeout;
            if (inativo && usuarioListener != null) {
                usuarioListener.usuarioRemovido(usuario);
            }
            return inativo;
        });
    }


    private class VerificaUsuariosInativos implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                removerUsuariosInativos();
                Thread.sleep(5000);
            }
        }
    }





    @Override
    public void usuarioAlterado(Usuario usuario) {
        this.meuUsuario = usuario;
    }

    private UDPServiceMensagemListener mensagemListener;
    @Override
    public void addListenerMensagem(UDPServiceMensagemListener listener) {
        this.mensagemListener = listener;
    }

    private UDPServiceUsuarioListener usuarioListener;
    @Override
    public void addListenerUsuario(UDPServiceUsuarioListener listener) {
        this.usuarioListener = listener;
    }

    public UDPServiceImpl() {
        new Thread(new EnviaMensagemUDP()).start();
        new Thread(new RecebeUDP()).start();
        new Thread(new VerificaUsuariosInativos()).start();

    }
}
