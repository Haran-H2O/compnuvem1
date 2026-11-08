package br.edu.utfpr.compnuvem1.repository;

import br.edu.utfpr.compnuvem1.config.CriadorTabela;
import br.edu.utfpr.compnuvem1.model.Leitura;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class LeituraRepository {

    private static final TableSchema<Leitura> SCHEMA = TableSchema.fromBean(Leitura.class);

    private final DynamoDbClient client;
    private final DynamoDbTable<Leitura> tabela;
    private final String nomeTabela;

    public LeituraRepository(DynamoDbClient client, DynamoDbEnhancedClient enhancedClient,
            @Value("${dynamodb.tabela}") String nomeTabela) {
        this.client = client;
        this.nomeTabela = nomeTabela;
        this.tabela = enhancedClient.table(nomeTabela, SCHEMA);
    }

    public void salvar(Leitura leitura) {
        tabela.putItem(leitura);
    }

    public Optional<Leitura> buscar(String sensorId, long timestamp) {
        return Optional.ofNullable(tabela.getItem(chave(sensorId, timestamp)));
    }

    public Optional<Leitura> remover(String sensorId, long timestamp) {
        return Optional.ofNullable(tabela.deleteItem(chave(sensorId, timestamp)));
    }

    public List<Leitura> listarPorSensor(String sensorId) {
        return tabela.query(QueryConditional.keyEqualTo(k -> k.partitionValue(sensorId)))
                .items().stream().toList();
    }

    public List<Leitura> listarPorLocalizacao(String localizacao) {
        return tabela.index(CriadorTabela.INDICE_LOCALIZACAO)
                .query(QueryConditional.keyEqualTo(k -> k.partitionValue(localizacao)))
                .stream()
                .flatMap(p -> p.items().stream())
                .toList();
    }

    public List<Leitura> listarPorSensorELocalizacao(String sensorId, String localizacao) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(sensorId)))
                .filterExpression(Expression.builder()
                        .expression("Localizacao = :loc")
                        .expressionValues(Map.of(":loc", AttributeValue.fromS(localizacao)))
                        .build())
                .build();
        return tabela.query(request).items().stream().toList();
    }

    public List<Leitura> listarTodos() {
        return tabela.scan().items().stream().toList();
    }

    public Optional<Leitura> atualizar(String sensorId, long timestamp, Leitura dados) {
        List<String> sets = new ArrayList<>();
        Map<String, AttributeValue> valores = new HashMap<>();
        if (dados.getLocalizacao() != null) {
            sets.add("Localizacao = :novo_local");
            valores.put(":novo_local", AttributeValue.fromS(dados.getLocalizacao()));
        }
        if (dados.getTemperatura() != null) {
            sets.add("Temperatura = :temp");
            valores.put(":temp", AttributeValue.fromN(dados.getTemperatura().toString()));
        }
        if (dados.getUmidade() != null) {
            sets.add("Umidade = :umid");
            valores.put(":umid", AttributeValue.fromN(dados.getUmidade().toString()));
        }
        if (dados.getStatusBateria() != null) {
            sets.add("StatusBateria = :bat");
            valores.put(":bat", AttributeValue.fromS(dados.getStatusBateria()));
        }
        if (sets.isEmpty()) {
            return buscar(sensorId, timestamp);
        }
        return executarUpdate(sensorId, timestamp, "SET " + String.join(", ", sets), valores);
    }

    public Optional<Leitura> incrementarLeituras(String sensorId, long timestamp) {
        return executarUpdate(sensorId, timestamp,
                "SET Leituras = if_not_exists(Leituras, :zero) + :inc",
                Map.of(":inc", AttributeValue.fromN("1"), ":zero", AttributeValue.fromN("0")));
    }

    private Optional<Leitura> executarUpdate(String sensorId, long timestamp, String expressao,
            Map<String, AttributeValue> valores) {
        try {
            var resposta = client.updateItem(UpdateItemRequest.builder()
                    .tableName(nomeTabela)
                    .key(Map.of(
                            "SensorID", AttributeValue.fromS(sensorId),
                            "Timestamp", AttributeValue.fromN(Long.toString(timestamp))))
                    .updateExpression(expressao)
                    .conditionExpression("attribute_exists(SensorID)")
                    .expressionAttributeValues(valores)
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());
            return Optional.of(SCHEMA.mapToItem(resposta.attributes()));
        } catch (ConditionalCheckFailedException e) {
            return Optional.empty();
        }
    }

    private Key chave(String sensorId, long timestamp) {
        return Key.builder().partitionValue(sensorId).sortValue(timestamp).build();
    }
}
