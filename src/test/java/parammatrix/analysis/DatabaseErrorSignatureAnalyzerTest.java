package parammatrix.analysis;

import org.junit.jupiter.api.Test;
import parammatrix.testing.database.DatabaseType;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseErrorSignatureAnalyzerTest {
    private final DatabaseErrorSignatureAnalyzer analyzer = new DatabaseErrorSignatureAnalyzer();

    @Test
    void identifiesNewMysqlError() {
        var match = analyzer.findNewError("normal page",
                "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version",
                EnumSet.allOf(DatabaseType.class));
        assertThat(match).isPresent();
        assertThat(match.orElseThrow().database()).isEqualTo(DatabaseType.MYSQL);
    }

    @Test
    void ignoresSignatureAlreadyPresentInBaseline() {
        String error = "java.sql.SQLException: bad query";
        assertThat(analyzer.findNewError(error, error + " repeated",
                EnumSet.allOf(DatabaseType.class))).isEmpty();
    }

    @Test
    void respectsSelectedSignatureFamilies() {
        assertThat(analyzer.findNewError("normal", "ORA-00933: SQL command not properly ended",
                EnumSet.of(DatabaseType.MYSQL))).isEmpty();
    }
}

