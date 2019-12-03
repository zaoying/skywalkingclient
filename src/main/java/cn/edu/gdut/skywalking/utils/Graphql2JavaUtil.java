package cn.edu.gdut.skywalking.utils;

import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Graphql2JavaUtil {
    public static final Pattern QUERY_METHOD = Pattern.compile("\\s*(\\w+)\\((.*)\\):\\s*(\\[?\\w+!?\\]?!?)$");
    public static final Pattern FIELD_ARGS = Pattern.compile("\\s*(\\w+)\\s*:\\s*(\\[?\\w+\\]?!?)$");

    public static final String JAVA_ENUM_TEMPLATE = "public enum %s {\n%s}";
    public static final String JAVA_ENUM_FIELD_TEMPLATE = "\t%s,\n";
    public static final String JAVA_CLASS_TEMPLATE = "public class %s {\n%s}";
    public static final String JAVA_INTERFACE_TEMPLATE = "public interface %s {\n%s}";
    public static final String JAVA_CLASS_FIELD_TEMPLATE = "\tprivate %s %s;\n";
    public static final String JAVA_CLASS_METHOD_TEMPLATE = "\t%s %s(%s);\n";
    public static final String JAVA_CLASS_SETTER_TEMPLATE = "\tpublic void set%s(%s %s) {\n\t\tthis.%s = %s;\n\t}\n";
    public static final String JAVA_CLASS_GETTER_TEMPLATE = "\tpublic %s get%s() {\n\t\treturn this.%s;\n\t}\n";
    public static final String JAVA_PACKAGE_IMPORT_TEMPLATE = "package cn.edu.gdut.skywalking.graphql%s;\n"
            + "import com.sun.istack.internal.NotNull;\n"
            + "import cn.edu.gdut.skywalking.graphql.input.*;\n"
            + "import cn.edu.gdut.skywalking.graphql.enums.*;\n"
            + "import cn.edu.gdut.skywalking.graphql.type.*;\n\n";

    public static final Map<String,Keyword> dictionary = new HashMap<>();
    public static final Map<String,String> SCALAR_MAP = new HashMap<>();

    public static final String FILE_PATH = "src\\main\\java\\cn\\edu\\gdut\\skywalking\\graphql\\";

    public enum Keyword {
        SCALAR,
        SCHEMA,
        TYPE,
        ENUM,
        EXTEND,
        INPUT,
        FIELD,
        FRAGMENT,
        END
    }

    static {
        for (Keyword keyword : Keyword.values()) {
            dictionary.put(keyword.name(), keyword);
            dictionary.put(keyword.name().toLowerCase(), keyword);
        }
        SCALAR_MAP.put("Boolean", "Boolean");
        SCALAR_MAP.put("ID", "String");
        SCALAR_MAP.put("Int", "Integer");
        SCALAR_MAP.put("Float", "Float");
        SCALAR_MAP.put("Long", "Long");
        SCALAR_MAP.put("String", "String");
    }

    public static class GraphqlSchema {

        private List<String> scalars;
        private Map<String,String> schema;
        private List<String> enums;
        private Map<String,String> types;
        private Map<String,String> inputs;
        private Map<String,String> fragments;
        private List<String> queries;
        private StringBuilder allQueries;

        private Map<String,String> scalarMap;

        private EachLine subRoutine;

        public GraphqlSchema(Map<String, String> scalarMap) {
            this.scalarMap = scalarMap;
            this.scalars = new ArrayList<>();
            this.schema = new HashMap<>();
            this.enums = new ArrayList<>();
            this.types = new HashMap<>();
            this.inputs = new HashMap<>();
            this.fragments = new HashMap<>();
            this.queries = new ArrayList<>();
            allQueries = new StringBuilder(1024 * 1024);
        }

        public GraphqlSchema() {
            this(Graphql2JavaUtil.SCALAR_MAP);
        }

        public void handle(EachLine eachLine){
            if (subRoutine == null) {
                routine(eachLine);
            }
            else {
                subRoutine(eachLine);
            }
        }

        public void enterRoutine(EachLine eachLine) {
            subRoutine = eachLine;
        }

        public void routine(EachLine eachLine) {
            Keyword keyword = eachLine.keyword;
            switch (keyword) {
                case SCALAR:
                    addScalar(eachLine.array);
                    break;
                case SCHEMA:
                case ENUM:
                case TYPE:
                case EXTEND:
                case INPUT:
                case FRAGMENT:
                    enterRoutine(eachLine);
                    break;
                case END:
                    String scalar = buildScalar();
                    String classContent = String.format(JAVA_PACKAGE_IMPORT_TEMPLATE, "") + scalar;
                    FileUtils.writeFile(FILE_PATH + "Scalar.java")
                    .apply(classContent);

                    String className = "Query";
                    String fields = allQueries.toString();
                    String query = String.format(JAVA_INTERFACE_TEMPLATE, className, fields);
                    classContent = String.format(JAVA_PACKAGE_IMPORT_TEMPLATE, "") + query;
                    FileUtils.writeFile(FILE_PATH + className + ".java")
                            .apply(classContent);
                    break;
            }
        }

        public void subRoutine(EachLine eachLine) {
            if (Keyword.END == eachLine.keyword) {
                exitRoutine();
                return;
            }
            Keyword keyword = subRoutine.keyword;
            switch (keyword) {
                case SCHEMA:
                    addSchema(eachLine.origin);
                    break;
                case ENUM:
                    addEnums(eachLine.array);
                    break;
                case TYPE:
                    addType(eachLine.origin);
                    break;
                case INPUT:
                    addInput(eachLine.origin);
                    break;
                case FRAGMENT:
                    addFragment(eachLine.origin);
                    break;
                case EXTEND:
                    addQuery(eachLine.origin);
                    break;
            }
        }

        public void exitRoutine() {
            String stringClass;
            Keyword keyword = subRoutine.keyword;
            String[] array = subRoutine.array;
            switch (keyword) {
                case SCHEMA:
                    stringClass = buildSchema();
                    break;
                case ENUM:
                    stringClass = buildEnum(array[1], enums);
                    break;
                case TYPE:
                    stringClass = buildType();
                    break;
                case INPUT:
                    stringClass = buildInput();
                    break;
                case FRAGMENT:
                    stringClass = buildFragment();
                    break;
                case EXTEND:
                    stringClass = buildQuery();
                    allQueries.append(stringClass);
                    subRoutine = null;
                default:
                    return;
            }
            writeClassToFile(keyword, stringClass, array);
            subRoutine = null;
        }

        public void writeClassToFile(Keyword keyword, String stringClass, String[] array){
            String path;
            String dir = null;
            String fileName;
            switch (keyword) {
                case SCHEMA:
                    path = FILE_PATH;
                    fileName = "Schema.java";
                    break;
                case ENUM:
                    dir = "enums";
                    path = FILE_PATH + dir + "\\";
                    fileName = array[1] + ".java";
                    break;
                case TYPE:
                case INPUT:
                case FRAGMENT:
                    dir = keyword.name().toLowerCase();
                    path = FILE_PATH + dir + "\\";
                    fileName = array[1] + ".java";
                    break;
                default:
                    return;
            }
            String filePath = path + fileName;
            String classContent = String.format(JAVA_PACKAGE_IMPORT_TEMPLATE, dir == null ? "" : "." + dir) + stringClass;
            FileUtils.writeFile(filePath).apply(classContent);
        }

        public void addScalar(String[] array){
            scalars.add(array[1]);
        }

        public void addSchema(String origin){
            Matcher matcher = FIELD_ARGS.matcher(origin);
            if (matcher.matches()) {
                String field = matcher.group(1);
                String type = matcher.group(2);

                schema.put(field, type);
            }
        }

        public void addEnums(String[] array) {
            enums.add(array[0]);
        }

        public void addType(String origin) {
            Matcher matcher = FIELD_ARGS.matcher(origin);
            if (matcher.matches()) {
                String field = matcher.group(1);
                String type = matcher.group(2);
                types.put(field, type);
            }
        }

        public void addInput(String origin) {
            Matcher matcher = FIELD_ARGS.matcher(origin);
            if (matcher.matches()) {
                String field = matcher.group(1);
                String type = matcher.group(2);
                inputs.put(field, type);
            }
        }

        public void addFragment(String origin) {
            Matcher matcher = FIELD_ARGS.matcher(origin);
            if (matcher.matches()) {
                String field = matcher.group(1);
                String type = matcher.group(2);
                fragments.put(field, type);
            }
        }

        public void addQuery(String origin) {
            queries.add(origin);
        }

        public String buildScalar() {
            String scalarName = Keyword.SCALAR.name();
            String className = scalarName.charAt(0) + scalarName.substring(1).toLowerCase();
            return buildEnum(className, scalars);
        }

        public String buildSchema() {
            String schemaName = Keyword.SCHEMA.name();
            String className = schemaName.charAt(0) + schemaName.substring(1).toLowerCase();

            String fields = schema.entrySet().stream()
                    .map(this::buildField)
                    .collect(Collectors.joining());
            schema.clear();
            return String.format(JAVA_CLASS_TEMPLATE, className, fields);
        }

        public String buildEnum(String className, List<String> arrayList) {
            String fields = arrayList.stream()
                    .map(name -> String.format(JAVA_ENUM_FIELD_TEMPLATE, name))
                    .collect(Collectors.joining());
            enums.clear();
            return String.format(JAVA_ENUM_TEMPLATE, className, fields);
        }

        public String[] covert2JavaType(String type) {
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

            if (scalarMap.containsKey(type)){
                type = scalarMap.get(type);
            }

            if (isArray) {
                type += "[]";
            }
            return new String[]{annotations, type};
        }

        public String convertMethodArgs(String methodArgString) {
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

        public String buildField(Map.Entry<String,String> entry) {
            String type = entry.getValue();
            String field = entry.getKey();

            String[] annotationAndType = covert2JavaType(type);

            String annotations = annotationAndType[0];
            type = annotationAndType[1];

            String line = String.format(JAVA_CLASS_FIELD_TEMPLATE, type, field);

            return annotations + line;
        }

        public String buildGetter(Map.Entry<String,String> entry) {
            String type = entry.getValue();
            String field = entry.getKey();

            String methodName = Java2GraphqlUtil.upperCaseOnFirstLetter(field);

            String[] annotationAndType = covert2JavaType(type);

            type = annotationAndType[1];

            return String.format(JAVA_CLASS_GETTER_TEMPLATE, type, methodName, field);
        }

        public String buildSetter(Map.Entry<String,String> entry) {
            String type = entry.getValue();
            String field = entry.getKey();

            String methodName = Java2GraphqlUtil.upperCaseOnFirstLetter(field);

            String[] annotationAndType = covert2JavaType(type);

            type = annotationAndType[1];

            return String.format(JAVA_CLASS_SETTER_TEMPLATE, methodName, type, field, field, field);
        }

        public String buildPOJO(String className, Map<String,String> metaData){
            String fields = metaData.entrySet().stream()
                    .map(this::buildField)
                    .collect(Collectors.joining());

            String getters = metaData.entrySet().stream()
                    .map(this::buildGetter)
                    .collect(Collectors.joining());

            String setters = metaData.entrySet().stream()
                    .map(this::buildSetter)
                    .collect(Collectors.joining());

            metaData.clear();

            return String.format(JAVA_CLASS_TEMPLATE, className, fields + "\n" + getters + "\n" + setters);
        }

        public String buildType() {
            String[] array = subRoutine.array;
            String typeName = array[1];
            return buildPOJO(typeName, types);
        }

        public String buildInput() {
            String[] array = subRoutine.array;
            String inputName = array[1];
            return buildPOJO(inputName, inputs);
        }

        public String buildFragment() {
            String[] array = subRoutine.array;
            String fragmentName = array[1];
            return buildPOJO(fragmentName, fragments);
        }

        public String buildMethod(String origin) {
            Matcher matcher = QUERY_METHOD.matcher(origin);

            if (matcher.matches()) {
                String methodName = matcher.group(1);

                String methodArgs = convertMethodArgs(matcher.group(2));

                String type = matcher.group(3);
                String[] annotationsAndType = covert2JavaType(type);
                String javaType = annotationsAndType[1];

                return String.format(JAVA_CLASS_METHOD_TEMPLATE, javaType, methodName, methodArgs);
            }
            return origin;
        }

        public String buildQuery() {
            String fields = queries.stream()
                    .map(this::buildMethod)
                    .collect(Collectors.joining());

            queries.clear();
            return fields;
        }
    }

    @Data
    public static class EachLine {
        private Keyword keyword;
        private String origin;
        private String[] array;

        public EachLine(Keyword keyword, String origin, String[] array) {
            this.keyword = keyword;
            this.origin = origin;
            this.array = array;
        }

        public static EachLine build(String line) {
            Keyword keyword;
            String[] array = new String[0];
            if (line == null || "}".equals(line)) {
                keyword = Keyword.END;
            }
            else {
                String trim = line.trim();
                array = trim.split(" ");
                String key = array[0];
                keyword = dictionary.get(key);
                if (keyword == null) {
                    keyword = Keyword.FIELD;
                }
            }
            return new EachLine(keyword, line, array);
        }
    }
}
