package cn.edu.gdut.skywalking.utils;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class FileUtils {

    public static Function<String,Void> writeFile(String path) {
        return writeFile(new File(path));
    }

    public static Function<String,Void> writeFile(File file) {
        return string -> {
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(string);
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        };
    }

    public static Function<Void,String> readLine(String path){
        return readLine(new File(path));
    }

    public static Function<Void,String> readLine(File file){
        AtomicReference<String> atomicReference = new AtomicReference<>();
        AtomicReference<Function<Void,?>> composition = new AtomicReference<>();
        AtomicBoolean earlyEvaluate = new AtomicBoolean(true);

        return new OnEachLine<String>(composition){
            @Override
            public String apply(Void aVoid) {
                // 防止死循环
                if (earlyEvaluate.get()) {
                    earlyEvaluate.set(false);
                    try {
                        BufferedReader reader =new BufferedReader(new FileReader(file));
                        while (true) {
                            String line = reader.readLine();
                            atomicReference.set(line);
                            if (line == null) break;

                            if (line.length() > 0) {
                                composition.get().apply(null);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return atomicReference.get();
            }
        };
    }

    static abstract class OnEachLine<N> implements Function<Void, N> {
        private OnEachLine<N> self;
        private AtomicReference<Function<Void,?>> composition;

        public OnEachLine(AtomicReference<Function<Void, ?>> composition) {
            this.composition = composition;
            self = this;
        }

        @Override
        public <V> Function<Void, V> andThen(Function<? super N, ? extends V> after) {
            Function<Void,V> function = new OnEachLine<V>(composition){
                @Override
                public V apply(Void aVoid) {
                    N n = self.apply(aVoid);
                    return after.apply(n);
                }
            };
            composition.set(function);
            return function;
        }
    }
}
