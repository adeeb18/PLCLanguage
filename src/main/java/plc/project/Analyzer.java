package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    //DONE
    @Override
    public Void visit(Ast.Source ast) {
        for(int i=0; i<ast.getFields().size(); i++) {
            visit(ast.getFields().get(i));
        }
        for(int i=0; i<ast.getMethods().size(); i++) {
            visit(ast.getMethods().get(i));
        }
        scope.lookupFunction("main", 0);
        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }
        ast.setVariable( scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL));

        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Method ast) {

        List<Environment.Type> parameterTypes = new ArrayList<>();
        Environment.Type returnType = Environment.Type.NIL;


        for (int i= 0; i < ast.getParameterTypeNames().size(); i++) {
            parameterTypes.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }

        if(ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }

        ast.setFunction(scope.defineFunction(ast.getName(),ast.getName(),parameterTypes, returnType, args->Environment.NIL ));

        try {
            scope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i),ast.getParameters().get(i),parameterTypes.get(i), Environment.NIL);
            }
            method = ast;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
        }
        finally {
            scope = scope.getParent();
            method = null;
        }
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        if(!ast.getExpression().getClass().equals(Ast.Expr.Function.class)) {
            throw new RuntimeException("Not an Ast.Expr.Function!");
        }
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        /* 'LET' identifier (':' identifier)? ('=' expression)?
        Optional<String> opTypeName = ast.getTypeName();
        Optional<Ast.Expr> optValue = ast.getValue();

        if(!opTypeName.isPresent() && !optValue.isPresent()){
            throw new RuntimeException("Declaration must have type or value to infer type.");
        }

        Environment.Type type = null;
        if(opTypeName.isPresent()) {
           type = Environment.getType(opTypeName.get());
        }
         */

        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration must have type or value to infer type.");
        }

        Environment.Type type = null;

        if(ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (type == null) {
                type = ast.getValue().get().getType();
            }
            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());
        if(!ast.getReceiver().getClass().equals(Ast.Expr.Access.class)) {
            throw new RuntimeException("Not an Ast.Exp.Access");
        }
        requireAssignable(ast.getReceiver().getType(),ast.getValue().getType());
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if(ast.getThenStatements().size() == 0) {
            throw new RuntimeException("Then Statements empty");
        }

        try{
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getThenStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }

        try{
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getElseStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }

        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE,ast.getValue().getType());

        if(ast.getStatements().size() == 0) {
            throw new RuntimeException("Statements empty");
        }

        try{
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(),ast.getName(), Environment.Type.INTEGER,Environment.NIL);
            for (Ast.Stmt stmt : ast.getStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }


        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN,ast.getCondition().getType());
        try{
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()){
                visit(stmt);
            }
        }
        finally{
            scope = scope.getParent();
        }
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());
        requireAssignable(method.getFunction().getReturnType(), ast.getValue().getType());
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
        }
        else if (ast.getLiteral() instanceof BigInteger) {
            BigInteger val = (BigInteger) ast.getLiteral();
            BigInteger max = new BigInteger(String.valueOf(Integer.MAX_VALUE));
            BigInteger min = new BigInteger(String.valueOf(Integer.MIN_VALUE));
            boolean OOR = val.compareTo(max) > 0 || val.compareTo(min) < 0;
            if(OOR) {
                throw new RuntimeException("Integer out of Range");
            }
            else {
                ast.setType(Environment.Type.INTEGER);
            }

        }
        else if (ast.getLiteral() instanceof BigDecimal) {
            double val = ((BigDecimal) ast.getLiteral()).doubleValue();
            if(val == Double.NEGATIVE_INFINITY || val == Double.POSITIVE_INFINITY) {
                throw new RuntimeException("Decimal out of Range");
            }
            else {
                ast.setType(Environment.Type.DECIMAL);
            }

        }
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Expr.Group ast) {
        visit(ast.getExpression());

        if(!ast.getExpression().getClass().equals(Ast.Expr.Binary.class)) {
            throw new RuntimeException("Expression is not Binary");
        }
        ast.setType(ast.getExpression().getType());

        return null;
    }

    //DONE... How to do comparable?
    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());

        //NEW EDIT 8/3/2021
        if(op.equals("AND") || op.equals("OR")) {

            requireAssignable(Environment.Type.BOOLEAN,ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN,ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);

        }
        //NEW EDIT 8/3/2021
        else if (op.equals(">") ||op.equals("<") ||op.equals(">=") ||op.equals("<=") ||op.equals("==") ||op.equals("!=")){

            requireAssignable(Environment.Type.COMPARABLE,ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE,ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (op.equals("+")) {
            if(ast.getRight().getType().equals(Environment.Type.STRING) || ast.getLeft().getType().equals(Environment.Type.STRING) ) {
                ast.setType(Environment.Type.STRING);
            }
            else if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if(ast.getRight().getType().equals(ast.getLeft().getType())){
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    throw new RuntimeException("Left is integer or Decimal but right is not the Same");
                }
            }
            else {
                throw new RuntimeException("Left is not integer or Decimal or String");
            }
        }
        else if (op.equals("-") || op.equals("*") || op.equals("/")) {
            if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if(ast.getRight().getType().equals(ast.getLeft().getType())){
                    ast.setType(ast.getLeft().getType());
                }
                else {
                    throw new RuntimeException("Left is integer or Decimal but right is not the Same");
                }
            }
            else {
                throw new RuntimeException("Left is not integer or Decimal or String");
            }
        }
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setVariable(ast.getReceiver().get().getType().getField(ast.getName()));
        }
        else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
    }

    //DONE
    @Override
    public Void visit(Ast.Expr.Function ast) {

        List<Ast.Expr> args= ast.getArguments();

        if(ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setFunction(ast.getReceiver().get().getType().getMethod(ast.getName(), args.size()));
            for (int i = 0; i < args.size(); i++) {
                visit(ast.getArguments().get(i));
            }
            for (int i = 1; i < args.size(); i++) {
                requireAssignable(scope.lookupFunction(ast.getName(), args.size()).getParameterTypes().get(i),args.get(i).getType());
            }

        }
        
        else {
            ast.setFunction(scope.lookupFunction(ast.getName(), args.size()));
            for (int i = 0; i < args.size(); i++) {
                visit(ast.getArguments().get(i));
            }
            for (int i = 0; i < args.size(); i++) {
                requireAssignable(scope.lookupFunction(ast.getName(), args.size()).getParameterTypes().get(i),args.get(i).getType());
            }
        }

        return null;
    }

    //Is this right? Is comparable allowed with all types?
    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type)) {
            return;
        }
        if(target.equals(Environment.Type.ANY)) {
            return;
        }
        if(target.equals(Environment.Type.COMPARABLE)) {
            if(type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)) {
                return;
            }
            else {
                throw new RuntimeException("Not Assignable: Comparable with Wrong Type");
            }
        }
        throw new RuntimeException("Not Assignable");
    }

}
