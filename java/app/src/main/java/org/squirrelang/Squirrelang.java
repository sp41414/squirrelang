package org.squirrelang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Squirrelang {
  private static final String RESET = "\033[0m";
  private static final String RED = "\033[1;31m";
  private static final String BLUE = "\033[1;34m";
  static boolean hadError = false;
  static String fileName = "<stdin>";
  static String source = "";

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: java Squirrelang <file>");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String pathName) throws IOException {
    fileName = pathName.substring(pathName.lastIndexOf("/") + 1);
    source = new String(Files.readAllBytes(Paths.get(pathName)), Charset.defaultCharset());
    run(source);
    if (hadError)
      System.exit(65);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      source = reader.readLine();
      if (source == null)
        break;
      run(source);
      hadError = false;
    }
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    Expr expression = parser.parse();

    if (hadError || expression == null)
      return;

    System.out.println(new AstPrinter().print(expression));
  }

  /**
   * Reports an error in format:
   * error: message
   * --> file:line:col
   * |
   * 
   * @param line
   * @param message
   */
  static void error(int line, int column, String message) {
    report(line, column, "", message);
  }

  /**
   * Reports an error in format:
   * error: message
   * --> file:line:col
   * |
   * l | token lexeme
   * ^^^^^
   * 
   * @param token
   * @param message
   */
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, token.column, null, message);
    } else {
      report(token.line, token.column, token.lexeme, message);
    }
  }

  private static void report(int line, int column, String where, String message) {
    String pad = " ".repeat(String.valueOf(line).length());
    String sourceLine = getLine(line);

    System.err.println(RED + "error" + RESET + ": " + message);
    System.err.println(BLUE + pad + " --> " + RESET + fileName + ":" + line + ":" + column);
    System.err.println(BLUE + pad + " |" + RESET);
    System.err.println(BLUE + line + " | " + RESET + sourceLine);
    System.err.println(BLUE + pad + " | " + RESET + " ".repeat(column - 1) + RED
        + "^".repeat(where == null || where.isEmpty() ? 1 : where.length()) + RESET);
  }

  private static String getLine(int line) {
    int currLine = 1;
    int start = 0;

    for (int i = 0; i < source.length(); i++) {
      if (source.charAt(i) == '\n') {
        if (currLine == line)
          return source.substring(start, i);
        currLine++;
        start = i + 1;
      }
    }

    return source.substring(start);
  }
}
