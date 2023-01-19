package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

/**
 * Visits and enriches an AST producing an EAST.
 * Enrichment is performed top-down.
 *
 * Processes for each scope:
 * > `the declarations` adding new entries
 * 		to the symbol table and reports any variable/methods
 * 		that is multiply declared.
 * > `the statements` finding uses of undeclared variables and adding a pointer
 * 		to the appropriate symbol table entry in ID nodes.
 *
 * ASSUMPTIONS:
 * -> Out language uses STATIC SCOPING
 * -> All names must be declared BEFORE they are used
 * -> Multiple declaration of a name `in the same scope` is NOT ALLOWED
 * -> Multiple declaration is ALLOWED in multiple NESTED scopes
 */
public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symbolTable = new ArrayList<>();
	Map<String, Map<String,STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int symbolTableErrors =0;

	int fieldOffset = -1;


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
		Map<String, STentry> scopeTable = symbolTable.get(nestingLevel);
		List<TypeNode> parametersTypes = new ArrayList<>();
		for (ParNode parameter : node.parametersList) {
			parametersTypes.add(parameter.getType());
		}
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parametersTypes,node.returnType),decOffset--);
		// Insert ID into the symbolTable
		if (scopeTable.put(node.id, entry) != null) {
			System.out.println("Fun id " + node.id + " at line "+ node.getLine() +" already declared");
			symbolTableErrors++;
		} 
		// Create a new hashmap for the symbolTable
		nestingLevel++;
		Map<String, STentry> newScopeTable = new HashMap<>();
		symbolTable.add(newScopeTable);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode parameter : node.parametersList)
			if (newScopeTable.put(parameter.id, new STentry(nestingLevel,parameter.getType(),parOffset++)) != null) {
				System.out.println("Par id " + parameter.id + " at line "+ node.getLine() +" already declared");
				symbolTableErrors++;
			}
		for (Node declaration : node.declarationsList) {
			visit(declaration);
		}
		visit(node.expression);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symbolTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode node) {
		if (print) {
			printNode(node);
		}
		visit(node.expression);
		Map<String, STentry> currentScopeTable = symbolTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,node.getType(),decOffset--);
		//inserimento di ID nella symtable
		if (currentScopeTable.put(node.id, entry) != null) {
			System.out.println("Var id " + node.id + " at line "+ node.getLine() +" already declared");
			symbolTableErrors++;
		}
		return null;
	}

	public Void visitNode(ClassNode node) {
		if (print) {
			printNode(node);
		}
		Map<String, STentry> globalScopeTable = symbolTable.get(0);
		var classType = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
		STentry entry = new STentry(0, classType, decOffset--);
		if (globalScopeTable.put(node.id, entry) != null) {
			System.out.println("Class id " + node.id + " at line "+ node.getLine() +" already declared");
			symbolTableErrors++;
		}
		Map<String, STentry> classScopeTable = new HashMap<>();
		classTable.put(node.id, classScopeTable);
		nestingLevel++;
		symbolTable.add(classScopeTable);
		int previousNLDecOffset = decOffset;
		decOffset = -2;
		fieldOffset = -1;
		for (var field : node.fields) {
			STentry fieldEntry =  new STentry(nestingLevel, field.getType(), fieldOffset--);
			classScopeTable.put(field.id, fieldEntry);
			classType.allFields.add(-fieldEntry.offset-1, fieldEntry.type);
		}
		int currentDecOffset = decOffset;
		decOffset = 0;
		for (var method : node.methods) {
			visit(method);
			classType.allMethods.add(method.offset, (ArrowTypeNode) classScopeTable.get(method.id).type);
		}
		decOffset = currentDecOffset;
		symbolTable.remove(--nestingLevel);
		decOffset = previousNLDecOffset;
		return null;
	}

	@Override
	public Void visitNode(MethodNode node){
		if (print) printNode(node);
		Map<String, STentry> currentScopeTable = symbolTable.get(nestingLevel);
		List<TypeNode> parametersTypes = new ArrayList<>();
		for (ParNode parameter : node.parametersList) parametersTypes.add(parameter.getType());
		STentry entry = new STentry(nestingLevel, new MethodTypeNode(new ArrowTypeNode(parametersTypes,node.returnType)),decOffset++);
		node.offset = entry.offset;
		//inserimento di ID nella symtable
		if (currentScopeTable.put(node.id, entry) != null) {
			System.out.println("Fun id " + node.id + " at line "+ node.getLine() +" already declared");
			symbolTableErrors++;
		}
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> methodScopeTable = new HashMap<>();
		symbolTable.add(methodScopeTable);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=-2;

		int parOffset=1;
		for (ParNode parameter : node.parametersList)
			if (methodScopeTable.put(parameter.id, new STentry(nestingLevel, parameter.getType(),parOffset++)) != null) {
				System.out.println("Par id " + parameter.id + " at line "+ node.getLine() +" already declared");
				symbolTableErrors++;
			}
		for (Node declaration : node.declarationsList) visit(declaration);
		visit(node.expression);
		//rimuovere la hashmap corrente poiche' esco dallo scope
		symbolTable.remove(nestingLevel--);
		decOffset = prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
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
			System.out.println("Fun id " + node.id + " at line "+ node.getLine() + " not declared");
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
			System.out.println("Object id " + node.objectId + " at line "+ node.getLine() + " not declared");
			symbolTableErrors++;
		} else {
			node.symbolTableEntry = entry;
			if (entry.type instanceof RefTypeNode){
				STentry methodEntry = classTable.get(((RefTypeNode) entry.type).id).get(node.methodId);
			} else{
				System.out.println("Object id " + node.objectId + " at line "+ node.getLine() + " has no method " + node.methodId);
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
			System.out.println("Var or Par id " + node.id + " at line "+ node.getLine() + " not declared");
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
}
