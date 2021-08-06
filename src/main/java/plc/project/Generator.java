package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        indent += 1;
        if(!ast.getFields().isEmpty()) {
            for(int i = 0; i < ast.getFields().size(); i++) {
                newline(indent);
                print(ast.getFields().get(i));
            }
            newline(0);
        }
        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        for(int i = 0; i < ast.getMethods().size(); i++) {
            newline(0);
            newline(indent);
            print(ast.getMethods().get(i));
        }
        newline(0);

        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getVariable().getType().getJvmName(), " ",ast.getVariable().getJvmName());
        if(ast.getValue().isPresent()){
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName(), "(");
        if(!ast.getParameters().isEmpty()) {
            for(int i = 0; i<ast.getParameters().size(); i++) {
                if(i!=0) {
                    print(", ");
                }
                print(ast.getFunction().getParameterTypes().get(i).getJvmName(), " ", ast.getParameters().get(i));
            }
        }
        print(") {");
        if(!ast.getStatements().isEmpty()){
            printStatements(ast.getStatements(),ast);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(),";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (", ast.getCondition(),") {");
        printStatements(ast.getThenStatements(),ast);
        print("}");

        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            printStatements(ast.getElseStatements(), ast);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (int ", ast.getName(), " : ", ast.getValue(), ") {");
        printStatements(ast.getStatements(), ast);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");

        printStatements(ast.getStatements(), ast);

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //NEW EDIT 8/3/2021 Expr Literal Nil
        if(ast.getType().equals(Environment.Type.NIL)) {
            print("nil");
        }
        //END EDIT
        if(ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
        }
        else if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("\'", ast.getLiteral(), "\'");
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String jvmOp = ast.getOperator();
        String op = ast.getOperator();

        if(op.equals("AND")) {
            jvmOp = "&&";
        }
        if (op.equals("OR")){
            jvmOp ="||";
        }

        print(ast.getLeft()," ", jvmOp," ",ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(),".");
        }

        print(ast.getVariable().getJvmName());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {

        if(ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(), ".");
        }

        print(ast.getFunction().getJvmName(), "(");
        if(!ast.getArguments().isEmpty()){
            for(int i = 0; i < ast.getArguments().size(); i++) {
                if(i!=0) {
                    print(", ");
                }
                print(ast.getArguments().get(i));
            }
        }
        print(")");

        return null;
    }

    private Void printStatements(List<Ast.Stmt> statements, Ast ast) {
        if(!statements.isEmpty()) {
            newline(++indent);
            for(int i = 0; i< statements.size(); i++){
                if (i != 0) {
                    newline(indent);
                }
                print(statements.get(i));
            }
            newline(--indent);
        }
        return null;
    }

}
