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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Receptor {

    private final int TAMANHO_BUFFER = 500;
    private final InetAddress HOSTNAME;
    private final int PORTA;

    private int idConexao;
    private DatagramSocket socket;
    private DatagramSocket socketResposta;
    private Map<Integer, Buffer> buffers;
    private int quantidadeDeBuffers;
    private int bufferASerGravado;
    private int ultimoPacoteOrdenado;
    private int quantidadeTotalDePacotes;
    private int quantidadeDeBuffersEnviandoRelatorio;
    private int tentativasDaConexao;
    private int pacing;
    private long timeout;
    private String diretorio;
    private String arquivo;
    private DataOutputStream saida;
    private volatile boolean recebendo;

    /**
     * Essa classe representa o recebimento de dados através da biblioteca
     * lstreamer.
     *
     * Ela recebe dados da rede e os encaminha a seus respectivos buffers,
     * ordenando-os para poderem ser gravados posteriormente. O valor padrão da
     * porta a ser usada durante a transmissão é 49500.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param hostname endereço ao qual será requisitado a transmissão dos dados
     * pelo transmissor.
     *
     */
    public Receptor(String hostname) throws UnknownHostException {
        this(hostname, 49500);
    }

    /**
     * Essa classe representa o recebimento de dados através da biblioteca
     * lstreamer.
     *
     * Ela recebe dados da rede e os encaminha a seus respectivos buffers,
     * ordenando-os para poderem ser gravados posteriormente.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param hostname endereço ao qual será requisitado a transmissão dos dados
     * pelo transmissor.
     * @param porta o número da porta que será usada para a transmissão dos
     * dados pelo transmissor.
     *
     */
    public Receptor(String hostname, int porta) throws UnknownHostException {
        this.HOSTNAME = InetAddress.getByName(hostname);
        this.PORTA = porta;
        this.buffers = new ConcurrentHashMap<Integer, Buffer>();
        this.quantidadeDeBuffers = 30;
        this.bufferASerGravado = 0;
        this.ultimoPacoteOrdenado = 0;
        this.quantidadeDeBuffersEnviandoRelatorio = quantidadeDeBuffers;
        this.tentativasDaConexao = 5;
        this.pacing = 7;
        this.timeout = 3000;
        this.recebendo = false;

    }

    /**
     * Retorna o nome do arquivo a ser recebido durante a transmissão.
     *
     * @author Marcelo Canzian Nunes
     *
     * @return retorna o nome do arquivo a ser recebido.
     *
     */
    public String getNomeArquivo() {
        return arquivo;
    }

    /**
     * Mostra a porcentagem de quanto já foi baixado do arquivo transmitido.
     *
     * @author Marcelo Canzian Nunes
     *
     * @return a porcentagem já concluida do arquivo transmitido.
     *
     */
    public synchronized float getPorcentagemDeConclusao() {
        float resultado;

        if (quantidadeTotalDePacotes == 0) {
            return 0;
        }

        resultado = (ultimoPacoteOrdenado * 100) / (float) quantidadeTotalDePacotes;

        return resultado;
    }

    /**
     * Altera a quantidade de buffers a serem utilizados pelo receptor.
     *
     * O receptor organiza os pacotes recebidos em buffers. Quanto mais buffers,
     * mais pacotes poderão ser recebidos, podendo agilizar a gravação dos
     * pacotes no caso de muitas perdas durante a transmissão. Porém quanto mais
     * buffers, mais recursos são usados pelo receptor. O receptor vem com 30
     * buffers por padrão, onde o número a ser alterado deverá ser maior que
     * zero.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param quantidade quantidade de buffers a serem utilizados pelo receptor.
     *
     */
    public void setQuantidadeBuffers(int quantidade) {
        if (quantidade < 1) {
            return;
        }

        quantidadeDeBuffers = quantidade;
    }

    /**
     * Altera o tempo de timeout de cada buffer do receptor.
     *
     * O receptor organiza os pacotes recebidos em buffers. Quando um buffer
     * fica um determinado tempo esperando para ser completo e ainda não está
     * pronto para ser gravado ocorre um timeout. O timeout faz com que o
     * receptor informe ao transmissor que alguns pacotes foram perdidos durante
     * a transmissão, deixando a cargo do transmissor o reenvio destes pacotes.
     * O timeout vem com o valor padrão de 3000 milisegundos, não podendo ser um
     * valor menor que zero.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param timeout tempo decorrido em milissegundos para que ocorra o
     * timeout.
     *
     */
    public void setTimeout(long timeout) {
        if (timeout >= 0) {
            this.timeout = timeout;
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
     * @param tentativasDaConexao número de tentativas para estabelecer e
     * encerrar uma conexão.
     *
     */
    public void setTentativasDaConexao(int tentativasDaConexao) {
        if (tentativasDaConexao > 0) {
            this.tentativasDaConexao = tentativasDaConexao;
        }
    }

    /**
     * Altera o número máximo de buffers enviando relatórios ao transmissor.
     *
     * Altera o número máximo de buffers enviando relatórios ao transmissor. Os
     * buffers permitidos a enviar relatórios são contados a partir do primeiro
     * buffers a ser gravado, dando prioridade a ordem de gravação dos buffers.
     * Caso o valor informado seja de zero, não haverá limitação de buffers
     * enviando relatórios. O valor padrão é ilimitado.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param quantidadeDeBuffers número de tentativas para estabelecer e
     * encerrar uma conexão.
     *
     */
    public void setQuantidadeDeBuffersEnviandoRelatorio(int quantidadeDeBuffers) {
        if (quantidadeDeBuffers < 0) {
            return;
        }

        if (quantidadeDeBuffers == 0) {
            quantidadeDeBuffers = this.quantidadeDeBuffers;
        } else {
            quantidadeDeBuffersEnviandoRelatorio = quantidadeDeBuffers;
        }
    }

    /**
     * Altera o pacing, em milisegundos, entre cada pacote durante o envio.
     *
     * Serve para criar um pequeno atraso proposital no envio entre pacotes
     * durante a transmissão, a fim de descongestionar o transmissor. Com isso,
     * há uma significativa redução na perda de pacotes durante a transmissão. O
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
     * Recebe um arquivo através de streaming.
     *
     * O receptor receberá vários pacotes, que serão armazenados em seus
     * respectivos buffers. Quando um buffer estiver concluido, este será
     * gravado no diretório indicado. O receptor se encarrega de gerenciar os
     * pedidos e a gravação do arquivo no diretório determinado.
     *
     * @author Marcelo Canzian Nunes
     *
     * @param arquivoASerRecebido o nome do arquivo a ser trnasmitido atraves do
     * transmissor.
     * @param diretorioASerGravado o diretorio em que o arquivo sera gravado.
     *
     */
    public void receber(final String arquivoASerRecebido, final String diretorioASerGravado) {
        new Thread() {
            @Override
            public void run() {
                diretorio = diretorioASerGravado;
                arquivo = arquivoASerRecebido;
                recebendo = true;

                try {
                    socket = new DatagramSocket();
                    socket.setSoTimeout(((int) timeout) * 3);

                    if (!criarConexao(arquivoASerRecebido)) {
                        System.err.println("Não foi possivel estabelecer uma conexão.");
                        socket.close();
                        return;
                    }
                    System.out.println(arquivo);

                    enviarRespostas();

                    byte[] buffer = new byte[Dados.TAMANHO_MAX];
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

                    System.out.println("Recebendo pacotes ...");
                    while (recebendo) {
                        try {
                            socket.receive(pacote);

                            if (!Pacote.isDados(pacote.getData())) {
                                continue;
                            }

                            Dados p = new Dados(pacote.getAddress(), pacote.getPort());
                            p.set(pacote.getData(), pacote.getLength());

                            if (p.getIdConexao() != idConexao) {
                                continue;
                            }

                            setPacoteNoBuffer(p);
                        } catch (SocketTimeoutException e) {
                        } catch (UnknownHostException ex) {
                            System.err.println("Ocorreu um erro na conexão.");
                        } catch (IOException ex) {
                            System.err.println("Ocorreu um erro na conexão.");
                        }

                    }
                } catch (SocketException ex) {
                    System.err.println("Ocorreu um erro na conexão.");
                } catch (IOException ex) {
                    System.err.println("Ocorreu um erro na conexão.");
                } finally {
                    socket.close();
                    System.out.println("Transmissao finalizada.");
                }
            }
        }.start();
    }

    private boolean criarConexao(String arquivoASerRecebido) throws IOException {
        byte[] buffer = new byte[Pacote.TAMANHO_MAX_DADOS];
        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
        SolicitarConexao pedidoSolicitacao = new SolicitarConexao(HOSTNAME, PORTA);

        System.out.println("Tentando estabelecer conexão ...");

        int tentativas = tentativasDaConexao;
        while (tentativas > 0) {
            try {
                socket.send(pedidoSolicitacao.set(
                	arquivoASerRecebido, quantidadeDeBuffers));
                socket.receive(resposta);

                if (!Pacote.isAceitarConexao(resposta.getData())) {
                    continue;
                }

                AceitarConexao aceitou = new AceitarConexao(resposta.getAddress(), resposta.getPort());
                aceitou.set(resposta.getData(), resposta.getLength());
                idConexao = aceitou.getIdConexao();
                quantidadeTotalDePacotes = aceitou.getQuantidadePacotesPrevistos();

                System.out.println("");
                System.out.println("Conexão aceita.");

                return true;
            } catch (SocketTimeoutException e) {
                tentativas--;
            }
        }

        System.out.println("");
        return false;
    }

    private synchronized void setPacoteNoBuffer(Dados pacote) throws UnknownHostException {
        int posicao = pacote.getSequencia() % TAMANHO_BUFFER;
        int key = pacote.getSequencia() - posicao;

        Buffer buffer;
        if (buffers.containsKey(key)) {
            buffer = buffers.get(key);
            buffer.set(posicao, pacote);
        } else {
            if (key >= bufferASerGravado) {
                int tamanho;

                if ((quantidadeTotalDePacotes - key) < TAMANHO_BUFFER) {
                    tamanho = (quantidadeTotalDePacotes - key);
                } else {
                    tamanho = TAMANHO_BUFFER;
                }

                buffer = new Buffer(tamanho);
                buffer.setTimeout(timeout);
                buffers.put(key, buffer);
                buffer.set(posicao, pacote);
            }
        }
    }

    private void enviarRespostas() {
        new Thread() {
            @Override
            public void run() {
                long tempo = System.currentTimeMillis();

                try {
                    socketResposta = new DatagramSocket();

                    String novoArquivo = ((diretorio.trim()) + (arquivo.trim()) + ".temp");
                    saida = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(novoArquivo)), Pacote.TAMANHO_MAX_DADOS));

                    Integer key;
                    Buffer buffer;

                    while (recebendo) {
                        if (bufferASerGravado > quantidadeTotalDePacotes) {
                            encerrarConexao();
                        }

                        for (Entry<Integer, Buffer> entrySet : buffers.entrySet()) {
                            key = entrySet.getKey();
                            buffer = entrySet.getValue();

                            if (buffer.isCheio() && bufferASerGravado == key) {
                                List<Dados> lista = buffer.getPacotes();
                                int idConexao = buffer.getIdConexao();
                                synchronized (this) {
                                    for (Dados p : lista) {
                                        saida.write(p.getDados(), 0, (p.getTamanho()-p.getTamanhoCabecalho()));

                                    }

                                    buffers.remove(key);

                                    List<Integer> bufferCompleto = new ArrayList<Integer>();
                                    bufferCompleto.add(-1);
                                    enviarRelatorio(bufferCompleto, idConexao);

                                    bufferASerGravado += TAMANHO_BUFFER;
                                    ultimoPacoteOrdenado = bufferASerGravado;
                                }

                            } else if (buffer.isTimeouted()) {
                                List<Integer> pacotesPerdidos = 
                                	buffer.getPacotesPerdidos(key);
                                boolean bufferPodeEnviarRelatorio = key < (bufferASerGravado + (TAMANHO_BUFFER * quantidadeDeBuffersEnviandoRelatorio));

                                int idConexao = buffer.getIdConexao();

                                if (bufferPodeEnviarRelatorio) {
                                    enviarRelatorio(pacotesPerdidos, idConexao);
                                }

                                buffer.iniciarTimeout();
                            }
                        }
                    }

                    saida.flush();
                    saida.close();
                    socketResposta.close();

                    renomearArquivo(novoArquivo);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Receptor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    System.err.println("Ocorreu um erro na conexão.");
                } catch (InterruptedException ex) {
                    System.err.println("Ocorreu um erro na conexão.");
                }
            }
        }.start();
    }

    private synchronized void enviarRelatorio(List<Integer> pacotesPerdidos, int idConexao) throws IOException, InterruptedException {
        int numeroDeRespostas = (int) Math.ceil(pacotesPerdidos.size() / ((double) Pacote.TAMANHO_MAX_DADOS));
        int i, j, quantidadeDados;

        for (i = 0; i < numeroDeRespostas; i++) {
            if (pacotesPerdidos.size() >= (Pacote.TAMANHO_MAX_DADOS / Integer.BYTES)) {
                quantidadeDados = (Pacote.TAMANHO_MAX_DADOS / Integer.BYTES);
            } else {
                quantidadeDados = pacotesPerdidos.size();
            }

            ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES + (2 * Integer.BYTES) + (quantidadeDados * Integer.BYTES));
            bb.put(Flag.RELATORIO.getByte());
            bb.putInt(idConexao);
            bb.putInt(ultimoPacoteOrdenado);

            int item;
            for (j = 0; j < quantidadeDados; j++) {
                item = pacotesPerdidos.remove(0);
                bb.putInt(item);
            }

            byte[] bytes = bb.array();

            socketResposta.send(new DatagramPacket(bytes, bytes.length, HOSTNAME, PORTA));
            TimeUnit.MILLISECONDS.sleep(pacing);
        }
    }

    private void renomearArquivo(String caminho) {
        File arquivo = new File(caminho);

        if (caminho.matches("^.+(\\.temp)$")) {
            String novoNome = caminho.substring(0, caminho.length() - 5);
            arquivo.renameTo(new File(novoNome));
        }
    }

    private synchronized void encerrarConexao() throws InterruptedException {
        EncerrarConexao encerrar = new EncerrarConexao(HOSTNAME, PORTA);

        for (int i = 0; i < tentativasDaConexao; i++) {
            try {
                socket.send(encerrar.set(idConexao));
            } catch (IOException ex) {
            }
            TimeUnit.MILLISECONDS.sleep(pacing);
        }

        ultimoPacoteOrdenado = quantidadeTotalDePacotes;
        recebendo = false;
    }

}


