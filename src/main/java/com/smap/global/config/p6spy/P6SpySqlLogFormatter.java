package com.smap.global.config.p6spy;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.util.Locale;

public class P6SpySqlLogFormatter implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category,
                                String prepared, String sql, String url) {
        if (sql == null || sql.isBlank()) return "";

        String formattedSql = isStatementCategory(category)
                ? FormatStyle.BASIC.getFormatter().format(sql)
                : sql;

        return String.format("\n[%s] | %d ms | %s", category, elapsed, formattedSql.trim());
    }

    private boolean isStatementCategory(String category) {
        return Category.STATEMENT.getName().equals(category)
                || Category.COMMIT.getName().equals(category.toLowerCase(Locale.ROOT))
                || Category.ROLLBACK.getName().equals(category.toLowerCase(Locale.ROOT));
    }
}
