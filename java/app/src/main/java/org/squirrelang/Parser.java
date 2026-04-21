package org.squirrelang;

import java.util.List;
import static org.squirrelang.TokenType.*;

/**
 * Parses a flat list of tokens into an expression tree using recursive descent.
 * Operator precedence (low to high): equality, bitwise, comparison, term,
 * factor, unary, primary.
 */
public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        Expr expr = ternary();
        while (match(COMMA)) {
            Token operator = previous();
            if (!canStartExpression()) {
                throw error(operator, "Expect expression after ','");
            }
            Expr right = ternary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr ternary() {
        Expr expr = bitwiseOr();

        if (match(QUESTION)) {
            Expr thenExpr = bitwiseOr();
            consume(COLON, "Expect ':' after ternary condition");
            Expr elsExpr = ternary();
            return new Expr.Ternary(expr, thenExpr, elsExpr);
        }

        return expr;
    }

    private Expr bitwiseOr() {
        Expr expr = bitwiseXor();
        while (match(OR)) {
            Token operator = previous();
            Expr right = bitwiseXor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr bitwiseXor() {
        Expr expr = bitwiseAnd();
        while (match(XOR)) {
            Token operator = previous();
            Expr right = bitwiseAnd();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr bitwiseAnd() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Expr comparison() {
        Expr expr = shift();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = shift();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr shift() {
        Expr expr = term();

        while (match(SHIFT_RIGHT, SHIFT_LEFT, SHIFT_RIGHT_UNSIGNED)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS, TILDE, PLUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(NUMBER, STRING))
            return new Expr.Literal(previous().literal);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression");
    }

    private Token consume(TokenType type, String errorMessage) {
        if (check(type))
            return advance();

        throw error(peek(), errorMessage);
    }

    private boolean canStartExpression() {
        return check(NUMBER) || check(STRING) || check(IDENTIFIER) || check(LEFT_PAREN) || check(BANG) || check(MINUS)
                || check(TILDE) || check(TRUE) || check(FALSE) || check(NIL);
    }

    private ParseError error(Token token, String message) {
        Squirrelang.error(token, message);
        return new ParseError();
    }

    /**
     * Discards tokens until a statement boundary to recover from a parser error
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case CLASS:
                case FUNCTION:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
