package cn.edu.gdut.skywalking;

import cn.edu.gdut.skywalking.utils.Java2GraphqlUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class CommonProxy {

    private GraphqlClient graphqlClient;
    private ObjectMapper mapper;
    private Map<String,String> graphqlQueryMap;

    public  CommonProxy(GraphqlClient graphqlClient, ObjectMapper mapper, Map<String,String> graphqlQueryMap) {
        this.graphqlClient = graphqlClient;
        this.mapper = mapper;
        this.graphqlQueryMap = graphqlQueryMap;
    }

    public <O> O call(Class<O> outputClass, Object... inputArgs) {
        Map<String, Object> variables = Java2GraphqlUtil.variablesFormat(inputArgs);
        return call(outputClass, variables);
    }

    public <O> O call(Class<O> outputClass, Map<String,Object> variables) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements[2].getMethodName();
        if ("call".equalsIgnoreCase(methodName)) {
            methodName = stackTraceElements[3].getMethodName();
        }
        String methodKey = methodName + "SixPointOne";

        if (graphqlQueryMap.containsKey(methodKey)) {
            try {
                String query = graphqlQueryMap.get(methodKey);

                String response = graphqlClient.graphql(query, mapper.writeValueAsString(variables));

                log.debug(response);

                Map<String,Object> result = mapper.readValue(response, new TypeReference<Map<String, Object>>(){});

                if (result.containsKey("data")) {
                    Object data = result.get("data");
                    return Java2GraphqlUtil.convert(outputClass, data, mapper);
                }

                if (result.containsKey("errors")) {
                    Object errors = result.get("errors");
                    String message = mapper.writeValueAsString(errors);
                    throw new RuntimeException(message);
                }

                throw new RuntimeException("返回结果无效：" + response);

            } catch (JsonProcessingException e) {
                log.error("GraphQL Client参数系列化失败", e);
                throw new RuntimeException(e);
            }
        }
        else throw new RuntimeException(String.format("无法执行%s查询，原因接口不存在", methodName));
    }
}
