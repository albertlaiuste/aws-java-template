package com.example.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.dynamodb.Query;
import com.example.model.AuthorizerPolicy;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomAuthorizerHandler
    implements RequestHandler<Map<String, Object>, Map<String, Object>> {

  private static final Logger logger = LogManager.getLogger(CustomAuthorizerHandler.class);

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    try {
      String token = (String) event.get("authorizationToken");
      if (token == null) {
        return generatePolicy("user", "Deny", (String) event.get("methodArn"));
      }
      if (token.startsWith("Bearer ")) {
        token = token.substring("Bearer ".length());
      }
      String resource = (String) event.get("methodArn");
      String providerId = Query.getTokenProviderId(token);

      return generatePolicy(providerId, providerId.equals("user") ? "Deny" : "Allow", resource);
    } catch (Exception e) {
      logger.error("Error processing event: {}", event.toString(), e);
      return generatePolicy("user", "Deny", (String) event.get("methodArn"));
    }
  }

  private Map<String, Object> generatePolicy(String principalId, String effect, String resource) {
    AuthorizerPolicy authPolicy = new AuthorizerPolicy(principalId, effect, resource);
    return authPolicy.toMap();
  }
}
