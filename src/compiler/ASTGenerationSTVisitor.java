package compiler;

import java.util.*;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;

import static compiler.lib.FOOLlib.*;

public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

    String indent;
    public boolean print;

    ASTGenerationSTVisitor() {
    }

    ASTGenerationSTVisitor(boolean debug) {
        print = debug;
    }

    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
        Class<?> ctxClass = ctx.getClass(), parentClass = ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
            prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
        System.out.println(indent + prefix + lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

    @Override
    public Node visit(ParseTree t) {
        if (t == null) {
            return null;
        }
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result;
    }

    @Override
    public Node visitProg(ProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return visit(c.progbody());
    }

    @Override
    public Node visitLetInProg(LetInProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        List<DecNode> declist = new ArrayList<>();
        for (var classDec : c.cldec()) {
            declist.add((DecNode) visit(classDec));
        }
        for (DecContext dec : c.dec()) {
            declist.add((DecNode) visit(dec));
        }
        return new ProgLetInNode(declist, visit(c.exp()));
    }

    @Override
    public Node visitNoDecProg(NoDecProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new ProgNode(visit(c.exp()));
    }

    @Override
    public Node visitClassDec(ClassDecContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        String classID = c.ID(0).getText();
        List<FieldNode> fields = new ArrayList<>();
        IntStream.range(1, c.ID().size()).forEach(i -> {
            fields.add(new FieldNode(c.ID(i).getText(), (TypeNode) visit(c.type(i))));
        });
        List<MethodNode> methods = new ArrayList<>();
        for (var method : c.methdec()) {
            methods.add((MethodNode) visit(method));
        }
        return new ClassNode(classID, fields, methods);
    }

    @Override
    public Node visitMethodDec(MethodDecContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        String methodId = c.ID(0).getText();
        TypeNode returnType = (TypeNode) visit(c.type(0));
        List<ParNode> parameters = new ArrayList<>();
        IntStream.range(1, c.ID().size()).forEach(i -> {
            parameters.add(new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i))));
        });
        List<DecNode> declarations = new ArrayList<>();
        for(var declaration : c.dec()) {
            declarations.add((DecNode) visit(declaration));
        }
        return new MethodNode(methodId, returnType, parameters, declarations, visit(c.exp()));
    }

    @Override
    public Node visitTimesDiv(TimesDivContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node node;
        if (c.TIMES() != null) {
            node = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.TIMES().getSymbol().getLine());
        } else {
            node = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.DIV().getSymbol().getLine());
        }
        return node;
    }

    @Override
    public Node visitAndOr(AndOrContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node node;
        if (c.AND() != null) {
            node = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.AND().getSymbol().getLine());
        } else {
            node = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.OR().getSymbol().getLine());
        }
        return node;
    }

    @Override
    public Node visitPlusMinus(PlusMinusContext c) {
        if (print) printVarAndProdName(c);
        Node node;
        if (c.PLUS() != null) {
            node = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.PLUS().getSymbol().getLine());
        } else {
            node = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.MINUS().getSymbol().getLine());
        }
        return node;
    }

    @Override
    public Node visitComp(CompContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node node = null;
        if (!Objects.isNull(c.EQ())) {
            node = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.EQ().getSymbol().getLine());
        } else if (!Objects.isNull(c.LE())) {
            node = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.LE().getSymbol().getLine());
        } else if (!Objects.isNull(c.GE())) {
            node = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            node.setLine(c.GE().getSymbol().getLine());
        }
        return node;
    }

    @Override
    public Node visitNot(NotContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node node = new NotNode(visit(c.exp()));
        node.setLine(c.NOT().getSymbol().getLine());
        return node;
    }

    @Override
    public Node visitVardec(VardecContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node node = null;
        if (c.ID() != null) { //non-incomplete ST
            node = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
            node.setLine(c.VAR().getSymbol().getLine());
        }
        return node;
    }

    @Override
    public Node visitFundec(FundecContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        List<ParNode> parList = new ArrayList<>();
        for (int i = 1; i < c.ID().size(); i++) {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parList.add(p);
        }
        List<DecNode> decList = new ArrayList<>();
        for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));
        Node node = null;
        if (c.ID().size() > 0) { //non-incomplete ST
            node = new FunNode(c.ID(0).getText(), (TypeNode) visit(c.type(0)), parList, decList, visit(c.exp()));
            node.setLine(c.FUN().getSymbol().getLine());
        }
        return node;
    }

    @Override
    public Node visitIntType(IntTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new IntTypeNode();
    }

    @Override
    public Node visitBoolType(BoolTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolTypeNode();
    }

    @Override
    public Node visitInteger(IntegerContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        int value = Integer.parseInt(c.NUM().getText());
        return new IntNode(c.MINUS() == null ? value : -value);
    }

    @Override
    public Node visitTrue(TrueContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolNode(true);
    }

    @Override
    public Node visitFalse(FalseContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolNode(false);
    }

    @Override
    public Node visitIf(IfContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node ifNode = visit(c.exp(0));
        Node thenNode = visit(c.exp(1));
        Node elseNode = visit(c.exp(2));
        Node node = new IfNode(ifNode, thenNode, elseNode);
        node.setLine(c.IF().getSymbol().getLine());
        return node;
    }

    @Override
    public Node visitPrint(PrintContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new PrintNode(visit(c.exp()));
    }

    @Override
    public Node visitPars(ParsContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return visit(c.exp());
    }

    @Override
    public Node visitId(IdContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        Node node = new IdNode(c.ID().getText());
        node.setLine(c.ID().getSymbol().getLine());
        return node;
    }

    @Override
    public Node visitCall(CallContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) {
            arglist.add(visit(arg));
        }
        Node node = new CallNode(c.ID().getText(), arglist);
        node.setLine(c.ID().getSymbol().getLine());
        return node;
    }

    @Override
    public Node visitDotCall(DotCallContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) {
            arglist.add(visit(arg));
        }
        Node node = new ClassCallNode(c.ID(0).getText(), c.ID(1).getText(), arglist);
        node.setLine(c.ID(1).getSymbol().getLine());
        return node;
    }
}
