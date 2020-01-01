package cn.edu.gdut.skywalking.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.edu.gdut.skywalking.utils.Graphql2JavaUtil.*;

@Slf4j
public class GraphqlQueryParser {
    private static final Pattern QUERY_REG_EXPRESSION = Pattern.compile("query\\s+(\\w+)\\((.*)\\)\\s*\\{");
    public static final Pattern FIELD_ARGS = Pattern.compile("\\s*\\$(\\w+)\\s*:\\s*(\\[?\\w+\\!?\\]?!?)");
    public static final String JAVA_PACKAGE_IMPORT_TEMPLATE = "package cn.edu.gdut.skywalking.graphql%s;\n"
            + "import java.util.Map;\n"
            + "import cn.edu.gdut.skywalking.graphql.input.*;\n";

    public static void saveAsClass(String methods) {
        String classString = String.format(JAVA_INTERFACE_TEMPLATE, "QueryV6", methods);

        String classContent = String.format(JAVA_PACKAGE_IMPORT_TEMPLATE, ".query") + classString;

        String filePath = FILE_PATH + "query\\" + "QueryV6.java";

        FileUtils.writeFile(filePath).apply(classContent);
    }

    public static String parseQuery(Map.Entry<String,String> entry) {
        String key = entry.getKey();
        String methodName = key.substring(0, key.length() - "SixPointOne".length());
        String query = entry.getValue().split("\n")[0];
        if (query == null || "".equalsIgnoreCase(query)) return query;

        Matcher matcher = QUERY_REG_EXPRESSION.matcher(query);
        if (matcher.matches()) {
//            String methodName = Java2GraphqlUtil.lowerCaseOnFirstLetter(matcher.group(1));
            String parameters = matcher.group(2);
            String converted = convertMethodArgs(parameters);
            return String.format(JAVA_CLASS_METHOD_TEMPLATE, "Map<String,Object>", methodName, converted);
        }
        log.error(query);
        return "";
    }

    public static String convertMethodArgs(String methodArgString) {
        List<String> converted = new LinkedList<>();
        String[] methodArgs = methodArgString.split(",");

        for (String methodArg : methodArgs) {
            Matcher matcher = FIELD_ARGS.matcher(methodArg.trim());
            if (matcher.matches()) {
                String field = matcher.group(1);
                String type = matcher.group(2);
                String[] annotationsAndType = covert2JavaType(type);
                String javaMethodArgs = annotationsAndType[1] + " " + field;
                converted.add(javaMethodArgs);
            }
            else converted.add(methodArg);
        }
        return String.join(", ", converted);
    }

    public static String[] covert2JavaType(String type) {
        String annotations = "";
        boolean notNull = type.contains("!");
        if (notNull) {
            annotations += "\t@NotNull\n";
            type = type.replace("!", "");
        }

        boolean isArray = type.startsWith("[") && type.endsWith("]");
        if (isArray) {
            type = type.substring(1, type.length() -1);
        }

        if (SCALAR_MAP.containsKey(type)){
            type = SCALAR_MAP.get(type);
        }

        if (isArray) {
            type += "[]";
        }
        return new String[]{annotations, type};
    }
}
