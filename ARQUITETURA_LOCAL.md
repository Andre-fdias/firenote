# Arquitetura Local-First - Fire Notes V6.0

Detalhamento da arquitetura offline da aplicação de emergências do corpo de bombeiros.

## Diretórios de Mídia Locais
Em conformidade com a LGPD e restrições de permissão do Android 10+, todas as fotos e mídias de OCR são arquivadas de forma organizada sob a pasta privada da aplicação (`Android/data/<package>/files/`):
- `/documentos`: Fotos de documentos CNH, RG, CIN.
- `/veiculos`: Scans e fotos de CRLV.
- `/evidencias`: Fotos da cena do sinistro.
- `/relatorios`: Exportações de PDFs, CSVs e planilhas Excel.
- `/temp`: Arquivos temporários de processamento.

## Segurança da Aplicação
- **Criptografia SQLCipher**: Banco de dados inteiramente encriptado por chave simétrica.
- **PIN de Acesso**: Bloqueio opcional por código numérico de 4 dígitos verificado na inicialização do app.
- **Autenticação Biométrica**: Utilização da biblioteca do Android para desbloqueio por face ou impressão digital.
