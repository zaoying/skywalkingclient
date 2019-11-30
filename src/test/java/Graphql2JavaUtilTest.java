import cn.edu.gdut.skywalking.graphql.Query;
import cn.edu.gdut.skywalking.graphql.input.Duration;
import cn.edu.gdut.skywalking.graphql.type.Service;
import cn.edu.gdut.skywalking.utils.FileUtils;
import cn.edu.gdut.skywalking.utils.Graphql2JavaUtil;
import cn.edu.gdut.skywalking.utils.Functions;
import cn.edu.gdut.skywalking.utils.Java2GraphqlUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

public class Graphql2JavaUtilTest {

    private URL url;

    @Before
    public void setup(){
        url = getClass().getResource("all.graphql");
    }

    @Test
    public void readFile() {
        Graphql2JavaUtil.GraphqlSchema graphqlSchema = new Graphql2JavaUtil.GraphqlSchema();
        File file = new File(url.getFile());
        FileUtils.readLine(file)
                .andThen(Graphql2JavaUtil.EachLine::build)
                .andThen(Functions.avoid(graphqlSchema::handle))
                .apply(null);
    }

    @Test
    public void readUrl() {
        FileUtils.readLine(url.getFile())
                .andThen(Functions.avoid(System.out::println))
                .apply(null);
    }

    @Test
    public void bean2graphql() {
        Map<String,String> fields = Java2GraphqlUtil.java2graphql().convertFields(Service.class);
        String string = String.join("\n", fields.values());
        System.out.println(string);
    }

    @Test
    public void interface2graphql() {
        Map<String,String> methods = Java2GraphqlUtil.java2graphql().convertMethods(Query.class);
        String string = String.join("\n", methods.values());
        System.out.println(string);
    }

    @Test
    public void class2graphql() {
        String graphql = Java2GraphqlUtil.java2graphql().convertAll(Duration.class);
        System.out.println(graphql);
    }
}
