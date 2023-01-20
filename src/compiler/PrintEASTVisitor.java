package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void, VoidException> {

    PrintEASTVisitor() {
        super(false, true);
    }

    @Override
    public Void visitNode(ProgLetInNode node) {
        printNode(node);
        for (Node declaration : node.declarationList) {
            visit(declaration);
        }
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(ProgNode node) {
        printNode(node);
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(ClassNode node) {
        printNode(node, node.id);
        for (var field : node.fields) {
            visit(field);
        }
        for (var method : node.methods) {
            visit(method);
        }
        return null;
    }

    @Override
    public Void visitNode(FieldNode node) {
        printNode(node, node.id);
        visit(node.getType());
        return null;
    }

    public Void visitNode(MethodNode node) {
        printNode(node, node.id);
        visit(node.returnType);
        for (ParNode parameter : node.parametersList) {
            visit(parameter);
        }
        for (DecNode declaration : node.declarationsList) {
            visit(declaration);
        }
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(FunNode node) {
        printNode(node, node.id);
        visit(node.returnType);
        for (ParNode parameter : node.parametersList) {
            visit(parameter);
        }
        for (Node declaration : node.declarationsList) {
            visit(declaration);
        }
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(ParNode node) {
        printNode(node, node.id);
        visit(node.getType());
        return null;
    }

    @Override
    public Void visitNode(VarNode node) {
        printNode(node, node.id);
        visit(node.getType());
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(PrintNode node) {
        printNode(node);
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(IfNode node) {
        printNode(node);
        visit(node.condition);
        visit(node.thenBranch);
        visit(node.elseBranch);
        return null;
    }

    @Override
    public Void visitNode(EqualNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode node) {
        printNode(node);
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(OrNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(AndNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(TimesNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(DivNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(PlusNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(MinusNode node) {
        printNode(node);
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(CallNode node) {
        printNode(node, node.id + " at nestinglevel " + node.nestingLevel);
        visit(node.symbolTableEntry);
        for (Node argument : node.argumentsList) {
            visit(argument);
        }
        return null;
    }

    public Void visitNode(ClassCallNode node) {
        printNode(node, node.objectId + "." +  node.methodId + " at nestingLevel " + node.nestingLevel);
        visit(node.methodEntry);
        visit(node.methodEntry);
        for (Node argument : node.argumentsList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(IdNode node) {
        printNode(node, node.id + " at nestinglevel " + node.nestingLevel);
        visit(node.symbolTableEntry);
        return null;
    }

    @Override
    public Void visitNode(BoolNode node) {
        printNode(node, node.value.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode node) {
        printNode(node, node.value.toString());
        return null;
    }

    @Override
    public Void visitNode(ArrowTypeNode node) {
        printNode(node);
        for (Node parameter : node.parametersList) {
            visit(parameter);
        }
        visit(node.returnType, "->"); //marks return type
        return null;
    }

    @Override
    public Void visitNode(BoolTypeNode node) {
        printNode(node);
        return null;
    }

    @Override
    public Void visitNode(IntTypeNode node) {
        printNode(node);
        return null;
    }

    public Void visitNode(ClassTypeNode node) {
        printNode(node);
        return null;
    }

    @Override
    public Void visitSTentry(STentry entry) {
        printSTentry("nestlev " + entry.nestingLevel);
        printSTentry("type");
        visit(entry.type);
        printSTentry("offset " + entry.offset);
        return null;
    }
}
