package de.neuland.pug4j.expression;

import de.neuland.pug4j.model.PugModel;

public abstract class AbstractExpressionHandler implements ExpressionHandler {

  /**
   * Registers all names declared by a leading {@code var}/{@code let}/{@code const} statement as
   * local to the current pug scope. The declaration keyword is the only signal distinguishing
   * pug's two scoping behaviors: declared names shadow outer scopes and die with the scope, bare
   * assignments propagate to the scope where the variable was defined.
   */
  protected void saveLocalVariableName(String expression, PugModel model) {
    DeclarationScanner.Result declaration = DeclarationScanner.scan(expression);
    if (declaration == null) {
      return;
    }
    for (DeclarationScanner.Declarator declarator : declaration.declarators) {
      for (String name : declarator.names) {
        model.putLocalVariableName(name);
      }
    }
  }

  @Override
  public void setCache(boolean cache) {}

  @Override
  public void clearCache() {}
}
