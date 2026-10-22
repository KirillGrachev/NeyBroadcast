package eu.neydev.broadcast.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка разборки HEX-цветов: оба синтаксиса, несколько кодов в строке,
 * невалидные последовательности и очистка форматирования.
 */
class HexColorUtilTest {

    @Test
    @DisplayName("Пустой и null-текст дают пустую строку")
    void nullAndEmptyBecomeEmptyString() {
        assertEquals("", HexColorUtil.color(null));
        assertEquals("", HexColorUtil.color(""));
    }

    @Test
    @DisplayName("Обычные &-коды переводятся без HEX-участия")
    void legacyCodesAreTranslated() {
        assertEquals("§aзеленый", HexColorUtil.color("&aзеленый"));
    }

    @Test
    @DisplayName("#RRGGBB раскладывается в &x&R&R&G&G&B&B")
    void bareHexIsExpanded() {

        String colored = HexColorUtil.color("#ff0000red");

        assertTrue(colored.startsWith("§x§f§f§0§0§0§0"), "HEX развернут: " + colored);
        assertTrue(colored.endsWith("red"), "текст после кода сохранен");

    }

    @Test
    @DisplayName("&#RRGGBB не оставляет лишнего амперсанда")
    void prefixedHexKeepsNoStrayAmpersand() {

        String colored = HexColorUtil.color("&#00ff00green");

        assertTrue(colored.startsWith("§x§0§0§f§f§0§0"), "префикс съеден: " + colored);
        assertFalse(colored.contains("&"), "амперсанда в результате нет");

    }

    @Test
    @DisplayName("Несколько HEX-кодов в одной строке")
    void severalHexCodesInOneString() {

        String colored = HexColorUtil.color("#ff0000a#0000ffb");

        assertTrue(colored.contains("a"), "текст между кодами сохранен");
        assertTrue(colored.endsWith("b"), "хвост строки сохранен");
        assertEquals(2, countOccurrences(colored, "§x"), "оба кода развернуты");

    }

    @Test
    @DisplayName("Невалидный HEX не трогается: #gggggg остается текстом")
    void invalidHexIsLeftAsText() {
        assertEquals("#gggggg", HexColorUtil.color("#gggggg"));
    }

    @Test
    @DisplayName("Короткий HEX (#fff) не считается кодом")
    void shortHexIsNotACode() {
        assertEquals("#fff", HexColorUtil.color("#fff"));
    }

    @Test
    @DisplayName("strip убирает и legacy, и HEX")
    void stripRemovesAllFormatting() {

        assertEquals("red", HexColorUtil.strip("#ff0000red"));
        assertEquals("red", HexColorUtil.strip("&cred"));
        assertEquals("", HexColorUtil.strip(null));
        assertEquals("", HexColorUtil.strip(""));

    }

    private int countOccurrences(String text, String token) {

        int count = 0;
        int index = text.indexOf(token);

        while (index >= 0) {

            count++;
            index = text.indexOf(token, index + token.length());

        }

        return count;

    }

}
