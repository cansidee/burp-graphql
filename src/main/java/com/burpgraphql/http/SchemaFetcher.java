package com.burpgraphql.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpgraphql.schema.IntrospectionParser;
import com.burpgraphql.schema.model.IntrospectionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static burp.api.montoya.http.message.requests.HttpRequest.httpRequestFromUrl;

public class SchemaFetcher {

    /**
     * Standard GraphQL introspection query. Uses fragment depth-7 to handle
     * deeply nested NON_NULL / LIST wrappers in real-world schemas.
     */
    private static final String INTROSPECTION_QUERY = """
            query IntrospectionQuery {
              __schema {
                queryType { name }
                mutationType { name }
                subscriptionType { name }
                types {
                  ...FullType
                }
              }
            }
            fragment FullType on __Type {
              kind
              name
              description
              fields(includeDeprecated: true) {
                name
                description
                isDeprecated
                deprecationReason
                args {
                  ...InputValue
                }
                type { ...TypeRef }
              }
              inputFields { ...InputValue }
              enumValues(includeDeprecated: true) {
                name
                description
                isDeprecated
                deprecationReason
              }
            }
            fragment InputValue on __InputValue {
              name
              description
              type { ...TypeRef }
              defaultValue
            }
            fragment TypeRef on __Type {
              kind name
              ofType {
                kind name
                ofType {
                  kind name
                  ofType {
                    kind name
                    ofType {
                      kind name
                      ofType {
                        kind name
                        ofType {
                          kind name
                          ofType { kind name }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    private final MontoyaApi api;
    private final ObjectMapper mapper;

    public SchemaFetcher(MontoyaApi api) {
        this.api = api;
        this.mapper = new ObjectMapper();
    }

    /**
     * Fetches the GraphQL schema via introspection and returns the parsed result.
     * Uses Montoya's HTTP client so Burp session-handling rules (auth headers,
     * cookies, macros) are applied automatically.
     */
    public IntrospectionParser.ParsedSchema fetch(String endpointUrl) throws Exception {
        String body = mapper.writeValueAsString(Map.of("query", INTROSPECTION_QUERY));

        HttpRequest request = httpRequestFromUrl(endpointUrl)
                .withMethod("POST")
                .withAddedHeader("Content-Type", "application/json")
                .withBody(body);

        HttpRequestResponse rr = api.http().sendRequest(request);
        if (rr.response() == null) {
            throw new Exception("No response from server");
        }

        String responseBody = rr.response().bodyToString();

        IntrospectionResponse introspection;
        try {
            introspection = mapper.readValue(responseBody, IntrospectionResponse.class);
        } catch (Exception e) {
            throw new Exception("Failed to parse response as JSON: " + e.getMessage());
        }

        if (introspection.hasErrors()) {
            throw new Exception("Introspection error: " + introspection.getFirstError());
        }

        if (introspection.data == null || introspection.data.schema == null) {
            throw new Exception("Response missing __schema – introspection may be disabled");
        }

        return IntrospectionParser.parse(introspection);
    }
}
