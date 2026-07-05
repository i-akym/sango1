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
import java.util.Set;

public interface PTypeSkel {

  Parser.SrcInfo getSrcInfo();

  int getCat();
  static final int CAT_BOTTOM = 1;
  static final int CAT_SOME = 2;
  static final int CAT_VAR = 3;
  static final int CAT_ANVAR = 4;  // anonymous var

  boolean isLiteralNaked();

  boolean isConcrete();

  PTypeSkel normalize() throws CompileException;

  PTypeSkel resolveBindings(Bindings bindings);

  PTypeSkel instanciate(InstanciationContext context);

  boolean accept(PTypeSkel type, Bindings bindings) throws CompileException ;

  boolean require(PTypeSkel type, Bindings bindings) throws CompileException ;

  // // width is
  // static final int EQUAL = 0;
  // static final int NARROWER = 1;
  // static final int WIDER = - NARROWER;

  boolean includesVar(PTypeVarSlot varSlot, Bindings bindings);

  PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException;
    // foward to following method internally
  JoinResult join2(PTypeSkel type, Bindings bindings) throws CompileException;

  MType toMType(PModule mod, Module.Builder modBuilder, boolean inReferredDef, List<PTypeVarSlot> slotList);

  void extractVars(List<PTypeVarSlot> extracted);

  PTypeSkel unalias(Bindings bindings) throws CompileException;

  void excludeBareTVarAtRet(Parser.SrcInfo si, boolean atRet, List<PTypeVarSlot> checked) throws CompileException;

  void collectTconKeys(Set<PDefDict.IdKey> keys);

  Repr repr();

  // static int calcWidth(int widthContext, Module.Variance variance) {
    // int w;
    // if (widthContext == EQUAL) {
      // w = EQUAL;
    // } else if (variance == Module.INVARIANT) {
      // w = EQUAL;
    // } else if (variance == Module.COVARIANT) {
      // w = widthContext;
    // } else if (variance == Module.CONTRAVARIANT) {
      // w = - widthContext;
    // } else {
      // throw new RuntimeException("Width calculation error.");
    // }
    // return w;
  // }

  static public class Bindings {
    Map<PTypeVarSlot, PTypeSkel> bindingDict;
    List<PTypeVarSkel> concreteVarList;
    List<PTypeVarSlot> givenTVarList;

    private Bindings() {}

    public static Bindings create() {
      return create(new ArrayList<PTypeVarSlot>());
    }

    static Bindings create(List<PTypeVarSlot> givenTVarList) {
      Bindings b = new Bindings();
      b.bindingDict = new HashMap<PTypeVarSlot, PTypeSkel>();
      b.concreteVarList = new ArrayList<PTypeVarSkel>();
      b.givenTVarList = givenTVarList;
      return b;
    }

    Bindings copy() {  // shallow copy
      Bindings b = new Bindings();
      b.bindingDict = new HashMap<PTypeVarSlot, PTypeSkel>();
      b.bindingDict.putAll(this.bindingDict);
      b.concreteVarList = new ArrayList<PTypeVarSkel>();
      b.concreteVarList.addAll(this.concreteVarList);
      b.givenTVarList = new ArrayList<PTypeVarSlot>();
      b.givenTVarList.addAll(this.givenTVarList);
      return b;
    }

    public String toString() {
      return this.bindingDict.toString()
        + " C" + this.concreteVarList.toString()
        + " G" + this.givenTVarList.toString();
    }

    boolean isBound(PTypeVarSkel var) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      return this.bindingDict.containsKey(var.varSlot);
    }

    void bind(PTypeVarSkel var, PTypeSkel typeSkel) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      if (this.isBound(var) || this.isGivenTVar(var)) {
        throw new IllegalArgumentException("Cannot bind. " + var.toString() + " " + this.toString());
      }
      this.bindingDict.put(var.varSlot, typeSkel);
      if (var.requiresConcrete) {
        this.addConcreteVar(var);
      }
    }

    PTypeSkel lookup(PTypeVarSkel var) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      PTypeSkel r = null;
      PTypeSkel t = this.bindingDict.get(var.varSlot);
      while (t != null) {
        r = t;
        if (t instanceof PTypeVarSkel) {
          PTypeVarSkel tv = (PTypeVarSkel)t;
          if (tv.varSlot != null) {
            t = this.bindingDict.get(tv.varSlot);
          } else {
            t = null;
          }
        } else {
          t = null;
        }
      }
      return r;
    }

    private void addConcreteVar(PTypeVarSkel var) {
      if (!this.concreteVarList.contains(var)) {
        this.concreteVarList.add(var);
      }
    }

    PTypeVarSkel getAnyInconcreteVar() {
      PTypeVarSkel iv = null;
      for (int i = 0; iv == null && i < this.concreteVarList.size(); i++) {
        PTypeVarSkel v = this.concreteVarList.get(i);
        if (!v.resolveBindings(this).isConcrete()) {
          iv = v;
        }
      }
      return iv;
    }

    boolean isGivenTVar(PTypeVarSkel var) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      return this.givenTVarList.contains(var.varSlot);
    }
  }

  public static class JoinResult {
    PTypeSkel joined;
    Bindings bindings;

    public static JoinResult create(PTypeSkel joined, Bindings bindings) {
      JoinResult r = new JoinResult();
      r.joined = joined;
      r.bindings = bindings;
      return r;
    }

    private JoinResult() {}
  }

  public static class InstanciationContext {
    List<PTypeVarSlot> givenTVarList;
    Map<PTypeVarSlot, PTypeVarSkel> bindingDict;

    public static InstanciationContext create() {
      return create(new ArrayList<PTypeVarSlot>());
    }

    public static InstanciationContext create(Bindings bindings) {
      return create(bindings.givenTVarList);
    }

    public static InstanciationContext create(List<PTypeVarSlot> givenTVarList) {
      InstanciationContext ic = new InstanciationContext();
      ic.givenTVarList = givenTVarList;
      ic.bindingDict = new HashMap<PTypeVarSlot, PTypeVarSkel>();
      return ic;
    }

    private InstanciationContext() {}

    boolean isGivenTVar(PTypeVarSkel var) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      return this.givenTVarList.contains(var.varSlot);
    }

    boolean isBound(PTypeVarSkel var) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      return this.bindingDict.containsKey(var.varSlot);
    }

    void bind(PTypeVarSkel var, PTypeVarSkel vs) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      if (this.bindingDict.containsKey(var.varSlot)) {
        throw new IllegalArgumentException("Already added. " + var);
      }
      this.bindingDict.put(var.varSlot, vs);
    }

    PTypeVarSkel lookup(PTypeVarSkel var) {
      if (var.varSlot == null) {
        throw new IllegalArgumentException("No slot. " + " " + var.toString());
      }
      return this.bindingDict.get(var.varSlot);
    }
  }

  public static class Repr {
    public static String topLevelRepr(PTypeSkel t) {
      return "<" + t.repr().toString() + ">";
    }

    static Repr create() {
      return new Repr();
    }

    List<String> items;

    private Repr() {
      this.items = new ArrayList<String>();
    }

    void add(String s) {
      if (s.length() > 0) {
        this.items.add(s);
      }
    }

    void add(Repr r) {
      if (r.items.isEmpty()) {
        ;
      } else if (r.items.size() == 1) {
        this.items.add(r.toString());
      } else {
        this.items.add("<" + r.toString() + ">");
      }
    }

    void append(Repr r) {
      this.items.addAll(r.items);
    }

    public String toString() {  // without top-level < >
      StringBuffer buf = new StringBuffer();
      String sep = "";
      for (int i = 0; i < this.items.size(); i++) {
        buf.append(sep);
        buf.append(this.items.get(i));
        sep = " ";
      }
      return buf.toString();
    }
  }
}
