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

class SolicitarConexao extends Pacote {

    static final int TAMANHO_MAX = Byte.BYTES+Integer.BYTES+Pacote.TAMANHO_MAX_DADOS;
    
    private int quantidadeDeBuffers; 
    private String arquivo;
    
    SolicitarConexao(InetAddress HOSTNAME, int PORTA) {
        super(HOSTNAME, PORTA, Flag.SOLICITA_CONEXAO.getByte());
    }

    int getQuatidadeDeBuffers() {
        return quantidadeDeBuffers;
    }

    void setQuatidadeDeBuffers(int quatidadePacotes) {
        this.quantidadeDeBuffers = quatidadePacotes;
    }
    
    String getArquivo() {
        return arquivo;
    }

    void setArquivo(String arquivo) {
        this.arquivo = arquivo;
    }

    DatagramPacket set(String arquivo, int quantidadeDeBuffers) {
        int bytesArquivo = arquivo.getBytes().length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(Byte.BYTES+Integer.BYTES+bytesArquivo);
        byteBuffer.clear();
        
        byteBuffer.put(Flag.SOLICITA_CONEXAO.getByte());
        byteBuffer.putInt(quantidadeDeBuffers);
        byteBuffer.put(arquivo.getBytes());
        
        byte[] bytes = byteBuffer.array();
        
        this.tamanho = bytes.length;
        
        DatagramPacket dp = new DatagramPacket(bytes, tamanho, HOSTNAME, PORTA);
        
        return dp;
    }
    
    void set(byte[] dados, int tamanhoDados) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(dados, 0, tamanhoDados);
        byteBuffer.clear();
        
        flag = byteBuffer.get();
        quantidadeDeBuffers = byteBuffer.getInt();
        
        int bytesRestantes = tamanhoDados - (Byte.BYTES+Integer.BYTES);
        byte[] bytes = new byte[bytesRestantes];
        byteBuffer.get(bytes);
        
        this.tamanho = bytes.length;
        this.arquivo = new String(bytes);
    }   
    
}
