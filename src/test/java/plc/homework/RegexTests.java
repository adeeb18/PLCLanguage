package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                //given
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),

                //new
                Arguments.of("Numeric Only Username" , "12345678@gmail.com", true),
                Arguments.of("Other Characters Username", "the-legend._@gmail.com", true),
                Arguments.of("Alphanumeric Domain", "thelegend27@123gmail.com", true),
                Arguments.of("Numeric Only Domain", "thelegend27@123.com", true),
                Arguments.of("2 character Domain", "thelegend27@gmail.co", true),

                Arguments.of("Missing Username", "@gmail.com", false),
                Arguments.of("Symbols in username", "thelegend27&$@gmail.com", false),
                Arguments.of("Symbols in Domain", "thelegend27@gmail$%.com", false),
                Arguments.of("Non-alphabetic after dot", "thelegend27@gmail.123", false),
                Arguments.of("4 character domain", "thelegend27@gmail.coms", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //given
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),

                //new
                Arguments.of("Numeric Only 10 Characters", "1234567890", true),
                Arguments.of("16 Characters", "automobilesrcool", true),
                Arguments.of("Escapes with 10 Characters", "automobil\\", true),
                Arguments.of("Mixed Characters 10 Characters", "b23-5_,.A&", true),

                Arguments.of("21 Characters", "automobileautomobilea", false),
                Arguments.of("Numeric Only 13 Characters", "1234567890123", false),
                Arguments.of("15 Characters", "automobileautom", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                //given
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),

                //new
                Arguments.of("Empty Bracket", "[]", true),
                Arguments.of("Integers larger than 1 digit", "[12, 12, 13]", true),
                Arguments.of("Multiple Elements with Space", "[1, 2, 3]", true),
                Arguments.of("Single integer larger than 1 digit", "[12314]", true),
                Arguments.of("Mix of spaces and commas", "[1,2,3, 4]", true),

                Arguments.of("Nonnumeric", "[a,b,c]", false),
                Arguments.of("Nonintegers", "[1.1,2.1,0.1]", false),
                Arguments.of("Zero", "[1,2,0]", false),
                Arguments.of("Trailing Comma", "[1,2,3,]", false),
                Arguments.of("Elements outside of bracket", "[1, 2], 3", false),
                Arguments.of("No ending Bracket", "[1,2,3", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                //new
                Arguments.of("Single Integer", "1", true),
                Arguments.of("Integers larger than 1 digit", "150", true),
                Arguments.of("Decimal", "123.456", true),
                Arguments.of("Positive", "+1243.4124", true),
                Arguments.of("Negative", "-1.0", true),
                Arguments.of("Leading 0", "0.12345", true),
                Arguments.of("Trailing 0", "-123.000", true),

                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Only sign", "+", false),
                Arguments.of("Multiple Decimals", "3.2.1", false),
                Arguments.of("Alphabetical Characters", "ab2.7", false),
                Arguments.of("Special Characters", "%134", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                //new
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("Full String", "\"Hello, World!\"", true),
                Arguments.of("String with escape t", "\"123Hello\tHello\"", true),
                Arguments.of("String with escape backslash", "\"Hello\\\"", true),
                Arguments.of("Alphanumeric with symbols", "\"Hello%_World&\"", true),

                Arguments.of("Unterminated", "\"Hello", false),
                Arguments.of("No beginning quote", "Hello\"", false),
                Arguments.of("Numbers outside quotes", "\"Helloworld\"123", false),
                Arguments.of("Words outside quotes", "\"Hello\" World", false),
                Arguments.of("No quotes", "Hello", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */

    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
