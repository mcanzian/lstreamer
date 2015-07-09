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

class Dados extends Pacote {

    static final int TAMANHO_MAX = Byte.BYTES+(2*Integer.BYTES)+Pacote.TAMANHO_MAX_DADOS;
    
    private int idConexao;
    private int sequencia;
    private byte[] dados;
    
    Dados(InetAddress hostname, int porta) {
        super(hostname, porta, Flag.DADOS.getByte());
    }

    int getIdConexao() {
        return idConexao;
    }

    void setIdConexao(int idConexao) {
        this.idConexao = idConexao;
    }

    int getSequencia() {
        return sequencia;
    }

    void setSequencia(int sequencia) {
        this.sequencia = sequencia;
    }

    byte[] getDados() {
        return dados;
    }
    
    int getTamanhoCabecalho() {
        return Byte.BYTES+(2*Integer.BYTES);
    }
    
    DatagramPacket get() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Byte.BYTES+(2*Integer.BYTES)+tamanho);
        byteBuffer.clear();
        
        byteBuffer.put(flag);
        byteBuffer.putInt(idConexao);
        byteBuffer.putInt(sequencia);
        byteBuffer.put(dados, 0, tamanho);
        
        byte[] bytes = byteBuffer.array();
        
        return new DatagramPacket(bytes, bytes.length, HOSTNAME, PORTA);
    }

    void set(int idConexao, int sequencia, byte[] dados, int tamanhoDados) {        
        ByteBuffer byteBuffer = ByteBuffer.allocate(Byte.BYTES+(2*Integer.BYTES)+tamanhoDados);
        byteBuffer.clear();
        
        byteBuffer.put(Flag.DADOS.getByte());
        byteBuffer.putInt(idConexao);
        byteBuffer.putInt(sequencia);
        byteBuffer.put(dados, 0, tamanhoDados);
        
        this.tamanho = tamanhoDados;
        this.idConexao = idConexao;
        this.sequencia = sequencia;
        
        byte[] array = new byte[tamanhoDados];
        
        for (int i = 0; i < tamanhoDados; i++)
            array[i] = dados[i];
        
        this.dados = array;
    }
    
    void set(byte[] dados, int tamanhoDados) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(dados, 0, tamanhoDados);
        byteBuffer.clear();
        
        flag = byteBuffer.get();
        idConexao = byteBuffer.getInt();
        sequencia = byteBuffer.getInt();
        
        int bytesRestantes = tamanhoDados - (Byte.BYTES+(2*Integer.BYTES));
        byte[] bytes = new byte[bytesRestantes];
        byteBuffer.get(bytes, 0, bytes.length);
        
        this.tamanho = tamanhoDados;
        this.dados = bytes;
    }   
    
}
