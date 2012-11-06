/*
 * Copyright 2012. Guidewire Software, Inc.
 */

package gw.lang.reflect.gs;

import gw.lang.Deprecated;
import gw.lang.PublishInGosu;
import gw.lang.Scriptable;
import gw.lang.annotation.ScriptabilityModifier;
import gw.lang.reflect.IType;

@PublishInGosu
public interface IGosuObject
{
  @Deprecated(value="Use the 'typeof' operator in Gosu instead. 'obj.IntrinsicType' becomes 'typeof obj'.")
  @Scriptable(ScriptabilityModifier.ALL)
  IType getIntrinsicType();

  //
  // Methods cooresponding with java.lang.Object
  //

  public String toString();

  public int hashCode();

  public boolean equals( Object o );

}
