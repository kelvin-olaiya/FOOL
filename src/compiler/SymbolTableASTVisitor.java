package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

/**
 * Visits and enriches an AST producing an EAST.
 * Enrichment is performed top-down.
 * <p>
 * Processes for each scope:
 * > `the declarations` adding new entries
 * to the symbol table and reports any variable/methods
 * that is multiply declared.
 * > `the statements` finding uses of undeclared variables and adding a pointer
 * to the appropriate symbol table entry in ID nodes.
 * <p>
 * ASSUMPTIONS:
 * -> Out language uses STATIC SCOPING
 * -> All names must be declared BEFORE they are used
 * -> Multiple declaration of a name `in the same scope` is NOT ALLOWED
 * -> Multiple declaration is ALLOWED in multiple NESTED scopes
 */
public class SymbolTableASTVisitor extends BaseASTVisitor<Void, VoidException> {

    private final List<Map<String, STentry>> symbolTable = new ArrayList<>();
    Map<String, Map<String, STentry>> classTable = new HashMap<>();
    private int nestingLevel = 0; // current nesting level
    private int declarationOffset = -2; // counter for offset of local declarations at current nesting level
    int symbolTableErrors = 0;
    int fieldOffset = -1;
    Set<String> onClassVisitScope;

    SymbolTableASTVisitor() {
    }

    // enables print for debugging
    SymbolTableASTVisitor(boolean debug) {
        super(debug);
    }

    private STentry stLookup(String id) {
        int tableIndex = nestingLevel;
        STentry entry = null;
        while (tableIndex >= 0 && entry == null) {
            entry = symbolTable.get(tableIndex--).get(id);
        }
        return entry;
    }

    @Override
    public Void visitNode(ProgLetInNode node) {
        if (print) {
            printNode(node);
        }
        Map<String, STentry> globalScopeTable = new HashMap<>();
        symbolTable.add(globalScopeTable);
        for (Node declaration : node.declarationList) {
            visit(declaration);
        }
        visit(node.expression);
        symbolTable.remove(0);
        return null;
    }

    @Override
    public Void visitNode(ProgNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(FunNode node) {
        if (print) {
            printNode(node);
        }
        /*
         * Get the list of function parameters types and build the functionType.
         */
        final List<TypeNode> parametersTypes = new ArrayList<>();
        for (ParNode parameter : node.parametersList) {
            parametersTypes.add(parameter.getType());
        }
        final TypeNode functionType = new ArrowTypeNode(parametersTypes, node.returnType);
        /*
         * Insert ID into the symbolTable. Output an error if ID already exists in current scope.
         */
        final Map<String, STentry> scopeTable = symbolTable.get(nestingLevel);
        final STentry entry = new STentry(nestingLevel, functionType, declarationOffset--);
        if (scopeTable.put(node.id, entry) != null) {
            System.out.println("Fun id " + node.id + " at line " + node.getLine() + " already declared");
            symbolTableErrors++;
        }
        /*
         * Create a new hashmap for the new scope in symbolTable.
         */
        nestingLevel++;
        Map<String, STentry> functionScopeTable = new HashMap<>();
        symbolTable.add(functionScopeTable);
        /*
         * Stores counter for offset of declarations at previous nesting level.
         */
        int previousNestingLevelDeclarationOffset = declarationOffset;
        declarationOffset = -2;
        int parameterOffset = 1;
        /*
         * Add parameters entries to symbolTable.
         */
        for (ParNode parameter : node.parametersList) {
            final STentry parameterEntry = new STentry(nestingLevel, parameter.getType(), parameterOffset++);
            if (functionScopeTable.put(parameter.id, parameterEntry) != null) {
                System.out.println("Par id " + parameter.id + " at line " + node.getLine() + " already declared");
                symbolTableErrors++;
            }
        }
        /*
         * Visit declarations in function.
         */
        for (Node declaration : node.declarationsList) {
            visit(declaration);
        }
        /*
         * Visit expression
         */
        visit(node.expression);
        /*
         * On scope exit remove current nesting level table.
         * Restore previous nestingLevel offsets.
         */
        symbolTable.remove(nestingLevel--);
        declarationOffset = previousNestingLevelDeclarationOffset;
        return null;
    }

    @Override
    public Void visitNode(VarNode node) {
        if (print) {
            printNode(node);
        }
        Map<String, STentry> currentScopeTable = symbolTable.get(nestingLevel);
        /*
         * Insert ID into symbolTable
         */
        STentry entry = new STentry(nestingLevel, node.getType(), declarationOffset--);
        if (currentScopeTable.put(node.id, entry) != null) {
            System.out.println("Var id " + node.id + " at line " + node.getLine() + " already declared");
            symbolTableErrors++;
        }
        visit(node.expression);
        return null;
    }

    public Void visitNode(ClassNode node) {
        if (print) {
            printNode(node);
        }
        var classType = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
        /*
         * Class is extending
         */
        if (node.superID != null && classTable.containsKey(node.superID)) {
            STentry superClassEntry = symbolTable.get(0).get(node.superID);
            classType = new ClassTypeNode(
                new ArrayList<>(((ClassTypeNode) superClassEntry.type).allFields),
                new ArrayList<>(((ClassTypeNode) superClassEntry.type).allMethods)
            );
            node.superClassEntry = superClassEntry;
        } else if (node.superID != null) {
            System.out.println("Extending class id " + node.superID + " at line " + node.getLine() + " is not declared");
        }
        STentry entry = new STentry(0, classType, declarationOffset--);
        node.type = classType;
        Map<String, STentry> globalScopeTable = symbolTable.get(0);
        if (globalScopeTable.put(node.id, entry) != null) {
            System.out.println("Class id " + node.id + " at line " + node.getLine() + " already declared");
            symbolTableErrors++;
        }
        /*
         * Add a the scope table for the id of the class.
         * Table should be added for both symbol table and class table.
         */
        nestingLevel++;
        onClassVisitScope = new HashSet<>();
        Map<String, STentry> virtualTable = new HashMap<>();
        var superClassVirtualTable = classTable.get(node.superID);
        if (node.superID != null) {
            virtualTable.putAll(superClassVirtualTable);
        }
        classTable.put(node.id, virtualTable);
        symbolTable.add(virtualTable);
        /*
         * Setting the fieldOffset for the extending class
         */
        fieldOffset = -1;
        if (node.superID != null) {
            fieldOffset = -((ClassTypeNode) symbolTable.get(0).get(node.superID).type).allFields.size()-1;
        }
        /*
         * Handle field declaration.
         */
        for (var field : node.fields) {
            if (onClassVisitScope.contains(field.id)) {
                System.out.println(
                    "Field with id " + field.id + " on line " + field.getLine() + " was already declared"
                );
                symbolTableErrors++;
            }
            onClassVisitScope.add(field.id);
            var overriddenFieldEntry = virtualTable.get(field.id);
            STentry fieldEntry;
            if (overriddenFieldEntry != null && !(overriddenFieldEntry.type instanceof MethodTypeNode)) {
                fieldEntry = new STentry(nestingLevel, field.getType(), overriddenFieldEntry.offset);
                classType.allFields.set(-fieldEntry.offset - 1, fieldEntry.type);
            } else {
                fieldEntry = new STentry(nestingLevel, field.getType(), fieldOffset--);
                classType.allFields.add(-fieldEntry.offset - 1, fieldEntry.type);
                if (overriddenFieldEntry != null) {
                    System.out.println("Cannot override field id " + field.id + " with a method");
                    symbolTableErrors++;
                }
            }
            /*
             * Add field id in symbol(virtual) table
             */
            virtualTable.put(field.id, fieldEntry);
            field.offset = fieldEntry.offset;
        }
        int currentDecOffset = declarationOffset;
        // method declarationOffset starts from 0
        int previousNestingLevelDeclarationOffset = declarationOffset;
        declarationOffset = 0;
        if (node.superID != null) {
            declarationOffset = ((ClassTypeNode) symbolTable.get(0).get(node.superID).type).allMethods.size();
        }
        for (var method : node.methods) {
            if (onClassVisitScope.contains(method.id)) {
                System.out.println(
                    "Method with id " + method.id + " on line " + method.getLine() + " was already declared"
                );
                symbolTableErrors++;
            }
            visit(method);
            classType.allMethods.add(
                method.offset,
                ((MethodTypeNode) virtualTable.get(method.id).type).functionalType
            );
        }
        declarationOffset = currentDecOffset; // restores the previous declaration offset
        symbolTable.remove(nestingLevel--);
        declarationOffset = previousNestingLevelDeclarationOffset;
        return null;
    }

    @Override
    public Void visitNode(MethodNode node) {
        if (print) printNode(node);
        Map<String, STentry> currentScopeTable = symbolTable.get(nestingLevel);
        List<TypeNode> parametersTypes = new ArrayList<>();
        for (ParNode parameter : node.parametersList) {
            parametersTypes.add(parameter.getType());
        }
        /*
         * Insert ID into the symbolTable.
         */
        var overriddenMethodEntry = currentScopeTable.get(node.id);
        final TypeNode methodType = new MethodTypeNode(new ArrowTypeNode(parametersTypes, node.returnType));
        STentry entry = null;
        if (overriddenMethodEntry != null && overriddenMethodEntry.type instanceof MethodTypeNode) {
            entry = new STentry(nestingLevel, methodType, overriddenMethodEntry.offset);
        } else {
            entry = new STentry(nestingLevel, methodType, declarationOffset++);
            if (overriddenMethodEntry != null) {
                System.out.println("Cannot override method id " + node.id + " with a field");
                symbolTableErrors++;
            }
        }
        node.offset = entry.offset;
        currentScopeTable.put(node.id, entry);
        /*
         * Create a new table for the method.
         */
        nestingLevel++;
        Map<String, STentry> methodScopeTable = new HashMap<>();
        symbolTable.add(methodScopeTable);
        int previousNestingLeveleDeclarationOffset = declarationOffset;
        declarationOffset = -2;
        int parameterOffset = 1;
        for (ParNode parameter : node.parametersList) {
            final STentry parameterEntry = new STentry(nestingLevel, parameter.getType(), parameterOffset++);
            if (methodScopeTable.put(parameter.id, parameterEntry) != null) {
                System.out.println("Par id " + parameter.id + " at line " + node.getLine() + " already declared");
                symbolTableErrors++;
            }
        }
        for (Node declaration : node.declarationsList) {
            visit(declaration);
        }
        visit(node.expression);
        /*
         * Remove the current nesting level symbolTable.
         */
        symbolTable.remove(nestingLevel--);
        declarationOffset = previousNestingLeveleDeclarationOffset;
        return null;
    }

    @Override
    public Void visitNode(NewNode node) {
        if (print) {
            printNode(node);
        }
        if (!classTable.containsKey(node.id)) {
            System.out.println("Class id " + node.id + " was not declared");
            symbolTableErrors++;
        }
        node.classSymbolTableEntry = symbolTable.get(0).get(node.id);
        for (var argument : node.argumentsList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(RefTypeNode node) {
        if (print) {
            printNode(node, node.id);
        }
        if (!classTable.containsKey(node.id)) {
            System.out.println("Class with id " + node.id + " on line " + node.getLine() + " was not declared");
            symbolTableErrors++;
        }
        return null;
    }

    @Override
    public Void visitNode(PrintNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(IfNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.condition);
        visit(node.thenBranch);
        visit(node.elseBranch);
        return null;
    }

    @Override
    public Void visitNode(EqualNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(NotNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.expression);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(OrNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(AndNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }


    @Override
    public Void visitNode(TimesNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(DivNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(PlusNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(MinusNode node) {
        if (print) {
            printNode(node);
        }
        visit(node.left);
        visit(node.right);
        return null;
    }

    @Override
    public Void visitNode(CallNode node) {
        if (print) {
            printNode(node);
        }
        STentry entry = stLookup(node.id);
        if (entry == null) {
            System.out.println("Fun or Method with id " + node.id + " at line " + node.getLine() + " not declared");
            symbolTableErrors++;
        } else {
            node.symbolTableEntry = entry;
            node.nestingLevel = nestingLevel;
        }
        for (Node argument : node.argumentsList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(ClassCallNode node) {
        if (print) {
            printNode(node);
        }
        STentry entry = stLookup(node.objectId);
        if (entry == null) {
            System.out.println("Object id " + node.objectId + " at line " + node.getLine() + " not declared");
            symbolTableErrors++;
        } else if (entry.type instanceof RefTypeNode) {
            node.symbolTableEntry = entry;
            node.methodEntry = classTable.get(((RefTypeNode) entry.type).id).get(node.methodId);
            if (node.methodEntry == null) {
                System.out.println(
                        "Object id " + node.objectId + " at line " + node.getLine() + " has no method " + node.methodId
                );
                symbolTableErrors++;
            }
        }
        for (Node argument : node.argumentsList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(IdNode node) {
        if (print) {
            printNode(node);
        }
        STentry entry = stLookup(node.id);
        if (entry == null) {
            System.out.println("Var or Par id " + node.id + " at line " + node.getLine() + " not declared");
            symbolTableErrors++;
        } else {
            node.symbolTableEntry = entry;
            node.nestingLevel = nestingLevel;
        }
        return null;
    }

    @Override
    public Void visitNode(BoolNode node) {
        if (print) {
            printNode(node, node.value.toString());
        }
        return null;
    }

    @Override
    public Void visitNode(IntNode node) {
        if (print) {
            printNode(node, node.value.toString());
        }
        return null;
    }

    @Override
    public Void visitNode(EmptyNode node) {
        if (print) {
            printNode(node);
        }
        return null;
    }
}
