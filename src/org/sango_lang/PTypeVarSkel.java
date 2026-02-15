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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PTypeVarSkel implements PTypeSkel {
  Compiler theCompiler;
  Parser.SrcInfo srcInfo;
  String name;  // maybe null
  PTypeVarSlot varSlot;  // null if anonymous
  boolean requiresConcrete;
  PFeatureSkel.List features;  // maybe null

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Compiler theCompiler, Parser.SrcInfo srcInfo, String name, PTypeVarSlot varSlot, boolean requiresConcrete, PFeatureSkel.List features) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.theCompiler = theCompiler;
    var.srcInfo = srcInfo;
    if (varSlot != null) {
      var.name = ((name != null)? name + ":": "") + varSlot.toString();
    } else {
      var.name = "(ANONYM)";
    }
    var.varSlot = varSlot;
    var.requiresConcrete = requiresConcrete;
    var.features = features;
    return var;
  }

  PTypeVarSkel cast(boolean newRequiresConcrete, PFeatureSkel.List newFeatures, PTypeSkelBindings bindings) {
    if (this.varSlot == null) { throw new RuntimeException("Unexpected " + this); }
    PTypeVarSlot s = PTypeVarSlot.create();
    String n = this.name + ":" + s.toString();
    PTypeVarSkel v = create(this.theCompiler, this.srcInfo, n, s, newRequiresConcrete, newFeatures);
    bindings.bind(this.varSlot, v);
    return v;
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof PTypeVarSkel)) {
      b = false;
    } else {
      PTypeVarSkel v = (PTypeVarSkel)o;
      b = (this.varSlot != null) & (v.varSlot == this.varSlot);  // ok?
    }
    return b;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("tvarskel[src=");
    buf.append(this.srcInfo);
    buf.append(",name=");
    buf.append(this.name);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    if (this.features != null) {
      buf.append(",features=");
      buf.append(this.features);
    }
    buf.append("]");
    return buf.toString();
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  public int getCat() {
    return (this.varSlot != null)? PTypeSkel.CAT_VAR: PTypeSkel.CAT_ANVAR;
  }

  public boolean isLiteralNaked() { return false; }

  public boolean isConcrete() {
    return this.requiresConcrete;
  }

  public PTypeSkel extractAnyInconcreteVar(PTypeSkel type) {
    PTypeSkel t = null;
    if (!this.requiresConcrete) {
      ; 
    } else if (!type.isConcrete()) {
      t = type;
    } else if (this.features != null) {
      t = this.features.extractAnyInconcreteVar();
    }
    return t;
  }

  public PTypeVarSkel normalize() throws CompileException {
    PFeatureSkel.List fs = (this.features != null)? this.features.normalize(): null;
    return create(this.theCompiler, this.srcInfo, this.name, this.varSlot, this.requiresConcrete, fs);
  }

  public PTypeSkel resolveBindings(PTypeSkelBindings bindings) {
    PTypeSkel t;
    PFeatureSkel.List fs = (this.features != null)? this.features.resolveBindings(bindings): null;
    if (this.varSlot == null) {
      t = create(this.theCompiler, this.srcInfo, this.name, this.varSlot, this.requiresConcrete, fs);
    } else if (bindings.isBound(this.varSlot)) {
      t = bindings.lookup(this.varSlot).resolveBindings(bindings);
    } else if (fs != null) {
      t = create(this.theCompiler, this.srcInfo, this.name, this.varSlot, this.requiresConcrete, fs);
    } else {
      t = this;
    }
    return t;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationContext context) {
    PTypeSkel t;
    if (this.varSlot == null) {
      PTypeVarSkel v = new PTypeVarSkel();
      v.srcInfo = this.srcInfo;
      v.varSlot = null;
      v.name = this.name /* + "." + v.varSlot.id */ ;
      v.requiresConcrete = this.requiresConcrete;
      v.features = (this.features != null)? this.features.instanciate(context): null;
      t = v;
    } else if (context.isGivenTVar(this.varSlot)) {
      t = this;
    } else if (context.isBound(this.varSlot)) {
      t = context.lookup(this.varSlot);  // created before
    } else {  // create new var for free
      PTypeVarSkel v = new PTypeVarSkel();
      v.srcInfo = this.srcInfo;
      v.varSlot = PTypeVarSlot.create();
      v.name = this.name /* + "." + v.varSlot.id */ ;
      v.requiresConcrete = this.requiresConcrete;
      v.features = (this.features != null)? this.features.instanciate(context): null;
      context.bind(this.varSlot, v);
      t = v;
    }
// /* DEBUG */ System.out.print("INSTANCIATE ! "); System.out.print(this); System.out.print(" => "); System.out.println(t);
    return t;
  }

  public boolean accept(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      b = ttv.accept1Anonym(width, ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv.varSlot)) {
        b = ttv.accept1Given(width, ttt, bindings);
      } else {
        b = ttv.accept1Free(width, ttt, bindings);
      }
    } else {
      b = tt.accept(width, ttt, bindings);  // forward
    }
    return b;
  }

  boolean accept1Anonym(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
      PTypeRefSkel tr = (PTypeRefSkel)type;
      b = this.accept1AnonymSome(width, tr, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (bindings.isGivenTVar(tv.varSlot)) {
        b = this.accept1AnonymGiven(width, tv, bindings);
      } else {
        b = this.accept1AnonymFree(width, tv, bindings);
      }
    } else {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      b = this.accept1AnonymAnonym(width, tv, bindings);
    }
    return b;
  }

  boolean accept1AnonymSome(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    if (this.features.acceptObj(width, tr, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1AnonymGiven(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.acceptList(width, tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1AnonymFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.acceptList(width, tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1AnonymAnonym(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.acceptList(width, tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1Given(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("PTypeVarSkel#accept1Given (to anonymous) not implementd.");
    }
    return b;
  }

  boolean accept1GivenVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    // tv: GIVEN or FREE
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tv.varSlot == null) {
      throw new RuntimeException("PTypeVarSkel#accept1GivenVar (anonymous tv) not implemented.");
    } else if (this.varSlot == tv.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (bindings.isGivenTVar(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenGiven(width, tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenFree(width, tv, bindings);
    }
    return b;
  }

  boolean accept1GivenGiven(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean accept1GivenFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.requiresConcrete == tv.requiresConcrete || !this.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
        bindings.bind(tv2.varSlot, this);
        b = true;
      } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
        bindings.bind(tv2.varSlot, this);
        b = true;
      } else if (this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
        bindings.bind(tv2.varSlot, this);
        b = true;
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        b = false;
      }
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean accept1Free(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, type);
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeSome(width, (PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("PTypeVarSkel#accept1Free (to anonymous) not implementd.");
    }
    return b;
  }

  boolean accept1FreeSome(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tr.includesVar(this.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, tr);  // this.varSlot might be bound in accepting features, so bind this.varSlot first
      if (this.features == null || this.features.acceptObj(width, tr, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2a "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
        b = true;
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2b "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
        b = false;
      }
    }
    return b;
  }

  boolean accept1FreeVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tv.varSlot == null) {
      throw new RuntimeException("PTypeVarSkel#accept1GivenVar (anonymous tv) not implemented.");
    } else if (this.varSlot == tv.varSlot) {
      b = true;
    } else if (bindings.isGivenTVar(tv.varSlot)) {
      b = this.accept1FreeGiven(width, tv, bindings);
    } else {
      b = this.accept1FreeFree(width, tv, bindings);
    }
    return b;
  }

  boolean accept1FreeGiven(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    bindings.bind(this.varSlot, tv);
    if (this.features == null || this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean accept1FreeFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    bindings.bind(this.varSlot, tv);
    if (this.features == null || this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;

    // PTypeVarSkel tv2;
    // if (this.requiresConcrete == tv.requiresConcrete || !this.requiresConcrete) {
      // if (this.features == null) {
        // tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
        // bindings.bind(this.varSlot, tv2);
        // b = true;
      // } else if (tv.features == null) {
        // tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
        // bindings.bind(this.varSlot, tv2);
        // b = true;
      // } else if (this.features.acceptList(width, tv.features, bindings)) {
        // tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
        // bindings.bind(this.varSlot, tv2);
        // b = true;
      // } else {
        // b = false;
      // }
    // } else {
      // b = false;
    // }
  }

  public boolean require(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      b = ttv.require1Anonym(width, ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv.varSlot)) {
        b = ttv.require1Given(width, ttt, bindings);
      } else {
        b = ttv.require1Free(width, ttt, bindings);
      }
    } else {
      b = tt.require(width, ttt, bindings);  // forward
    }
    return b;
  }

  boolean require1Anonym(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Anonym "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = this.features.requireObj(width, type, bindings);
    return b;
  }

  boolean require1Given(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("PTypeVarSkel#require1Given (to anonymous) not implementd.");
    }
    return b;
  }

  boolean require1GivenVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    // tv: GIVEN or FREE
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tv.varSlot == null) {
      throw new RuntimeException("PTypeVarSkel#requireGivenVar (anonymous tv) not implemented.");
    } else if (this.varSlot == tv.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (bindings.isGivenTVar(tv.varSlot) /* || bindings.getGivenBound(tv.varSlot) != null */) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenGiven(width, tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenFree(width, tv, bindings);
    }
    return b;
  }

  boolean require1GivenGiven(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean require1GivenFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      bindings.bind(tv2.varSlot, this);
      b = true;
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(tv2.varSlot, this);
      b = true;
    } else if (this.features.requireList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      bindings.bind(tv2.varSlot, this);
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean require1Free(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, type);
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1FreeSome(width, (PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1FreeVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("PTypeVarSkel#require1Free (to anonymous) not implementd.");
    }
    return b;
  }

  boolean require1FreeSome(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeSome "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean require1FreeVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tv.varSlot == null) {
      throw new RuntimeException("PTypeVarSkel#require1FreeVar (anonymous tv) not implemented.");
    } else if (this.varSlot == tv.varSlot) {
      b = true;
    } else if (bindings.isGivenTVar(tv.varSlot)) {
      b = this.require1FreeGiven(width, tv, bindings);
    } else {
      b = this.require1FreeFree(width, tv, bindings);
    }
    return b;
  }

  boolean require1FreeGiven(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean require1FreeFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this.varSlot, tv2);
      b = true;
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this.varSlot, tv2);
      b = true;
    } else if (this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this.varSlot, tv2);
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    if (this.varSlot == varSlot) {
      b = true;
    // } else if (b = this.features.includesVar(varSlot, bindings)) {  // HERE: needed?
      // ;
    } else if (bindings.isBound(this.varSlot)) {
      b = bindings.lookup(this.varSlot).includesVar(varSlot, bindings);
    }
    return b;
  }

  // public PTypeVarSlot getVarSlot() { return this.varSlot; }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(givenTVarList);
}
    PTypeSkel t;
    PTypeSkel.JoinResult r;
    if ((r = this.join2(PTypeSkel.WIDER, type, PTypeSkelBindings.create(givenTVarList))) != null) {
      t = r.joined.instanciate(PTypeSkel.InstanciationContext.create(r.bindings));
    } else {
      t = null;
    }
    return t;
  }

  public PTypeSkel.JoinResult join2(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      r = ttv.join3Anonym(width, ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv.varSlot)) {
        r = ttv.join3Given(width, ttt, bindings);
      } else {
        r = ttv.join3Free(width, ttt, bindings);
      }
    } else {
      r = tt.join2(width, ttt, bindings);  // forward
    }
    return r;
  }

  public PTypeSkel.JoinResult join3Anonym(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    throw new RuntimeException("PTypeVarSkel#join3Anonym not implemented.");
  }

  PTypeSkel.JoinResult join3Given(int width, PTypeSkel type, PTypeSkelBindings bindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      r = null;
    } else if (cat == PTypeSkel.CAT_VAR) {
      r = this.join3GivenVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("PTypeVarSkel#join3Given (to anonymous) not implementd.");
    }
    return r;
  }

  PTypeSkel.JoinResult join3GivenVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    if (tv.varSlot == null) {
      throw new RuntimeException("PTypeVarSkel#accept1GivenVar (anonymous tv) not implemented.");
    } else if (this.varSlot == tv.varSlot) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (bindings.isGivenTVar(tv.varSlot)) {
      r = null;
    } else {
      r = this.join3GivenFree(width, tv, bindings);
    }
    return r;
  }

  PTypeSkel.JoinResult join3GivenFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    r = tv.join3FreeGiven(width, this, bindings);  // swap and forward
    return r;
  }

  PTypeSkel.JoinResult join3Free(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    if (type instanceof PTypeVarSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      PTypeVarSkel tv = (PTypeVarSkel)type;
      PTypeSkel t = tv.resolveBindings(bindings);
      if (t instanceof PTypeVarSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free A1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        PTypeVarSkel tv2 = (PTypeVarSkel)t;
        if (this.varSlot == tv2.varSlot)  {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free A11 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
          r = PTypeSkel.JoinResult.create(this, bindings);
        } else if (bindings.isGivenTVar(tv2.varSlot))  {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free A12 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
          r = this.join3FreeGiven(width, tv2, bindings);
        } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free A13 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
          r = this.join3FreeFree(width, tv2, bindings);
        }
      } else if (t instanceof PTypeRefSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free A2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        r = this.join3FreeTypeRef(width, (PTypeRefSkel)t, bindings);
      } else {
        throw new IllegalArgumentException("Unknown type. " + type.toString());
      }
    } else if (type instanceof PTypeRefSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free B "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      r = this.join3FreeTypeRef(width, (PTypeRefSkel)type, bindings);
    } else {
      throw new IllegalArgumentException("Unknown type. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join3FreeGiven(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.JoinResult r;
    PTypeSkelBindings b = bindings.copy();
    b.bind(this.varSlot, tv);
    r = PTypeSkel.JoinResult.create(this, b);
    return r;
  }

  PTypeSkel.JoinResult join3FreeTypeRef(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    if (tr.includesVar(this.varSlot, bindings)) {
      r = null;
    } else if (this.features != null && !this.features.acceptObj(width, tr, bindings)) {
      r = null;
    } else {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tr);
      r = PTypeSkel.JoinResult.create(tr, b);
    }
    return r;
  }

  PTypeSkel.JoinResult join3FreeFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    PFeatureSkel.JoinResult fr;
    PTypeSkelBindings b = bindings.copy();
    if (this.features == null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      bindings.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if (tv.features == null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if ((fr = this.features.joinList(width, tv.features, b)) == null) {
      r = null;
    } else {
      String n = this.name /* + "." + s.id */;
      PTypeVarSlot s = PTypeVarSlot.create();
      PTypeVarSkel v = create(this.theCompiler, fr.srcInfo, n, s, this.requiresConcrete | tv.requiresConcrete, fr.pack());
      PTypeSkelBindings bb = fr.bindings.copy();
      bb.bind(this.varSlot, v);
      bb.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bb);
    }
    return r;
  }

  public MType toMType(PModule mod, Module.Builder modBuilder, boolean inReferredDef, List<PTypeVarSlot> slotList) {
    MTypeVar tv;
    int index;
    boolean conc;
    MFeature.List fs;
    if (this.varSlot == null) {
      index = -1;
      conc = this.requiresConcrete;
      fs = (this.features != null)? this.features.toMType(mod, modBuilder, inReferredDef, slotList): null;
    } else if ((index = slotList.indexOf(this.varSlot)) < 0) {  // new def
      index = slotList.size();
      slotList.add(this.varSlot);
      conc = this.requiresConcrete;
      fs = (this.features != null)? this.features.toMType(mod, modBuilder, inReferredDef, slotList): null;
    } else {  // reference
      conc = false;  // do not care
      fs = null;  // do not coare
    }
    return MTypeVar.create(index, conc, fs);
  }

  public void extractVars(List<PTypeVarSlot> extracted) {
    if (this.varSlot != null && !extracted.contains(this.varSlot)) {
      extracted.add(this.varSlot);
    }
    if (this.features != null) {
      this.features.extractVars(extracted);
    }
  }

  public void collectVarVariances(PTypeVarSlot slot, Module.Variance contextVariance, List<Module.Variance> variances) throws CompileException {
    if (this.varSlot == slot) {
      variances.add(contextVariance);
    }
  }

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    PTypeSkel t;
    return ((t = bindings.lookup(this.varSlot)) != null)? t: this;
  }

  public void collectTconKeys(Set<PDefDict.IdKey> keys) {}

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    StringBuffer buf = new StringBuffer();
    buf.append(name);
    if (this.features != null) {
      buf.append(this.features.repr());
    }
    r.add(buf.toString());
    return r;
  }
}
