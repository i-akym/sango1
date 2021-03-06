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
  Map<PVarSlot, PTypeSkel> bindingDict;
  List<PVarSlot> givenTvarList;

  private PTypeSkelBindings() {}

  public static PTypeSkelBindings create() {
    return create(new ArrayList<PVarSlot>());
  }

  static PTypeSkelBindings create(List<PVarSlot> givenTvarList) {
    PTypeSkelBindings b = new PTypeSkelBindings();
    b.bindingDict = new HashMap<PVarSlot, PTypeSkel>();
    b.givenTvarList = givenTvarList;
    return b;
  }

  public String toString() {
    return this.bindingDict.toString()
      + " G" + this.givenTvarList.toString();
  }

  boolean isBound(PVarSlot var) {
    return this.bindingDict.containsKey(var);
  }

  void bind(PVarSlot var, PTypeSkel typeSkel) {
    this.bindingDict.put(var, typeSkel);
  }

  PTypeSkel lookup(PVarSlot var) {
// /* DEBUG */ System.out.println("PTypeSkelBindings#lookup called " + var);
    PTypeSkel t = this.bindingDict.get(var);
    if (t != null) {
      PVarSlot varSlot = t.getVarSlot();
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

  boolean isGivenTvar(PVarSlot var) { return this.givenTvarList.contains(var); }
}
