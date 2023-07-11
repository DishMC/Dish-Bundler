package net.ouja.bundler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        new Main().run(args);
    }

    private void run(String[] args) {
        try {
            String defaultMainClassName = readResource("main-class", BufferedReader::readLine);
            String mainClassName = System.getProperty("bundlerMainClass", defaultMainClassName);
            String repoDir = System.getProperty("bundlerRepoDir", "");
            Path outputDir = Paths.get(repoDir);
            Files.createDirectories(outputDir, (FileAttribute<?>[])new FileAttribute[0]);
            List<URL> extractedUrls = new ArrayList<>();
            readAndExtractDir("versions", outputDir, extractedUrls);
            readAndExtractDir("libraries", outputDir, extractedUrls);
            if (mainClassName == null || mainClassName.isEmpty()) {
                System.out.println("Empty main class specified, exiting");
                System.exit(0);
            }
            ClassLoader maybePlatformClassLoader = getClass().getClassLoader().getParent();
            URLClassLoader classLoader = new URLClassLoader(extractedUrls.<URL>toArray(new URL[0]), maybePlatformClassLoader);
            System.out.println("Starting " + mainClassName);
            Thread runThread = new Thread(() -> {
                try {
                    Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
                    MethodHandle mainHandle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
                    mainHandle.invoke(args);
                } catch (Throwable t) {
                    Thrower.INSTANCE.sneakyThrow(t);
                }
            }, "ServerMain");
            runThread.setContextClassLoader(classLoader);
            runThread.start();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println("Failed to extract server libraries, exiting");
        }
    }

    private <T> T readResource(String resource, ResourceReader<T> parser) throws Exception {
        String fullPath = "/META-INF/" + resource;
        InputStream is = getClass().getResourceAsStream(fullPath);
        try {
            if (is == null) throw new IllegalStateException("Resource " + fullPath + " not found");
            T t = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
            is.close();
            return t;
        } catch (Throwable throwable) {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }
            throw throwable;
        }
    }

    private void readAndExtractDir(String subdir, Path outputDir, List<URL> extractedUrls) throws Exception {
        List<FileEntry> entries = readResource(subdir + ".list", reader -> reader.lines().map(FileEntry::parseLine).toList());
        Path subdirPath = outputDir.resolve(subdir);
        for (FileEntry entry : entries) {
            Path outputFile = subdirPath.resolve(entry.path());
            checkAndExtractJar(subdir, entry, outputFile);
            extractedUrls.add(outputFile.toUri().toURL());
        }
    }

    private void checkAndExtractJar(String subdir, FileEntry entry, Path outputFile) throws Exception {
//        if (!Files.exists(outputFile, new java.nio.file.LinkOption[0]) || !checkIntegrity(outputFile, entry.hash())) {
            System.out.printf("Unpacking %s (%s:%s) to %s%n", entry.path(), subdir, entry.id(), outputFile);
            extractJar(subdir, entry.path(), outputFile);
//        }
    }

    private void extractJar(String subdir, String jarPath, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent(), (FileAttribute<?>[])new FileAttribute[0]);
        InputStream input = getClass().getResourceAsStream("/META-INF/" + subdir + "/" + jarPath);
        try {
            if (input == null) throw new IllegalStateException("Declared library " + jarPath + " not found");
            Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING);
            input.close();
        } catch (Throwable throwable) {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }
            throw throwable;
        }
    }
}