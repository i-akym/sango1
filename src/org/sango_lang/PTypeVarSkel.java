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

public class PTypeVarSkel implements PTypeSkel {
  Parser.SrcInfo srcInfo;
  String name;
  PTVarSlot varSlot;
  PTypeRefSkel constraint;  // maybe null

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Parser.SrcInfo srcInfo, String name, PTVarSlot varSlot, PTypeRefSkel constraint) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = srcInfo;
    var.name =  name + "." + Integer.toString(varSlot.id);
    var.varSlot = varSlot;
    var.constraint = constraint;
    return var;
  }

  PTypeVarSkel copy() {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = this.srcInfo;
    var.name = this.name;
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
    buf.append(",constraint=");
    buf.append(this.constraint);
    buf.append(",name=");
    buf.append(this.name);
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
    } else {  // HERE: for what?
      PTVarSlot s = PTVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete);
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

  public void checkVariance(int width) throws CompileException {
    boolean b;
    switch (width) {
    case PTypeSkel.EQUAL:
      switch (this.varSlot.variance) {
      case Module.INVARIANT:
        b = true;
        break;
      default:
        b = false;
      }
      break;
    case PTypeSkel.NARROWER:
      switch (this.varSlot.variance) {
      case Module.INVARIANT:
      case Module.CONTRAVARIANT:
        b = true;
        break;
      default:
        b = false;
      }
      break;
    default:
      switch (this.varSlot.variance) {
      case Module.INVARIANT:
      case Module.COVARIANT:
        b = true;
        break;
      default:
        b = false;
      }
      break;
    }
    if (!b) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Incoherent variance ");
      emsg.append("for *");
      emsg.append(this.name);
      if (this.srcInfo != null) {  // should be true!
        emsg.append(" at ");
        emsg.append(this.srcInfo.toString());
      }
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
  }

  public PTypeSkelBindings accept(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#accept "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    PTypeSkel tt = this.resolveBindings(trialBindings);
    if (tt == this) {
      if (trialBindings.isGivenTVar(this.varSlot)) {
        b = this.acceptGiven(width, bindsRef, type.resolveBindings(trialBindings), trialBindings);
      } else {
        b = this.acceptFree(width, bindsRef, type.resolveBindings(trialBindings), trialBindings);
      }
    } else {
      b = tt.accept(width, bindsRef, type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptGiven(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (type instanceof PNoRetSkel) {
      b = this.acceptGivenNoRet(width, bindsRef, (PNoRetSkel)type, trialBindings);
    } else if (type instanceof PTypeRefSkel) {
      b = this.acceptGivenTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
      PTypeVarSkel v = (PTypeVarSkel)type;
      if (v.bindConstraint(trialBindings)) {
        b = this.acceptGiven(width, bindsRef, v, trialBindings);  // retry
      } else {
        b = this.acceptGivenVar(width, bindsRef, v, trialBindings);
      }
    }
    return b;
  }

  PTypeSkelBindings acceptGivenNoRet(int width, boolean bindsRef, PNoRetSkel nr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenNoRet "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(nr); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings acceptGivenTypeRef(int width, boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    return null;
  }

  PTypeSkelBindings acceptGivenVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.varSlot == tv.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenVar 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (trialBindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenVar 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (!tv.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenVar 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(tv.varSlot, this);  // a kind of casting
      b = trialBindings;
    } else if (this.varSlot.requiresConcrete) {  // if tv.varSlot.requiresConcrete and ...
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenVar 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(tv.varSlot, this);  // a kind of casting
      b = trialBindings;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptGivenVar 5 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    }
    return b;
  }

  PTypeSkelBindings acceptFree(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;

    if (this.constraint != null) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFree 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.constraint.accept(width, bindsRef, type, trialBindings);
      if (b != null) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFree 1-1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        trialBindings = b;
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFree 1-2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        return null;
      }
    }
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFree 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    if (type instanceof PNoRetSkel) {
      b = this.acceptFreeNoRet(width, bindsRef, (PNoRetSkel)type, trialBindings);
    } else if (type instanceof PTypeRefSkel) {
      b = this.acceptFreeTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
      PTypeVarSkel v = (PTypeVarSkel)type;
      b = this.acceptFreeVar(width, bindsRef, v, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptFreeNoRet(int width, boolean bindsRef, PNoRetSkel nr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeNoRet "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(nr); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings acceptFreeTypeRef(int width, boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (!bindsRef) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeTypeRef 0 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (tr.includesVar(this.varSlot, trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeTypeRef 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (this.varSlot.requiresConcrete & !tr.isConcrete(trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeTypeRef 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeTypeRef 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tr);
      b = trialBindings;
    }
    return b;
  }

  PTypeSkelBindings acceptFreeVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.varSlot == tv.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeVar 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (!this.varSlot.requiresConcrete) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeVar 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tv);
      b = trialBindings;
    } else if (tv.isConcrete(trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeVar 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tv);
      b = trialBindings;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#acceptFreeVar 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
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

  boolean bindConstraint(PTypeSkelBindings bindings) {
    boolean bound;
    if (this.constraint != null && !bindings.isBound(this.varSlot)) {
      bindings.bind(this.varSlot, this.constraint);
      bound = true;
    } else {
      bound = false;
    }
    return bound;
  }

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
// /* DEBUG */ System.out.print("{VV "); System.out.print(this); System.out.print(slotList);
    MTypeVar tv;
    int index = slotList.indexOf(this.varSlot);
// /* DEBUG */ System.out.print(index);
    if (index < 0) {
      index = slotList.size();
      slotList.add(this.varSlot);
// /* DEBUG */ System.out.print(" added ");
    }
    MTypeRef c = (this.constraint != null)? (MTypeRef)this.constraint.toMType(mod, slotList): null;
// /* DEBUG */ System.out.print(" constraint "); System.out.print(c);
    MType mv = MTypeVar.create(index, this.varSlot.variance, this.varSlot.requiresConcrete, c);
// /* DEBUG */ System.out.print(" -> "); System.out.print(mv); System.out.println(" vv}");
    return mv;
    // return MTypeVar.create(index, this.varSlot.variance, this.varSlot.requiresConcrete,
      // (this.constraint != null)? (MTypeRef)this.constraint.toMType(mod, slotList): null);
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

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    if (this.constraint!= null) {
      r.append(this.constraint.repr());
      r.add("=");
    }
    r.add(this.varSlot.repr());
    return r;
  }
}
