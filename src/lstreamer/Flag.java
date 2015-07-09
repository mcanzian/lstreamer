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

enum Flag {
    SOLICITA_CONEXAO(new Byte("0")),
    ACEITA_CONEXAO(new Byte("1")),
    DADOS(new Byte("2")),
    RELATORIO(new Byte("3")),
    ENCERA_CONEXAO(new Byte("4"));
    
    final byte b;

    private Flag(byte b) {
        this.b = b;
    }

    byte getByte() {
        return b;
    }
    
}
