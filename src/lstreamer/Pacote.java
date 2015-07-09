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

abstract class Pacote {

    static final int TAMANHO_MAX_DADOS = 500;

    protected final InetAddress HOSTNAME;
    protected final int PORTA;
    protected int tamanho;
    protected byte flag;

    protected Pacote(InetAddress hostname, int porta, byte flag) {
        this.HOSTNAME = hostname;
        this.PORTA = porta;
        this.flag = flag;
    }

    InetAddress getHostname() {
        return HOSTNAME;
    }

    int getPorta() {
        return PORTA;
    }

    int getTamanho() {
        return tamanho;
    }

    void setTamanho(int tamanho) {
        this.tamanho = tamanho;
    }
    
    static boolean isSolicitarConexao(byte[] dados) {
        return dados[0] == Flag.SOLICITA_CONEXAO.getByte();
    }
    
    static boolean isAceitarConexao(byte[] dados) {
        return dados[0] == Flag.ACEITA_CONEXAO.getByte();
    }
    
    static boolean isDados(byte[] dados) {
        return dados[0] == Flag.DADOS.getByte();
    }
    
    static boolean isRelatorio(byte[] dados) {
        return dados[0] == Flag.RELATORIO.getByte();
    }
    
    static boolean isEncerarConexao(byte[] dados) {
        return dados[0] == Flag.ENCERA_CONEXAO.getByte();
    }
    
}
