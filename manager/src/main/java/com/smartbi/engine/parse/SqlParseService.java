package com.smartbi.engine.parse;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SQL structural analysis using Apache Calcite ({@link SqlParser} + AST walk).
 */
@Service
public class SqlParseService {

    private static final SqlParser.Config PARSER_CONFIG = SqlParser.configBuilder()
            .setLex(Lex.MYSQL)
            .setConformance(SqlConformanceEnum.LENIENT)
            .build();

    public ParseOutcome analyze(String sql) {
        if (!StringUtils.hasText(sql)) {
            return ParseOutcome.skipped("empty_sql");
        }
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            return ParseOutcome.skipped("empty_sql");
        }

        try {
            SqlNode node = parseRoot(trimmed);
            QuerySignature sig = new QuerySignature();
            sig.setRootKind(node.getKind().name());
            if (node instanceof SqlSelect) {
                fillFromSelect((SqlSelect) node, sig);
            } else if (node instanceof SqlOrderBy) {
                SqlNode q = ((SqlOrderBy) node).operand(0);
                if (q instanceof SqlSelect) {
                    fillFromSelect((SqlSelect) q, sig);
                } else {
                    sig.setSelectItems(Collections.singletonList(node.toString()));
                }
            } else {
                sig.setSelectItems(Collections.singletonList(node.toString()));
            }
            return ParseOutcome.ok(sig);
        } catch (SqlParseException e) {
            return ParseOutcome.error(e.getMessage());
        } catch (Exception e) {
            return ParseOutcome.error(e.getMessage());
        }
    }

    private static SqlNode parseRoot(String sql) throws SqlParseException {
        try {
            return SqlParser.create(sql, PARSER_CONFIG).parseQuery();
        } catch (SqlParseException ex) {
            return SqlParser.create(sql, PARSER_CONFIG).parseStmt();
        }
    }

    private static void fillFromSelect(SqlSelect select, QuerySignature sig) {
        Set<String> tables = new LinkedHashSet<>();
        collectFrom(select.getFrom(), tables);
        sig.setTables(new ArrayList<>(tables));

        SqlNodeList group = select.getGroup();
        if (group != null) {
            List<String> g = new ArrayList<>();
            for (SqlNode n : group) {
                g.add(n.toString());
            }
            sig.setGroupBy(g);
        }

        SqlNodeList selectList = select.getSelectList();
        List<String> items = new ArrayList<>();
        List<String> aggs = new ArrayList<>();
        if (selectList != null) {
            for (SqlNode n : selectList) {
                items.add(n.toString());
                collectAggregates(n, aggs);
            }
        }
        sig.setSelectItems(items);
        sig.setAggregates(aggs);
    }

    private static void collectFrom(SqlNode from, Set<String> tables) {
        if (from == null) return;
        
        if (from instanceof SqlIdentifier) {
            tables.add(from.toString());
        } else if (from instanceof SqlCall) {
            SqlCall call = (SqlCall) from;
            SqlOperator op = call.getOperator();
            if (op == SqlStdOperatorTable.AS) {
                // Operand 0 is the table/subquery, Operand 1 is the alias
                collectFrom(call.operand(0), tables);
            } else if (from instanceof SqlJoin) {
                SqlJoin join = (SqlJoin) from;
                collectFrom(join.getLeft(), tables);
                collectFrom(join.getRight(), tables);
            } else {
                // Handle union, lateral, etc.
                for (SqlNode operand : call.getOperandList()) {
                    collectFrom(operand, tables);
                }
            }
        } else if (from instanceof SqlSelect) {
            collectFrom(((SqlSelect) from).getFrom(), tables);
        }
    }

    private static void collectAggregates(SqlNode node, List<String> out) {
        if (node == null) {
            return;
        }
        node.accept(new SqlBasicVisitor<Void>() {
            @Override
            public Void visit(SqlCall call) {
                if (call.getOperator() != null && call.getOperator().isAggregator()) {
                    out.add(call.toString());
                    return null;
                }
                for (SqlNode child : call.getOperandList()) {
                    if (child != null) {
                        child.accept(this);
                    }
                }
                return null;
            }
        });
    }
}
