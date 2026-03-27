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

  /**
   * Interprets a file passed in.
   * @param pathName
   * @throws IOException
   */
  private static void runFile(String pathName) throws IOException {
    fileName = pathName.substring(pathName.lastIndexOf("/") + 1);
    byte[] bytes = Files.readAllBytes(Paths.get(pathName));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError) System.exit(65);
  }

  /**
   * A REPL prompt that interprets as it comes in stdin.
   * @throws IOException
   */
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (; ; ) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;
    }
  }

  /**
   * Scans, parses and interprets source code.
   * @param source
   */
  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    Expr expression = parser.parse();

    if (hadError) return;

    System.out.println(new AstPrinter().print(expression));
  }

  /**
   * Reports an error in format:
   * error: message
   *   --> file:line
   *    |
   * @param line
   * @param message
   */
  static void error(int line, String message) {
    report(line, "", message);
  }

  /**
   * Reports an error in format:
   * error: message
   *   --> file:line
   *   |
   * l | token lexeme
   * @param token
   * @param message
   */
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, null, message);
    } else {
      report(token.line, token.lexeme, message);
    }
  }

  /**
   * Formats and prints errors to stderr Rust-style.
   * @param line
   * @param where
   * @param message
   */
  private static void report(int line, String where, String message) {
    String pad = " ".repeat(String.valueOf(line).length());
    System.err.println(RED + "error" + RESET + ": " + message);
    System.err.println(BLUE + pad + " --> " + RESET + fileName + ":" + line);
    System.err.println(BLUE + pad + "  |" + RESET);
    if (where != null && !where.isEmpty()) {
      System.err.println(BLUE + line + "  | " + RESET + where);
    }
  }
}
