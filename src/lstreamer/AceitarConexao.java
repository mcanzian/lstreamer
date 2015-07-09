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

class AceitarConexao extends Pacote {

    static final int TAMANHO_MAX = (Byte.BYTES+(2*Integer.BYTES));
    
    private int idConexao;
    private int quantidadePacotesPrevistos;
    
    AceitarConexao(InetAddress hostname, int porta) {
        super(hostname, porta, Flag.ACEITA_CONEXAO.getByte());
    }

    int getIdConexao() {
        return idConexao;
    }

    void setIdConexao(int idConexao) {
        this.idConexao = idConexao;
    }

    public int getQuantidadePacotesPrevistos() {
        return quantidadePacotesPrevistos;
    }

    public void setQuantidadePacotesPrevistos(int quantidadePacotesPrevistos) {
        this.quantidadePacotesPrevistos = quantidadePacotesPrevistos;
    }
    
    DatagramPacket set(int idConexao, int quantidadePacotesPrevistos) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(TAMANHO_MAX);
        byteBuffer.put(Flag.ACEITA_CONEXAO.getByte());
        byteBuffer.putInt(idConexao);
        byteBuffer.putInt(quantidadePacotesPrevistos);
        
        byte[] bytes = byteBuffer.array();
        
        this.tamanho = bytes.length;
        this.idConexao = idConexao;
        this.quantidadePacotesPrevistos = quantidadePacotesPrevistos;

        DatagramPacket dp = new DatagramPacket(bytes, bytes.length, HOSTNAME, PORTA);
        
        return dp;
    }
    
    void set(byte[] dados, int tamanhoDados) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(dados, 0, tamanhoDados);
        byteBuffer.clear();
        
        flag = byteBuffer.get();
        idConexao = byteBuffer.getInt();
        quantidadePacotesPrevistos = byteBuffer.getInt();
        
        int bytesRestantes = tamanhoDados - (Byte.BYTES+(2*Integer.BYTES));
        byte[] bytes = new byte[bytesRestantes];
        byteBuffer.get(bytes);
        
        this.tamanho = bytes.length;
    }   
    
}
