package com.rezzedup.discordsrv.staffchat.util;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Throwable>
{
    void accept(T thing) throws E;
}
