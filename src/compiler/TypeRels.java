package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class TypeRels {

	public static Map<String, String> superType = new HashMap<>();
	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
			String directSuperType = ((RefTypeNode) a).id;
			while (directSuperType != null && !directSuperType.equals(((RefTypeNode) b).id)) {
				directSuperType = superType.get(directSuperType);
			}
			return directSuperType != null;
		}
		if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode) {
			return isSubtype(((ArrowTypeNode) a).returnType, ((ArrowTypeNode) b).returnType) &&
					IntStream.range(0, ((ArrowTypeNode) a).parametersList.size())
							.allMatch(i -> isSubtype(
								((ArrowTypeNode) b).parametersList.get(i),
								((ArrowTypeNode) a).parametersList.get(i))
							);
		}
		return a.getClass().equals(b.getClass())
				|| ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
				|| a instanceof EmptyTypeNode;
	}

}
