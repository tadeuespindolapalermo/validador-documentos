# validador-documentos

Aplicacao Spring Boot + Thymeleaf para validar dados cadastrais em RG, CNH ou CIN.

O fluxo atual faz duas validacoes:

- OpenAI extrai nome, data de nascimento e CPF do documento anexado.
- CompreFace compara a foto enviada pelo usuario com a face encontrada no documento.

Para o cadastro ser aprovado, todos os campos textuais e a biometria facial precisam ser confirmados.

## Variaveis de ambiente

Configure pelo PowerShell antes de iniciar a aplicacao:

```powershell
$env:OPENAI_API_KEY="sua-chave-openai"
$env:COMPREFACE_BASE_URL="http://localhost:8000"
$env:COMPREFACE_API_KEY="api-key-do-servico-de-verificacao"
$env:COMPREFACE_THRESHOLD="0.70"
```

`COMPREFACE_THRESHOLD` aceita `0.70` ou `70`. O valor padrao do projeto e `0.70`, ou seja, 70% de similaridade facial.

## Como subir o CompreFace local com Docker

1. Instale Docker Desktop e deixe o Docker rodando.
2. Baixe o arquivo `zip` ou `tar.gz` da release do CompreFace em `https://github.com/exadel-inc/CompreFace/releases`.
3. Extraia o arquivo em uma pasta local.
4. Pelo PowerShell, entre na pasta extraida do CompreFace.
5. Suba os containers:

```powershell
docker compose up -d
```

Se sua instalacao usa o comando antigo:

```powershell
docker-compose up -d
```

6. Aguarde os servicos iniciarem e acesse `http://localhost:8000/login`.
7. Crie o usuario administrador.
8. Crie uma aplicacao no painel do CompreFace.
9. Dentro da aplicacao, crie um servico do tipo `Face Verification`.
10. Copie a API key desse servico e coloque em `COMPREFACE_API_KEY`.

Para conferir os containers:

```powershell
docker compose ps
```

Para acompanhar logs:

```powershell
docker compose logs -f
```

Para parar:

```powershell
docker compose down
```

## Como rodar a aplicacao

Com as variaveis configuradas:

```powershell
mvn spring-boot:run
```

Acesse:

```text
http://localhost:8080
```

## Privacidade e consentimento

A tela exige aceite explicito para a validacao biometrica. A aplicacao nao persiste os arquivos em disco: documento e foto ficam em memoria durante a requisicao.

Em producao, use HTTPS, proteja as chaves em secret manager, defina politica de retencao, audite acessos e revise os avisos/termos conforme LGPD e as regras internas do seu negocio.
