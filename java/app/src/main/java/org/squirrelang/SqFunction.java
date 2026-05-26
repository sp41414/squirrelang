package org.squirrelang;

import java.util.List;

public class SqFunction implements SqCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;
    private final boolean isStatic;

    SqFunction(List<Token> params, List<Stmt> body, Environment closure) {
        this.closure = closure;
        this.declaration = new Stmt.Function(false, null, params, body);
        this.isInitializer = false;
        this.isStatic = false;
    }

    SqFunction(Stmt.Function declaration, Environment closure, boolean isInitializer, boolean isStatic) {
        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
        this.isStatic = isStatic;
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
        return new SqFunction(declaration, environment, isInitializer, isStatic);
    }

    @Override
    public String toString() {
        if (declaration.name == null) return "<fn>";
        return "<fn " + declaration.name.lexeme + ">";
    }
}
