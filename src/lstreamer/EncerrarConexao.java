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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class EncerrarConexao extends Pacote {

    static final int TAMANHO_MAX = (Byte.BYTES+Integer.BYTES);
    
    private int idConexao;
    
    EncerrarConexao(InetAddress HOSTNAME, int PORTA) {
        super(HOSTNAME, PORTA, Flag.ENCERA_CONEXAO.getByte());
    }

    int getIdConexao() {
        return idConexao;
    }

    void setIdConexao(int idConexao) {
        this.idConexao = idConexao;
    }

    DatagramPacket set(int idConexao) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(TAMANHO_MAX);
        byteBuffer.put(Flag.ENCERA_CONEXAO.getByte());
        byteBuffer.putInt(idConexao);
        
        byte[] bytes = byteBuffer.array();
        
        this.tamanho = bytes.length;
        this.idConexao = idConexao;

        DatagramPacket dp = new DatagramPacket(bytes, bytes.length, HOSTNAME, PORTA);
        
        return dp;
    }
        
}
