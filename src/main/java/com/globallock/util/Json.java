package com.globallock.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Json {
    private Json() {
    }

    public static Map<String, String> parseObject(String json) {
        if (json == null) {
            throw new IllegalArgumentException("request body is required");
        }

        Parser parser = new Parser(json);
        return parser.parseObject();
    }

    public static String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + escape(string) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(stringify(String.valueOf(entry.getKey())) + ":" + stringify(entry.getValue()));
            }
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> items = new ArrayList<>();
            for (Object item : iterable) {
                items.add(stringify(item));
            }
            return "[" + String.join(",", items) + "]";
        }
        return stringify(value.toString());
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = Objects.requireNonNull(source).trim();
        }

        private Map<String, String> parseObject() {
            skipWhitespace();
            expect('{');
            skipWhitespace();

            Map<String, String> result = new LinkedHashMap<>();
            if (peek('}')) {
                index++;
                return result;
            }

            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parsePrimitiveValue();
                result.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    return result;
                }

                expect(',');
                skipWhitespace();
            }
        }

        private String parsePrimitiveValue() {
            if (peek('"')) {
                return parseString();
            }

            int start = index;
            while (index < source.length()) {
                char current = source.charAt(index);
                if (current == ',' || current == '}' || Character.isWhitespace(current)) {
                    break;
                }
                index++;
            }

            if (start == index) {
                throw new IllegalArgumentException("expected value at position " + index);
            }

            return source.substring(start, index);
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < source.length()) {
                char current = source.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (index >= source.length()) {
                        throw new IllegalArgumentException("unterminated escape sequence");
                    }
                    char escaped = source.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        default -> throw new IllegalArgumentException("unsupported escape sequence: \\" + escaped);
                    }
                } else {
                    builder.append(current);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        private void expect(char expected) {
            if (index >= source.length() || source.charAt(index) != expected) {
                throw new IllegalArgumentException("expected '" + expected + "' at position " + index);
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < source.length() && source.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }
    }
}
