package org.squirrelang;

import static org.squirrelang.TokenType.*;

/*
TODO list:
[x] implement ternary operator
[] bitwise operators by booleans aswell (convert to 0 and 1)
[] 3 < "pancake" should be 3 < "pancake".length() for example
[] implement "scone" + 4 = "scone4"
[] error on division by 0
 */
public class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Squirrelang.runtimeError(error);
        }
    }

    private String stringify(Object value) {
        if (value == null) return "nil";
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return value.toString();
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                return +(double) right;
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            // cant do NOT on doubles, so make it LONG, apply NOT, then back to double again
            case TILDE:
                return Double.longBitsToDouble(~Double.doubleToLongBits((double) right));
        };
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case MINUS:
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case XOR:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation((double) left, XOR, (double) right);
            case AND:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation((double) left, AND, (double) right);
            case OR:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation((double) left, OR, (double) right);
            case SHIFT_LEFT:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation((double) left, SHIFT_LEFT, (double) right);
            case SHIFT_RIGHT:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation((double) left, SHIFT_RIGHT, (double) right);
            case SHIFT_RIGHT_UNSIGNED:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation((double) left, SHIFT_RIGHT_UNSIGNED, (double) right);
        }
        return null;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);
        if (isTruthy(condition)) return evaluate(expr.thenBranch);
         else return evaluate(expr.elseBranch);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private double binaryBitwiseOperation(double a, TokenType op, double b) {
        long longA = Double.doubleToLongBits(a);
        long longB = Double.doubleToLongBits(b);

        switch(op) {
            case XOR: return Double.longBitsToDouble(longA ^ longB);
            case AND: return Double.longBitsToDouble(longA & longB);
            case OR: return Double.longBitsToDouble(longA | longB);
            case SHIFT_RIGHT: return Double.longBitsToDouble(longA >> longB);
            case SHIFT_LEFT: return Double.longBitsToDouble(longA << longB);
            case SHIFT_RIGHT_UNSIGNED: return Double.longBitsToDouble(longA >>> longB);
        };

        return 0.0;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
}