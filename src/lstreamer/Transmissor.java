/* 
Copyright [2015] [Marcelo Canzian Nunes]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package lstreamer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Transmissor {

    private final int TAMANHO_BUFFER = 500;

    private InetAddress hostnameReceptor;
    private int portaReceptor;
    private DatagramSocket socket;
    private int idConexao;
    private int sequencia;
    private List<Integer> pacotesPerdidos;
    private List<Dados> pacotes;
    private int quantidadePacotesNaLista;
    private int tamanhoLista;
    private int ultimoPacoteOrdenado;
    private int pacing;
    private int numeroDeReenvios;
    private int tentativasDaConexao;
    private String repositorio;
    private String arquivo;
    private DataInputStream entrada;
    private boolean fimArquivo;
    private volatile boolean enviando;
    private volatile boolean houvePerdas;

    /**
     * Essa classe representa o envio de dados para o receptor.
     *
     * Ela faz o envio dos dados de um ponto a outro através de streaming,
     * garantindo a confiabilidade dos dados através da transmissão. O valor
     * padrão da porta a ser utilizada é 49500. O valor padrão do repositório
     * onde se localizam os arquivos que serão transmitidos é o diretório atual
     * do transmissor.
     *
     * @author Marcelo Canzian Nunes
     *
     */
    public Transmissor() throws UnknownHostException {
        this("."+File.separator, 49500);
    }

    /**
     * Essa classe representa o envio de dados para o receptor.
     *
     * Ela faz o envio dos dados de um ponto a outro através de streaming,
     * garantindo a confiabilidade dos dados através da transmissão. O valor
     * padrão da porta a ser utilizada é 49500.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param repositorio o diretorio onde se localizam os arquivos que o
     * transmissor enviará por streaming.
     *
     */
    public Transmissor(String repositorio) throws UnknownHostException {
        this(repositorio, 49500);
    }

    /**
     * Essa classe representa o envio de dados para o receptor.
     *
     * Ela faz o envio dos dados de um ponto a outro através de streaming,
     * garantindo a confiabilidade dos dados através da transmissão.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param repositorio o diretorio onde se localizam os arquivos que o
     * transmissor enviará por streaming.
     * @param porta o número da porta que será usada para a transmissão dos
     * dados.
     *
     */
    public Transmissor(String repositorio, int porta) throws UnknownHostException {
        idConexao = Long.hashCode(System.currentTimeMillis());
        portaReceptor = porta;
        sequencia = 0;
        pacing = 7;
        numeroDeReenvios = 1;
        pacotes = new ArrayList<Dados>();
        quantidadePacotesNaLista = 0;
        fimArquivo = false;
        enviando = false;
        pacotesPerdidos = new ArrayList<Integer>();
        houvePerdas = false;
        ultimoPacoteOrdenado = -1;
        tentativasDaConexao = 5;
        this.repositorio = repositorio;
    }

    /**
     * Altera o pacing, em milisegundos, entre cada pacote durante o envio.
     *
     * Serve para criar um pequeno atraso proposital no envio entre pacotes
     * durante a transmissão, a fim de descongestionar o receptor. Com isso, há
     * uma significativa redução na perda de pacotes durante a transmissão. O
     * pacing deve ser um número maior ou igual a zero. Por padrão ele começa
     * com o valor de 7 milisegundos. Caso desejar, o pacing pode ser retirado
     * alterando o valor para zero, porém ocorrerá uma grande perda de pacotes
     * durante a transmissão.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param pacing o tempo em milisegundos de espaçamento entre o envio dos
     * pacotes.
     *
     */
    public void setPacing(int pacing) {
        if (pacing >= 0) {
            this.pacing = pacing;
        }
    }

    /**
     * Altera o número de vezes que um pacote perdido será reenviado após a
     * detecção da perda.
     *
     * Serve para aumentar as tentativas de reenvio por pacote perdido durante a
     * transmissão. Quanto maior for o valor, maior será as tentativas de
     * reenvio de um único pacote perdido. O valor vem como padrão em 1. Ao
     * aumentar esse valor, a chance de haver muitos pacotes perdidos no
     * receptor diminuem, porém isso faz com que haja mais tráfego na rede e que
     * possa aumentar o tempo de transmissão. O valor de reenvios não pode ser
     * menor ou igual a zero.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param reenvios o número de vezes que um pacote será reenviado.
     *
     */
    public void setNumeroDeReenviosEmPacotesPerdidos(int reenvios) {
        if (reenvios > 0) {
            this.numeroDeReenvios = reenvios;
        }
    }

    /**
     * Altera o número de tentativas para estabelecer e encerrar uma conexão.
     *
     * Serve para aumentar o número as tentativas para o estabelecimento e
     * encerramento de uma conexão. O valor vem como padrão em 5 tentativas. Ao
     * aumentar esse valor, o número de pacotes enviados para encerrar uma
     * conexão aumenta. O valor de tentativas não pode ser menor ou igual a
     * zero.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param tentativas o número de vezes que o Transmissor enviará pacote de
     * encerrar conexão ao Receptor.
     *
     */
    public void setTentativasDaConexao(int tentativas) {
        if (tentativas > 0) {
            this.tentativasDaConexao = tentativas;
        }
    }

    /**
     * Envia um arquivo por streaming.
     *
     * O transmissor fica aguardando por uma conexão. Após a conexão ser
     * estabelecida, o arquivo é dividido em vários buffers, onde cada buffer
     * contém um determinado número de pacotes que serão transmitidos através da
     * rede. O transmissor se encarrega de gerenciar os envios e reenvios de
     * pacotes perdidos.
     *
     * @author Marcelo Canzian Nunes
     *
     * @return retorna true no termino do envio do arquivo e false caso tenha
     * ocorrido alguma falha ao enviar
     *
     */
    public boolean enviar() throws FileNotFoundException, IOException, InterruptedException {
        this.enviando = true;

        try {
            socket = new DatagramSocket(portaReceptor);

            if (!criarConexao()) {
                return false;
            }

            socket.setSoTimeout(3000);

            entrada = new DataInputStream(new BufferedInputStream(new FileInputStream(repositorio + arquivo), Pacote.TAMANHO_MAX_DADOS));
            preencherListaPacotes();

            receberRespostas();
            TimeUnit.MILLISECONDS.sleep(100);

            int ultimoPacoteNaLista = Integer.MIN_VALUE;

            System.out.println("Enviando pacotes ...");
            while (enviando) {
                int i;
                for (i = 0; i < pacotes.size(); i++) {
                    if (houvePerdas) {
                        reenviarPacotesPerdidos();
                    }

                    if (ultimoPacoteNaLista < ultimoPacoteOrdenado) {
                        try {
                            synchronized (this) {
                                socket.send(pacotes.get(i).get());
                            }
                        } catch (IndexOutOfBoundsException e) {
                        }
                        TimeUnit.MILLISECONDS.sleep(pacing);
                    }
                }
            }

        } catch (SocketTimeoutException ex) {
            System.err.println("Ocorreu um erro na conexão.");
            return false;
        }

        enviando = false;
        entrada.close();
        socket.close();

        return true;
    }

    private boolean criarConexao() throws SocketException, UnknownHostException, InterruptedException {
        byte[] buffer = new byte[Pacote.TAMANHO_MAX_DADOS];
        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);

        System.out.println("Aguardando conexão...");
        while (true) {
            try {
                synchronized (this) {
                    socket.receive(resposta);
                }

                if (Pacote.isSolicitarConexao(resposta.getData())) {
                    SolicitarConexao solicitacao = new SolicitarConexao(resposta.getAddress(), resposta.getPort());
                    hostnameReceptor = resposta.getAddress();
                    portaReceptor = resposta.getPort();

                    solicitacao.set(resposta.getData(), resposta.getLength());
                    if (!setConexao(solicitacao)) {
                        System.err.println("Não foi possível realizar a conexão.");
                        return false;
                    }

                    File f = new File(repositorio + solicitacao.getArquivo());
                    int quantidadePacotesPrevistos = (int) Math.ceil(f.length() / (float) TAMANHO_BUFFER);
                    AceitarConexao aceitaConexao = new AceitarConexao(hostnameReceptor, portaReceptor);
                    for (int i = 0; i < tentativasDaConexao; i++) {
                        synchronized (this) {
                            socket.send(aceitaConexao.set(idConexao, quantidadePacotesPrevistos));
                        }
                        TimeUnit.MILLISECONDS.sleep(pacing);
                    }

                    System.out.println("Conexão aceita.");

                    return true;
                }
            } catch (SocketTimeoutException e) {
            } catch (IOException ex) {
                System.err.println("Não foi possível realizar a conexão.");
                return false;
            }
        }

    }

    private boolean setConexao(SolicitarConexao solicitacao) {
        if (solicitacao.getArquivo().length() > 250) {
            System.err.println("O nome do arquivo deve ser menor que 250 caracteres.");
            return false;
        }

        arquivo = solicitacao.getArquivo();
        tamanhoLista = solicitacao.getQuatidadeDeBuffers() * TAMANHO_BUFFER;
        quantidadePacotesNaLista = 0;
        return true;
    }

    private synchronized void preencherListaPacotes() throws IOException {
        if (fimArquivo) {
            return;
        }

        byte[] bytes = new byte[Pacote.TAMANHO_MAX_DADOS];
        int bytesLidos;
        for (int i = quantidadePacotesNaLista; i < tamanhoLista; i++) {
            bytesLidos = entrada.read(bytes);

            if (bytesLidos == -1) {
                fimArquivo = true;
                break;
            }

            Dados pacote = new Dados(hostnameReceptor, portaReceptor);
            pacote.set(idConexao, sequencia, bytes, bytesLidos);

            addPacote(pacote);
        }

    }

    private void receberRespostas() {
        new Thread() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[Relatorio.TAMANHO_MAX];
                    DatagramPacket pacote = new DatagramPacket(buffer, Relatorio.TAMANHO_MAX);

                    while (enviando) {
                        try {
                            socket.receive(pacote);
                            
                            if (Pacote.isEncerarConexao(pacote.getData())) {
                                enviando = false;
                                continue;
                            }

                            Relatorio pacotesPerdidos = new Relatorio(pacote.getAddress(), pacote.getPort());
                            pacotesPerdidos.set(pacote.getData(), pacote.getLength());

                            int numeroSequencia = pacotesPerdidos.getUltimoPacoteOrdenado();
                            if (numeroSequencia > ultimoPacoteOrdenado) {
                                ultimoPacoteOrdenado = numeroSequencia;
                                atualizarListaDePacotes();
                            }

                            if (pacotesPerdidos.getPacotesPerdidos().get(0) != -1) {
                                atualizarListaDePacotesPerdidos(pacotesPerdidos.getPacotesPerdidos());
                                houvePerdas = true;
                            }
                        } catch (SocketTimeoutException e) {
                        } catch (SocketException e) {
                            enviando = false;
                        }
                    }
                } catch (SocketException ex) {
                    enviando = false;
                } catch (IOException ex) {
                    enviando = false;
                }

            }
        }.start();
    }

    private synchronized void reenviarPacotesPerdidos() throws IOException, InterruptedException {
        while (!pacotesPerdidos.isEmpty()) {
            reenviarPacotePerdido();
        }

        atualizarListaDePacotes();

        houvePerdas = false;
    }

    private synchronized void reenviarPacotePerdido() throws IOException, InterruptedException {
        int pacotePerdido;

        try {
            pacotePerdido = pacotesPerdidos.remove(0);

            Dados pacote = pacotes.get(pacotePerdido - pacotes.get(0).getSequencia());

            for (int i = numeroDeReenvios; i > 0; i--) {
                socket.send(pacote.get());
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private synchronized void atualizarListaDePacotes() throws IOException {
        for (int i = pacotes.get(0).getSequencia(); i < ultimoPacoteOrdenado; i++) {
            removePacote(0);
        }

        preencherListaPacotes();
    }

    private synchronized void atualizarListaDePacotesPerdidos(List<Integer> lista) {
        int tamanho = lista.size();

        for (int i = 0; i < tamanho; i++) {
            addPacotePerdido(lista.get(i));
        }
    }

    private synchronized void addPacotePerdido(Integer numeroDeSequencia) {
        pacotesPerdidos.add(numeroDeSequencia);
    }

    private synchronized void addPacote(Dados pacote) {
        pacotes.add(pacote);
        quantidadePacotesNaLista++;
        sequencia++;
    }

    private synchronized void removePacote(int index) {
        if (!pacotes.isEmpty()) {
            pacotes.remove(index);
            quantidadePacotesNaLista--;
        }
    }

}
