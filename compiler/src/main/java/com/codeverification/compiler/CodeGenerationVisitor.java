package com.codeverification.compiler;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.codeverification.Var3Lexer;
import com.codeverification.Var3Parser.ArgDefContext;
import com.codeverification.Var3Parser.ExprContext;
import com.codeverification.Var3Parser.PlaceExprContext;
import com.codeverification.Var3Parser.StatementContext;

/**
 * @author Dmitrii Nazukin
 */
public class CodeGenerationVisitor extends com.codeverification.Var3BaseVisitor<Void> {

    MethodSignature methodSignature;

    List<Command> programm = new ArrayList<>();

    int lastComNum = -1;

    Map<String, Integer> vars = new LinkedHashMap<>();

    int lastVarNum = -1;

    Map<Const, Integer> consts = new LinkedHashMap<>();

    int lastConstNum = -1;

    int lastRegistrNum = -1;

    Map<String, Integer> funcs = new LinkedHashMap<>();

    int lastFuncNum = -1;

    List<Integer> breakJumpCom = new ArrayList<>();


    @Override
    public Void visitFuncDef(com.codeverification.Var3Parser.FuncDefContext ctx) {
        visit(ctx.funcSignature());
        for (StatementContext st : ctx.statement()) {
            visit(st);
        }
        gen("END");
        return null;
    }

    @Override
    public Void visitFuncSignature(com.codeverification.Var3Parser.FuncSignatureContext ctx) {
        methodSignature = new MethodSignature(ctx.identifier().IDENTIFIER().getText(), ctx.listArgDef().argDef().size());
        if (ctx.typeRef() != null) {
            methodSignature.setReturnType(DataType.getDataType(ctx.typeRef().getText()));
        } else {
            methodSignature.setReturnType(DataType.UNDEFINED);
        }
        vars.put(ctx.identifier().IDENTIFIER().getText(), ++lastVarNum);
        for (ArgDefContext arg : ctx.listArgDef().argDef()) {
            vars.put(arg.identifier().IDENTIFIER().getText(), ++lastVarNum);
            methodSignature.getArgsType().add(DataType.getDataType(arg.typeRef().getText()));
        }
        return null;
    }

    @Override
    public Void visitIfStatement(com.codeverification.Var3Parser.IfStatementContext ctx) {
        visit(ctx.expr());
        gen("JMPFALSE", lastRegistrNum--);
        int adr1 = lastComNum;
        for (com.codeverification.Var3Parser.StatementContext stCtx : ctx.trueSts) {
            visit(stCtx);
        }
        gen("JMP");
        int adr2 = lastComNum;
        programm.get(adr1).addArg(lastComNum + 1);
        for (com.codeverification.Var3Parser.StatementContext stCtx : ctx.falseSts) {
            visit(stCtx);
        }
        programm.get(adr2).addArg(lastComNum + 1);
        return null;
    }

    @Override
    public Void visitWhileStatement(com.codeverification.Var3Parser.WhileStatementContext ctx) {
        int startWhile = lastComNum + 1;
        visit(ctx.expr());
        gen("JMPFALSE", lastRegistrNum--);
        int jmpFalse = lastComNum;
        for (com.codeverification.Var3Parser.StatementContext stCtx : ctx.statement()) {
            visit(stCtx);
        }
        gen("JMP", startWhile);

        programm.get(jmpFalse).addArg(lastComNum + 1);
        if (!breakJumpCom.isEmpty()) {
            breakJumpCom.forEach(com -> programm.get(com).addArg(lastComNum + 1));
            breakJumpCom.clear();
        }
        return null;
    }

    @Override
    public Void visitDoStatement(com.codeverification.Var3Parser.DoStatementContext ctx) {
        int startDo = lastComNum + 1;
        for (com.codeverification.Var3Parser.StatementContext stCtx : ctx.statement()) {
            visit(stCtx);
        }
        visit(ctx.expr());
        if (ctx.type.getType() == com.codeverification.Var3Lexer.WHILE) {
            gen("JMPTRUE", lastRegistrNum--, startDo);
        }
        if (ctx.type.getType() == com.codeverification.Var3Lexer.UNTIL) {
            gen("JMPFALSE", lastRegistrNum--, startDo);
        }

        if (!breakJumpCom.isEmpty()) {
            breakJumpCom.forEach(com -> programm.get(com).addArg(lastComNum + 1));
            breakJumpCom.clear();
        }
        return null;
    }

    @Override
    public Void visitBreakStatement(com.codeverification.Var3Parser.BreakStatementContext ctx) {
        gen("JMP");
        breakJumpCom.add(lastComNum);
        return null;
    }

    @Override
    public Void visitExpressionStatement(com.codeverification.Var3Parser.ExpressionStatementContext ctx) {
        visit(ctx.expr());
        return null;
    }

    @Override
    public Void visitPlaceExpr(com.codeverification.Var3Parser.PlaceExprContext ctx) {
        visit(ctx.identifier());
        return null;
    }

    @Override
    public Void visitUnaryExpr(com.codeverification.Var3Parser.UnaryExprContext ctx) {
        String unOp = ctx.unOp().getText();
        switch (unOp) {
            case "-":
                visit(ctx.expr());
                gen("UNMINUS", lastRegistrNum, lastRegistrNum);
                break;
            case "+":
                visit(ctx.expr());
                gen("UNADD", lastRegistrNum, lastRegistrNum);
                break;
            default:
                throw new RuntimeException("Unexpected unary operator" + unOp);
        }
        return null;
    }

    @Override
    public Void visitBracesExpr(com.codeverification.Var3Parser.BracesExprContext ctx) {
        visit(ctx.expr());
        return null;
    }

    @Override
    public Void visitLiteralExpr(com.codeverification.Var3Parser.LiteralExprContext ctx) {
        Const constt = new Const(ctx.getText());
        switch (ctx.value.getType()) {
            case Var3Lexer.BOOL:
                constt.setType(DataType.BOOL);
                break;
            case Var3Lexer.STR:
                constt.setType(DataType.STRING);
                break;
            case Var3Lexer.CHAR:
                constt.setType(DataType.CHAR);
                break;
            case Var3Lexer.HEX:
                constt.setType(DataType.LONG);
                break;
            case Var3Lexer.BITS:
                constt.setType(DataType.LONG);
                break;
            case Var3Lexer.DEC:
                constt.setType(DataType.LONG);
                break;
            default:
                throw new RuntimeException();
        }
        consts.put(constt, ++lastConstNum);
        gen("PUSHCONST", lastConstNum, ++lastRegistrNum);
        return null;
    }

    @Override
    // TODO don't store redundant values
    public Void visitBinaryExpr(com.codeverification.Var3Parser.BinaryExprContext ctx) {
        String binOp = ctx.binOp().getText();
        int firstReg;
        switch (binOp) {
            case "=":
                if (!(ctx.expr(0) instanceof com.codeverification.Var3Parser.PlaceExprContext)) {
                    throw new RuntimeException("=. Left operand should be identifier");
                } else {
                    String var = ((com.codeverification.Var3Parser.PlaceExprContext)ctx.expr(0)).identifier().getText();
                    visit(ctx.expr(1));
                    if (vars.containsKey(var)) {
                        gen("LOADVAR", lastRegistrNum, vars.get(var));
                    } else {
                        vars.put(var, ++lastVarNum);
                        gen("LOADVAR", lastRegistrNum, vars.get(var));
                    }
                }
                break;
            case "+":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("ADD", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "-":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("MINUS", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "*":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("MULT", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "/":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("DIV", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "%":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("MOD", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "<":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("LESS", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case ">":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("LARGER", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "==":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("EQUAL", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "||":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("OR", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            case "&&":
                visit(ctx.expr(0));
                firstReg = lastRegistrNum;
                visit(ctx.expr(1));
                gen("AND", firstReg, lastRegistrNum, firstReg);
                lastRegistrNum = firstReg;
                break;
            default:
                throw new RuntimeException("Unexpected binary operator" + binOp);
        }
        return null;
    }

    @Override
    public Void visitCallExpr(com.codeverification.Var3Parser.CallExprContext ctx) {
        List<Integer> args = new ArrayList<>();
        for (ExprContext arg : ctx.listExpr().expr()) {
            visit(arg);
            args.add(lastRegistrNum);
        }
        if (ctx.expr() instanceof PlaceExprContext) {
            gen("CALL", args.get(0), getFunNum(ctx.expr().getText()));
            programm.get(lastComNum).addArg(args.toArray(new Integer[args.size()]));
            lastRegistrNum = args.get(0);
        } else {
            throw new RuntimeException("Wrong func name:" + ctx.expr().getText());
        }
        return null;
    }

    @Override
    public Void visitIdentifier(com.codeverification.Var3Parser.IdentifierContext ctx) {
        String varName = ctx.IDENTIFIER().getText();
        if (vars.containsKey(varName)) {
            gen("PUSHVAR", vars.get(varName), ++lastRegistrNum);
        } else {
            throw new RuntimeException("Variable is not defined:" + varName);
        }
        return null;
    }

    private void gen(String com, int... strings) {
        Command command = new Command(com);
        programm.add(command);
        lastComNum++;
        for (int arg : strings) {
            command.addArg(arg);
        }
    }

    private Integer getFunNum(String func) {
        if (!funcs.containsKey(func)) {
            funcs.put(func, ++lastFuncNum);
        }
        return funcs.get(func);
    }

    public void print(PrintStream printStream) {
        printStream.println(".methodSignature");
        printStream.println(methodSignature);
        printStream.println(".funcs");
        funcs.entrySet()
                .forEach(e -> printStream.println(e.getValue() + ":" + e.getKey()));
        printStream.println(".vars");
        vars.entrySet()
                .forEach(e -> printStream.println(e.getValue() + ":" + e.getKey()));
        printStream.println(".consts");
        consts.entrySet()
                .forEach(e -> printStream.println(e.getValue() + ":" + e.getKey()));
        printStream.println(".programm");
        IntStream.range(0, programm.size()).forEach(idx -> printStream.println(idx + ": " + programm.get(idx)));
    }

    public MethodDefinition map2MethodDefinition() {
        MethodDefinition methodDefinition = new MethodDefinition();
        methodDefinition.setMethodSignature(methodSignature);
        methodDefinition.setVarsCount(vars.values().size());
        methodDefinition.getCommands().addAll(programm);
        methodDefinition.getConsts().addAll(consts.keySet());
        methodDefinition.getFuncs().addAll(funcs.keySet());
        return methodDefinition;
    }

    public static class Const implements Serializable {
        private static final long serialVersionUID = 4;

        private String constName;
        private DataType type;

        public Const(String constName) {
            this.constName = constName;
        }

        public Const(String constName, DataType type) {
            this.constName = constName;
            this.type = type;
        }

        public String getConstName() {
            return constName;
        }

        public void setConstName(String constName) {
            this.constName = constName;
        }

        public DataType getType() {
            return type;
        }

        public void setType(DataType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Const{" +
                    "constName='" + constName + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
}
