package compiler;

import compiler.lib.*;

/**
 * Symbol Table Entry. It's a set of attributes associated
 * with a name (ID). It includes e.g:
 *  -> Kind of name: variable, class, field, method, etc.
 *  -> Type: int, float, etc.
 *  -> Nesting level
 *  -> Memory location: the offset where to be found in memory at runtime.
 */
public class STentry implements Visitable {

	final int nestingLevel;
	final TypeNode type;
	final int offset;

	public STentry(int nestingLevel, TypeNode type, int offset) {
		this.nestingLevel = nestingLevel;
		this.type = type;
		this.offset = offset;
	}

	@Override
	public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {
		return ((BaseEASTVisitor<S,E>) visitor).visitSTentry(this);
	}
}
