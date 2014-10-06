package com.flightstats.util;

import java.util.function.BinaryOperator;

public class BinaryOperators {

    public static <T> BinaryOperator<T> firstOne(){
        return (t, t2) -> t;
    }
}
