package cn.edu.gdut.skywalking.utils;

import java.util.function.Function;

public interface Functions<A,B> {

    <C> Functions<A,C> next(Function<B,C> next);

    <C> Function<A,C> last(Function<B,C> next);

    static <A,B> Functions<A,B> first(Function<A, B> first){
        return new Functions<A,B>() {
            @Override
            public <C> Functions<A,C> next(Function<B,C> next) {
                Function<A,C> composition = compose(first, next);
                return first(composition);
            }

            @Override
            public <C> Function<A, C> last(Function<B, C> next) {
                return compose(first, next);
            }
        };
    }

    static <A,B,C> Function<A,C> compose(Function<A,B> first, Function<B,C> second){
        return input -> second.apply(first.apply(input));
    }

    /**
     * 返回值为void的函数
     * function which returns void
     * @param <I> 函数的参数类型 function's parameter type
     */
    interface VoidFunction<I>{
        void apply(I input);
    }

    static <A> Function<A,Void> avoid(VoidFunction<A> voidFunction) {
        return new Function<A, Void>() {
            @Override
            public Void apply(A a) {
                voidFunction.apply(a);
                return null;
            }
        };
    }
}