# DoseVoz na Google Play

A branch de publicação parte da versão `v1.3-testada`. O identificador de produção é `com.lembretemedicamentos.app`. O APK 1.4 de teste usa o sufixo `.seguranca` e pode ser instalado ao lado do 1.3 testado. A versão Android preparada para a loja usa a API 36.

## Geração do pacote

Em GitHub Actions, a opção manual `play` gera e verifica um AAB assinado com a chave de upload. A configuração usa os secrets `MEU_REMEDIO_PLAY_KEYSTORE_BASE64`, `MEU_REMEDIO_PLAY_STORE_PASSWORD`, `MEU_REMEDIO_PLAY_KEY_ALIAS` e `MEU_REMEDIO_PLAY_KEY_PASSWORD`. Configure a chave da conta que será responsável pelo app e mantenha a cópia sob controle do titular. O workflow também mantém as opções de APK de teste e APK de atualização assinada.

O link de instalação compartilhado pelo app oficial aponta para a ficha do DoseVoz na Google Play. Ele fica utilizável pelos testadores quando a ficha e a faixa de teste estiverem configuradas. No APK de teste, o botão compartilha o link direto do APK 1.4. O Familiar copia separadamente o novo código de uso único, válido por 24 horas.

## Teste do vínculo

O APK 1.4 usa as coleções novas `familiasDoseVoz` e `convitesDoseVoz`. A instalação pode ser feita antes da troca de regras. Para habilitar o vínculo, publique `firestore.dosevoz.rules` como arquivo completo de regras no banco **(default)** do Firebase `meusremedios`, na janela combinada para a troca.

A regra encerra o acesso dos apps antigos às coleções anteriores. Confirme o impacto sobre todos os apps e famílias que ainda usam esse banco antes de publicar. A publicação de regras não apaga os documentos. Em seguida, crie uma família fictícia no Familiar, use o convite uma vez no Idoso e simule um lembrete e uma confirmação nos dois celulares.

## Registros e titularidade

O arquivo de código-fonte e seu SHA-256 servem de referência para o pedido de registro de software, depois de revisar autoria, titularidade e licenças dos recursos incluídos. Identifique se o titular será pessoa física ou jurídica. A marca **DoseVoz** requer pedido próprio no INPI. A consulta preliminar feita em novembro de 2026 não equivale a um registro nem à aprovação de um pedido.

## Antes de abrir a distribuição

- Configurar a conta titular, o Play App Signing, a ficha da loja e a faixa de teste. Revalidar o lembrete de voz, a confirmação e o vínculo Familiar–Idoso no pacote da faixa de teste.
- Revisar as regras de acesso do Firestore, incluindo adesão e revogação de membros da família, e preparar a política de privacidade, a declaração de saúde e a seção de segurança dos dados.
- Criar na Play Console a assinatura mensal por família com preço inicial proposto de R$ 9,90. Implementar e testar a compra, a validação do token no servidor, a recuperação em outro aparelho e os estados de renovação, cancelamento e expiração. Os lembretes já sincronizados continuam locais no Idoso. O AAB desta preparação ainda não implementa cobrança.
- Manter o projeto Firebase existente no Spark enquanto o uso couber nas cotas e os recursos da versão permanecerem compatíveis. A validação de assinaturas pode rodar em um backend separado. Se ele for implantado no próprio projeto Firebase mediante Cloud Functions, a implantação exige Blaze. O push de atraso com app Familiar fechado fica fora desta base.
