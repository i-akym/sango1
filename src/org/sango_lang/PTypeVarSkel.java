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

  PTypeVarSkel cast(boolean newRequiresConcrete, PFeatureSkel.List newFeatures, PTypeSkel.Bindings bindings) {
    if (this.varSlot == null) { throw new RuntimeException("Unexpected " + this); }
    PTypeVarSlot s = PTypeVarSlot.create();
    String n = this.name + ":" + s.toString();
    PTypeVarSkel v = create(this.theCompiler, this.srcInfo, n, s, newRequiresConcrete, newFeatures);
    bindings.bind(this, v);
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
    return (this.varSlot == null) || this.requiresConcrete;
  }

  // public PTypeSkel extractAnyInconcreteVar(PTypeSkel type) {
    // PTypeSkel t = null;
    // if (!this.requiresConcrete) {
      // ; 
    // } else if (!type.isConcrete()) {
      // t = type;
    // } else if (this.features != null) {
      // t = this.features.extractAnyInconcreteVar();
    // }
    // return t;
  // }

  public PTypeVarSkel normalize() throws CompileException {
    PFeatureSkel.List fs = (this.features != null)? this.features.normalize(): null;
    return create(this.theCompiler, this.srcInfo, this.name, this.varSlot, this.requiresConcrete, fs);
  }

  public PTypeSkel resolveBindings(PTypeSkel.Bindings bindings) {
    PTypeSkel t;
    PFeatureSkel.List fs = (this.features != null)? this.features.resolveBindings(bindings): null;
    if (this.varSlot == null) {
      t = create(this.theCompiler, this.srcInfo, this.name, this.varSlot, this.requiresConcrete, fs);
    } else if (bindings.isBound(this)) {
      t = bindings.lookup(this).resolveBindings(bindings);
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
    } else if (context.isGivenTVar(this)) {
      t = this;
    } else if (context.isBound(this)) {
      t = context.lookup(this);  // created before
    } else {  // create new var for free
      PTypeVarSkel v = new PTypeVarSkel();
      v.srcInfo = this.srcInfo;
      v.varSlot = PTypeVarSlot.create();
      v.name = this.name /* + "." + v.varSlot.id */ ;
      v.requiresConcrete = this.requiresConcrete;
      v.features = (this.features != null)? this.features.instanciate(context): null;
      context.bind(this, v);
      t = v;
    }
// /* DEBUG */ System.out.print("INSTANCIATE ! "); System.out.print(this); System.out.print(" => "); System.out.println(t);
    return t;
  }

  public boolean accept(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      b = ttv.accept1Anonym(ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv)) {
        b = ttv.accept1Given(ttt, bindings);
      } else {
        b = ttv.accept1Free(ttt, bindings);
      }
    } else {
      b = tt.accept(ttt, bindings);  // forward
    }
    return b;
  }

  boolean accept1Anonym(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
      PTypeRefSkel tr = (PTypeRefSkel)type;
      b = this.accept1AnonymSome(tr, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (bindings.isGivenTVar(tv)) {
        b = this.accept1AnonymGiven(tv, bindings);
      } else {
        b = this.accept1AnonymFree(tv, bindings);
      }
    } else {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      b = this.accept1AnonymAnonym(tv, bindings);
    }
    return b;
  }

  boolean accept1AnonymSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (this.features.acceptObj(tr, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1AnonymGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.acceptList(tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1AnonymFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.acceptList(tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1AnonymAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.acceptList(tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean accept1Given(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given B "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given C "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      b = this.accept1GivenAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return b;
  }

  boolean accept1GivenVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    // tv: GIVEN or FREE; not ANVAR
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.varSlot == tv.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (bindings.isGivenTVar(tv)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenGiven(tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenFree(tv, bindings);
    }
    return b;
  }

  boolean accept1GivenGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean accept1GivenFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.requiresConcrete == tv.requiresConcrete || !this.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
        bindings.bind(tv2, this);
        b = true;
      } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A2 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
        bindings.bind(tv2, this);
        b = true;
      } else if (this.features.acceptList(tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A3 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
        bindings.bind(tv2, this);
        b = true;
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A4 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        b = false;
      }
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean accept1GivenAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenAnonym "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean accept1Free(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this, type);
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return b;
  }

  boolean accept1FreeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tr.includesVar(this.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this, tr);  // this.varSlot might be bound in accepting features, so bind this.varSlot first
      if (this.features == null || this.features.acceptObj(tr, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2a "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
        b = true;
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2b "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
        b = false;
      }
    }
    return b;
  }

  boolean accept1FreeVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.varSlot == tv.varSlot) {
      b = true;
    } else if (bindings.isGivenTVar(tv)) {
      b = this.accept1FreeGiven(tv, bindings);
    } else {
      b = this.accept1FreeFree(tv, bindings);
    }
    return b;
  }

  boolean accept1FreeGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    bindings.bind(this, tv);
    if (this.features == null || this.features.acceptList(tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean accept1FreeFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    bindings.bind(this, tv);
    if (this.features == null || this.features.acceptList(tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean accept1FreeAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeAnonym "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    bindings.bind(this, tv);
    if (this.features == null || this.features.acceptList(tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeAnonym A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeAnonym B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  public boolean require(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      b = ttv.require1Anonym(ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv)) {
        b = ttv.require1Given(ttt, bindings);
      } else {
        b = ttv.require1Free(ttt, bindings);
      }
    } else {
      b = tt.require(ttt, bindings);  // forward
    }
    return b;
  }

  boolean require1Anonym(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Anonym "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
      PTypeRefSkel tr = (PTypeRefSkel)type;
      b = this.require1AnonymSome(tr, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (bindings.isGivenTVar(tv)) {
        b = this.require1AnonymGiven(tv, bindings);
      } else {
        b = this.require1AnonymFree(tv, bindings);
      }
    } else {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      b = this.require1AnonymAnonym(tv, bindings);
    }
    return b;
  }

  boolean require1AnonymSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (this.features.requireObj(tr, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean require1AnonymGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.requireList(tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean require1AnonymFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.requireList(tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean require1AnonymAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    if (tv.features == null || this.features.requireList(tv.features, bindings)) {
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  boolean require1Given(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given B "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given C "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      b = this.require1GivenAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return b;
  }

  boolean require1GivenVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    // tv: GIVEN or FREE; not ANVAR
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.varSlot == tv.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (bindings.isGivenTVar(tv) /* || bindings.getGivenBound(tv.varSlot) != null */) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenGiven(tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenFree(tv, bindings);
    }
    return b;
  }

  boolean require1GivenGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean require1GivenFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      bindings.bind(tv2, this);
      b = true;
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(tv2, this);
      b = true;
    } else if (this.features.requireList(tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      bindings.bind(tv2, this);
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean require1GivenAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    b = false;
    return b;
  }

  boolean require1Free(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this, type);
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free C "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1FreeSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free D "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1FreeVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      b = this.require1FreeAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return b;
  }

  boolean require1FreeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeSome "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean require1FreeVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.varSlot == tv.varSlot) {
      b = true;
    } else if (bindings.isGivenTVar(tv)) {
      b = this.require1FreeGiven(tv, bindings);
    } else {
      b = this.require1FreeFree(tv, bindings);
    }
    return b;
  }

  boolean require1FreeGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean require1FreeFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this, tv2);
      b = true;
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this, tv2);
      b = true;
    } else if (this.features.acceptList(tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this, tv2);
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean require1FreeAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeVarSkel tv2;
    boolean b;
    if (this.features == null) {
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this, tv2);
      b = true;
    } else if (tv.features == null) {
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this, tv2);
      b = true;
    } else if (this.features.acceptList(tv.features, bindings)) {
      tv2 = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(this, tv2);
      b = true;
    } else {
      b = false;
    }
    return b;
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkel.Bindings bindings) {
    boolean b = false;
    if (this.varSlot == null || varSlot == null) {
      b = false;
    } else if (this.varSlot == varSlot) {
      b = true;
    // } else if (b = this.features.includesVar(varSlot, bindings)) {  // HERE: needed?
      // ;
    } else if (bindings.isBound(this)) {
      b = bindings.lookup(this).includesVar(varSlot, bindings);
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
    if ((r = this.join2(type, PTypeSkel.Bindings.create(givenTVarList))) != null) {
      t = r.joined.instanciate(PTypeSkel.InstanciationContext.create(r.bindings));
    } else {
      t = null;
    }
    return t;
  }

  public PTypeSkel.JoinResult join2(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      r = ttv.join3Anonym(ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv)) {
        r = ttv.join3Given(ttt, bindings);
      } else {
        r = ttv.join3Free(ttt, bindings);
      }
    } else {
      r = tt.join2(ttt, bindings);  // forward
    }
    return r;
  }

  public PTypeSkel.JoinResult join3Anonym(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Anonym "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      r = this.join3AnonymSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      if (bindings.isGivenTVar(tv2))  {
        r = tv2.join3GivenAnonym(this, bindings);  // exchange
      } else {
        r = tv2.join3FreeAnonym(this, bindings);  // exchange
      }
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      r = this.join3AnonymAnonym(tv2, bindings);
    } else {
      throw new IllegalArgumentException("Unknown category. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join3AnonymSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    if (this.features != null && !this.features.acceptObj(tr, bindings)) {
      r = null;
    } else {
      PTypeSkel.Bindings b = bindings.copy();
      r = PTypeSkel.JoinResult.create(tr, b);
    }
    return r;
  }

  PTypeSkel.JoinResult join3AnonymAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    PFeatureSkel.JoinResult fr;
    if (this.features == null || tv.features == null) {
      PTypeVarSkel v = create(this.theCompiler, this.srcInfo, this.name, null, this.requiresConcrete | tv.requiresConcrete, null);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if ((fr = this.features.joinList(tv.features, bindings)) != null) {
      PTypeVarSkel v = create(this.theCompiler, this.srcInfo, this.name, null, this.requiresConcrete | tv.requiresConcrete, fr.pack());
      r = PTypeSkel.JoinResult.create(v, fr.bindings);
    } else {
      r = null;
    }
    return r;
  }

  PTypeSkel.JoinResult join3Given(PTypeSkel type, PTypeSkel.Bindings bindings) {
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
      r = this.join3GivenVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      r = this.join3GivenAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return r;
  }

  PTypeSkel.JoinResult join3GivenVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    PTypeSkel.JoinResult r;
    if (this.varSlot == tv.varSlot) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (bindings.isGivenTVar(tv)) {
      r = null;
    } else {
      r = this.join3GivenFree(tv, bindings);
    }
    return r;
  }

  PTypeSkel.JoinResult join3GivenFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    PTypeSkel.JoinResult r;
    r = tv.join3FreeGiven(this, bindings);  // swap and forward
    return r;
  }

  PTypeSkel.JoinResult join3GivenAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    PTypeSkel.JoinResult r;
    r = null;  // temporally
    return r;
  }

  PTypeSkel.JoinResult join3Free(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      r = this.join3FreeSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      if (this.varSlot == tv2.varSlot)  {
        r = PTypeSkel.JoinResult.create(this, bindings);
      } else if (bindings.isGivenTVar(tv2))  {
        r = this.join3FreeGiven(tv2, bindings);
      } else {
        r = this.join3FreeFree(tv2, bindings);
      }
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      r = this.join3FreeAnonym(tv2, bindings);
    } else {
      throw new IllegalArgumentException("Unknown category. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join3FreeGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.JoinResult r;
    PTypeSkel.Bindings b = bindings.copy();
    b.bind(this, tv);
    r = PTypeSkel.JoinResult.create(this, b);
    return r;
  }

  PTypeSkel.JoinResult join3FreeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    if (tr.includesVar(this.varSlot, bindings)) {
      r = null;
    } else if (this.features != null && !this.features.acceptObj(tr, bindings)) {
      r = null;
    } else {
      PTypeSkel.Bindings b = bindings.copy();
      b.bind(this, tr);
      r = PTypeSkel.JoinResult.create(tr, b);
    }
    return r;
  }

  PTypeSkel.JoinResult join3FreeFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    PFeatureSkel.JoinResult fr;
    if (this.features == null && tv.features == null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, null, bindings);
      bindings.bind(tv, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if (tv.features == null && tv.features != null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      bindings.bind(tv, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if (tv.features != null && tv.features == null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      bindings.bind(tv, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, null /* dummy */, bindings);
      bindings.bind(tv, v);
      if ((fr = this.features.joinList(tv.features, bindings)) != null) {
        v.features = fr.pack();
        r = PTypeSkel.JoinResult.create(v, fr.bindings);
      } else {
        r = null;
      }
    }
    return r;
  }

  PTypeSkel.JoinResult join3FreeAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    PFeatureSkel.JoinResult fr;
    if (this.features == null && tv.features == null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, null, bindings);
      //## bindings.bind(tv, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if (tv.features == null && tv.features != null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bindings);
      //## bindings.bind(tv, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if (tv.features != null && tv.features == null) {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bindings);
      //## bindings.bind(tv, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else {
      PTypeVarSkel v = this.cast(this.requiresConcrete | tv.requiresConcrete, null /* dummy */, bindings);
      //## bindings.bind(tv, v);
      if ((fr = this.features.joinList(tv.features, bindings)) != null) {
        v.features = fr.pack();
        r = PTypeSkel.JoinResult.create(v, fr.bindings);
      } else {
        r = null;
      }
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

  public PTypeSkel unalias(PTypeSkel.Bindings bindings) {
    PTypeSkel t;
    return ((t = bindings.lookup(this)) != null)? t: this;
  }

  public void excludeBareTVarAtRet(Parser.SrcInfo si, boolean atRet, List<PTypeVarSlot> checked) throws CompileException {
    if (this.varSlot == null) {
      ;
    } else if (checked.contains(this.varSlot)) {
      ;
    } else if (!atRet) {
      checked.add(this.varSlot);
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Bare type var not allowed");
      if (this.name != null) {
        emsg.append(" to ");
        emsg.append(this.name);
      }
      if (si != null) {
        emsg.append(" at ");
        emsg.append(si);
      }
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
  }

  public void collectTconKeys(Set<PDefDict.IdKey> keys) {}

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    StringBuffer buf = new StringBuffer();
    buf.append(name);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    if (this.features != null) {
      buf.append(this.features.repr());
    }
    r.add(buf.toString());
    return r;
  }
}
