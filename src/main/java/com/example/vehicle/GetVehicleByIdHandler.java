package com.example.vehicle;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.model.ApiGatewayResponse;
import com.example.model.Response;
import com.example.util.Input;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;

public class GetVehicleByIdHandler
    implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
  private static final Logger logger = LogManager.getLogger(GetVehicleByIdHandler.class);
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .setSerializationInclusion(Include.NON_NULL);

  @Override
  public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
    try {
      String thingName = Input.validatePathParameter(input.get("pathParameters"), "id");
      String result = getIotShadow(thingName);
      return buildResponse(200, result);
    } catch (IllegalArgumentException e) {
      return logAndRespond(400, "Invalid request parameters", e);
    } catch (ResourceNotFoundException e) {
      return logAndRespond(404, "No vehicle found with given ID", e);
    } catch (Exception e) {
      return logAndRespond(500, "Internal server error", e);
    }
  }

  private ApiGatewayResponse buildResponse(int statusCode, Object body) {
    ApiGatewayResponse.Builder builder = ApiGatewayResponse.builder()
        .setStatusCode(statusCode)
        .setHeaders(Collections.singletonMap("Content-Type", "application/json"));

    if (statusCode == 200 && body instanceof String) {
      builder.setRawBody((String) body);
    } else {
      builder.setObjectBody(body);
    }
    return builder.build();
  }

  private ApiGatewayResponse logAndRespond(int statusCode, String message, Exception e) {
    logger.error(message + ": {}", e.getMessage());
    return buildResponse(statusCode, new Response(message));
  }

  private String getIotShadow(String thingName) throws Exception {
    try (IotDataPlaneClient iotClient = IotDataPlaneClient.builder()
        .region(Region.US_EAST_1)
        .httpClient(ApacheHttpClient.builder().build())
        .build()) {

      GetThingShadowRequest getShadowRequest = GetThingShadowRequest.builder()
          .thingName(thingName)
          .shadowName(thingName + "-shadow")
          .build();

      GetThingShadowResponse shadowResponse = iotClient.getThingShadow(getShadowRequest);
      JsonNode jsonResponse = objectMapper.readTree(shadowResponse.payload().asByteArray());
      return jsonResponse.get("state").get("reported").toString();
    }
  }
}
