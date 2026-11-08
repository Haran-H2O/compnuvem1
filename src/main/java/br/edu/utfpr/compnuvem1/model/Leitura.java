package br.edu.utfpr.compnuvem1.model;

import br.edu.utfpr.compnuvem1.config.CriadorTabela;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Leitura {

    private String sensorId;
    private Long timestamp;
    private String localizacao;
    private Double temperatura;
    private Double umidade;
    private String statusBateria;
    private Long leituras;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("SensorID")
    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    @DynamoDbSortKey
    @DynamoDbSecondarySortKey(indexNames = CriadorTabela.INDICE_LOCALIZACAO)
    @DynamoDbAttribute("Timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = CriadorTabela.INDICE_LOCALIZACAO)
    @DynamoDbAttribute("Localizacao")
    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    @DynamoDbAttribute("Temperatura")
    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    @DynamoDbAttribute("Umidade")
    public Double getUmidade() {
        return umidade;
    }

    public void setUmidade(Double umidade) {
        this.umidade = umidade;
    }

    @DynamoDbAttribute("StatusBateria")
    public String getStatusBateria() {
        return statusBateria;
    }

    public void setStatusBateria(String statusBateria) {
        this.statusBateria = statusBateria;
    }

    @DynamoDbAttribute("Leituras")
    public Long getLeituras() {
        return leituras;
    }

    public void setLeituras(Long leituras) {
        this.leituras = leituras;
    }
}
