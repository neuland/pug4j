package de.neuland.pug4j.lexer;

import java.io.StringReader;
import junit.framework.TestCase;

public class ScannerTest extends TestCase {

  public void testConsume() {
    Scanner scanner = new Scanner(new StringReader("hallo\r\nwelt"));
    String input = scanner.getInput();
    assertEquals("hallo\nwelt", input);
  }
}
