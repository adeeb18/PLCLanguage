package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field fld : ast.getFields()) {
            visit(fld);
        }
        for (Ast.Method mthd : ast.getMethods()) {
            visit(mthd);
        }

        return scope.lookupFunction("main",0).invoke(new ArrayList<>());

    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Scope capture = scope;

        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope curr = scope;
            scope = new Scope(capture);

            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), args.get(i));
            }

            try {
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            catch(Return e) {
                return e.value;
            }
            finally {
                scope = curr;
            }
            return Environment.NIL;
        });

        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if(ast.getReceiver().getClass().equals(Ast.Expr.Access.class)){
            if (((Ast.Expr.Access) ast.getReceiver()).getReceiver().isPresent()){
                visit(((Ast.Expr.Access) ast.getReceiver()).getReceiver().get()).setField(((Ast.Expr.Access) ast.getReceiver()).getName(), visit(ast.getValue()));
            }
            else {
                scope.lookupVariable(((Ast.Expr.Access) ast.getReceiver()).getName()).setValue(visit(ast.getValue()));
            }
        }
        else {
            throw new RuntimeException();
        }

        return Environment.NIL;

    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        else if(!requireType(Boolean.class,visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable iter = requireType(Iterable.class,visit(ast.getValue()));
        for(Object obj : iter) {
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), (Environment.PlcObject) obj);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }

       }

        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        Environment.PlcObject obj = visit(ast.getValue());
        throw new Return(obj);
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        Environment.PlcObject obj = Environment.create(ast.getLiteral());
        return obj;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        Environment.PlcObject obj = Environment.create(visit(ast.getExpression()).getValue());
        return obj;
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {

        String op = ast.getOperator();
        if(op.equals("AND")) {
            if (requireType(Boolean.class, visit(ast.getLeft()))) {
                if (requireType(Boolean.class, visit(ast.getRight()))) {
                    Environment.PlcObject Obj = Environment.create(true);
                    return Obj;
                }
                else {
                    Environment.PlcObject Obj = Environment.create(false);
                    return Obj;
                }
            }
            else {
                Environment.PlcObject Obj = Environment.create(false);
                return Obj;
            }
        }
        else if (op.equals("OR")) {
            if (requireType(Boolean.class, visit(ast.getLeft()))) {
                Environment.PlcObject Obj = Environment.create(true);
                return Obj;
            }
            else if (requireType(Boolean.class, visit(ast.getRight()))){
                Environment.PlcObject Obj = Environment.create(true);
                return Obj;
            }
            else {
                Environment.PlcObject Obj = Environment.create(false);
                return Obj;
            }
        }
        else if (op.equals("<")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))<0);


        }
        else if (op.equals("<=")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))<=0);

        }
        else if (op.equals(">")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))>0);


        }
        else if (op.equals(">=")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            return Environment.create(requireType(Comparable.class,left).compareTo(requireType(left.getValue().getClass(),visit(ast.getRight())))>=0);

        }
        else if (op.equals("==")){

            if (ast.getLeft().equals(ast.getRight())) {
                Environment.PlcObject obj = Environment.create(true);
                return obj;
            }
            else {
                Environment.PlcObject obj = Environment.create(false);
                return obj;
            }
        }
        else if (op.equals("!=")){
            if (ast.getLeft().equals(ast.getRight())) {
                Environment.PlcObject obj = Environment.create(false);
                return obj;
            }
            else {
                Environment.PlcObject obj = Environment.create(true);
                return obj;
            }
        }
        else if (op.equals("+")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            if(left.getValue() instanceof String) {
                return Environment.create(""+left.getValue() + visit(ast.getRight()).getValue());
            }
            else if(left.getValue() instanceof BigInteger) {
                Environment.PlcObject sum = Environment.create(((BigInteger) left.getValue()).add(requireType(BigInteger.class,visit(ast.getRight()))));
                return sum;
            }
            else if(left.getValue() instanceof BigDecimal) {
                Environment.PlcObject sum = Environment.create(((BigDecimal) left.getValue()).add(requireType(BigDecimal.class,visit(ast.getRight()))));
                return sum;
            }
            else {
                throw new RuntimeException();
            }


        }
        else if (op.equals("-")) {

            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            if(left.getValue() instanceof BigInteger) {
                Environment.PlcObject sum = Environment.create(((BigInteger) left.getValue()).subtract(requireType(BigInteger.class,visit(ast.getRight()))));
                return sum;
            }
            else if(left.getValue() instanceof BigDecimal) {
                Environment.PlcObject sum = Environment.create(((BigDecimal) left.getValue()).subtract(requireType(BigDecimal.class,right)));
                return sum;
            }
            else {
                throw new RuntimeException();
            }
        }
        else if (op.equals("*")){
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            if(left.getValue() instanceof BigInteger) {
                Environment.PlcObject sum = Environment.create(((BigInteger) left.getValue()).multiply(requireType(BigInteger.class,visit(ast.getRight()))));
                return sum;
            }
            else if(left.getValue() instanceof BigDecimal) {
                Environment.PlcObject sum = Environment.create(((BigDecimal) left.getValue()).multiply(requireType(BigDecimal.class,right)));
                return sum;
            }
            else {
                throw new RuntimeException();
            }
        }
        else if (op.equals("/")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

            //NEW EDIT 8/3/2021
            if(right.getValue().toString().equals("0")) {
                throw new RuntimeException();
            }
            //END EDIT

            if (left.getValue() instanceof BigInteger) {
                Environment.PlcObject sum = Environment.create(((BigInteger) left.getValue()).divide(requireType(BigInteger.class,visit(ast.getRight()))));
                return sum;
            }
            else if(left.getValue() instanceof BigDecimal) {
                Environment.PlcObject sum = Environment.create(((BigDecimal) left.getValue()).divide(requireType(BigDecimal.class,right),BigDecimal.ROUND_HALF_EVEN));
                return sum;
            }
            else {
                throw new RuntimeException();
            }

        }
        throw new RuntimeException();
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    //DONE
    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        List<Environment.PlcObject> lst = new ArrayList<>();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            lst.add(visit(ast.getArguments().get(i)));
        }

        if(ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).callMethod(ast.getName(),lst);
        }
        else {
            return scope.lookupFunction(ast.getName(),ast.getArguments().size()).invoke(lst);
        }

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
