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

  boolean isLiteralNaked();

  boolean isConcrete();
  // boolean isConcrete(List<PTypeVarSlot> givenTVarList);
  // boolean isConcrete(PTypeSkelBindings bindings);

  PTypeSkel extractAnyInconcreteVar(PTypeSkel type);
  // PTypeSkel extractAnyInconcreteVar(PTypeSkel type, List<PTypeVarSlot> givenTVarList);

  PTypeSkel normalize() throws CompileException;

  PTypeSkel resolveBindings(PTypeSkelBindings bindings);

  PTypeSkel instanciate(InstanciationContext context);

  boolean accept(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException ;

  boolean require(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException ;

  // width is
  static final int EQUAL = 0;
  static final int NARROWER = 1;
  static final int WIDER = - NARROWER;

  boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings);

  PTypeVarSlot getVarSlot();

  PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException;
    // foward to following method internally
  JoinResult join2(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException;

  MType toMType(PModule mod, List<PTypeVarSlot> slotList);

  void extractVars(List<PTypeVarSlot> extracted);

  // void collectTconProps(List<PDefDict.TconProps> list);

  PTypeSkel unalias(PTypeSkelBindings bindings) throws CompileException;

  void collectTconKeys(Set<PDefDict.IdKey> keys);

  Repr repr();

  static int calcWidth(int widthContext, Module.Variance variance) {
    int w;
    if (widthContext == EQUAL) {
      w = EQUAL;
    } else if (variance == Module.INVARIANT) {
      w = EQUAL;
    } else if (variance == Module.COVARIANT) {
      w = widthContext;
    } else if (variance == Module.CONTRAVARIANT) {
      w = - widthContext;
    } else {
      throw new RuntimeException("Width calculation error.");
    }
    return w;
  }

  public static class JoinResult {
    PTypeSkel joined;
    PTypeSkelBindings bindings;

    public static JoinResult create(PTypeSkel joined, PTypeSkelBindings bindings) {
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

    public static InstanciationContext create(PTypeSkelBindings bindings) {
      return create(bindings.givenTVarList);
    }

    public static InstanciationContext create(List<PTypeVarSlot> givenTVarList) {
      InstanciationContext ic = new InstanciationContext();
      ic.givenTVarList = givenTVarList;
      ic.bindingDict = new HashMap<PTypeVarSlot, PTypeVarSkel>();
      return ic;
    }

    private InstanciationContext() {}

    boolean isGivenTVar(PTypeVarSlot var) { return this.givenTVarList.contains(var); }

    // boolean isInFeatureImpl(PTypeVarSlot var) { return this.applBindings.isInFeatureImpl(var); }

    boolean isBound(PTypeVarSlot var) {
      return this.bindingDict.containsKey(var);
    }

    void bind(PTypeVarSlot var, PTypeVarSkel vs) {
      PTypeVarSlot s = vs.getVarSlot();
      if (this.bindingDict.containsKey(var)) {
        throw new IllegalArgumentException("Already added. " + var);
      }
      this.bindingDict.put(var, vs);
    }

    PTypeVarSkel lookup(PTypeVarSlot var) {
      return this.bindingDict.get(var);
    }

    // boolean isBoundAppl(PTypeVarSlot var) {
      // return this.applBindings.isBound(var);
    // }

    // PTypeSkel lookupAppl(PTypeVarSlot var) {
      // return this.applBindings.lookup(var);
    // }
  }

  static class VarianceTab {
    PTypeVarSlot[] varSlots;
    Module.Variance[] variances;

    static VarianceTab create(PTypeVarSlot[] ss, Module.Variance[] vs) {
      if (ss.length != vs.length) {
        throw new IllegalArgumentException("Length mismatch.");
      }
      VarianceTab vt = new VarianceTab();
      vt.varSlots = new PTypeVarSlot[ss.length];
      vt.variances = new Module.Variance[ss.length];
      for (int i = 0; i < ss.length; i++) {
        vt.varSlots[i] = ss[i];
        vt.variances[i] = vs[i];
      }
      return vt;
    }

    private VarianceTab() {}

    VarianceTab forContext(Module.Variance v) {
      VarianceTab vt;
      if (v == Module.INVARIANT) {
        vt = this;
        for (int i = 0; vt != null && i < this.variances.length; i++) {
          Module.Variance w = this.variances[i];
          if (w == Module.INVARIANT) {
            ;  // ok
          } else if (w == Module.COVARIANT) {
            vt = null;  // error
          } else if (w == Module.CONTRAVARIANT) {
            vt = null;  // error
          } else {
            throw new RuntimeException("Unexpected variance. " + w);
          }
        }
      } else if (v == Module.COVARIANT) {
        vt = this;
      } else if (v == Module.CONTRAVARIANT) {
        Module.Variance[] ws = new Module.Variance[this.variances.length];
        for (int i = 0; i < this.variances.length; i++) {
          Module.Variance w = this.variances[i];
          if (w == Module.INVARIANT) {
            ws[i] = Module.INVARIANT;
          } else if (w == Module.COVARIANT) {
            ws[i] = Module.CONTRAVARIANT;
          } else if (w == Module.CONTRAVARIANT) {
            ws[i] = Module.COVARIANT;
          } else {
            throw new RuntimeException("Unexpected variance. " + w);
          }
        }
        vt = VarianceTab.create(this.varSlots, ws);
      } else {
        throw new IllegalArgumentException("Unknown variance. " + v);
      }
      return vt;
    }

    boolean isCompatible(PTypeVarSlot s, Module.Variance v) {  // v: variance def in attr typeref
      Module.Variance w = v;  // if not founed, assume to be same value
      for (int i = 0; i < this.varSlots.length; i++) {
        if (this.varSlots[i] == s) {
          w = this.variances[i];
          break;
        }
      }
      boolean b;
      if (v == w) {
        b = true;
      } else if (w == Module.INVARIANT) {
        b = true;
      } else {
        b = false;
      }
      return b;
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
