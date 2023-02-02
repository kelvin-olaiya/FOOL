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
            return ((RefTypeNode) a).id.equals(((RefTypeNode) b).id) || directSuperType != null;
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

    public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {
        if (a instanceof RefTypeNode && b instanceof RefTypeNode
                || a instanceof EmptyTypeNode || b instanceof EmptyTypeNode) {
            if (a instanceof EmptyTypeNode) {
                return b;
            } else if (b instanceof EmptyTypeNode) {
                return a;
            }
            if (((RefTypeNode) a).id.equals(((RefTypeNode) b).id)) {
                return a;
            }
            String type = superType.get(((RefTypeNode) a).id);
            while (type != null && isSubtype(b, new RefTypeNode(type))) {
                type = superType.get(type);
            }
            return type != null ? new RefTypeNode(type) : null;
        } else if ((a instanceof BoolTypeNode || a instanceof IntTypeNode)
                && (b instanceof BoolTypeNode || b instanceof IntTypeNode)) {
            if (a instanceof IntTypeNode || b instanceof IntTypeNode) {
                return new IntTypeNode();
            }
            return new BoolTypeNode();
        }
        return null;
    }
}
