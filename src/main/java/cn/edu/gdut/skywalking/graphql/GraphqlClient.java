package cn.edu.gdut.skywalking.graphql;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface GraphqlClient {

    @RequestLine("POST /graphql")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    // json curly braces must be escaped!
    @Body("%7B\"query\": \"{query}\", \"variables\": {variables}%7D")
    String graphql(@Param("query") String query, @Param(value = "variables") String variables);
}
