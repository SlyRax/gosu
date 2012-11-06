/*
 * Copyright 2012. Guidewire Software, Inc.
 */

package gw.lang.reflect;

import gw.lang.parser.CaseInsensitiveCharSequence;
import gw.lang.parser.IExpression;

public interface IInvocableType extends IType, INonLoadableType
{
  public IType[] getParameterTypes();

  public String[] getParameterNames();

  public IExpression[] getDefaultValueExpressions();

  public boolean hasOptionalParams();

  public CaseInsensitiveCharSequence getParamSignature();
}
