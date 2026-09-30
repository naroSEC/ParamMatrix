package parammatrix.scan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScanOptionsTest {
    @Test
    void rejectsLineBreaksInCustomCookieHeader() {
        assertThatThrownBy(() -> new ScanOptions(true, false, true, true,
                true, false, false, ScanCookieMode.CUSTOM_HEADER,
                "example.test", "session=ok\r\nX-Injected: yes", 500, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("single line");
    }

    @Test
    void customModeRequiresHostAndCookieValue() {
        assertThatThrownBy(() -> new ScanOptions(true, false, true, true,
                true, false, false, ScanCookieMode.CUSTOM_HEADER,
                "", "session=value", 500, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target host");
    }
}
