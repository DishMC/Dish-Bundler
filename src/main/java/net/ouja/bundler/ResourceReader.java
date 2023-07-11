package net.ouja.bundler;

import java.io.BufferedReader;

public interface ResourceReader<T> {
    T parse(BufferedReader paramBufferedReader) throws Exception;
}
