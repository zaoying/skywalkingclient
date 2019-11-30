package cn.edu.gdut.skywalking.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Java2GraphqlUtil {

    public static final String GRAPHQL_GLOBAL_TEMPLATE = "%s\n%s";

    public static final String GRAPHQL_QUERY_TEMPLATE = "query %s(%s): %s{\n%s\n}";

    public static final String GRAPHQL_TYPE_TEMPLATE = "[%s]";

    public static final String GRAPHQL_DELIMITER = "\n";

    public static final Map<String,String> DEFAULT_SCALAR_MAP = new HashMap<>();

    static {
        DEFAULT_SCALAR_MAP.put("Boolean", "Boolean");
        DEFAULT_SCALAR_MAP.put("Short", "Int");
        DEFAULT_SCALAR_MAP.put("Integer", "Int");
        DEFAULT_SCALAR_MAP.put("Long", "String");
        DEFAULT_SCALAR_MAP.put("Float", "Float");
        DEFAULT_SCALAR_MAP.put("Double", "Double");
        DEFAULT_SCALAR_MAP.put("String", "String");
        DEFAULT_SCALAR_MAP.put("Date", "String");
        DEFAULT_SCALAR_MAP.put("Datetime", "String");
        DEFAULT_SCALAR_MAP.put("Timestamp", "String");
    }

    public static String lowerCaseOnFirstLetter(String source) {
        return String.valueOf(source.charAt(0)).toLowerCase() + source.substring(1);
    }

    public static String upperCaseOnFirstLetter(String source) {
        return String.valueOf(source.charAt(0)).toUpperCase() + source.substring(1);
    }

    public static <O> O convert(Class<O> outputClass, Object data, ObjectMapper mapper) {
        if (data instanceof Map) {
            if (outputClass.isPrimitive()) {
                throw new RuntimeException("不能将复杂类型转换基本类型：" + outputClass.getSimpleName());
            }
            else {
                if (outputClass == String.class){
                    try {
                        return outputClass.cast(mapper.writeValueAsString(data));
                    } catch (JsonProcessingException e) {
                        log.error("GraphQL Client参数系列化失败", e);
                        throw new RuntimeException(e);
                    }
                }
                else return mapper.convertValue(data, outputClass);
            }
        }
        else return outputClass.cast(data);
    }

    public static Map<String, Object> variablesFormat(Object... variables) {

        Map<String,Object> arguments = new HashMap<>();

        for (Object variable : variables) {
            String simpleName = variable.getClass().getSimpleName();
            String fieldName = Java2GraphqlUtil.lowerCaseOnFirstLetter(simpleName);
            arguments.put(fieldName, variable);
        }

        return arguments;
    }

    public static Converter java2graphql() {
        return new Java2GraphqlConverter(GRAPHQL_GLOBAL_TEMPLATE, GRAPHQL_TYPE_TEMPLATE, GRAPHQL_DELIMITER);
    }

    public interface Converter {
        <I> Map<String,String> convertFields(Class<I> clazz);
        <I> Map<String,String> convertMethods(Class<I> clazz);
        <I> String convertAll(Class<I> clazz);
    }

    public static class Java2GraphqlConverter implements Converter {
        private Map<String,String> scalarMap;
        private String typeTemplate;
        private String globalTemplate;
        private String delimiter;
        private String arraySuffix;
        private Class<? extends Annotation> notNullAnnotation;

        public Java2GraphqlConverter(String globalTemplate, String typeTemplate, String delimiter) {
            this(DEFAULT_SCALAR_MAP, globalTemplate, typeTemplate, delimiter, "s", NonNull.class);
        }

        public Java2GraphqlConverter(Map<String,String> scalarMap, String globalTemplate, String typeTemplate, String delimiter, String arraySuffix, Class<? extends Annotation> notNullAnnotation) {
            this.scalarMap = scalarMap;
            this.globalTemplate = globalTemplate;
            this.typeTemplate = typeTemplate;
            this.delimiter = delimiter;
            this.arraySuffix = arraySuffix;
            this.notNullAnnotation = notNullAnnotation;
        }

        @Override
        public <I> Map<String,String> convertFields(Class<I> clazz) {
            Class<?> type = clazz.isArray() ? clazz.getComponentType() : clazz;

            return Arrays.stream(type.getDeclaredFields())
                    .collect(Collectors.toMap(Field::getName, this::field2String));
        }

        @Override
        public <I> Map<String,String> convertMethods(Class<I> clazz) {
            return Arrays.stream(clazz.getDeclaredMethods())
                    .collect(Collectors.toMap(Method::getName, this::method2String));
        }

        @Override
        public <I> String convertAll(Class<I> clazz) {

            String fields = String.join(delimiter, convertFields(clazz).values());
            String methods = String.join(delimiter, convertMethods(clazz).values());

            return String.format(globalTemplate, fields, methods);
        }

        String field2String(Field field) {
            String fieldName = "  " + field.getName();

            if ("ID".equalsIgnoreCase(fieldName)) {
                return fieldName + ":" + " ID!";
            }

            return fieldName + ": " + type2String(field.getType());
        }

        String method2String(Method method) {
            Class<?> returnType = method.getReturnType();

            String returnTypeName = type2String(returnType);

            String returnTypeFields = String.join(delimiter, convertFields(returnType).values());

            String methodName = method.getName();

            String parameters = Arrays.stream(method.getParameters())
                    .map(this::parameter2String)
                    .collect(Collectors.joining(", "));

            String line = String.format(GRAPHQL_QUERY_TEMPLATE, methodName, parameters, returnTypeName, returnTypeFields);

            return line + (method.isAnnotationPresent(notNullAnnotation) ? "!" : "");
        }

        String parameter2String(Parameter parameter) {
            Class<?> parameterType = parameter.getType();

            if (parameterType.isArray()) {
                parameterType = parameterType.getComponentType();
            }
            String parameterName = parameterType.getSimpleName() + arraySuffix;

            return lowerCaseOnFirstLetter(parameterName) + ": " + type2String(parameterType);
        }

        <I> String type2String(Class<I> type) {
            String typeName;
            if (type.isArray()) {
                Class<?> componentType = type.getComponentType();
                if (typeTemplate != null) {
                    typeName = String.format(typeTemplate, componentType.getSimpleName());
                }
                else typeName = componentType.getSimpleName();
            }
            else typeName = type.getSimpleName();

            return scalarMap.getOrDefault(typeName, typeName);
        }
    }
}
