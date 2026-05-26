package org.squirrelang;

import java.util.List;

public class SqFunction implements SqCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    SqFunction(List<Token> params, List<Stmt> body, Environment closure) {
        this.closure = closure;
        this.declaration = new Stmt.Function(null, params, body);
        this.isInitializer = false;
    }

    SqFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, args.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return value) {
            if (isInitializer) return closure.getAt(0, "this");
            return value.value;
        }

        if (isInitializer) return closure.getAt(0, "self");
        return null;
    }

    SqFunction bind(SqInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("self", instance);
        return new SqFunction(declaration, environment, isInitializer);
    }

    @Override
    public String toString() {
        if (declaration.name == null) return "<fn>";
        return "<fn " + declaration.name.lexeme + ">";
    }
}
