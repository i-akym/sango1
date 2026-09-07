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

  PTypeSkel.CastResult cast(boolean newRequiresConcrete, PFeatureSkel.List newFeatures, PTypeSkel.Bindings bindings) {
  // PTypeVarSkel cast(boolean newRequiresConcrete, PFeatureSkel.List newFeatures, PTypeSkel.Bindings bindings) {
    if (this.varSlot == null) { throw new RuntimeException("Unexpected " + this); }
    PTypeSkel.Bindings bx = bindings;
    PTypeVarSlot s = PTypeVarSlot.create();
    String n = this.name + ":" + s.toString();
    PTypeVarSkel v = create(this.theCompiler, this.srcInfo, n, s, newRequiresConcrete, newFeatures);
    bx = bx.bind(this, v);
    return PTypeSkel.CastResult.create(v, bx);
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
    return ((this.varSlot == null) || this.requiresConcrete)
      && (this.features == null || this.features.isConcrete());
  }

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

  public PTypeSkel.Bindings accept(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel tt = this.resolveBindings(bx);
    PTypeSkel ttt = type.resolveBindings(bx);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      bx = ttv.accept1Anonym(ttt, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bx.isGivenTVar(ttv)) {
        bx = ttv.accept1Given(ttt, bx);
      } else {
        bx = ttv.accept1Free(ttt, bx);
      }
    } else {
      bx = tt.accept(ttt, bx);  // forward
    }
    return bx;
  }

  PTypeSkel.Bindings accept1Anonym(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      ;  // ok
    } else if (cat == PTypeSkel.CAT_SOME) {
      PTypeRefSkel tr = (PTypeRefSkel)type;
      bx = this.accept1AnonymSome(tr, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (bx.isGivenTVar(tv)) {
        bx = this.accept1AnonymGiven(tv, bx);
      } else {
        bx = this.accept1AnonymFree(tv, bx);
      }
    } else {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      bx = this.accept1AnonymAnonym(tv, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings accept1AnonymSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    bx = this.features.acceptObj(tr, bx);
    return bx;
  }

  PTypeSkel.Bindings accept1AnonymGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    if (tv.features == null) {
      ;  // ok
    } else {
      bx = this.features.acceptList(tv.features, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings accept1AnonymFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    if (tv.features == null) {
      ;  // ok
    } else {
      bx = this.features.acceptList(tv.features, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings accept1AnonymAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    if (tv.features == null) {
      ;  // ok
    } else {
      bx = this.features.acceptList(tv.features, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings accept1Given(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given B "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given C "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.accept1GivenVar((PTypeVarSkel)type, bx);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      bx = this.accept1GivenAnonym((PTypeVarSkel)type, bx);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return bx;
  }

  PTypeSkel.Bindings accept1GivenVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    // tv: GIVEN or FREE; not ANVAR
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    if (this.varSlot == tv.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      ;  // ok
    } else if (bx.isGivenTVar(tv)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = this.accept1GivenGiven(tv, bx);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = this.accept1GivenFree(tv, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings accept1GivenGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = null;
    return bx;
  }

  PTypeSkel.Bindings accept1GivenFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.CastResult cr;
    if (this.requiresConcrete == tv.requiresConcrete || !this.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
        cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
        bx = cr.bindings;
        bx = bx.bind((PTypeVarSkel)cr.casted, this);
      } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A2 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
        cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
        bx = cr.bindings;
        bx = bx.bind((PTypeVarSkel)cr.casted, this);
      } else if ((bx = this.features.acceptList(tv.features, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A3 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
        cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
        bx = cr.bindings;
        bx = bx.bind((PTypeVarSkel)cr.casted, this);
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A4 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
        bx = null;
      }
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    }
    return bx;
  }

  PTypeSkel.Bindings accept1GivenAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenAnonym "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = null;
    return bx;
  }

  PTypeSkel.Bindings accept1Free(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = bx.bind(this, type);
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.accept1FreeSome((PTypeRefSkel)type, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.accept1FreeVar((PTypeVarSkel)type, bx);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free 4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.accept1FreeAnonym((PTypeVarSkel)type, bx);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return bx;
  }

  PTypeSkel.Bindings accept1FreeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    if (tr.includesVar(this.varSlot, bx)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bx);
}
      bx = bx.bind(this, tr);  // this.varSlot might be bound in accepting features, so bind this.varSlot first
      if (this.features == null || (bx = this.features.acceptObj(tr, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2a "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bx);
}
        ;  // ok
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2b "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bx);
}
        bx = null;
      }
    }
    return bx;
  }

  PTypeSkel.Bindings accept1FreeVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    if (this.varSlot == tv.varSlot) {
      ;
    } else if (bx.isGivenTVar(tv)) {
      bx = this.accept1FreeGiven(tv, bx);
    } else {
      bx = this.accept1FreeFree(tv, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings accept1FreeGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = bx.bind(this, tv);
    if (this.features == null || (bx = this.features.acceptList(tv.features, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      ;  // ok
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    }
    return bx;
  }

  PTypeSkel.Bindings accept1FreeFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = bx.bind(this, tv);
    if (this.features == null || (bx = this.features.acceptList(tv.features, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      ;  // ok
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    }
    return bx;
  }

  PTypeSkel.Bindings accept1FreeAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeAnonym "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = bx.bind(this, tv);
    if (this.features == null || (bx = this.features.acceptList(tv.features, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeAnonym A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      ;  // ok
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeAnonym B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    }
    return bx;
  }

  public PTypeSkel.Bindings require(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel tt = this.resolveBindings(bx);
    PTypeSkel ttt = type.resolveBindings(bx);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      bx = ttv.require1Anonym(ttt, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bx.isGivenTVar(ttv)) {
        bx = ttv.require1Given(ttt, bx);
      } else {
        bx = ttv.require1Free(ttt, bx);
      }
    } else {
      bx = tt.require(ttt, bx);  // forward
    }
    return bx;
  }

  PTypeSkel.Bindings require1Anonym(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Anonym "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      ;  // ok
    } else if (cat == PTypeSkel.CAT_SOME) {
      PTypeRefSkel tr = (PTypeRefSkel)type;
      bx = this.require1AnonymSome(tr, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (bx.isGivenTVar(tv)) {
        bx = this.require1AnonymGiven(tv, bx);
      } else {
        bx = this.require1AnonymFree(tv, bx);
      }
    } else {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      bx = this.require1AnonymAnonym(tv, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings require1AnonymSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    bx = this.features.requireObj(tr, bx);
    return bx;
  }

  PTypeSkel.Bindings require1AnonymGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    if (tv.features == null) {
      ;  // ok
    } else {
      bx = this.features.requireList(tv.features, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings require1AnonymFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    if (tv.features == null) {
      ;  // ok
    } else {
      bx = this.features.requireList(tv.features, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings require1AnonymAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    if (tv.features == null) {
      ;  // ok
    } else {
      bx = this.features.requireList(tv.features, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings require1Given(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given B "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Given C "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.require1GivenVar((PTypeVarSkel)type, bx);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      bx = this.require1GivenAnonym((PTypeVarSkel)type, bx);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return bx;
  }

  PTypeSkel.Bindings require1GivenVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    // tv: GIVEN or FREE; not ANVAR
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    if (this.varSlot == tv.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      ;  // ok
    } else if (bx.isGivenTVar(tv)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = this.require1GivenGiven(tv, bx);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = this.require1GivenFree(tv, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings require1GivenGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = null;
    return bx;
  }

  PTypeSkel.Bindings require1GivenFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.CastResult cr;
    if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
      bx = cr.bindings;
      bx = bx.bind((PTypeVarSkel)cr.casted, this);
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      bx = bx.bind((PTypeVarSkel)cr.casted, this);
    } else if ((bx = this.features.requireList(tv.features, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
      bx = cr.bindings;
      bx = bx.bind((PTypeVarSkel)cr.casted, this);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    }
    return bx;
  }

  PTypeSkel.Bindings require1GivenAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    bx = null;
    return bx;
  }

  PTypeSkel.Bindings require1Free(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = bx.bind(this, type);
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free C "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.require1FreeSome((PTypeRefSkel)type, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1Free D "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bx);
}
      bx = this.require1FreeVar((PTypeVarSkel)type, bx);
    } else if (cat == PTypeSkel.CAT_VAR) {
      bx = this.require1FreeAnonym((PTypeVarSkel)type, bx);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return bx;
  }

  PTypeSkel.Bindings require1FreeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeSome "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = null;
    return bx;
  }

  PTypeSkel.Bindings require1FreeVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    if (this.varSlot == tv.varSlot) {
      ;  // ok
    } else if (bx.isGivenTVar(tv)) {
      bx = this.require1FreeGiven(tv, bx);
    } else {
      bx = this.require1FreeFree(tv, bx);
    }
    return bx;
  }

  PTypeSkel.Bindings require1FreeGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    bx = null;
    return bx;
  }

  PTypeSkel.Bindings require1FreeFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.CastResult cr;
    if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree A "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
      bx = cr.bindings;
      bx = bx.bind(this, cr.casted);
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree B "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      bx = bx.bind(this, cr.casted);
    } else if ((bx = this.features.acceptList(tv.features, bx)) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree C "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      bx = bx.bind(this, cr.casted);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree D "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bx);
}
      bx = null;
    }
    return bx;
  }

  PTypeSkel.Bindings require1FreeAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    PTypeVarSkel.CastResult cr;
    if (this.features == null) {
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
      bx = cr.bindings;
      bx = bx.bind(this, cr.casted);
    } else if (tv.features == null) {
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      bx = bx.bind(this, cr.casted);
    } else if ((bx = this.features.acceptList(tv.features, bx)) != null) {
      cr = tv.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      bx = bx.bind(this, cr.casted);
    } else {
      bx = null;
    }
    return bx;
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkel.Bindings bindings) {
    boolean b;
    if (varSlot == null) {
      b = false;
    } else if (this.varSlot != null && bindings.isBound(this)) {
      b = bindings.lookup(this).includesVar(varSlot, bindings);
    } else if (this.varSlot == varSlot) {
      b = true;
    } else if (this.features != null) {
      b = this.features.includesVar(varSlot, bindings) ;
    } else {
      b = false;
    }
    return b;
  }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(givenTVarList);
}
    PTypeSkel t;
    PTypeSkel.JoinResult jr;
    if ((jr = this.join2(type, PTypeSkel.Bindings.create(givenTVarList))) != null) {
      t = jr.joined.instanciate(PTypeSkel.InstanciationContext.create(jr.bindings));
    } else {
      t = null;
    }
    return t;
  }

  public PTypeSkel.JoinResult join2(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult jr;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    int cat = tt.getCat();
    if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      jr = ttv.join3Anonym(ttt, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel ttv = (PTypeVarSkel)tt;
      if (bindings.isGivenTVar(ttv)) {
        jr = ttv.join3Given(ttt, bindings);
      } else {
        jr = ttv.join3Free(ttt, bindings);
      }
    } else {
      jr = tt.join2(ttt, bindings);  // forward
    }
    return jr;
  }

  public PTypeSkel.JoinResult join3Anonym(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Anonym "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult jr;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      jr = PTypeSkel.JoinResult.create(this, bindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      jr = this.join3AnonymSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      if (bindings.isGivenTVar(tv2))  {
        jr = tv2.join3GivenAnonym(this, bindings);  // exchange
      } else {
        jr = tv2.join3FreeAnonym(this, bindings);  // exchange
      }
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      jr = this.join3AnonymAnonym(tv2, bindings);
    } else {
      throw new IllegalArgumentException("Unknown category. " + type.toString());
    }
    return jr;
  }

  PTypeSkel.JoinResult join3AnonymSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.JoinResult jr;
    if ((bx = this.features.acceptObj(tr, bx)) != null) {
      jr = PTypeSkel.JoinResult.create(tr, bx);
    } else {
      jr = null;
    }
    return jr;
  }

  PTypeSkel.JoinResult join3AnonymAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.JoinResult jr;
    PFeatureSkel.JoinResult fjr;
    if (tv.features == null) {
      PTypeVarSkel v = create(this.theCompiler, this.srcInfo, this.name, null, this.requiresConcrete | tv.requiresConcrete, null);
      jr = PTypeSkel.JoinResult.create(v, bx);
    } else if ((fjr = this.features.joinList(tv.features, bx)) != null) {
      bx = fjr.bindings;
      PTypeVarSkel v = create(this.theCompiler, this.srcInfo, this.name, null, this.requiresConcrete | tv.requiresConcrete, fjr.pack());
      jr = PTypeSkel.JoinResult.create(v, bx);
    } else {
      jr = null;
    }
    return jr;
  }

  PTypeSkel.JoinResult join3Given(PTypeSkel type, PTypeSkel.Bindings bindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult jr;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      jr = PTypeSkel.JoinResult.create(this, bindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      jr = null;
    } else if (cat == PTypeSkel.CAT_VAR) {
      jr = this.join3GivenVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      jr = this.join3GivenAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexpected category");
    }
    return jr;
  }

  PTypeSkel.JoinResult join3GivenVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    PTypeSkel.JoinResult jr;
    if (this.varSlot == tv.varSlot) {
      jr = PTypeSkel.JoinResult.create(this, bindings);
    } else if (bindings.isGivenTVar(tv)) {
      jr = null;
    } else {
      jr = this.join3GivenFree(tv, bindings);
    }
    return jr;
  }

  PTypeSkel.JoinResult join3GivenFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    PTypeSkel.JoinResult jr;
    jr = tv.join3FreeGiven(this, bindings);  // swap and forward
    return jr;
  }

  PTypeSkel.JoinResult join3GivenAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    PTypeSkel.JoinResult jr;
    jr = null;  // temporally
    return jr;
  }

  PTypeSkel.JoinResult join3Free(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join3Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult jr;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      jr = PTypeSkel.JoinResult.create(this, bindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      jr = this.join3FreeSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      if (this.varSlot == tv2.varSlot)  {
        jr = PTypeSkel.JoinResult.create(this, bindings);
      } else if (bindings.isGivenTVar(tv2))  {
        jr = this.join3FreeGiven(tv2, bindings);
      } else {
        jr = this.join3FreeFree(tv2, bindings);
      }
    } else if (cat == PTypeSkel.CAT_ANVAR) {
      PTypeVarSkel tv2 = (PTypeVarSkel)type;
      jr = this.join3FreeAnonym(tv2, bindings);
    } else {
      throw new IllegalArgumentException("Unknown category. " + type.toString());
    }
    return jr;
  }

  PTypeSkel.JoinResult join3FreeGiven(PTypeVarSkel tv, PTypeSkel.Bindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.JoinResult jr;
    bx = bx.bind(this, tv);
    jr = PTypeSkel.JoinResult.create(this, bx);
    return jr;
  }

  PTypeSkel.JoinResult join3FreeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.JoinResult jr;
    if (tr.includesVar(this.varSlot, bx)) {
      jr = null;
    } else if (this.features != null && (bx = this.features.acceptObj(tr, bx)) == null) {
      jr = null;
    } else {
      // PTypeSkel.Bindings b = bindings.copy();
      bx = bx.bind(this, tr);
      jr = PTypeSkel.JoinResult.create(tr, bx);
    }
    return jr;
  }

  PTypeSkel.JoinResult join3FreeFree(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.CastResult cr;
    PTypeSkel.JoinResult jr;
    PFeatureSkel.JoinResult fjr;
    if (this.features == null && tv.features == null) {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, null, bx);
      bx = cr.bindings;
      bx = bx.bind(tv, cr.casted);
      jr = PTypeSkel.JoinResult.create(cr.casted, bx);
    } else if (this.features == null && tv.features != null) {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
      bx = cr.bindings;
      bx = bx.bind(tv, cr.casted);
      jr = PTypeSkel.JoinResult.create(cr.casted, bx);
    } else if (this.features != null && tv.features == null) {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      bx = bx.bind(tv, cr.casted);
      jr = PTypeSkel.JoinResult.create(cr.casted, bx);
    } else {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, null /* dummy */, bx);
      bx = cr.bindings;
      bx = bx.bind(tv, cr.casted);
      if ((fjr = this.features.joinList(tv.features, bindings)) != null) {
        bx = fjr.bindings;
        ((PTypeVarSkel)cr.casted).features = fjr.pack();
        jr = PTypeSkel.JoinResult.create(cr.casted, bx);
      } else {
        jr = null;
      }
    }
    return jr;
  }

  PTypeSkel.JoinResult join3FreeAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel.Bindings bx = bindings;
    PTypeSkel.CastResult cr;
    PTypeSkel.JoinResult jr;
    PFeatureSkel.JoinResult fjr;
    if (this.features == null && tv.features == null) {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, null, bx);
      bx = cr.bindings;
      jr = PTypeSkel.JoinResult.create(cr.casted, bx);
    } else if (this.features == null && tv.features != null) {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, tv.features, bx);
      bx = cr.bindings;
      jr = PTypeSkel.JoinResult.create(cr.casted, bx);
    } else if (this.features != null && tv.features == null) {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, this.features, bx);
      bx = cr.bindings;
      jr = PTypeSkel.JoinResult.create(cr.casted, bx);
    } else {
      cr = this.cast(this.requiresConcrete | tv.requiresConcrete, null /* dummy */, bx);
      bx = cr.bindings;
      if ((fjr = this.features.joinList(tv.features, bindings)) != null) {
        ((PTypeVarSkel)cr.casted).features = fjr.pack();
        jr = PTypeSkel.JoinResult.create(cr.casted, fjr.bindings);
      } else {
        jr = null;
      }
    }
    return jr;
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

  public PTypeVarSkel checkNewVar(Parser.SrcInfo si, boolean atRet, List<PTypeVarSlot> checked) throws CompileException {
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
    if (this.features != null) {
      this.features.checkNewVar(si, atRet, checked);
    }
    return this;
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
