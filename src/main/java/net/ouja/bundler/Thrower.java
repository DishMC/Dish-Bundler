package net.ouja.bundler;

public class Thrower<T extends Throwable> {
    public static final Thrower<RuntimeException> INSTANCE = new Thrower();

    public void sneakyThrow(Throwable exception) throws T {
        throw (T)exception;
    }
}
