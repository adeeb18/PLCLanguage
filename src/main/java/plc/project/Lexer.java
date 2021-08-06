package plc.project;

import java.util.List;
import java.util.ArrayList;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> list = new ArrayList<Token>();

        if (!chars.has(0)){
            throw new ParseException("Empty entry", 0);
        }

        while (chars.has(0)) {
            if (!(peek(" ") || peek("\t") || peek("\n") || peek("\r") || peek("\b"))) {
                list.add(lexToken());
            }
            else {
                chars.advance();
                chars.skip();
            }
        }

        return list;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        Token Ret = null;

        // Identifier case
        if (peek("[A-Za-z_]")){
            Ret = lexIdentifier();
        }

        // Number case
        else if (peek("[+-]") || peek("[0-9]+")){
            if (chars.get(0) == '+' || chars.get(0) == '-') {
                if (chars.has(1) && Character.isDigit(chars.get(1))) {
                    if (Character.isDigit(chars.get(1))) {
                        Ret = lexNumber();
                    }
                }
                else {
                    Ret = lexOperator();
                }
            }
            else {
                Ret = lexNumber();
            }

        }

        // Character case
        else if (peek("[']")){
            Ret = lexCharacter();
        }

        // String case
        else if (peek("\"")){
            Ret = lexString();
        }

        // Operator case
        else {
            Ret = lexOperator();
        }

        return Ret;
    }

    public Token lexIdentifier() {
        match("[A-Za-z_]");
        while(match("[A-Za-z0-9_-]*"));
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        match("[+-]?");
        while(match("[0-9]*"));
        if (!peek("\\.")){
            return chars.emit(Token.Type.INTEGER);
        }
        if (chars.has(1) && Character.isDigit(chars.get(1))){
            match("[\\.]");
            while (match("[0-9]*"));
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        match("'");

        if (match("[\n\r]")){
            return chars.emit(null);
        }
        if (match("\\\\")){
            if (match("[bnrt'\"\\\\]") && match("'")){
                return chars.emit(Token.Type.CHARACTER);
            }
            else{
                return chars.emit(null);
            }
        }
        else if (!match("[^']")){
            return chars.emit(null);
        };
        if (match("'")){
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("Unterminated single quote in character", chars.index);
    }

    public Token lexString() {
        match("\"");

        while (match("[^\"]")){
            if (match("[\n\r]")){
                return chars.emit(null);
            }
            if (match("\\\\")){
                if (peek("[^bnrt\"'\\\\]")){
                    throw new ParseException("Invalid Escape", chars.index);
                }
                match("[bnrt\"'\\\\]");
            }
        }

        if (!match("\"")){
            throw new ParseException("Unterminated quote in a String", chars.index);

        }
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        System.out.println("Ran into escape! Quick! Scatter!"); //TODO
    }

    public Token lexOperator() {
        if(match("[<>!=]")) {
            match("=");
        }
        else {
            match("[^ \\t\\n\\r\\f]");
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {


        for (int i = 0; i < patterns.length; i++){

            if ( !chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){

                return false;

            }

        }

        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {

        boolean peek = peek(patterns);

        if (peek){
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        // The string itself
        private final String input;

        // The index in the string you are currently on
        private int index = 0;

        // The length of the string in it's entirety
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        // Checks if the input string has a given amount of characters left
        public boolean has(int offset) {
            return index + offset < input.length();
        }

        // Return character at given offset position specified
        public char get(int offset) {
            return input.charAt(index + offset);
        }

        // Advanced to the next character position in the string
        public void advance() {
            index++;
            length++;
        }

        // Reset the size of the current token to 0, you'd use this alongside advance
        public void skip() {
            length = 0;
        }

        // Instantiates the current token
        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}