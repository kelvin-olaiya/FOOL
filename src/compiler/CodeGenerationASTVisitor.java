package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import visualsvm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;
import static compiler.lib.FOOLlib.nlJoin;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

    List<List<String>> dispatchTables = new ArrayList<>();

    CodeGenerationASTVisitor() {
    }

    CodeGenerationASTVisitor(boolean debug) {
        super(false, debug);
    }

    @Override
    public String visitNode(ProgLetInNode node) {
        if (print) {
            printNode(node);
        }
        String declarationListCode = null;
        for (Node declaration : node.declarationList) {
            declarationListCode = nlJoin(declarationListCode, visit(declaration));
        }
        return nlJoin(
            "push 0",
            declarationListCode, // generate code for declarations (allocation)
            visit(node.expression),
            "halt",
            getCode()
        );
    }

    @Override
    public String visitNode(ProgNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
            visit(node.expression),
            "halt"
        );
    }

    @Override
    public String visitNode(FunNode node) {
        if (print) {
            printNode(node, node.id);
        }
        String declarationListCode = null;
        String popDeclarationsList = null;
        String popParametersList = null;
        for (Node declaration : node.declarationsList) {
            declarationListCode = nlJoin(declarationListCode, visit(declaration));
            popDeclarationsList = nlJoin(popDeclarationsList, "pop");
        }
        for (int i = 0; i < node.parametersList.size(); i++) {
            popParametersList = nlJoin(popParametersList, "pop");
        }
        String functionLabel = freshFunLabel();
        putCode(
            nlJoin(
                functionLabel + ":",
                "cfp", // set $fp to $sp value
                "lra", // load $ra value
                declarationListCode, // generate code for local declarations (they use the new $fp!!!)
                visit(node.expression), // generate code for function body expression
                "stm", // set $tm to popped value (function result)
                popDeclarationsList, // remove local declarations from stack
                "sra", // set $ra to popped value
                "pop", // remove Access Link from stack
                popParametersList, // remove parameters from stack
                "sfp", // set $fp to popped value (Control Link)
                "ltm", // load $tm value (function result)
                "lra", // load $ra value
                "js"  // jump to to popped address
            )
        );
        return "push " + functionLabel;
    }

    @Override
    public String visitNode(MethodNode node) {
        if (print) {
            printNode(node, node.id);
        }
        String declarationListCode = null;
        String popDeclarationsList = null;
        for (Node declaration : node.declarationsList) {
            declarationListCode = nlJoin(declarationListCode, visit(declaration));
            popDeclarationsList = nlJoin(popDeclarationsList, "pop");
        }
        String popParametersList = null;
        for (int i = 0; i < node.parametersList.size(); i++) {
            popParametersList = nlJoin(popParametersList, "pop");
        }
        String functionLabel = freshFunLabel();
        node.label = functionLabel;
        putCode(
            nlJoin(
                functionLabel + ":",
                "cfp", // set $fp to $sp value
                "lra", // load $ra value
                declarationListCode, // generate code for local declarations (they use the new $fp!!!)
                visit(node.expression), // generate code for function body expressionˇ
                "stm", // set $tm to popped value (function result)
                popDeclarationsList, // remove local declarations from stack
                "sra", // set $ra to popped value
                "pop", // remove Access Link from stack
                popParametersList, // remove parameters from stack
                "sfp", // set $fp to popped value (Control Link)
                "ltm", // load $tm value (function result)
                "lra", // load $ra value
                "js"  // jump to to popped address
            )
        );
        return null;
    }

    @Override
    public String visitNode(ClassNode node) {
        if (print) {
            printNode(node, node.id);
        }
        List<String> dispatchTable = new ArrayList<>();
        dispatchTables.add(dispatchTable);
        if (node.superID != null) {
            var superClassDispatchTable = dispatchTables.get(-node.superClassEntry.offset-2);
            dispatchTable.addAll(superClassDispatchTable);
        }
        node.methods.forEach(methodNode -> {
            visit(methodNode);
            dispatchTable.add(methodNode.offset, methodNode.label);
        });
        String createDispatchTable = null;
        for (String label : dispatchTable) {
            createDispatchTable = nlJoin(
                createDispatchTable,
                "push " + label,
                "lhp",
                "sw",
                "lhp",
                "push 1",
                "add",
                "shp"
            );
        }
        return nlJoin(
            "lhp",
            createDispatchTable
        );
    }

    @Override
    public String visitNode(EmptyNode node) {
        if (print) {
            printNode(node);
        }
        return "push -1";
    }


    @Override
    public String visitNode(VarNode node) {
        if (print) {
            printNode(node, node.id);
        }
        return visit(node.expression);
    }

    @Override
    public String visitNode(PrintNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
            visit(node.expression),
            "print"
        );
    }

    @Override
    public String visitNode(IfNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
            visit(node.condition),
            "push 1",
            "beq " + label1,
            visit(node.elseBranch),
            "b " + label2,
            label1 + ":",
            visit(node.thenBranch),
            label2 + ":"
        );
    }

    @Override
    public String visitNode(EqualNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
            visit(node.left),
            visit(node.right),
            "beq " + label1,
            "push 0",
            "b " + label2,
            label1 + ":",
            "push 1",
            label2 + ":"
        );
    }

    @Override
    public String visitNode(OrNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        String label3 = freshLabel();
        String label4 = freshLabel();
        return nlJoin(
            visit(node.left),
            "push 0",
            "beq " + label1,
            "b " + label2,
            label1 + ":",
            visit(node.right),
            "push 0",
            "beq " + label3,
            label2 + ":",
            "push 1",
            "b " + label4,
            label3 + ":",
            "push 0",
            label4 + ":"
        );
    }

    @Override
    public String visitNode(AndNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
            visit(node.left),
            "push 0",
            "beq " + label1,
            visit(node.right),
            "push 0",
            "beq " + label1,
            "push 1",
            "b  " + label2,
            label1 + ":",
            "push 0",
            label2 + ":"
        );
    }

    @Override
    public String visitNode(NotNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
            visit(node.expression),
            "push 0",
            "beq " + label1,
            "push 0",
            "b " + label2,
            label1 + ":",
            "push 1",
            label2 + ":"
        );
    }

    @Override
    public String visitNode(LessEqualNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
            visit(node.left),
            visit(node.right),
            "bleq " + label1,
            "push 0",
            "b " + label2,
            label1 + ":",
            "push 1",
            label2 + ":"
        );
    }

    @Override
    public String visitNode(GreaterEqualNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
            visit(node.right),
            visit(node.left),
            "sub",
            "push 0",
            "bleq " + label1,
            "push 0",
            "b " + label2,
            label1 + ":",
            "push 1",
            label2 + ":"
        );
    }

    @Override
    public String visitNode(TimesNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
            visit(node.left),
            visit(node.right),
            "mult"
        );
    }

    @Override
    public String visitNode(DivNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
            visit(node.left),
            visit(node.right),
            "div"
        );
    }

    @Override
    public String visitNode(PlusNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
            visit(node.left),
            visit(node.right),
            "add"
        );
    }

    @Override
    public String visitNode(MinusNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
            visit(node.left),
            visit(node.right),
            "sub"
        );
    }

    @Override
    public String visitNode(CallNode node) {
        if (print) {
            printNode(node, node.id);
        }

        String argumentsCode = null;
        String getActivationRecordCode = null;
        for (int i = node.argumentsList.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.argumentsList.get(i)));
        }
        for (int i = 0; i < node.nestingLevel - node.symbolTableEntry.nestingLevel; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }
        String commonCode = nlJoin(
            "lfp", // load Control Link (pointer to frame of function "id" caller)
            argumentsCode, // generate code for argument expressions in reversed order
            "lfp", getActivationRecordCode, // retrieve address of frame containing "id" declaration
            // by following the static chain (of Access Links)
            "stm", // set $tm to popped value (with the aim of duplicating top of stack)
            "ltm", // load Access Link (pointer to frame of function "id" declaration)
            "ltm" // duplicate top of stack
        );
        if (node.symbolTableEntry.type instanceof MethodTypeNode) {
            commonCode = nlJoin(commonCode, "lw"); // load dispatchPointer
        }
        return nlJoin(commonCode,
            "push " + node.symbolTableEntry.offset,
            "add", // compute address of "id" declaration
            "lw", // load address of "id" function
            "js"  // jump to popped address (saving address of subsequent instruction in $ra)
        );
    }

    @Override
    public String visitNode(ClassCallNode node) {
        if (print) {
            printNode(node);
        }
        String argumentsCode = null;
        String getActivationRecordCode = null;
        for (int i = node.argumentsList.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.argumentsList.get(i)));
        }
        for (int i = 0; i < node.nestingLevel - node.symbolTableEntry.nestingLevel; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }
        return nlJoin(
            "lfp", // load Control Link (pointer to frame of function "id" caller)
            argumentsCode, // generate code for argument expressions in reversed order
            "lfp", getActivationRecordCode, // retrieve address of frame containing "id" declaration
            // by following the static chain (of Access Links)
            "push " + node.symbolTableEntry.offset, // offset where to find the object pointer
            "add",
            "lw", // put the objectPointer
            "stm", // set $tm to popped value (with the aim of duplicating top of stack)
            "ltm", // load Access Link (pointer to frame of function "id" declaration)
            "ltm", // duplicate top of stack
            "lw",
            "push " + node.methodEntry.offset, "add", // compute address of "id" declaration
            "lw", // load address of "id" function
            "js"  // jump to popped address (saving address of subsequent instruction in $ra)
        );
    }

    @Override
    public String visitNode(NewNode node) {
        if (print) {
            printNode(node);
        }
        String argumentsVisit = null;
        for(var argument : node.argumentsList) {
            argumentsVisit = nlJoin(
                argumentsVisit,
                visit(argument)
            );
        }
        String loadArguments = null;
        for ( var i = 0; i < node.argumentsList.size(); i++) {
            loadArguments = nlJoin(
                loadArguments,
                "lhp",
                "sw",
                "lhp",
                "push 1",
                "add",
                "shp"
            );
        }
        return nlJoin(
            argumentsVisit,
            loadArguments,
            "push " + ExecuteVM.MEMSIZE,
            "push " + node.classSymbolTableEntry.offset,
            "add",
            "lw", // get dispatch pointer
            "lhp",
            "sw",
            "lhp",
            "lhp",
            "push 1",
            "add",
            "shp"
        );
    }

    @Override
    public String visitNode(IdNode node) {
        if (print) {
            printNode(node, node.id);
        }
        String getActivationRecordCode = null;
        for (int i = 0; i < node.nestingLevel - node.symbolTableEntry.nestingLevel; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }
        return nlJoin(
            "lfp",
            /*
             * Retrieve address of frame containing "id" declaration by following the static chain (of Access Links)
             */
            getActivationRecordCode,
            "push " + node.symbolTableEntry.offset,
            "add", // compute address of "id" declaration
            "lw" // load value of "id" variable
        );
    }

    @Override
    public String visitNode(BoolNode node) {
        if (print) {
            printNode(node, node.value.toString());
        }
        return "push " + (node.value ? 1 : 0);
    }

    @Override
    public String visitNode(IntNode node) {
        if (print) {
            printNode(node, node.value.toString());
        }
        return "push " + node.value;
    }
}
