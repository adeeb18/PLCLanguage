
package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> field = new ArrayList<>();
        List<Ast.Method> method = new ArrayList<>();
        boolean methodPassed = false;
        while (tokens.has(0)){
            if (peek("LET")){
                if(methodPassed){
                    throw new ParseException("Recieved fields after methods", tokens.get(0).getIndex());
                }
                field.add(parseField());
            }
            else if (peek("DEF")){
                method.add(parseMethod());
                methodPassed = true;
            }
        }

        return new Ast.Source(field, method);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        String name = "";
        String type = "";
        match("LET");

        Optional<Ast.Expr> val = Optional.empty();
        if (match(Token.Type.IDENTIFIER)){
            name = tokens.get(-1).getLiteral();
            if (match(":")) {
                if(match(Token.Type.IDENTIFIER)) {
                    type = tokens.get(-1).getLiteral();
                    if (match("=")) {
                        if (!peek(";")) {
                            val = Optional.of(parseExpression());
                        } else {
                            // Throw error, no value (LET x =;)
                            throw new ParseException("No value entered", tokens.get(0).getIndex());
                        }
                    }
                }
                else {
                    throw new ParseException("Missing Type", tokens.get(0).getIndex());
                }
            }
            else {
                throw new ParseException("Missing colon", tokens.get(0).getIndex());
            }

            if (!peek(";") && tokens.has(0)){
                throw new ParseException("Missing equal sign", tokens.get(0).getIndex());
            }

            if (!match(";")){
                // Throw error, no semicolon (LET x = 2 )
                int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("No semicolon", index);
            }

        }
        else {
            // Error, no identifier (LET )
            throw new ParseException("No variable name found", tokens.get(0).getIndex());
        }
        return new Ast.Field(name, type, val);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        match("DEF");
        // Layout: (DEF add(num1, num2) DO return num1 + num2 END)
        String name = "";
        List<String> parameters = new ArrayList<>();
        List<Ast.Stmt> statements = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();
        Optional<String> returnType = Optional.empty();

        if (peek(Token.Type.IDENTIFIER)){
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

        }
        else {
            // Error, no name for method (def (asd) END)
            throw new ParseException("No method name", tokens.get(0).getIndex());
        }

        if (!match("(")){
            // Error, no opening parentheses!
            throw new ParseException("No opening parantheses", tokens.get(0).getIndex());
        }

        if (peek(Token.Type.IDENTIFIER)){
            parameters.add(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);
            if (match(":")){
                if(peek(Token.Type.IDENTIFIER)) {
                    parameterTypes.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);
                    while (match(",")){
                        if (match(Token.Type.IDENTIFIER)){
                            parameters.add(tokens.get(-1).getLiteral());
                            if(match(":")) {
                                if(match(Token.Type.IDENTIFIER)) {
                                    parameterTypes.add(tokens.get(-1).getLiteral());
                                }
                                else {
                                    throw new ParseException("Missing Type", tokens.get(0).getIndex());
                                }
                            }
                            else {
                                throw new ParseException("Missing Type", tokens.get(0).getIndex());
                            }
                        }
                        else {
                            // Error, no identifier after comma (DEF add(num1, ) ...)
                            throw new ParseException("Dangling comma", tokens.get(0).getIndex());
                        }
                    }
                }
                else {
                    throw new ParseException("Missing Type", tokens.get(0).getIndex());
                }

            }
            else {
                throw new ParseException("Missing colon", tokens.get(0).getIndex());
            }
        }



        if (match(Token.Type.IDENTIFIER)){
            // Error, two identifiers without a delimiting comma (DEF add(num1 num2)...
            throw new ParseException("No comma", tokens.get(0).getIndex());
        }

        if (!match(")")){
            // Error, no ending parentheses (DEF add(num1, num2 ...)
            throw new ParseException("No closing parentheses", tokens.get(0).getIndex());
        }

        if (match(":")){
            if(match(Token.Type.IDENTIFIER)){
                returnType = Optional.of(tokens.get(-1).getLiteral());
            }
            else {
                throw new ParseException("Missing Type Name", tokens.get(0).getIndex());
            }
        }

        if (!match("DO")){
            // Error, no DO (DEF add(num1, num2) return num1 + num2 END)
            throw new ParseException("No DO found", tokens.get(0).getIndex());
        }

        while (tokens.has(0) && !peek("END")){
            statements.add(parseStatement());
        }

        if (!match("END")){
            // Error, no END (DEF add(num1, num2) return num1 + num2 )
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("No END found", index);
        }

        return new Ast.Method(name, parameters, parameterTypes,returnType,statements);

    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if(peek("LET")) {
            return parseDeclarationStatement();
        }
        else if (peek("IF")) {
            return parseIfStatement();
        }
        else if (peek("FOR")) {
            return parseForStatement();
        }
        else if (peek("WHILE")) {
            return parseWhileStatement();
        }
        else if (peek("RETURN")) {
            return parseReturnStatement();
        }

        Ast.Expr beg = parseExpression();

        if (match("=")){
            Ast.Expr end = parseExpression();
            if (!match(";")) {
                // Throw parse error, it's missing a semicolon
                int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                throw new ParseException("Missing semicolon", index);
            }
            return new Ast.Stmt.Assignment(beg, end);
        }
        else if (!match(";")){
            // Throw parse error, it's missing a semicolon
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing semicolon", index);
        }

        return new Ast.Stmt.Expression(beg);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        Optional<String> typeName = Optional.empty();

        match("LET");


        if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            if (match(":")){
                if(match(Token.Type.IDENTIFIER)){
                    typeName = Optional.of(tokens.get(-1).getLiteral());
                }
                else {
                    throw new ParseException("Missing Type Name", tokens.get(0).getIndex());
                }
            }


            if (match("=")) {
                String value = tokens.get(0).getLiteral();
                Ast.Expr val = parseExpression();
                if (match(";")) {
                    return new Ast.Stmt.Declaration(name, typeName, Optional.of(val));
                }
                else {
                    int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                    throw new ParseException("Missing semicolon", index);
                }

            }
            else {
                if (match(";")) {

                    return new Ast.Stmt.Declaration(name, typeName, Optional.empty());
                }
                else {
                    int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
                    throw new ParseException("Missing semicolon", index);
                }
            }
        }
        else {
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("No identifier found", index);
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        Ast.Expr condition;
        List<Ast.Stmt> then = new ArrayList<>();
        List<Ast.Stmt> els = new ArrayList<>();

        match("IF");

        if (tokens.has(0) && peek(Token.Type.IDENTIFIER)){
            condition = parseExpression();
        }
        else {
            // Error, nothing
            throw new ParseException("Missing everything", 2);
        }

        if (!match("DO")){
            // Error, no DO found
            if(tokens.has(0)) {
                throw new ParseException("No DO found", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("No DO found", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }

        while (tokens.has(0) && !peek("ELSE") && !peek("END")){
            then.add(parseStatement());
        }

        if (match("ELSE")){
            while (tokens.has(0) && !peek("END")){
                els.add(parseStatement());
            }
        }

        if (!match("END")){
            // Error, no END
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("No END found", index);
        }

        return new Ast.Stmt.If(condition, then, els);

    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        String name = "";
        Ast.Expr Val;
        List<Ast.Stmt> Statements = new ArrayList<>();

        match("FOR");

        match(Token.Type.IDENTIFIER);
        name = tokens.get(-1).getLiteral();

        if (!match("IN")){
            throw new ParseException("No IN found", tokens.get(0).getIndex());
        }

        if (peek("DO")){
            throw new ParseException("No value found", tokens.get(0).getIndex());
        }

        Val = parseExpression();

        if (!match("DO")){
            throw new ParseException("No DO found", tokens.get(0).getIndex());
        }

        while (tokens.has(0) && !peek("END")){
            Statements.add(parseStatement());
        }

        if (!match("END")){
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("No END found", index);
        }

        return new Ast.Stmt.For(name, Val, Statements);

    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        match("WHILE");

        if (match("DO")){
            // Error, no condition set
            throw new ParseException("No condition set", tokens.get(0).getIndex());
        }
        Ast.Expr condition = parseExpression();
        List<Ast.Stmt> statements = new ArrayList<>();

        if (!match("DO")){
            // Error, no DO after condition
            throw new ParseException("No DO after condition", tokens.get(0).getIndex());
        }

        while (tokens.has(0) && !peek("END")){
            statements.add(parseStatement());
        }

        if (!match("END")){
            // Error, no END
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("No END found", index);
        }

        return new Ast.Stmt.While(condition, statements);

    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expr returnVal = parseExpression();
        if (match(";")) {
            return new Ast.Stmt.Return(returnVal);
        }
        else {
            int index = tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
            throw new ParseException("Missing semicolon", index);
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        Ast.Expr val = parseLogicalExpression();
        return val;
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {

        Ast.Expr beg = parseEqualityExpression();
        Ast.Expr.Binary accumulate = null;


        while (match("AND") || match("OR")){

            String op = tokens.get(-1).getLiteral();

            if (!tokens.has(0)){
                int index = tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex();
                throw new ParseException("Dangling Operator", index);
            }

            Ast.Expr end = parseEqualityExpression();

            if (accumulate == null){
                accumulate = new Ast.Expr.Binary(op, beg, end);
            }
            else{
                accumulate = new Ast.Expr.Binary(op, accumulate, end);
            }
        }

        if (accumulate == null){
            return beg;
        }
        return accumulate;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr left = parseAdditiveExpression();
        Ast.Expr.Binary accumulate = null;

        while (match("<")
                || match(">")
                || match("<=")
                || match(">=")
                || match("==")
                || match("!=")){

            String op = tokens.get(-1).getLiteral();

            if (!tokens.has(0)){
                int index = tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex();
                throw new ParseException("Dangling Operator", index);
            }


            Ast.Expr right = parseAdditiveExpression();

            if (accumulate == null){
                accumulate = new Ast.Expr.Binary(op, left, right);
            }
            else{
                accumulate = new Ast.Expr.Binary(op, accumulate, right);
            }

        }

        if (accumulate == null){
            return left;
        }
        return accumulate;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr left = parseMultiplicativeExpression();
        Ast.Expr.Binary accumulate = null;

        while (match("+") || match("-")){
            String op = tokens.get(-1).getLiteral();

            if (!tokens.has(0)){
                int index = tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex();
                throw new ParseException("Dangling Operator", index);
            }

            Ast.Expr right = parseMultiplicativeExpression();


            if (accumulate == null){
                accumulate = new Ast.Expr.Binary(op, left, right);
            }
            else{
                accumulate = new Ast.Expr.Binary(op, accumulate, right);
            }
        }

        if (accumulate == null){
            return left;
        }
        return accumulate;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr left = parseSecondaryExpression();
        Ast.Expr.Binary accumulate = null;

        while (match("*") || match("/")){
            String op = tokens.get(-1).getLiteral();

            if (!tokens.has(0)){
                int index = tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex();
                throw new ParseException("Dangling Operator", index);
            }

            Ast.Expr right = parseSecondaryExpression();

            if (accumulate == null){
                accumulate = new Ast.Expr.Binary(op, left, right);
            }
            else{
                accumulate = new Ast.Expr.Binary(op, accumulate, right);
            }
        }

        if (accumulate == null){
            return left;
        }
        return accumulate;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        List<Ast.Expr> param = new ArrayList<Ast.Expr>();
        Ast.Expr left = parsePrimaryExpression();
        String func = null;

        while (match(".")){
            if (match(Token.Type.IDENTIFIER)){
                func = tokens.get(-1).getLiteral();

                if (match("(")){
                    if (!peek(")")){
                        param.add(parseExpression());

                        while (match(",")) {
                            param.add(parseExpression());
                        }
                    }
                    if (!match(")")){
                        // Throw parse error, no closing parentheses
                        throw new ParseException("Missing closing parentheses", tokens.get(0).getIndex());
                    }
                    return new Ast.Expr.Function(Optional.of(left), func, param);
                }
                else {
                    left = new Ast.Expr.Access(Optional.of(left), func);
                }

            }
            else {
                //Throw Parse Error, Invalid token type
                throw new ParseException("Invalid Token Type", -1);
            }
        }

        return left;

    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if (match("NIL")){
            return new Ast.Expr.Literal(null);
        }
        else if (match("TRUE")){
            return new Ast.Expr.Literal(true);
        }
        else if (match("FALSE")){
            return new Ast.Expr.Literal(false);
        }
        else if (match(Token.Type.INTEGER)){
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) {
            char chars = tokens.get(-1).getLiteral().charAt(1);
            if (chars == '\\') {
                char x = tokens.get(-1).getLiteral().charAt(2);
                if (x == 'n') {
                    chars = '\n';
                }
                else if (x == 'b') {
                    chars = '\b';
                }
                else if (x == 'r') {
                    chars = '\r';
                }
                else if (x == 't') {
                    chars = '\t';
                }
                else if (x == '\'') {
                    chars = '\'';
                }
                else if (x == '\"') {
                    chars = '\"';
                }
                else if (x == '\\') {
                    chars = '\\';
                }
            }

            return new Ast.Expr.Literal(chars);
        }
        else if (match(Token.Type.STRING)) {
            String it = tokens.get(-1).getLiteral();
            it = it.substring(1, it.length() - 1);
            it = it.replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\b", "\b")
                    .replace("\\r", "\r")
                    .replace("\\\'", "\'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\\\");
            return new Ast.Expr.Literal(it);
        }
        else if (match("(")){
            Ast.Expr expr = parseExpression();
            if (!match(")")){
                if(tokens.has(0)) {
                    throw new ParseException("No ending Parenthesis", tokens.get(0).getIndex());
                }
                else {
                    throw new ParseException("No ending Parenthesis", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            else {
                return new Ast.Expr.Group(expr);
            }
        }
        else if (match(Token.Type.IDENTIFIER)){
            String identity = tokens.get(-1).getLiteral();

            if (match("(")){
                if (match(")")){
                    List<Ast.Expr> empty = new ArrayList<Ast.Expr>();
                    return new Ast.Expr.Function(Optional.empty(), identity, empty);
                }
                else{
                    Ast.Expr express = parseExpression();
                    List<Ast.Expr> params = new ArrayList<Ast.Expr>();
                    params.add(express);

                    while (match(",")){
                        params.add(parseExpression());
                    }
                    if (match(")")){
                        return new Ast.Expr.Function(Optional.empty(), identity, params);
                    }
                    else {
                        //Throw parse error, no ending parentheses TODO
                        throw new ParseException("No ending Parenthesis", tokens.get(0).getIndex());
                    }
                }
            }
            else {
                return new Ast.Expr.Access(Optional.empty(), identity);
            }
        }
        throw new ParseException("Invalid Primary Expression", tokens.get(0).getIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++){
            if (!tokens.has(i)){
                return false;
            }

            else if (patterns[i] instanceof  Token.Type){
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }

            else if (patterns[i] instanceof  String){
                if (!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            }
            else{
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}