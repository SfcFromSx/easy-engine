package com.smartbi.analyze.parse;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SqlStructureAnalyzer {

    private static final SqlParser.Config PARSER_CONFIG = SqlParser.configBuilder()
            .setLex(Lex.MYSQL)
            .setConformance(SqlConformanceEnum.LENIENT)
            .build();

    public ParseOutcome analyze(String sql) {
        if (!hasText(sql)) {
            return ParseOutcome.skipped("empty_sql");
        }
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            return ParseOutcome.skipped("empty_sql");
        }

        try {
            SqlNode node = parseRoot(trimmed);
            QuerySignature signature = new QuerySignature();
            signature.setRootKind(node.getKind().name());
            if (node instanceof SqlSelect) {
                fillFromSelect((SqlSelect) node, signature);
            } else if (node instanceof SqlOrderBy) {
                SqlNode query = ((SqlOrderBy) node).operand(0);
                if (query instanceof SqlSelect) {
                    fillFromSelect((SqlSelect) query, signature);
                } else {
                    signature.setSelectItems(Collections.singletonList(node.toString()));
                }
            } else {
                signature.setSelectItems(Collections.singletonList(node.toString()));
            }
            return ParseOutcome.ok(signature);
        } catch (SqlParseException ex) {
            return ParseOutcome.error(ex.getMessage());
        } catch (Exception ex) {
            return ParseOutcome.error(ex.getMessage());
        }
    }

    private static SqlNode parseRoot(String sql) throws SqlParseException {
        try {
            return SqlParser.create(sql, PARSER_CONFIG).parseQuery();
        } catch (SqlParseException ex) {
            return SqlParser.create(sql, PARSER_CONFIG).parseStmt();
        }
    }

    private static void fillFromSelect(SqlSelect select, QuerySignature signature) {
        Set<String> tables = new LinkedHashSet<String>();
        collectFrom(select.getFrom(), tables);
        signature.setTables(new ArrayList<String>(tables));

        SqlNodeList group = select.getGroup();
        if (group != null) {
            List<String> groups = new ArrayList<String>();
            for (SqlNode node : group) {
                groups.add(node.toString());
            }
            signature.setGroupBy(groups);
        }

        SqlNodeList selectList = select.getSelectList();
        List<String> items = new ArrayList<String>();
        List<String> aggregates = new ArrayList<String>();
        if (selectList != null) {
            for (SqlNode node : selectList) {
                items.add(node.toString());
                collectAggregates(node, aggregates);
            }
        }
        signature.setSelectItems(items);
        signature.setAggregates(aggregates);
    }

    private static void collectFrom(SqlNode from, Set<String> tables) {
        if (from == null) {
            return;
        }
        if (from instanceof SqlIdentifier) {
            tables.add(from.toString());
        } else if (from instanceof SqlCall) {
            SqlCall call = (SqlCall) from;
            SqlOperator operator = call.getOperator();
            if (operator == SqlStdOperatorTable.AS) {
                collectFrom(call.operand(0), tables);
            } else if (from instanceof SqlJoin) {
                SqlJoin join = (SqlJoin) from;
                collectFrom(join.getLeft(), tables);
                collectFrom(join.getRight(), tables);
            } else {
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

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
