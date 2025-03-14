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
  String name;
  public PTypeVarSlot varSlot;  // public for native impl of sango.lang.module
  PFeatureSkel.List features;  // maybe null

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Compiler theCompiler, Parser.SrcInfo srcInfo, String name, PTypeVarSlot varSlot, PFeatureSkel.List features) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.theCompiler = theCompiler;
    var.srcInfo = srcInfo;
    var.name = ((name != null)? name + ":": "") + varSlot.toString();
    var.varSlot = varSlot;
    var.features = features;
    return var;
  }

  PTypeVarSkel cast(boolean newRequiresConcrete, PFeatureSkel.List newFeatures, PTypeSkelBindings bindings) {
    PTypeVarSlot s = PTypeVarSlot.createInternal(newRequiresConcrete);
    String n = this.name + ":" + s.toString();
    PTypeVarSkel v = create(this.theCompiler, this.srcInfo, n, s, newFeatures);
    bindings.bind(this.varSlot, v);
    return v;
  }

  PTypeSkel project(PFeatureSkel.List newFeatures) {
    PTypeVarSlot s = PTypeVarSlot.createInternal(this.varSlot.requiresConcrete);
    String n = this.name + ":" + s.toString();
    PTypeVarSkel v = create(this.theCompiler, this.srcInfo, n, s, newFeatures);
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
      b = v.varSlot.equals(this.varSlot);
    }
    return b;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("tvarskel[src=");
    buf.append(this.srcInfo);
    buf.append(",name=");
    buf.append(this.name);
    if (this.features != null) {
      buf.append(",features=");
      buf.append(this.features);
    }
    buf.append("]");
    return buf.toString();
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  public int getCat() {
    return PTypeSkel.CAT_VAR;
  }

  public boolean isLiteralNaked() { return false; }

  public boolean isConcrete() {
    return this.varSlot.requiresConcrete;
  }

  public PTypeSkel extractAnyInconcreteVar(PTypeSkel type) {
    PTypeSkel t = null;
    if (!this.varSlot.requiresConcrete) {
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
    return create(this.theCompiler, this.srcInfo, this.name, this.varSlot, fs);
  }

  public PTypeSkel resolveBindings(PTypeSkelBindings bindings) {
    return (bindings.isBound(this.varSlot))?
      bindings.lookup(this.varSlot).resolveBindings(bindings):
      this;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationContext context) {
    PTypeSkel t;
    if (context.isGivenTVar(this.varSlot)) {
      t = this;
    } else if (context.isBound(this.varSlot)) {
      t = context.lookup(this.varSlot);  // created before
    } else {  // create new var for free
      PTypeVarSkel v = new PTypeVarSkel();
      v.srcInfo = this.srcInfo;
      v.varSlot = PTypeVarSlot.createInternal(this.varSlot.requiresConcrete);
      v.name = this.name /* + "." + v.varSlot.id */ ;
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
    if (tt != this) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = tt.accept(width, ttt, bindings);  // forward
    } else if (ttt.getVarSlot() == this.varSlot) {  // me?
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1(width, ttt, bindings);
    }
    return b;
  }

  boolean accept1(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    // type != this
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(this.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1 A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1Given(width, type, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1 B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1Free(width, type, bindings);
    }
    return b;
  }

  boolean accept1Given(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    // type != this
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
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean accept1GivenVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    // tv: GIVEN or FREE
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenGiven(width, tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
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
    if (this.varSlot.requiresConcrete == tv.varSlot.requiresConcrete || !this.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
        bindings.bind(tv2.varSlot, this);
        b = true;
      } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
        bindings.bind(tv2.varSlot, this);
        b = true;
      } else if (this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree A3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
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
    // type != this
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
      throw new RuntimeException("Unexspected type. " + type.toString());
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
    } else if (this.features != null && !this.features.acceptObj(width, tr, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome F "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, tr);
      b = true;
    }
    return b;
  }

  boolean accept1FreeVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv.varSlot)) {
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
    if (this.features != null && !this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.varSlot.requiresConcrete && !tv.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeGiven 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, tv);
      b = true;
    }
    return b;
  }

  boolean accept1FreeFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeVarSkel tv2;
    if (this.varSlot.requiresConcrete == tv.varSlot.requiresConcrete || !this.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      if (this.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
        bindings.bind(this.varSlot, tv2);
        b = true;
      } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
        bindings.bind(this.varSlot, tv2);
        b = true;
      } else if (this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
        bindings.bind(this.varSlot, tv2);
        b = true;
      } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree A4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
        b = false;
      }
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  public boolean require(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    PTypeSkel tt = this.resolveBindings(bindings);
    PTypeSkel ttt = type.resolveBindings(bindings);
    if (tt != this) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = tt.require(width, ttt, bindings);  // forward
    } else if (ttt.getVarSlot() == this.varSlot) {  // me?
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1(width, ttt, bindings);
    }
    return b;
  }

  boolean require1(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    // type != this
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(this.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1Given(width, type, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1Free(width, type, bindings);
    }
    return b;
  }

  boolean require1Given(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    // type != this
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
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean require1GivenVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    // tv: GIVEN or FREE
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv.varSlot) /* || bindings.getGivenBound(tv.varSlot) != null */) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.require1GivenGiven(width, tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
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
      tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
      bindings.bind(tv2.varSlot, this);
      b = true;
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
      bindings.bind(tv2.varSlot, this);
      b = true;
    } else if (this.features.requireList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1GivenFree C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
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
    // type != this
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
      throw new RuntimeException("Unexspected type. " + type.toString());
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
    if (bindings.isGivenTVar(tv.varSlot)) {
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
      tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
      bindings.bind(this.varSlot, tv2);
      b = true;
    } else if (tv.features == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
      bindings.bind(this.varSlot, tv2);
      b = true;
    } else if (this.features.acceptList(width, tv.features, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#require1FreeFree C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv2 = tv.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
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

  public PTypeVarSlot getVarSlot() { return this.varSlot; }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(givenTVarList);
}
    PTypeSkel t;
    PTypeSkel.JoinResult r;
    if ((r = this.join2(PTypeSkel.WIDER, /* true, */ type, PTypeSkelBindings.create(givenTVarList))) != null) {
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
    PTypeSkel t = this.resolveBindings(bindings);
    if (t != this) {
      r = t.join2(width, /* bindsRef, */ type, bindings);
    } else if (type.getCat() == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (bindings.isGivenTVar(this.varSlot)) {
      r = this.join2Given(width, /* bindsRef, */ type, bindings);
    } else {
      r = this.join2Free(width, /* bindsRef, */ type, bindings);
    }
    return r;
  }

  PTypeSkel.JoinResult join2Given(int width, /* boolean bindsRef, */ PTypeSkel type, PTypeSkelBindings bindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2Given "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    if (type instanceof PTypeRefSkel) {
      r = null;
    } else if (type instanceof PTypeVarSkel) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      PTypeSkel t = tv.resolveBindings(bindings);
      if (t != tv) {
        r = this.join2Given(width, /* bindsRef, */ t, bindings);
      } else if (this.varSlot == tv.varSlot)  {
        r = PTypeSkel.JoinResult.create(this, bindings);
      } else if (bindings.isGivenTVar(tv.varSlot))  {
        r = null;
      } else {
        r = this.join2GivenFree(width, /* bindsRef, */ tv, bindings);
      }
    } else {
      throw new IllegalArgumentException("Unknown type. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join2GivenFree(int width, /* boolean bindsRef, */ PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    r = tv.join2FreeGiven(width, /* bindsRef, */ this, bindings);
    return r;
  }

  PTypeSkel.JoinResult join2Free(int width, /* boolean bindsRef, */ PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2Free "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    if (type instanceof PTypeRefSkel) {
      r = this.join2FreeTypeRef(width, /* bindsRef, */ (PTypeRefSkel)type, bindings);
    } else if (type instanceof PTypeVarSkel) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      PTypeSkel t = tv.resolveBindings(bindings);
      if (t != tv) {
        r = this.join2Free(width, /* bindsRef, */ t, bindings);
      } else if (this.varSlot == tv.varSlot)  {
        r = PTypeSkel.JoinResult.create(this, bindings);
      } else if (bindings.isGivenTVar(tv.varSlot))  {
        r = this.join2FreeGiven(width, /* bindsRef, */ tv, bindings);
      } else {
        r = this.join2FreeFree(width, /* bindsRef, */ tv, bindings);
      }
    } else {
      throw new IllegalArgumentException("Unknown type. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join2FreeGiven(int width, /* boolean bindsRef, */ PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.JoinResult r;
    // if (!bindsRef) {
      // r = null;
    // } else {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tv);
      r = PTypeSkel.JoinResult.create(this, b);
    // }
    return r;
  }

  PTypeSkel.JoinResult join2FreeTypeRef(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
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

  PTypeSkel.JoinResult join2FreeFree(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel.JoinResult r;
    PFeatureSkel.JoinResult fr;
    PTypeSkelBindings b = bindings.copy();
    if (this.features == null) {
      PTypeVarSkel v = this.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, tv.features, bindings);
      bindings.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if (tv.features == null) {
      PTypeVarSkel v = this.cast(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete, this.features, bindings);
      bindings.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bindings);
    } else if ((fr = this.features.joinList(width, tv.features, b)) == null) {
      r = null;
    } else {
      PTypeVarSlot s = PTypeVarSlot.createInternal(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete);
      String n = this.name /* + "." + s.id */;
      PTypeVarSkel v = create(this.theCompiler, fr.srcInfo, n, s, fr.pack());
      PTypeSkelBindings bb = fr.bindings.copy();
      bb.bind(this.varSlot, v);
      bb.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bb);
    }
    return r;
  }

  public MType toMType(PModule mod, List<PTypeVarSlot> slotList) {
    MTypeVar tv;
    int index = slotList.indexOf(this.varSlot);
    if (index < 0) {  // definition
      index = slotList.size();
      slotList.add(this.varSlot);
      MFeature.List fs = (this.features != null)? this.features.toMType(mod, slotList): null;
      tv = MTypeVar.create(index, this.varSlot.requiresConcrete, fs);
    } else {  // reference
      tv = MTypeVar.create(index, false, null);  // do not care attributes
    }
    return tv;
  }

  public void extractVars(List<PTypeVarSlot> extracted) {
    if (!extracted.contains(this.varSlot)) {
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

  // public void collectTconProps(List<PDefDict.TconProps> list) {}

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
