package com.smartbi.benchmark.sql;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SqlStatementSplitter {

    private SqlStatementSplitter() {
    }

    public static List<String> split(String raw) {
        List<String> statements = new ArrayList<String>();
        if (!StringUtils.hasText(raw)) {
            return statements;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            char next = i + 1 < raw.length() ? raw.charAt(i + 1) : '\0';

            if (inLineComment) {
                current.append(ch);
                if (ch == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                current.append(ch);
                if (ch == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            if (inSingleQuote) {
                current.append(ch);
                if (ch == '\'' && next == '\'') {
                    current.append(next);
                    i++;
                } else if (ch == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                current.append(ch);
                if (ch == '"' && next == '"') {
                    current.append(next);
                    i++;
                } else if (ch == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (ch == '-' && next == '-') {
                current.append(ch).append(next);
                i++;
                inLineComment = true;
                continue;
            }

            if (ch == '/' && next == '*') {
                current.append(ch).append(next);
                i++;
                inBlockComment = true;
                continue;
            }

            if (ch == '\'') {
                current.append(ch);
                inSingleQuote = true;
                continue;
            }

            if (ch == '"') {
                current.append(ch);
                inDoubleQuote = true;
                continue;
            }

            if (ch == ';') {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String value = current.toString().trim();
        String executable = value
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)--.*$", " ")
                .trim();
        if (StringUtils.hasText(value) && StringUtils.hasText(executable)) {
            statements.add(value);
        }
    }
}
