package cn.edu.gdut.skywalking;

import cn.edu.gdut.skywalking.graphql.query.QueryV6;
import cn.edu.gdut.skywalking.graphql.query.QueryV6Proxy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import feign.Feign;
import feign.Request;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class ClientBuilder {
    public static final String GRAPHQL_V6_FILE = "graphqlv6.yml";

    public static final Map<String,String> graphqlQueryMap = new HashMap<>();

    static {
        try {
            URL url = ClientBuilder.class.getResource(GRAPHQL_V6_FILE);
            File file = new File(url.getFile());

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            Map<String,String> graphqlQueries = mapper.readValue(file, new TypeReference<HashMap<String,String>>(){});
            graphqlQueryMap.putAll(graphqlQueries);

        } catch (IOException io) {
            log.error(String.format("解析%s文件失败", GRAPHQL_V6_FILE), io);
        }
    }

    public static <T> T build(String hostPort, Class<T> clazz) {
        return Feign.builder()
                .requestInterceptor(requestTemplate -> log.debug(requestTemplate.requestBody().asString()))
                .options(new Request.Options(2000, 5000))
                .target(clazz, hostPort);
    }

    public static QueryV6 buildV6(String hostPort) {
        GraphqlClient graphqlClient = build(hostPort, GraphqlClient.class);
        CommonProxy commonProxy = new CommonProxy(graphqlClient, new ObjectMapper(), graphqlQueryMap);
        return new QueryV6Proxy(commonProxy);
    }

    public static Callback<? extends Map> proxy(String hostPort) {
        return proxy(hostPort, LinkedHashMap.class);
    }

    public static <O> Callback<O> proxy(String hostPort, Class<O> output) {

        GraphqlClient graphqlClient = build(hostPort, GraphqlClient.class);

        GraphqlClientProxy<O> graphqlClientProxy = new GraphqlClientProxy<>(graphqlClient, output);

        return new CallbackImpl<>(graphqlClientProxy);
    }

    public interface Callback<O> {
        <N> Callback<O> onComplete(Function<Map<String,Object>,N> onComplete);
        <N> Callback<O> onSuccess(Function<O,N> onSuccess);
        <N> Callback<O> onError(Function<List<Map<String,String>>,N> onError);
        void call(String query, Object... args);
        void call(String query, Map<String,Object> map);
    }

    public static class CallbackImpl<O> implements Callback<O> {
        private GraphqlClientProxy<O> graphqlClientProxy;

        public CallbackImpl(GraphqlClientProxy<O> graphqlClientProxy) {
            this.graphqlClientProxy = graphqlClientProxy;
        }

        @Override
        public <N> Callback<O> onComplete(Function<Map<String,Object>, N> onComplete) {
            graphqlClientProxy.setOnComplete(onComplete);
            return this;
        }

        @Override
        public <N> Callback<O> onSuccess(Function<O, N> onSuccess) {
            graphqlClientProxy.setOnSuccess(onSuccess);
            return this;
        }

        @Override
        public <N> Callback<O> onError(Function<List<Map<String,String>>, N> onError) {
            graphqlClientProxy.setOnError(onError);
            return this;
        }

        @Override
        public void call(String query, Object... args) {
            graphqlClientProxy.call(query, args);
        }

        @Override
        public void call(String query, Map<String, Object> map) {
            graphqlClientProxy.call(query, map);
        }
    }
}
