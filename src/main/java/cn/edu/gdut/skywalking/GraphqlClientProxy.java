package cn.edu.gdut.skywalking;

import cn.edu.gdut.skywalking.utils.Java2GraphqlUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

@Slf4j
public class GraphqlClientProxy<O> {

    private GraphqlClient graphqlClient;

    private ObjectMapper mapper;

    private Function<O,?> onSuccess;

    private Function<List<Map<String,String>>,?> onError;

    private Function<Map<String,Object>, ?> onComplete;

    public GraphqlClientProxy(GraphqlClient graphqlClient, Class<O> outputClass) {
        this.graphqlClient = graphqlClient;
        this.mapper = new ObjectMapper();

        onComplete = result -> {
            if (result.containsKey("data")) {
                Object data = result.get("data");

                O output = Java2GraphqlUtil.convert(outputClass, data, mapper);

                Optional.ofNullable(onSuccess).ifPresent(existed -> existed.apply(output));
            }

            if (result.containsKey("errors")) {
                Object errors = result.get("errors");
                List<Map<String,String>> errorList = mapper.convertValue(errors, new TypeReference<List<Map<String, String>>>(){});

                Optional.ofNullable(onError).ifPresent(existed -> existed.apply(errorList));
            }
            return null;
        };
    }

    public void call(String query, Object... input) {
        Map<String, Object> variables = Java2GraphqlUtil.variablesFormat(input);
        call(query, variables);
    }

    public void call(String query, Map<String,Object> input) {
        try {
            String variables = mapper.writeValueAsString(input);
            String response = graphqlClient.graphql(query, variables);

            log.debug(response);

            Map<String,Object> result = mapper.readValue(response, new TypeReference<Map<String, Object>>(){});
            onComplete.apply(result);

        } catch (JsonProcessingException e) {
            log.error("GraphQL Client参数系列化失败", e);
            throw new RuntimeException(e);
        }
    }

    public void setOnSuccess(Function<O, ?> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnError(Function<List<Map<String, String>>, ?> onError) {
        this.onError = onError;
    }

    public void setOnComplete(Function<Map<String, Object>, ?> onComplete) {
        this.onComplete = onComplete;
    }
}
