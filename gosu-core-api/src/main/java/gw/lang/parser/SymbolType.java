/*
 * Copyright 2012. Guidewire Software, Inc.
 */

package gw.lang.parser;

public enum SymbolType {
  CATCH_VARIABLE(true),
  OBJECT_INITIALIZER(false),
  NAMED_PARAMETER(false),
  PARAMETER_DECLARATION(true);

  private boolean isLocal;

  private SymbolType(boolean isLocal) {
    this.isLocal = isLocal;
  }

  public boolean isLocal() {
    return isLocal;
  }
}
