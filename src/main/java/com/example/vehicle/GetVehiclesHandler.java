package com.example.vehicle;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.dynamodb.Query;
import com.example.model.ApiGatewayResponse;
import com.example.model.Response;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;

public class GetVehiclesHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
  private static final Logger logger = LogManager.getLogger(GetVehiclesHandler.class);
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .setSerializationInclusion(Include.NON_NULL);

  @Override
  public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
    try {
      Map<String, Object> requestContext = (Map<String, Object>) input.get("requestContext");
      Map<String, Object> authorizerContext = (Map<String, Object>) requestContext.get("authorizer");
      String providerName = (String) authorizerContext.get("providerId");
      List<Map<String, AttributeValue>> items = Query.vehicleProviderRelations(providerName);
      List<Map<String, Object>> shadows = getIotShadows(items);
      return buildResponse(200, shadows);
    } catch (IllegalArgumentException e) {
      return logAndRespond(400, "Invalid request parameters", e);
    } catch (Exception e) {
      return logAndRespond(500, "Internal server error", e);
    }
  }

  private ApiGatewayResponse buildResponse(int statusCode, Object body) {
    return ApiGatewayResponse.builder()
        .setStatusCode(statusCode)
        .setObjectBody(body)
        .setHeaders(Collections.singletonMap("Content-Type", "application/json"))
        .build();
  }

  private ApiGatewayResponse logAndRespond(int statusCode, String message, Exception e) {
    logger.error(message + ": {}", e.getMessage());
    return buildResponse(statusCode, new Response(message));
  }

  private List<Map<String, Object>> getIotShadows(List<Map<String, AttributeValue>> items)
      throws Exception {
    try (IotDataPlaneClient iotClient = IotDataPlaneClient.builder()
        .region(Region.US_EAST_1)
        .httpClient(ApacheHttpClient.builder().build())
        .build()) {
      List<Map<String, Object>> vehicles = new ArrayList<>();
      for (Map<String, AttributeValue> item : items) {
        String thingName = item.get("vehicleId").s();
        GetThingShadowRequest getShadowRequest = GetThingShadowRequest.builder()
            .thingName(thingName)
            .shadowName(thingName + "-shadow")
            .build();

        GetThingShadowResponse shadowResponse = iotClient.getThingShadow(getShadowRequest);
        JsonNode jsonResponse = objectMapper.readTree(shadowResponse.payload().asByteArray());
        vehicles.add(Map.of("id", thingName, "state", jsonResponse.get("state").get("reported")));
      }
      return vehicles;
    }
  }
}
