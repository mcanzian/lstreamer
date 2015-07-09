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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class Relatorio extends Pacote {

    static final int TAMANHO_MAX = Byte.BYTES+(2*Integer.BYTES)+Pacote.TAMANHO_MAX_DADOS;
    
    private int idConexao;
    private int ultimoPacoteOrdenado;
    private List<Integer> pacotesPerdidos;
    
    Relatorio(InetAddress HOSTNAME, int PORTA) {
        super(HOSTNAME, PORTA, Flag.RELATORIO.getByte());
    }

    int getIdConexao() {
        return idConexao;
    }

    void setIdConexao(int idConexao) {
        this.idConexao = idConexao;
    }

    int getUltimoPacoteOrdenado() {
        return ultimoPacoteOrdenado;
    }

    void setUltimoPacoteOrdenado(int ultimoPacoteOrdenado) {
        this.ultimoPacoteOrdenado = ultimoPacoteOrdenado;
    }

    List<Integer> getPacotesPerdidos() {
        return pacotesPerdidos;
    }

    void set(byte[] dados, int tamanhoDados) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(dados, 0, tamanhoDados);
        byteBuffer.clear();
        
        flag = byteBuffer.get();
        idConexao = byteBuffer.getInt();
        ultimoPacoteOrdenado = byteBuffer.getInt();
        
        pacotesPerdidos = new ArrayList<Integer>();
        while (byteBuffer.position() < tamanhoDados) {
            pacotesPerdidos.add(byteBuffer.getInt());
            int a = 1;
        }
    }

}
