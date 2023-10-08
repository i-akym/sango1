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
  Map<PTypeVarSlot, PTypeVarSkel> givenBindingDict;
  List<PTypeVarSlot> givenTVarList;
  List<PTypeVarSlot> featureTVarList;

  private PTypeSkelBindings() {}

  public static PTypeSkelBindings create() {
    return create(new ArrayList<PTypeVarSlot>());
  }

  static PTypeSkelBindings create(List<PTypeVarSlot> givenTVarList) {
    PTypeSkelBindings b = new PTypeSkelBindings();
    b.bindingDict = new HashMap<PTypeVarSlot, PTypeSkel>();
    b.givenBindingDict = new HashMap<PTypeVarSlot, PTypeVarSkel>();
    b.givenTVarList = givenTVarList;
    b.featureTVarList = new ArrayList<PTypeVarSlot>();
    return b;
  }

  PTypeSkelBindings copy() {  // shallow copy
    PTypeSkelBindings b = new PTypeSkelBindings();
    b.bindingDict = new HashMap<PTypeVarSlot, PTypeSkel>();
    b.bindingDict.putAll(this.bindingDict);
    b.givenBindingDict = new HashMap<PTypeVarSlot, PTypeVarSkel>();
    b.givenBindingDict.putAll(this.givenBindingDict);
    b.givenTVarList = new ArrayList<PTypeVarSlot>();
    b.givenTVarList.addAll(this.givenTVarList);
    b.featureTVarList = new ArrayList<PTypeVarSlot>();
    b.featureTVarList.addAll(this.featureTVarList);
    return b;
  }

  public String toString() {
    return this.bindingDict.toString()
      + " G" + this.givenTVarList.toString()
      + " GB" + this.givenBindingDict.toString()
      + " F" + this.featureTVarList.toString();
  }

  boolean isBound(PTypeVarSlot var) {
    return this.bindingDict.containsKey(var);
  }

  void bind(PTypeVarSlot var, PTypeSkel typeSkel) {
/* DEBUG */ if (this.isBound(var) || this.givenBindingDict.get(var) != null || this.isGivenTVar(var)) {
      throw new IllegalArgumentException("Cannot bind. " + var.toString() + " " + this.toString());
    }
    this.bindingDict.put(var, typeSkel);
  }

  void bindGiven(PTypeVarSlot var, PTypeVarSkel given) {
/* DEBUG */ if (this.isBound(var) || this.givenBindingDict.get(var) != null || this.isGivenTVar(var)) {
      throw new IllegalArgumentException("Cannot bind. " + var.toString() + " " + this.toString());
    }
    this.givenBindingDict.put(var, given);
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

  PTypeVarSkel getGivenBound(PTypeVarSlot var) {
    return this.givenBindingDict.get(var);
  }

  boolean isGivenTVar(PTypeVarSlot var) { return this.givenTVarList.contains(var); }

  // void addGivenTVar(PTypeVarSlot var) {
// /* DEBUG */ if (this.isGivenTVar(var) ) { throw new IllegalArgumentException("Already listed as given. " + var.toString() + " " + this.toString()); }
// /* DEBUG */ if (this.isBound(var)) { throw new IllegalArgumentException("Not free. " + var.toString() + " " + this.toString()); }
    // this.givenTVarList.add(var);
  // }

  PTypeSkelBindings copyForFeatureImpl(PTypeVarSlot var) {
    PTypeSkelBindings b = this.copy();
    b.featureTVarList.add(var);
    return b;
  }

  boolean isInFeatureImpl(PTypeVarSlot var) { return this.featureTVarList.contains(var); }
}
