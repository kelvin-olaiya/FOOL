package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import java.lang.annotation.Native;
import java.util.stream.Collectors;

import static compiler.TypeRels.*;

/**
 * Visits an EAST.
 *
 * visitNode(node) is does a type checking of a node and returns:
 * -> (For expressions) The type of expression (BoolTypeNode or IntTypeNode).
 * -> (For declarations) "null" as it only checks the inner correctness of the declaration.
 * -> (For a type) "null" as it only checks that the type is not incomplete.
 *
 * visitSTentry(entry) returns the type included in the symbol table entry.
 */
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	TypeCheckEASTVisitor() {
		// enables incomplete tree exceptions
		super(true);
	}
	TypeCheckEASTVisitor(boolean debug) {
		// enables print for debugging
		super(true,debug);
	}

	//checks that a type object is visitable (not incomplete) 
	private TypeNode checkVisit(TypeNode typeNode) throws TypeException {
		visit(typeNode);
		return typeNode;
	} 
	
	@Override
	public TypeNode visitNode(ProgLetInNode node) throws TypeException {
		if (print) printNode(node);
		for (Node declaration : node.declarationList)
			try {
				visit(declaration);
			} catch (IncomplException e) {
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		return visit(node.expression);
	}

	@Override
	public TypeNode visitNode(ProgNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		return visit(node.expression);
	}

	@Override
	public TypeNode visitNode(FunNode node) throws TypeException {
		if (print) {
			printNode(node, node.id);
		}
		for (Node declaration : node.declarationsList)
			try {
				visit(declaration);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		if (!isSubtype(visit(node.expression), checkVisit(node.returnType))) {
			throw new TypeException("Wrong return type for function " + node.id,node.getLine());
		}
		return null;
	}

	@Override
	public TypeNode visitNode(NewNode node) throws TypeException {
		if (print) {
			printNode(node, node.id);
		}
		var classFields = ((ClassTypeNode) node.classSymbolTableEntry.type).allFields;
		if (node.argumentsList.size() != classFields.size()) {
			throw new TypeException(
				"Wrong number of parameters for new instance of class id " + node.id,
				node.getLine()
			);
		}
		for (var i = 0; i < classFields.size(); i++) {
			if (!isSubtype(visit(node.argumentsList.get(i)), classFields.get(i))) {
				throw new TypeException(
					"Wrong type for " + (i+1) + "-th parameter in the invocation of " + node.id,
					node.getLine()
				);
			}
		}
		return new RefTypeNode(node.id);
	}

	@Override
	public TypeNode visitNode(MethodNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		for (Node declaration : node.declarationsList) {
			try {
				visit(declaration);
			} catch (IncomplException e) {
			} catch (TypeException e) {
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		}
		if (!isSubtype(visit(node.expression), checkVisit(node.returnType))) {
			throw new TypeException("Wrong return type for method " + node.id,node.getLine());
		}
		return null;
	}

	@Override
	public TypeNode visitNode(ClassNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		if (node.superID != null) {
			superType.put(node.id, node.superID);
			var classType = node.type;
			var superClassType = (ClassTypeNode) node.superClassEntry.type;
			for (var i = 0; i < superClassType.allFields.size(); i++) {
				if (!isSubtype(classType.allFields.get(i), superClassType.allFields.get(i))) {
					// TODO: better exception message
					throw new TypeException("Wrong field", node.getLine());
				}
			}
			for (var i = 0; i < superClassType.allMethods.size(); i++) {
				if (!isSubtype(classType.allMethods.get(i), superClassType.allMethods.get(i))) {
					// TODO: better exception message
					throw new TypeException("Wrong method", node.getLine());
				}
			}
		}
		for (var method : node.methods) {
			visit(method);
		}
		return null;
	}

	@Override
	public TypeNode visitNode(VarNode node) throws TypeException {
		if (print) {
			printNode(node, node.id);
		}
		if (!isSubtype(visit(node.expression), checkVisit(node.getType()))) {
			throw new TypeException("Incompatible value for variable " + node.id, node.getLine());
		}
		return null;
	}

	@Override
	public TypeNode visitNode(PrintNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		return visit(node.expression);
	}

	@Override
	public TypeNode visitNode(IfNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		if (!(isSubtype(visit(node.condition), new BoolTypeNode()))) {
			throw new TypeException("Non boolean condition in if", node.getLine());
		}
		TypeNode thenBranchType = visit(node.thenBranch);
		TypeNode elseBranchType = visit(node.elseBranch);
		if (isSubtype(thenBranchType, elseBranchType)) return elseBranchType;
		if (isSubtype(elseBranchType, thenBranchType)) return thenBranchType;
		throw new TypeException("Incompatible types in then-else branches", node.getLine());
	}

	@Override
	public TypeNode visitNode(EqualNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode left = visit(node.left);
		TypeNode right = visit(node.right);
		if (!(isSubtype(left, right) || isSubtype(right, left))) {
			throw new TypeException("Incompatible types in equal",node.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(LessEqualNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode left = visit(node.left);
		TypeNode right = visit(node.right);
		if (!(isSubtype(left, right) || isSubtype(right, left))) {
			throw new TypeException("Incompatible types in less equal",node.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(GreaterEqualNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode l = visit(node.left);
		TypeNode r = visit(node.right);
		if (!(isSubtype(l, r) || isSubtype(r, l))) {
			throw new TypeException("Incompatible types in greater equal",node.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(OrNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode left = visit(node.left);
		TypeNode right = visit(node.right);
		if (!(isSubtype(left, right) || isSubtype(right, left))) {
			throw new TypeException("Incompatible types in OR ",node.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(AndNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode left = visit(node.left);
		TypeNode right = visit(node.right);
		if (!(isSubtype(left, right) || isSubtype(right, left))) {
			throw new TypeException("Incompatible types in AND",node.getLine());
		}
		return new BoolTypeNode();
	}
  
  	@Override
	public TypeNode visitNode(NotNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode expressionType = visit(node.expression);
		if (!(isSubtype(expressionType, new IntTypeNode()))) {
			throw new TypeException("Incompatible type in not", node.getLine());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(TimesNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		if (!(isSubtype(visit(node.left), new IntTypeNode())
				&& isSubtype(visit(node.right), new IntTypeNode()))) {
			throw new TypeException("Non integers in multiplication",node.getLine());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(DivNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		if (!(isSubtype(visit(node.left), new IntTypeNode())
				&& isSubtype(visit(node.right), new IntTypeNode()))) {
			throw new TypeException("Non integers in division",node.getLine());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(PlusNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		if ( !(isSubtype(visit(node.left), new IntTypeNode())
				&& isSubtype(visit(node.right), new IntTypeNode())) ) {
			throw new TypeException("Non integers in sum",node.getLine());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(MinusNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		if ( !(isSubtype(visit(node.left), new IntTypeNode())
				&& isSubtype(visit(node.right), new IntTypeNode())) ) {
			throw new TypeException("Non integers in sub",node.getLine());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(CallNode node) throws TypeException {
		if (print) {
			printNode(node, node.id);
		}
		TypeNode functionType = visit(node.symbolTableEntry);
		if (functionType instanceof MethodTypeNode) {
			functionType = ((MethodTypeNode) functionType).functionalType;
		} else if (!(functionType instanceof ArrowTypeNode)) {
			throw new TypeException("Invocation of a non-function "+node.id,node.getLine());
		}
		ArrowTypeNode arrowType = (ArrowTypeNode) functionType;
		if (!(arrowType.parametersList.size() == node.argumentsList.size())) {
			throw new TypeException("Wrong number of parameters in the invocation of "+node.id, node.getLine());
		}
		for (int i = 0; i < node.argumentsList.size(); i++) {
			if (!(isSubtype(visit(node.argumentsList.get(i)), arrowType.parametersList.get(i)))) {
				throw new TypeException(
					"Wrong type for " + (i+1) + "-th parameter in the invocation of " + node.id, node.getLine()
				);
			}
		}
		return arrowType.returnType;
	}

	@Override
	public TypeNode visitNode(ClassCallNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		TypeNode methodType = visit(node.methodEntry);
		if (!(methodType instanceof MethodTypeNode)) {
			throw new TypeException("Invocation of a non-method " + node.methodId, node.getLine());
		}
		ArrowTypeNode arrowType = ((MethodTypeNode) methodType).functionalType;
		if (node.argumentsList.size() != arrowType.parametersList.size()) {
			throw new TypeException("Wrong number of parameters in the invocation of " + node.methodId, node.getLine());
		}
		for (var i = 0; i < node.argumentsList.size(); i++) {
			if (!isSubtype(visit(node.argumentsList.get(i)), arrowType.parametersList.get(i))) {
				throw new TypeException(
					"Wrong type for " + (i+1) + "-th parameter in the invocation of " + node.methodId, node.getLine()
				);
			}
		}
		return arrowType.returnType;
	}

	@Override
	public TypeNode visitNode(IdNode node) throws TypeException {
		if (print) {
			printNode(node, node.id);
		}
		TypeNode idType = visit(node.symbolTableEntry);
		if (idType instanceof ArrowTypeNode) {
			throw new TypeException("Wrong usage of function identifier " + node.id,node.getLine());
		}
		return idType;
	}

	@Override
	public TypeNode visitNode(BoolNode node) {
		if (print) {
			printNode(node, node.value.toString());
		}
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(IntNode node) {
		if (print) {
			printNode(node, node.value.toString());
		}
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(EmptyNode node) {
		if (print) {
			printNode(node);
		}
		return new EmptyTypeNode();
	}

	@Override
	public TypeNode visitNode(MethodTypeNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		visit(node.functionalType);
		return null;
	}

	// gestione tipi incompleti	(se lo sono lancia eccezione)
	@Override
	public TypeNode visitNode(ArrowTypeNode node) throws TypeException {
		if (print) {
			printNode(node);
		}
		for (Node parameter : node.parametersList) {
			visit(parameter);
		}
		visit(node.returnType,"->"); //marks return type
		return null;
	}

	@Override
	public TypeNode visitNode(BoolTypeNode node) {
		if (print) {
			printNode(node);
		}
		return null;
	}

	@Override
	public TypeNode visitNode(IntTypeNode node) {
		if (print) {
			printNode(node);
		}
		return null;
	}

	@Override
	public TypeNode visitNode(RefTypeNode node) {
		if (print) {
			printNode(node);
		}
		return null;
	}

	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (print) {
			printSTentry("type");
		}
		return checkVisit(entry.type);
	}
}
