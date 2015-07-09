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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

class Buffer {

    private final int TAMANHO;

    private List<Dados> pacotes;
    private int quantidadePacotesNaLista;
    private long timeout;
    private long tempoParaTimeout;
    private int IdConexao;
    private InetAddress hostname;
    private int porta;

    Buffer(int tamanho) throws UnknownHostException {
        this.TAMANHO = tamanho;
        this.quantidadePacotesNaLista = 0;
        this.pacotes = new ArrayList<Dados>();
        this.tempoParaTimeout = 3000;

        for (int i = 0; i < TAMANHO; i++) {
            this.pacotes.add(null);
        }
    }

    List<Dados> getPacotes() {
        return pacotes;
    }

    int getQuantidadeDePacotesNoBuffer() {
        return quantidadePacotesNaLista;
    }

    int getTamanho() {
        return TAMANHO;
    }

    int getIdConexao() {
        return IdConexao;
    }
    
    InetAddress getHostname() {
        return hostname;
    }

    int getPorta() {
        return porta;
    }
    
    List<Integer> getPacotesPerdidos(int key) {
        List<Integer> pacotesPerdidos = new ArrayList<Integer>();
        for (int i = 0; i < pacotes.size(); i++) {
            if (pacotes.get(i) == null) {
                pacotesPerdidos.add(key + i);
            }
        }

        return pacotesPerdidos;
    }

    void setTimeout(long timeout) {
        if (timeout < 0)
            return;
        
        this.tempoParaTimeout = timeout;
    }

    void iniciarTimeout() {
        this.timeout = System.currentTimeMillis() + tempoParaTimeout;
    }
    
    boolean isTimeouted() {
        return (timeout <= System.currentTimeMillis());
    }

    boolean isCheio() {
        return quantidadePacotesNaLista == TAMANHO;
    }

    synchronized boolean set(int index, Dados pacote) {
        if (quantidadePacotesNaLista >= TAMANHO || index >= TAMANHO || index < 0) {
            return false;
        }

        if (pacotes.get(index) != null) {
            return false;
        }

        if (quantidadePacotesNaLista == 0) {
            iniciarTimeout();
            hostname = pacote.getHostname();
            porta = pacote.getPorta();
            IdConexao = pacote.getIdConexao();
        }

        pacotes.set(index, pacote);
        quantidadePacotesNaLista++;

        return true;
    }

    boolean remove(int index) {
        if (pacotes.isEmpty()) {
            return false;
        }

        pacotes.set(index, null);
        quantidadePacotesNaLista--;

        return true;
    }

}
