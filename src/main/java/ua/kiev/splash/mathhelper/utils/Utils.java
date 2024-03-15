package ua.kiev.splash.mathhelper.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.kiev.splash.mathhelper.exceptions.UnexpectedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static String getResourceFileAsString(String fileName) {
        log.debug("Loading resource {}", fileName);
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }
}
