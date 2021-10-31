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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PTypeVarSkel implements PTypeSkel {
  Parser.SrcInfo srcInfo;
  PTVarSlot varSlot;

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Parser.SrcInfo srcInfo, PScope scope, PTVarSlot varSlot) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = srcInfo;
    // /* DEBUG */ if (scope == null) { throw new IllegalArgumentException("scope is null. " + srcInfo + " " + varSlot.toString()); }
    var.varSlot = varSlot;
    return var;
  }

  PTypeVarSkel copy() {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = this.srcInfo;
    var.varSlot = this.varSlot;
    return var;
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
    buf.append(",slot=");
    buf.append(this.varSlot);
    buf.append("]");
    return buf.toString();
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  public boolean isLiteralNaked() { return false; }

  public int getVariance() { return this.varSlot.variance; }

  public boolean isConcrete() { return this.varSlot.requiresConcrete; }

  public boolean isConcrete(PTypeSkelBindings bindings) {
    PTypeSkel t = this.resolveBindings(bindings);
    boolean b;
    if (t == this) {
      b = this.isConcrete();
    } else {
      b = t.isConcrete(bindings);
    }
    return b;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    PTypeSkel t;
    if (iBindings.isGivenTVar(this.varSlot)) {
      t = this;
    } else if (iBindings.isBoundAppl(this.varSlot)) {
      t = iBindings.lookupAppl(this.varSlot);
    } else if (iBindings.isBound(this.varSlot)) {
      t = iBindings.lookup(this.varSlot);
    } else {
      PTVarSlot s = PTVarSlot.create(this.varSlot.varDef);
      PTypeVarSkel v = this.copy();
      v.varSlot = s;
      iBindings.bind(this.varSlot, v);
      t = v;
    }
    return t;
  }

  public PTypeSkel resolveBindings(PTypeSkelBindings bindings) {
    return (bindings.isBound(this.varSlot))?
      bindings.lookup(this.varSlot).resolveBindings(bindings):
      this;
  }

  public PTypeSkelBindings applyTo(int width, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyTo "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    PTypeSkel tt = this.resolveBindings(trialBindings);
    if (tt == this) {
      if (trialBindings.isGivenTVar(this.varSlot)) {
        b = this.applyGivenTo(width, type.resolveBindings(trialBindings), trialBindings);
      } else {
        b = this.applyFreeTo(width, type.resolveBindings(trialBindings), trialBindings);
      }
    } else {
      b = tt.applyTo(width, type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings applyGivenTo(int width, PTypeSkel type, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenTo "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (type instanceof PNoRetSkel) {
      b = this.applyGivenToNoRet(width, (PNoRetSkel)type, trialBindings);
    } else if (type instanceof PTypeRefSkel) {
      b = this.applyGivenToTypeRef(width, (PTypeRefSkel)type, trialBindings);
    } else {
      b = this.applyGivenToVar(width, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings applyGivenToNoRet(int width, PNoRetSkel nr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToNoRet "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(nr); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings applyGivenToTypeRef(int width, PTypeRefSkel tr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    return null;
  }

  PTypeSkelBindings applyGivenToVar(int width, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.varSlot == tv.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToVar 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (trialBindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToVar 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (!tv.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToVar 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(tv.varSlot, this);  // a kind of casting
      b = trialBindings;
    } else if (this.varSlot.requiresConcrete) {  // if tv.varSlot.requiresConcrete and ...
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToVar 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(tv.varSlot, this);  // a kind of casting
      b = trialBindings;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyGivenToVar 5 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    }
    return b;
  }

  PTypeSkelBindings applyFreeTo(int width, PTypeSkel type, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeTo "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (type instanceof PNoRetSkel) {
      b = this.applyFreeToNoRet(width, (PNoRetSkel)type, trialBindings);
    } else if (type instanceof PTypeRefSkel) {
      b = this.applyFreeToTypeRef(width, (PTypeRefSkel)type, trialBindings);
    } else {
      b = this.applyFreeToVar(width, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings applyFreeToNoRet(int width, PNoRetSkel nr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToNoRet "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(nr); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings applyFreeToTypeRef(int width, PTypeRefSkel tr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (tr.includesVar(this.varSlot, trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToTypeRef 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (this.varSlot.requiresConcrete & !tr.isConcrete(trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToTypeRef 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToTypeRef 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tr);
      b = trialBindings;
    }
    return b;
  }

  PTypeSkelBindings applyFreeToVar(int width, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.varSlot == tv.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToVar 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (!this.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToVar 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tv);
      b = trialBindings;
    } else if (tv.isConcrete(trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToVar 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tv);
      b = trialBindings;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#applyFreeToVar 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      tv.constrainAs(this, trialBindings);
      b = trialBindings;
    }
    return b;
  }

  public boolean includesVar(PTVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    if (this.varSlot == varSlot) {
      b = true;
    } else if (bindings.isBound(this.varSlot)) {
      b = bindings.lookup(this.varSlot).includesVar(varSlot, bindings);
    }
    return b;
  }

  public PTVarSlot getVarSlot() { return this.varSlot; }

  public PTypeSkel join(PTypeSkel type, List<PTVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    if (type instanceof PNoRetSkel) {
      t = type.join2(this, givenTVarList);  // forward to PNoRetSkel
    } else if (type instanceof PTypeRefSkel) {
      t = type.join2(this, givenTVarList);  // forward to PTypeRefSkel
    } else {
      t = this.join2(type, givenTVarList);
    }
    return t;
  }

  public PTypeSkel join2(PTypeSkel type, List<PTVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    PTypeVarSkel tv = (PTypeVarSkel)type;  // other types do not reach here
    if (tv.varSlot == this.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
      t = this;
    } else if (givenTVarList.contains(this.varSlot)) {
      if (givenTVarList.contains(tv.varSlot)) {
        t = this.join2GivenGiven(tv, givenTVarList);
      } else {
        t = this.join2GivenFree(tv, givenTVarList);
      }
    } else {
      if (givenTVarList.contains(tv.varSlot)) {
        t = tv.join2GivenFree(this, givenTVarList);  // swap
      } else {
        t = this.join2FreeFree(tv, givenTVarList);
      }
    }
    return t;
  }

  PTypeSkel join2GivenGiven(PTypeVarSkel tv, List<PTVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2GivenGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
    return null;
  }

  PTypeSkel join2GivenFree(PTypeVarSkel tv, List<PTVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2GivenFree "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
    PTypeSkel t;
    if (this.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2GivenFree 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
      t = tv;
    } else if (tv.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2GivenFree 2 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
      t = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2GivenFree 3 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
      t = this;
    }
    return t;
  }

  PTypeSkel join2FreeFree(PTypeVarSkel tv, List<PTVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2FreeFree "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
    PTypeSkel t;
    if (this.varSlot.requiresConcrete == tv.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2FreeFree 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
      t = tv;
    } else if (this.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2FreeFree 2 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
      t = tv;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2FreeFree 3 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
      t = this;
    }
    return t;
  }

  public MType toMType(PModule mod, List<PTVarSlot> slotList) {
    MTypeVar tv;
    int index = slotList.indexOf(this.varSlot);
    if (index < 0) {
      index = slotList.size();
      slotList.add(this.varSlot);
    }
    return MTypeVar.create(index, this.varSlot.variance, this.varSlot.requiresConcrete);
  }

  public List<PTVarSlot> extractVars(List<PTVarSlot> alreadyExtracted) {
    List<PTVarSlot> newlyExtracted = null;
    if (!alreadyExtracted.contains(this.varSlot)) {
      newlyExtracted = new ArrayList<PTVarSlot>();
      newlyExtracted.add(this.varSlot);
    }
    return newlyExtracted;
  }

  PTypeRefSkel castTo(PTypeRefSkel tr, PTypeSkelBindings bindings) {
    return tr.castFor(this, bindings);
  }

  PTypeVarSkel constrainAs(PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = this.srcInfo;
    var.varSlot = PTVarSlot.createInternal(tv.varSlot.variance, tv.varSlot.requiresConcrete);
    bindings.bind(this.varSlot, var);
    return var;
  }

  public void collectTconInfo(List<PDefDict.TconInfo> list) {}

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    PTypeSkel t;
    return ((t = bindings.lookup(this.varSlot)) != null)? t: this;
  }

  public String repr() {
    return this.varSlot.repr();
  }
}
