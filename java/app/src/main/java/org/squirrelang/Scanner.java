package org.squirrelang;

import static org.squirrelang.TokenType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private static final Map<String, TokenType> keywords;
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private int column = 1;
  private int columnStart = 1;
  private int multiLineCommentDepth = 0;

  static {
    keywords = new HashMap<>();
    keywords.put("class", TokenType.CLASS);
    keywords.put("else", TokenType.ELSE);
    keywords.put("false", TokenType.FALSE);
    keywords.put("for", TokenType.FOR);
    keywords.put("fn", TokenType.FUNCTION);
    keywords.put("if", TokenType.IF);
    keywords.put("nil", TokenType.NIL);
    keywords.put("out", TokenType.PRINT);
    keywords.put("ret", TokenType.RETURN);
    keywords.put("super", TokenType.SUPER);
    keywords.put("this", TokenType.THIS);
    keywords.put("true", TokenType.TRUE);
    keywords.put("let", TokenType.VAR);
    keywords.put("while", TokenType.WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  /**
   * Produces tokens from a file.
   * The last token is always EOF.
   *
   * @return
   */
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      columnStart = column;
      scanToken();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line, column));
    return tokens;
  }

  /**
   * Checks whether the scanner's location is at the source's end.
   *
   * @return
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }

  /**
   * Increments the scanner's location by one.
   *
   * @return
   */
  private char advance() {
    column++;
    return source.charAt(current++);
  }

  /**
   * Check if the character is a digit.
   *
   * @param c
   * @return
   */
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  /**
   * Consumes a character and compares against input.
   *
   * @param expected
   * @return
   */
  private boolean match(char expected) {
    if (isAtEnd())
      return false;
    if (source.charAt(current) != expected)
      return false;
    column++;
    current++;
    return true;
  }

  /**
   * Returns the character at scanner's location.
   *
   * @return
   */
  private char peek() {
    if (isAtEnd())
      return '\0';
    return source.charAt(current);
  }

  /**
   * Returns the character after the scanner's location
   * without consuming the current character.
   *
   * @return
   */
  private char peekNext() {
    if (current + 1 >= source.length())
      return '\0';
    return source.charAt(current + 1);
  }

  /**
   * Checks if a character is alphabetical or _.
   *
   * @param c
   * @return
   */
  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  /**
   * Checks if a character is either alphabetical,
   * a number, or _.
   *
   * @param c
   * @return
   */
  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  /**
   * Add a token without literal value such as
   * numbers, strings, or booleans.
   *
   * @param type
   */
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  /**
   * Creates a token for the current lexeme and adds it to the list.
   *
   * @param type    e.g. NUMBER, or STRING
   * @param literal The value, e.g. 12.3, "Hello, World"
   */
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line, columnStart));
  }

  /**
   * Scans the next token, handling single and multi-character tokens (!=, ==, <<,
   * &&, etc.),
   * whitespace, line comments, nested block comments, strings, numbers, and
   * identifiers.
   * Unrecognised characters trigger an error and scanning continues.
   */
  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(LEFT_PAREN);
        break;
      case ')':
        addToken(RIGHT_PAREN);
        break;
      case '{':
        addToken(LEFT_BRACE);
        break;
      case '}':
        addToken(RIGHT_BRACE);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '.':
        addToken(DOT);
        break;
      case '-':
        addToken(MINUS);
        break;
      case '+':
        addToken(PLUS);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '*':
        addToken(STAR);
        break;
      case '?':
        addToken(QUESTION);
      case ':':
        addToken(COLON);
      case '^':
        addToken(XOR);
        break;
      case '~':
        addToken(TILDE);
        break;
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        if (match('<')) {
          addToken(SHIFT_LEFT);
        } else if (match('=')) {
          addToken(LESS_EQUAL);
        } else {
          addToken(LESS);
        }
        break;
      case '>':
        if (match('>')) {
          addToken(SHIFT_RIGHT);
        } else if (match('=')) {
          addToken(GREATER_EQUAL);
        } else {
          addToken(GREATER);
        }
        break;
      case '&':
        addToken(match('&') ? AND_AND : AND);
        break;
      case '|':
        addToken(match('|') ? OR_OR : OR);
        break;
      case '/':
        if (match('/')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd())
            advance();
        } else if (match('*')) {
          multiLineCommentDepth++;
          multiLineComment();
        } else {
          addToken(SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;
      case '\n':
        line++;
        column = 1;
        break;
      case '"':
        string();
        break;
      default:
        if (isDigit(c)) {
          while (isDigit(peek()))
            advance();
          if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek()))
              advance();
          }
          addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Squirrelang.error(line, column, "Unexpected character: " + c);
        }
    }
  }

  /**
   * Consumes a nested block comment, tracking depth with multiLineCommentDepth.
   * Errors if the file ends before the comment is closed.
   */
  private void multiLineComment() {
    while (multiLineCommentDepth != 0) {
      if (isAtEnd()) {
        Squirrelang.error(line, column, "Unterminated multi-line comment.");
        return;
      }

      if (peek() == '\n') {
        line++;
        column = 1;
      }

      if (peek() == '*' && peekNext() == '/') {
        multiLineCommentDepth--;
        advance();
      } else if (peek() == '/' && peekNext() == '*') {
        multiLineCommentDepth++;
        advance();
      }
      advance();
    }
  }

  /**
   * Consumes and adds the string value token to the list.
   * Errors if the file ends and the string is not closed.
   */
  private void string() {
    while (!isAtEnd() && peek() != '"') {
      if (peek() == '\n') {
        line++;
        column = 1;
      }
      advance();
    }

    if (isAtEnd()) {
      Squirrelang.error(line, column, "Unterminated string.");
      return;
    }

    advance();
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  /**
   * Checks an alphanumeric word, if it is a keyword,
   * it is added to the token list as that keyword.
   * Otherwise, it is (probably) a user-defined identifier.
   */
  private void identifier() {
    while (isAlphaNumeric(peek()))
      advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null)
      type = IDENTIFIER;
    addToken(type);
  }
}
