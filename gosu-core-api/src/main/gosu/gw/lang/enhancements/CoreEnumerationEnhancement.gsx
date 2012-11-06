package gw.lang.enhancements

uses java.util.Enumeration
uses java.util.Collections

/**
 *  Copyright 2010 Guidewire Software, Inc.
 */
enhancement CoreEnumerationEnhancement<E> : Enumeration<E> {

  function toList() : List<E> {
    return Collections.list( this )
  }

}
