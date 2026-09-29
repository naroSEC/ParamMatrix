package parammatrix.analysis;

import parammatrix.testing.database.DatabaseErrorMatch;
import parammatrix.testing.database.DatabaseType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseErrorSignatureAnalyzer {
    private final Map<DatabaseType, List<Pattern>> patterns = patterns();

    public Optional<DatabaseErrorMatch> findNewError(String baseline, String tested,
                                                      Set<DatabaseType> enabled) {
        List<DatabaseType> priority = List.of(DatabaseType.MYSQL, DatabaseType.POSTGRESQL,
                DatabaseType.MSSQL, DatabaseType.ORACLE, DatabaseType.SQLITE,
                DatabaseType.DB2, DatabaseType.GENERIC);
        for (DatabaseType database : priority) {
            if (!enabled.contains(database)) continue;
            for (Pattern pattern : patterns.getOrDefault(database, List.of())) {
                Matcher testMatch = pattern.matcher(tested);
                if (!testMatch.find()) continue;
                if (pattern.matcher(baseline).find()) continue;
                String signature = testMatch.group();
                return Optional.of(new DatabaseErrorMatch(database, signature,
                        evidence(tested, testMatch.start(), testMatch.end())));
            }
        }
        return Optional.empty();
    }

    private String evidence(String body, int matchStart, int matchEnd) {
        int start = Math.max(0, matchStart - 140);
        int end = Math.min(body.length(), matchEnd + 140);
        return body.substring(start, end).replace("\r", "\\r").replace("\n", "\\n");
    }

    private Map<DatabaseType, List<Pattern>> patterns() {
        Map<DatabaseType, List<Pattern>> values = new EnumMap<>(DatabaseType.class);
        values.put(DatabaseType.MYSQL, compile(
                "You have an error in your SQL syntax",
                "check the manual that corresponds to your (?:MySQL|MariaDB) server version",
                "mysql_(?:fetch|query|num_rows)",
                "com\\.mysql\\.(?:jdbc|cj\\.jdbc)"));
        values.put(DatabaseType.POSTGRESQL, compile(
                "org\\.postgresql\\.util\\.PSQLException",
                "PostgreSQL[^\\r\\n]{0,80}(?:ERROR|Exception)",
                "unterminated quoted string at or near",
                "syntax error at or near"));
        values.put(DatabaseType.MSSQL, compile(
                "Unclosed quotation mark after the character string",
                "Microsoft OLE DB Provider for SQL Server",
                "System\\.Data\\.SqlClient\\.SqlException",
                "com\\.microsoft\\.sqlserver\\.jdbc\\.SQLServerException"));
        values.put(DatabaseType.ORACLE, compile(
                "ORA-\\d{5}",
                "oracle\\.jdbc(?:\\.driver)?",
                "Oracle error"));
        values.put(DatabaseType.SQLITE, compile(
                "SQLiteException",
                "sqlite3\\.OperationalError",
                "SQLite/JDBCDriver",
                "near [\"'][^\"']+[\"']:\\s*syntax error"));
        values.put(DatabaseType.DB2, compile(
                "SQLCODE\\s*=\\s*-\\d+",
                "com\\.ibm\\.db2\\.jcc",
                "DB2 SQL error"));
        values.put(DatabaseType.GENERIC, compile(
                "java\\.sql\\.SQLException",
                "org\\.hibernate\\.(?:exception|JDBCException)",
                "ODBC SQL",
                "SQLSTATE\\s*[\\[(:=]"));
        return values;
    }

    private List<Pattern> compile(String... expressions) {
        List<Pattern> result = new ArrayList<>();
        for (String expression : expressions) {
            result.add(Pattern.compile(expression, Pattern.CASE_INSENSITIVE));
        }
        return List.copyOf(result);
    }
}
