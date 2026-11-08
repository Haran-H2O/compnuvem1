package br.edu.utfpr.compnuvem1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalSecondaryIndexAction;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexUpdate;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;

@Component
public class CriadorTabela implements ApplicationRunner {

    public static final String INDICE_LOCALIZACAO = "LocalizacaoIndex";

    private final DynamoDbClient client;
    private final String tabela;

    public CriadorTabela(DynamoDbClient client, @Value("${dynamodb.tabela}") String tabela) {
        this.client = client;
        this.tabela = tabela;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        criarTabela();
        criarIndiceLocalizacao();
    }

    private void criarTabela() {
        if (descreverTabela() != null) {
            return;
        }
        client.createTable(CreateTableRequest.builder()
                .tableName(tabela)
                .attributeDefinitions(
                        atributo("SensorID", ScalarAttributeType.S),
                        atributo("Timestamp", ScalarAttributeType.N))
                .keySchema(
                        chave("SensorID", KeyType.HASH),
                        chave("Timestamp", KeyType.RANGE))
                .provisionedThroughput(capacidade())
                .build());
        client.waiter().waitUntilTableExists(r -> r.tableName(tabela));
    }

    private void criarIndiceLocalizacao() throws InterruptedException {
        if (statusIndice() != null) {
            return;
        }
        client.updateTable(UpdateTableRequest.builder()
                .tableName(tabela)
                .attributeDefinitions(
                        atributo("Localizacao", ScalarAttributeType.S),
                        atributo("Timestamp", ScalarAttributeType.N))
                .globalSecondaryIndexUpdates(GlobalSecondaryIndexUpdate.builder()
                        .create(CreateGlobalSecondaryIndexAction.builder()
                                .indexName(INDICE_LOCALIZACAO)
                                .keySchema(
                                        chave("Localizacao", KeyType.HASH),
                                        chave("Timestamp", KeyType.RANGE))
                                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                .provisionedThroughput(capacidade())
                                .build())
                        .build())
                .build());
        while (statusIndice() != IndexStatus.ACTIVE) {
            Thread.sleep(500);
        }
    }

    private IndexStatus statusIndice() {
        return descreverTabela().globalSecondaryIndexes().stream()
                .filter(i -> INDICE_LOCALIZACAO.equals(i.indexName()))
                .map(GlobalSecondaryIndexDescription::indexStatus)
                .findFirst()
                .orElse(null);
    }

    private TableDescription descreverTabela() {
        try {
            return client.describeTable(r -> r.tableName(tabela)).table();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private ProvisionedThroughput capacidade() {
        return ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build();
    }

    private AttributeDefinition atributo(String nome, ScalarAttributeType tipo) {
        return AttributeDefinition.builder().attributeName(nome).attributeType(tipo).build();
    }

    private KeySchemaElement chave(String nome, KeyType tipo) {
        return KeySchemaElement.builder().attributeName(nome).keyType(tipo).build();
    }
}
