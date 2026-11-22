# DoseVoz na Google Play

A branch de publicação parte da versão `v1.3-testada`. O identificador de produção é `com.lembretemedicamentos.app`. O identificador de teste continua com o sufixo `.teste`. A versão Android preparada para a loja usa a API 36.

## Geração do pacote

Em GitHub Actions, a opção manual `play` gera e verifica um AAB assinado com a chave de upload. A configuração usa os secrets `MEU_REMEDIO_PLAY_KEYSTORE_BASE64`, `MEU_REMEDIO_PLAY_STORE_PASSWORD`, `MEU_REMEDIO_PLAY_KEY_ALIAS` e `MEU_REMEDIO_PLAY_KEY_PASSWORD`. Configure a chave da conta que será responsável pelo app e mantenha a cópia sob controle do titular. O workflow também mantém as opções de APK de teste e APK de atualização assinada.

O link de instalação compartilhado pelo app oficial aponta para a ficha do DoseVoz na Google Play. Ele fica utilizável pelos testadores quando a ficha e a faixa de teste estiverem configuradas. O Familiar continua copiando o código da família separadamente.

## Antes de abrir a distribuição

- Configurar a conta titular, o Play App Signing, a ficha da loja e a faixa de teste. Revalidar o lembrete de voz, a confirmação e o vínculo Familiar–Idoso no pacote da faixa de teste.
- Revisar as regras de acesso do Firestore, incluindo adesão e revogação de membros da família, e preparar a política de privacidade, a declaração de saúde e a seção de segurança dos dados.
- Criar na Play Console a assinatura mensal por família com preço inicial proposto de R$ 9,90. Implementar e testar a compra, a validação do token no servidor, a recuperação em outro aparelho e os estados de renovação, cancelamento e expiração. Os lembretes já sincronizados continuam locais no Idoso. O AAB desta preparação ainda não implementa cobrança.
- Manter o projeto Firebase existente no Spark enquanto o uso couber nas cotas e os recursos da versão permanecerem compatíveis. A validação de assinaturas pode rodar em um backend separado. Se ele for implantado no próprio projeto Firebase mediante Cloud Functions, a implantação exige Blaze. O push de atraso com app Familiar fechado fica fora desta base.
