package compiler;

import java.util.*;
import java.util.stream.Collectors;

import compiler.lib.*;

public class AST {

    public static class ProgLetInNode extends Node {

        final List<DecNode> declarationList;
        final Node expression;

        ProgLetInNode(List<DecNode> declarations, Node expression) {
            this.declarationList = Collections.unmodifiableList(declarations);
            this.expression = expression;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class ProgNode extends Node {

        final Node expression;

        ProgNode(Node expression) {
            this.expression = expression;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class FunNode extends DecNode {

        final String id;
        final TypeNode returnType;
        final List<ParNode> parametersList;
        final List<DecNode> declarationsList;
        final Node expression;

        FunNode(String id, TypeNode returnType, List<ParNode> parameters, List<DecNode> declarations, Node expression) {
            this.id = id;
            this.returnType = returnType;
            this.parametersList = Collections.unmodifiableList(parameters);
            this.declarationsList = Collections.unmodifiableList(declarations);
            this.expression = expression;
        }

        //void setType(TypeNode t) {type = t;}

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class ParNode extends DecNode {

        final String id;

        ParNode(String id, TypeNode type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class VarNode extends DecNode {

        final String id;
        final Node expression;

        VarNode(String id, TypeNode type, Node expression) {
            this.id = id;
            this.type = type;
            this.expression = expression;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class ClassNode extends DecNode {

        final String id;
        final List<FieldNode> fields;
        final List<MethodNode> methods;

        public ClassNode(String id, List<FieldNode> fields, List<MethodNode> methods) {
            this.id = id;
            this.fields = Collections.unmodifiableList(fields);
            this.methods = Collections.unmodifiableList(methods);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class FieldNode extends DecNode {

        final String id;

        FieldNode(String id, TypeNode type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class MethodNode extends DecNode {

        final String id;
        final TypeNode returnType;
        final List<ParNode> parametersList;
        final List<DecNode> declarationsList;
        final Node expression;
        int offset;

        MethodNode(String id, TypeNode returnType, List<ParNode> parametersList, List<DecNode> declarationsList, Node expression) {
            this.id = id;
            this.returnType = returnType;
            this.parametersList = parametersList;
            this.declarationsList = declarationsList;
            this.expression = expression;
            this.type = new ArrowTypeNode(this.parametersList.stream().map(ParNode::getType).collect(Collectors.toList()), this.returnType);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class PrintNode extends Node {

        final Node expression;

        PrintNode(Node expression) {
            this.expression = expression;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class IfNode extends Node {

        final Node condition;
        final Node thenBranch;
        final Node elseBranch;

        IfNode(Node condition, Node thenBranch, Node elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class EqualNode extends Node {

        final Node left;
        final Node right;

        EqualNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class OrNode extends Node {

        final Node left;
        final Node right;

        OrNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class AndNode extends Node {

        final Node left;
        final Node right;

        AndNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }


    public static class NotNode extends Node {

        final Node expression;

        NotNode(Node expression) {
            this.expression = expression;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }


    public static class GreaterEqualNode extends Node {

        final Node left;
        final Node right;

        GreaterEqualNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }


    public static class LessEqualNode extends Node {

        final Node left;
        final Node right;

        LessEqualNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }


    public static class TimesNode extends Node {

        final Node left;
        final Node right;

        TimesNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }


    public static class DivNode extends Node {

        final Node left;
        final Node right;

        DivNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class PlusNode extends Node {

        final Node left;
        final Node right;

        PlusNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class MinusNode extends Node {

        final Node left;
        final Node right;

        MinusNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class CallNode extends Node {

        final String id;
        final List<Node> argumentsList;
        STentry symbolTableEntry;
        int nestingLevel;

        CallNode(String id, List<Node> arguments) {
            this.id = id;
            argumentsList = Collections.unmodifiableList(arguments);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class ClassCallNode extends Node {

        final String objectId;
        final String methodId;

        int nestingLevel;
        STentry symbolTableEntry;
        STentry methodEntry;
        final List<Node> argumentsList;

        public ClassCallNode(String objectId, String methodId, List<Node> arguments) {
            this.objectId = objectId;
            this.methodId = methodId;
            this.argumentsList = arguments;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class IdNode extends Node {

        final String id;
        STentry symbolTableEntry;
        int nestingLevel;

        IdNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class RefTypeNode extends TypeNode {

        final String id;
        RefTypeNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class BoolNode extends Node {

        final Boolean value;

        BoolNode(boolean value) {
            this.value = value;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class IntNode extends Node {

        final Integer value;

        IntNode(Integer value) {
            this.value = value;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class ArrowTypeNode extends TypeNode {

        final List<TypeNode> parametersList;
        final TypeNode returnType;

        ArrowTypeNode(List<TypeNode> parameters, TypeNode returnType) {
            this.parametersList = Collections.unmodifiableList(parameters);
            this.returnType = returnType;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class MethodTypeNode extends TypeNode {

        final ArrowTypeNode functionalType;

        public MethodTypeNode( ArrowTypeNode functionalType) {
            this.functionalType = functionalType;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class BoolTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class IntTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class ClassTypeNode extends TypeNode {

        final List<TypeNode> allFields;
        final List<ArrowTypeNode> allMethods;

        ClassTypeNode(List<TypeNode> allFields, List<ArrowTypeNode> allMethods) {
            this.allFields = allFields;
            this.allMethods = allMethods;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }
}
