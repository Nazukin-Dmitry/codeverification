package com.codeverification.interpretator;

import java.io.IOException;

/**
 * @author Dmitrii Nazukin
 */
public class Test {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        System.out.println(0b01010 + 0xFA22);
//        System.out.println(Integer.decode("0b01010"));
        Caller caller = (Caller) LibraryProxy.newInstance(args[0], Caller.class);
        System.out.println(caller.Evkl(55,44));
        System.out.println(caller.and(true,true));
        System.out.println(caller.calc(55,44, '+'));
        System.out.println(caller.concat("55","44"));
        System.out.println(caller.dummy());
        System.out.println(caller.Factorial(10));
        System.out.println(caller.Fib(10));
        System.out.println(caller.FibByte((byte)10));
        System.out.println(caller.fun1(10, 100));
        System.out.println(caller.hello("World"));
        System.out.println(caller.sto());
        System.out.println(caller.sum(-50,50));

    }
}
