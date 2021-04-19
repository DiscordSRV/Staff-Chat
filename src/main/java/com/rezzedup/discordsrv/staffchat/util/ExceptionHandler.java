package com.rezzedup.discordsrv.staffchat.util;

@FunctionalInterface
public interface ExceptionHandler<E extends Exception>
{
    void handle(E e);
    
    static void print(Exception e) { e.printStackTrace(); }
    
    static void rethrow(Exception e) { throw new RuntimeException(e); }
}
