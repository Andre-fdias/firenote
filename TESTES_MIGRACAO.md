# Plano de Testes de Migração - Fire Notes V6.0

Casos de teste práticos executados para homologação das rotinas locais.

## Casos de Teste

1.  **Testar SQLCipher Criptografia**:
    - Tentar copiar o arquivo `firenotes.db` para o computador e abri-lo usando um leitor SQLite padrão.
    - **Resultado esperado**: O arquivo deve retornar erro de formato inválido/encriptado.

2.  **Imagens Opcionais**:
    - Cadastrar ocorrências, pessoas, documentos e veículos sem capturar imagens.
    - **Resultado esperado**: A validação deve prosseguir normalmente sem erros de formulário.

3.  **Fluxo de Backup e Restauração**:
    - Realizar login com conta Google -> Gerar backup manual -> Verificar arquivo ZIP gerado na pasta do app do Drive -> Executar restauração e confirmar recarregamento correto dos dados e mídias locais.
