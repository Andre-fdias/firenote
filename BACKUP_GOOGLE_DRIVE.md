# Backup no Google Drive - Fire Notes V6.0

Este guia detalha a arquitetura de backups locais e a integração com o Google Drive.

## Escopo e Permissões do Google Sign-In
- A conexão utiliza o Google Sign-In com escopo exclusivo para a pasta oculta do aplicativo (`appDataFolder`).
- O aplicativo nunca tem acesso aos arquivos pessoais ou e-mails do usuário, garantindo a privacidade e conformidade com a LGPD.

## Compactação e Envio
- Ao acionar o backup, o banco de dados encriptado SQLite, todas as imagens e o arquivo `config.json` são compactados em um arquivo ZIP com nomenclatura `Backup_AAAA_MM_DD_HH_MM.zip`.
- O arquivo é enviado via requisições HTTP REST diretas utilizando a credencial OAuth2 do Google Play Services.

## Restauração
- A tela de restauração lê os arquivos zip salvos no AppData do Drive do usuário.
- Ao selecionar, é feito o download, descompactação sobre os arquivos locais e a reinicialização segura do processo do aplicativo.
