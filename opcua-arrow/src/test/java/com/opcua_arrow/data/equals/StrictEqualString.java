package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StrictEqualStringTest {

    private StrictEqualString strictEqualString;

    @BeforeEach
    void setUp() {
        strictEqualString = new StrictEqualString();
    }

    @Test
    @DisplayName("Should return true for identical strings")
    void testIdenticalStrings() {
        String value1 = "Hello World";
        String value2 = "Hello World";

        assertTrue(strictEqualString.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return false for different strings")
    void testDifferentStrings() {
        String value1 = "Hello World";
        String value2 = "Hello World!";

        assertFalse(strictEqualString.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return true for empty strings")
    void testEmptyStrings() {
        String value1 = "";
        String value2 = "";

        assertTrue(strictEqualString.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should be case sensitive")
    void testCaseSensitive() {
        String value1 = "Hello";
        String value2 = "hello";

        assertFalse(strictEqualString.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should handle strings with spaces correctly")
    void testStringsWithSpaces() {
        String value1 = "Hello World";
        String value2 = "Hello  World"; // Two spaces

        assertFalse(strictEqualString.isSameValue(value2, value1));

        String value3 = " Hello World"; // Leading space
        assertFalse(strictEqualString.isSameValue(value3, value1));

        String value4 = "Hello World "; // Trailing space
        assertFalse(strictEqualString.isSameValue(value4, value1));
    }

    @Test
    @DisplayName("Should handle single character strings")
    void testSingleCharacterStrings() {
        String value1 = "a";
        String value2 = "a";
        String value3 = "A";
        String value4 = "b";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
        assertFalse(strictEqualString.isSameValue(value4, value1));
    }

    @Test
    @DisplayName("Should handle strings with special characters")
    void testSpecialCharacters() {
        String value1 = "Hello\nWorld";
        String value2 = "Hello\nWorld";
        String value3 = "Hello\rWorld";
        String value4 = "Hello\tWorld";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
        assertFalse(strictEqualString.isSameValue(value4, value1));
    }

    @Test
    @DisplayName("Should handle Unicode strings correctly")
    void testUnicodeStrings() {
        String value1 = "Hello 世界 🌍";
        String value2 = "Hello 世界 🌍";
        String value3 = "Hello 世界 🌎";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle very long strings")
    void testLongStrings() {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb1.append("a");
            sb2.append("a");
        }

        String value1 = sb1.toString();
        String value2 = sb2.toString();

        assertTrue(strictEqualString.isSameValue(value2, value1));

        // Change last character
        sb2.setCharAt(9999, 'b');
        String value3 = sb2.toString();
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should return true for same string reference")
    void testSameReference() {
        String value = "Test String";

        assertTrue(strictEqualString.isSameValue(value, value));
    }

    @Test
    @DisplayName("Should handle string pool correctly")
    void testStringPool() {
        String value1 = "Hello";
        String value2 = "Hello"; // From string pool
        String value3 = new String("Hello"); // New object

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertTrue(strictEqualString.isSameValue(value3, value1)); // equals() compares content
    }

    @ParameterizedTest
    @CsvSource({
            "'Hello', 'Hello', true",
            "'Hello', 'hello', false",
            "'', '', true",
            "' ', '  ', false",
            "'abc', 'abc ', false",
            "' abc', 'abc', false"
    })
    @DisplayName("Parameterized test for various string combinations")
    void testStringComparisons(String str1, String str2, boolean expected) {
        assertEquals(expected, strictEqualString.isSameValue(str2, str1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "  ", "\t", "\n", "\r", "a", "ABC", "123", "!@#$%" })
    @DisplayName("Should return true when comparing same string value with itself")
    void testSameStringValues(String value) {
        assertTrue(strictEqualString.isSameValue(value, value));
        assertTrue(strictEqualString.isSameValue(new String(value), value));
    }

    @Test
    @DisplayName("Should handle strings with null characters")
    void testNullCharacterInString() {
        String value1 = "Hello\0World";
        String value2 = "Hello\0World";
        String value3 = "HelloWorld";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle strings with all whitespace types")
    void testAllWhitespaceTypes() {
        String value1 = "a b\tc\nd\re\ff";
        String value2 = "a b\tc\nd\re\ff";
        String value3 = "a b c d e f";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should be consistent with multiple calls")
    void testConsistency() {
        String value1 = "Test String";
        String value2 = "Test String";
        String value3 = "Different String";

        // Multiple calls should return same result
        for (int i = 0; i < 10; i++) {
            assertTrue(strictEqualString.isSameValue(value2, value1));
            assertFalse(strictEqualString.isSameValue(value3, value1));
        }
    }

    @Test
    @DisplayName("Should handle string concatenation correctly")
    void testStringConcatenation() {
        String value1 = "Hello" + "World";
        String value2 = "HelloWorld";
        String value3 = "Hello" + " " + "World";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle StringBuilder and StringBuffer results")
    void testStringBuilderBuffer() {
        StringBuilder sb = new StringBuilder("Test");
        sb.append("String");
        String value1 = sb.toString();

        StringBuffer sbf = new StringBuffer("Test");
        sbf.append("String");
        String value2 = sbf.toString();

        String value3 = "TestString";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertTrue(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle substring correctly")
    void testSubstring() {
        String original = "Hello World";
        String value1 = original.substring(0, 5); // "Hello"
        String value2 = "Hello";
        String value3 = original.substring(6); // "World"

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle strings from different sources")
    void testDifferentSources() {
        String value1 = String.valueOf(123);
        String value2 = Integer.toString(123);
        String value3 = "123";
        String value4 = "124";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertTrue(strictEqualString.isSameValue(value3, value1));
        assertFalse(strictEqualString.isSameValue(value4, value1));
    }

    @Test
    @DisplayName("Complex scenario with various string operations")
    void testComplexScenario() {
        // Empty string
        assertTrue(strictEqualString.isSameValue("", ""));

        // Single space
        assertTrue(strictEqualString.isSameValue(" ", " "));
        assertFalse(strictEqualString.isSameValue(" ", ""));

        // Trimmed strings
        String untrimmed = "  Hello  ";
        String trimmed = untrimmed.trim();
        assertFalse(strictEqualString.isSameValue(untrimmed, trimmed));
        assertTrue(strictEqualString.isSameValue("Hello", trimmed));

        // Replace operations
        String original = "Hello World";
        String replaced = original.replace("o", "0");
        assertFalse(strictEqualString.isSameValue(original, replaced));
        assertTrue(strictEqualString.isSameValue("Hell0 W0rld", replaced));

        // Case transformations
        String upper = "HELLO";
        String lower = upper.toLowerCase();
        assertFalse(strictEqualString.isSameValue(upper, lower));
        assertTrue(strictEqualString.isSameValue("hello", lower));
    }

    @Test
    @DisplayName("Should handle repeated characters")
    void testRepeatedCharacters() {
        String value1 = "aaaa";
        String value2 = "aaaa";
        String value3 = "aaa";
        String value4 = "aaaaa";

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
        assertFalse(strictEqualString.isSameValue(value4, value1));
    }

    @Test
    @DisplayName("Should handle palindromes correctly")
    void testPalindromes() {
        String value1 = "racecar";
        String value2 = "racecar";
        String value3 = "raceca"; // Not complete

        assertTrue(strictEqualString.isSameValue(value2, value1));
        assertFalse(strictEqualString.isSameValue(value3, value1));
    }
}
