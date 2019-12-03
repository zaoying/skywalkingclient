import cn.edu.gdut.skywalking.ClientBuilder;
import cn.edu.gdut.skywalking.graphql.query.QueryV6;
import cn.edu.gdut.skywalking.graphql.enums.Step;
import cn.edu.gdut.skywalking.graphql.input.Duration;
import cn.edu.gdut.skywalking.graphql.input.LabelDuration;
import cn.edu.gdut.skywalking.utils.GraphqlQueryParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphqlQueryParserTest {

    private Map<String,String> graphqlQueryMap;

    private String hostPort = "http://localhost:12800";

    @Before
    public void setup() throws IOException {
        URL url = getClass().getResource("graphqlv6.yaml");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        File file = new File(url.getFile());
        graphqlQueryMap = mapper.readValue(file, new TypeReference<HashMap<String,String>>(){});
    }

    @Test
    public void queryConvert() {
        String methods = graphqlQueryMap.entrySet().stream()
                .map(GraphqlQueryParser::parseQuery)
                .peek(System.out::println)
                .collect(Collectors.joining("\n"));
        GraphqlQueryParser.saveAsClass(methods);
    }

    @Test
    public void getAllServices() throws JsonProcessingException {

        QueryV6 query = ClientBuilder.buildV6(hostPort);

        Duration duration = new Duration();
        duration.setStart("2019-11-29 0800");
        duration.setEnd("2019-11-29 1200");
        duration.setStep(Step.MINUTE);

        Map<String,Object> response = query.allServices(duration);

        System.out.println(new ObjectMapper().writeValueAsString(response));
    }

    @Test
    public void globalTopology() throws JsonProcessingException {

        QueryV6 query = ClientBuilder.buildV6(hostPort);

        LabelDuration duration = new LabelDuration();
        duration.setStart("2019-11-29 0800");
        duration.setEnd("2019-11-29 1200");
        duration.setStep(Step.MINUTE);

        Map<String,Object> response = query.globalTopology(duration);

        System.out.println(new ObjectMapper().writeValueAsString(response));
    }
}
