package org.delcom.ifs25050springbootcashflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class DotEnv {

    private DotEnv() {
    }

    static void load() throws IOException {
        load(Path.of(".env"));
    }

    static void load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return;
        }
        List<String> lines = Files.readAllLines(path);
        for (String raw : lines) {
            applyLine(raw);
        }
    }

    static void applyLine(String raw) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String key = line.substring(0, eq).trim();
        String value = stripQuotes(line.substring(eq + 1).trim());
        if (System.getenv(key) != null || System.getProperty(key) != null) {
            return;
        }
        System.setProperty(key, value);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}