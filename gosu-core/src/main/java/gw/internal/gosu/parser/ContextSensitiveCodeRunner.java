/*
 * Copyright 2012. Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.internal.gosu.ir.transform.expression.EvalExpressionTransformer;
import gw.lang.cli.SystemExitIgnoredException;
import gw.lang.parser.CaseInsensitiveCharSequence;
import gw.lang.parser.ExternalSymbolMapForMap;
import gw.lang.parser.IDynamicFunctionSymbol;
import gw.lang.parser.IParseTree;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.IParsedElementWithAtLeastOneDeclaration;
import gw.lang.parser.IProgramClassFunctionSymbol;
import gw.lang.parser.IStatement;
import gw.lang.parser.ISymbol;
import gw.lang.parser.ISymbolTable;
import gw.lang.parser.StandardSymbolTable;
import gw.lang.parser.expressions.ILocalVarDeclaration;
import gw.lang.parser.expressions.IParameterDeclaration;
import gw.lang.parser.expressions.IVarStatement;
import gw.lang.parser.statements.IFunctionStatement;
import gw.lang.parser.statements.IStatementList;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IExternalSymbolMap;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.java.JavaTypes;
import gw.util.CaseInsensitiveHashMap;

/**
 */
public class ContextSensitiveCodeRunner {

  //!! Needed to ensure this class is loaded so a debugger can call into it remotely
  static void ensureLoadedForDebuggerEval() {
  }

  //!! Do not remove! This is called from the debugger via jdwp.
  /**
   * Intended for use with a debugger to evaluate arbitrary expressions/programs
   * in the context of a source position being debugged, usually at a breakpoint.
   *
   * @param enclosingInstance The instance of the object immediately enclosing the source position.
   * @param extSyms An array of adjacent name/value pairs corresponding with the names and values of local symbols in scope.
   * @param strText The text of the expression/program.
   * @param strClassContext The name of the top-level class enclosing the the source position.
   * @param strContextElementClass The name of the class immediately enclosing the source position (can be same as strClassContext).
   * @param iSourcePosition  The index of the source position within the containing file.
   * @return The result of the expression or, in the case of a program, the return value of the program.
   */
  public static Object runMeSomeCode( Object enclosingInstance, Object[] extSyms, String strText, final String strClassContext, String strContextElementClass, int iSourcePosition )
  {
    try
    {
      IType type = TypeSystem.getByFullName( strClassContext );
      if( !(type instanceof IGosuClassInternal) ) {
        System.out.println( strClassContext + " is not a Gosu class" );
        return null;
      }
      IGosuClassInternal gsClass = (IGosuClassInternal)type;
      gsClass.isValid();
      IParsedElement ctxElem = findElemAt( gsClass, iSourcePosition );
      ISymbolTable compileTimeLocalContextSymbols = findCompileTimeSymbols( gsClass, iSourcePosition );
      IExternalSymbolMap runtimeLocalSymbolValues = makeRuntimeNamesAndValues( extSyms );
      IGosuClassInternal gsImmediateClass = (IGosuClassInternal)TypeSystem.getByFullName( strContextElementClass );
      return EvalExpressionTransformer.compileAndRunEvalSource( strText, enclosingInstance, null, null, gsImmediateClass, ctxElem, compileTimeLocalContextSymbols, runtimeLocalSymbolValues );
    }
    catch( Exception e )
    {
      boolean print = true;
      Throwable t = e;
      while( t != null ) {
        if( t instanceof SystemExitIgnoredException ) {
          print = false;
        }
        t = t.getCause();
      }
      if( print ) {
        assert e != null;
        e.printStackTrace();
      }
    }
    return new String[]{null, null};
  }

  private static IExternalSymbolMap makeRuntimeNamesAndValues( Object[] extSyms ) {
    CaseInsensitiveHashMap<CaseInsensitiveCharSequence, ISymbol> map = new CaseInsensitiveHashMap();
    for( int i = 0; i < extSyms.length; i++ ) {
      String name = (String)extSyms[i];
      map.put( CaseInsensitiveCharSequence.get( name ), new Symbol( name, JavaTypes.OBJECT(), extSyms[++i] ) );
    }

    return new ExternalSymbolMapForMap( map );
  }

  private static IParsedElement findElemAt( IGosuClassInternal gsClass, int iContextLocation ) {
    IParseTree elem = ((IGosuClass)TypeLord.getOuterMostEnclosingClass( gsClass )).getClassStatement().getClassFileStatement().getLocation().getDeepestLocation( iContextLocation, false );
    return elem == null ? gsClass.getClassStatement().getClassFileStatement() : elem.getParsedElement();
  }

  private static ISymbolTable findCompileTimeSymbols( IGosuClassInternal enclosingClass, int iLocation ) {
    ISymbolTable symTable = new StandardSymbolTable( false );
    IParseTree deepestLocation = enclosingClass.getClassStatement().getClassFileStatement().getLocation().getDeepestLocation( iLocation, false );
    collectLocalSymbols( symTable,
                         deepestLocation.getParsedElement(),
                         iLocation );
//    for( Symbol s : (Collection<Symbol>)symTable.getSymbols().values() ) {
//      System.out.println( "Symbol: " + s.getName() );
//    }
    return symTable;
  }

  public static void collectLocalSymbols( ISymbolTable symTable, IParsedElement parsedElement, int iOffset ) {
    if( parsedElement == null ) {
      return;
    }

    if( parsedElement instanceof IFunctionStatement ) {
      IFunctionStatement declStmt = (IFunctionStatement)parsedElement;
      for( IParameterDeclaration localVar : declStmt.getParameters() ) {
        if( localVar != null && localVar.getLocation().getOffset() < iOffset ) {
          ISymbol symbol = localVar.getSymbol();
          symTable.putSymbol( symbol );
        }
      }
    }
    else if( parsedElement instanceof IParsedElementWithAtLeastOneDeclaration ) {
      IParsedElementWithAtLeastOneDeclaration declStmt = (IParsedElementWithAtLeastOneDeclaration)parsedElement;
      for( String strVar : declStmt.getDeclarations() ) {
        ILocalVarDeclaration localVar = findLocalVarSymbol( strVar, declStmt );
        if( localVar != null && localVar.getLocation().getOffset() < iOffset ) {
          ISymbol symbol = localVar.getSymbol();
          symTable.putSymbol( symbol );
        }
      }
    }
    else if( parsedElement instanceof IStatementList ) {
      IStatementList stmtList = (IStatementList)parsedElement;
      for( IStatement stmt : stmtList.getStatements() ) {
        if( stmt instanceof IVarStatement && !((IVarStatement)stmt).isFieldDeclaration() && stmt.getLocation().getOffset() < iOffset ) {
          ISymbol symbol = ((IVarStatement)stmt).getSymbol();
          if( isProgramFieldVar( stmt ) ) {
            continue;
          }
          symTable.putSymbol( symbol );
        }
      }
    }
    IParsedElement parent = parsedElement.getParent();
    if( parent != parsedElement ) {
      collectLocalSymbols( symTable, parent, iOffset );
    }
  }

  private static boolean isProgramFieldVar( IStatement stmt ) {
    if( stmt.getParent() != null ) {
      IParsedElement parent = stmt.getParent().getParent();
      if( parent instanceof IFunctionStatement ) {
        IDynamicFunctionSymbol dfs = ((IFunctionStatement)parent).getDynamicFunctionSymbol();
        if( dfs instanceof IProgramClassFunctionSymbol ) {
          return true;
        }
      }
    }
    return false;
  }

  private static ILocalVarDeclaration findLocalVarSymbol( String strVar, IParsedElement pe ) {
    if( pe instanceof ILocalVarDeclaration ) {
      ISymbol symbol = ((ILocalVarDeclaration)pe).getSymbol();
      if( symbol != null && symbol.getName().equals( strVar ) ) {
        return (ILocalVarDeclaration)pe;
      }
      return null;
    }
    if( pe == null ) {
      return null;
    }
    for( IParseTree child : pe.getLocation().getChildren() ) {
      ILocalVarDeclaration localVar = findLocalVarSymbol( strVar, child.getParsedElement() );
      if( localVar != null ) {
        return localVar;
      }
    }
    return null;
  }
}
