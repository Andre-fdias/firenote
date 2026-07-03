# Criptografia SQLCipher - Fire Notes V6.0

Este documento apresenta a camada de criptografia aplicada no banco de dados local da aplicação.

## Integração do SQLCipher com Room
- Adicionada a biblioteca oficial `net.zetetic:android-database-sqlcipher:4.5.4`.
- Configurado o `SupportOpenHelperFactory` no construtor do banco de dados Room:
```kotlin
val passphrase = "FireNotesSecuredLocalDatabasePassphraseKey123".toByteArray()
val factory = SupportOpenHelperFactory(passphrase)
Room.databaseBuilder(...)
    .openHelperFactory(factory)
    .build()
```
- Toda a leitura e gravação no arquivo SQLite `firenotes.db` ocorre de forma transparente e encriptada. Se o arquivo for extraído do dispositivo, seu conteúdo não poderá ser aberto ou lido em formato texto claro.
