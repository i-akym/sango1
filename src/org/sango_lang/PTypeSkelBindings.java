/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2018 Isao Akiyama                                         *
 *                                                                         *
 * Permission is hereby granted, free of charge, to any person obtaining   *
 * a copy of this software and associated documentation files (the         *
 * "Software"), to deal in the Software without restriction, including     *
 * without limitation the rights to use, copy, modify, merge, publish,     *
 * distribute, sublicense, and/or sell copies of the Software, and to      *
 * permit persons to whom the Software is furnished to do so, subject to   *
 * the following conditions:                                               *
 *                                                                         *
 * The above copyright notice and this permission notice shall be          *
 * included in all copies or substantial portions of the Software.         *
 *                                                                         *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         *
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      *
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  *
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    *
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    *
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       *
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  *
 ***************************************************************************/
package org.sango_lang;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PTypeSkelBindings {
  Map<PTypeVarSlot, PTypeSkel> bindingDict;
  List<PTypeVarSlot> givenTVarList;

  private PTypeSkelBindings() {}

  public static PTypeSkelBindings create() {
    return create(new ArrayList<PTypeVarSlot>());
  }

  static PTypeSkelBindings create(List<PTypeVarSlot> givenTVarList) {
    PTypeSkelBindings b = new PTypeSkelBindings();
    b.bindingDict = new HashMap<PTypeVarSlot, PTypeSkel>();
    b.givenTVarList = givenTVarList;
    return b;
  }

  PTypeSkelBindings copy() {  // shallow copy
    PTypeSkelBindings b = new PTypeSkelBindings();
    b.bindingDict = new HashMap<PTypeVarSlot, PTypeSkel>();
    b.bindingDict.putAll(this.bindingDict);
    b.givenTVarList = new ArrayList<PTypeVarSlot>();
    b.givenTVarList.addAll(this.givenTVarList);
    return b;
  }

  public String toString() {
    return this.bindingDict.toString()
      + " G" + this.givenTVarList.toString();
  }

  boolean isBound(PTypeVarSlot var) {
    return this.bindingDict.containsKey(var);
  }

  void bind(PTypeVarSlot var, PTypeSkel typeSkel) {
/* DEBUG */ if (this.isBound(var) || this.isGivenTVar(var)) {
      throw new IllegalArgumentException("Cannot bind. " + var.toString() + " " + this.toString());
    }
    this.bindingDict.put(var, typeSkel);
  }

  PTypeSkel lookup(PTypeVarSlot var) {
// /* DEBUG */ System.out.println("PTypeSkelBindings#lookup called " + var);
    PTypeSkel t = this.bindingDict.get(var);
    if (t != null) {
      PTypeVarSlot varSlot = t.getVarSlot();
      while (varSlot != null) {
        PTypeSkel t2 = this.bindingDict.get(varSlot);
        if (t2 != null) {
          varSlot = t.getVarSlot();
          t = t2;
        } else {
          varSlot = null;
        }
      }
    }
// /* DEBUG */ System.out.println("PTypeSkelBindings#lookup returns " + t);
    return t;
  }

  boolean isGivenTVar(PTypeVarSlot var) { return this.givenTVarList.contains(var); }

  void addGivenTVar(PTypeVarSlot var) {
/* DEBUG */ if (this.isGivenTVar(var) ) { throw new IllegalArgumentException("Already listed as given. " + var.toString() + " " + this.toString()); }
/* DEBUG */ if (this.isBound(var)) { throw new IllegalArgumentException("Not free. " + var.toString() + " " + this.toString()); }
    this.givenTVarList.add(var);
  }
}
