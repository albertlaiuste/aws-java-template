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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowResponse;

public class UpdateVehicleByIdHandler
    implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
  private static final Logger logger = LogManager.getLogger(UpdateVehicleByIdHandler.class);
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .setSerializationInclusion(Include.NON_NULL);

  @Override
  public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
    try {
      logger.info("received: {}", input);
      String thingName = Input.validatePathParameter(input.get("pathParameters"), "id");
      String body = (String) input.get("body");
      String result = updateIoTShadow(thingName, body);
      return buildResponse(200, result.toString());
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

  private String updateIoTShadow(String thingName, String body) throws Exception {
    try (IotDataPlaneClient iotClient = IotDataPlaneClient.builder()
        .region(Region.US_EAST_1)
        .httpClient(ApacheHttpClient.builder().build())
        .build()) {

      ObjectNode desiredNode = objectMapper.createObjectNode().set("desired", objectMapper.readTree(body));
      ObjectNode state = objectMapper.createObjectNode().set("state", desiredNode);
      SdkBytes payloadBytes = SdkBytes.fromUtf8String(state.toString());
      UpdateThingShadowRequest updateShadowRequest = UpdateThingShadowRequest.builder()
          .thingName(thingName)
          .shadowName(thingName + "-shadow")
          .payload(payloadBytes)
          .build();
      UpdateThingShadowResponse shadowResponse = iotClient.updateThingShadow(updateShadowRequest);

      JsonNode jsonResponse = objectMapper.readTree(shadowResponse.payload().asByteArray());
      logger.info("Shadow updated: {}", jsonResponse.toString());

      return jsonResponse.get("state").get("desired").toString();
    }
  }
}
