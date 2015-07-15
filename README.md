# lstreamer

Código mínimo para iniciar o servidor:

  Transmissor transmissor = new Transmissor("/repositorio/");
  transmissor.enviar();
  
  
Código mínimo para iniciar o servidor:

  Receptor receptor = new Receptor("127.0.0.1");
  receptor.receber("arquivo.bin", "/tmp/");
