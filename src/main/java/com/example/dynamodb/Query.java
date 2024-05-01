package com.example.dynamodb;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class Query {

    public static String getTokenProviderId(String token) {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .httpClient(ApacheHttpClient.builder().build())
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(
                ScanRequest.builder()
                        .tableName(System.getenv("PROVIDER_AUTHORIZATIONS_RELATIONS_TABLE"))
                        .build());

        String providerId = scanResponse.items().stream()
                .filter(item -> item.get("authorizationKey").s().equals(token))
                .findFirst()
                .map(item -> item.get("providerId").s())
                .orElse("user");

        return providerId;
    }

    public static List<Map<String, AttributeValue>> vehicleProviderRelations(String providerName) {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .httpClient(ApacheHttpClient.builder().build())
                .build();

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(System.getenv("VEHICLE_PROVIDER_RELATIONS_TABLE"))
                .keyConditionExpression("providerId = :provider_id")
                .expressionAttributeValues(
                        Map.of(":provider_id", AttributeValue.builder().s(providerName).build()))
                .build();

        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        return queryResponse.items();
    }
}
