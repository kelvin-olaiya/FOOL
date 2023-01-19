package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void, VoidException> {

    PrintEASTVisitor() {
        super(false, true);
    }

    @Override
    public Void visitNode(ProgLetInNode n) {
        printNode(n);
        for (Node dec : n.declarationList) visit(dec);
        visit(n.expression);
        return null;
    }

    @Override
    public Void visitNode(ProgNode n) {
        printNode(n);
        visit(n.expression);
        return null;
    }

    @Override
    public Void visitNode(FunNode n) {
        printNode(n, n.id);
        visit(n.returnType);
        for (ParNode par : n.parametersList) visit(par);
        for (Node dec : n.declarationsList) visit(dec);
        visit(n.expression);
        return null;
    }

    @Override
    public Void visitNode(ParNode n) {
        printNode(n, n.id);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        printNode(n, n.id);
        visit(n.getType());
        visit(n.expression);
        return null;
    }

    @Override
    public Void visitNode(PrintNode n) {
        printNode(n);
        visit(n.expression);
        return null;
    }

    @Override
    public Void visitNode(IfNode n) {
        printNode(n);
        visit(n.condition);
        visit(n.thenBranch);
        visit(n.elseBranch);
        return null;
    }

    @Override
    public Void visitNode(EqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        printNode(n);
        visit(n.expression);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }


    @Override
    public Void visitNode(OrNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }


    @Override
    public Void visitNode(AndNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }


    @Override
    public Void visitNode(TimesNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(DivNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(PlusNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(MinusNode n) {
        printNode(n);
        visit(n.left);
        visit(n.right);
        return null;
    }

    @Override
    public Void visitNode(CallNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nl);
        visit(n.symbolTableEntry);
        for (Node arg : n.argumentsList) visit(arg);
        return null;
    }

    @Override
    public Void visitNode(IdNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nestingLevel);
        visit(n.symbolTableEntry);
        return null;
    }

    @Override
    public Void visitNode(BoolNode n) {
        printNode(n, n.value.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode n) {
        printNode(n, n.value.toString());
        return null;
    }

    @Override
    public Void visitNode(ArrowTypeNode n) {
        printNode(n);
        for (Node par : n.parametersList) visit(par);
        visit(n.returnType, "->"); //marks return type
        return null;
    }

    @Override
    public Void visitNode(BoolTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(IntTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitSTentry(STentry entry) {
        printSTentry("nestlev " + entry.nl);
        printSTentry("type");
        visit(entry.type);
        printSTentry("offset " + entry.offset);
        return null;
    }

}
