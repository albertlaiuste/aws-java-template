package com.example.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;

public class AuthorizerPolicy {
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .setSerializationInclusion(Include.NON_NULL);

  @JsonProperty("principalId")
  private String principalId;

  @JsonProperty("policyDocument")
  private PolicyDocument policyDocument;

  @JsonProperty("context")
  private Map<String, String> context;

  public AuthorizerPolicy(String principalId, String effect, String resource) {
    this.principalId = principalId;
    this.policyDocument = new PolicyDocument(effect, resource);
    this.context = Collections.singletonMap("providerId", principalId);
  }

  @Override
  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Object> toMap() {
    try {
      String json = objectMapper.writeValueAsString(this);
      return objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to convert AuthorizerPolicy to map", e);
    }
  }

  public String getPrincipalId() {
    return principalId;
  }

  public PolicyDocument getPolicyDocument() {
    return policyDocument;
  }

  public Map<String, String> getContext() {
    return context;
  }

  public static class PolicyDocument {
    @JsonProperty("Version")
    private String Version = "2012-10-17";

    @JsonProperty("Statement")
    private Statement[] Statement;

    public PolicyDocument(String effect, String resource) {
      this.Statement = new Statement[] { new Statement(effect, resource) };
    }

    public String getVersion() {
      return Version;
    }

    public Statement[] getStatement() {
      return Statement;
    }
  }

  public static class Statement {
    @JsonProperty("Action")
    private String Action = "execute-api:Invoke";

    @JsonProperty("Effect")
    private String Effect;

    @JsonProperty("Resource")
    private String Resource;

    public Statement(String effect, String resource) {
      this.Effect = effect;
      this.Resource = resource;
    }

    public String getAction() {
      return Action;
    }

    public String getEffect() {
      return Effect;
    }

    public String getResource() {
      return Resource;
    }
  }
}
