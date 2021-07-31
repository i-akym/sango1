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
  boolean polymorphic;
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

  boolean isPolymorphic() { return this.polymorphic; }

  void setPolymorphic(boolean b) {
    this.polymorphic = b;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    PTypeSkel t;
    if (iBindings.isGivenTVar(this.varSlot)) {
// /* DEBUG */ System.out.println("instanciate 1 " + this.toString());
      t = this;
    } else if (iBindings.isBoundAppl(this.varSlot)) {
// /* DEBUG */ System.out.println("instanciate 2 " + this.toString());
      t = iBindings.lookupAppl(this.varSlot);
    } else if (iBindings.isBound(this.varSlot)) {
// /* DEBUG */ System.out.println("instanciate 3 " + this.toString());
      t = iBindings.lookup(this.varSlot);
    // } else if (scope!= null) {
// /* DEBUG */ System.out.println("instanciate 4 " + this.toString());
      // PTypeVarSkel v = scope.getNewTvar(this.srcInfo).getSkel();
      // v.polymorphic = true;
      // iBindings.bind(this.varSlot, v);
      // t = v;
    } else {
// /* DEBUG */ System.out.println("instanciate 5 " + this.toString());
      PTVarSlot s = PTVarSlot.create(this.varSlot.varDef);
      PTypeVarSkel v = this.copy();
      v.polymorphic = true;
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

  public PTypeSkelBindings applyTo(PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isBound(this.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.resolveBindings(trialBindings).applyTo(type, trialBindings);
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      PTypeSkel t = type.resolveBindings(trialBindings);
      if (t instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply 3-1 "); System.out.print(this); System.out.print(" "); System.out.print(t); System.out.print(" "); System.out.println(trialBindings);
}
        trialBindings.bind(this.varSlot, type);
        b = trialBindings;
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply 3-2 "); System.out.print(this); System.out.print(" "); System.out.print(t); System.out.print(" "); System.out.println(trialBindings);
}
        b = this.applyTo2(t, trialBindings);
      }
    }
    return b;
  }

  public PTypeSkelBindings applyTo2(PTypeSkel type, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isGivenTVar(this.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      if (type instanceof PTypeVarSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        PTypeVarSkel tv = (PTypeVarSkel)type;
        if (this.varSlot == tv.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 1-1-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
          b = trialBindings;
        } else if (!trialBindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 1-1-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
          trialBindings.bind(tv.varSlot, this);
          b = trialBindings;
        } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 1-1-3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
          b = null;
        }
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;
      }
    } else {  // 'this' is guaranteed to be unbound by applyTo
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      PTVarSlot v = type.getVarSlot();
      if (v == null) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 2-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        if (type.includesVar(this.varSlot, trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 2-1-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
          b = null;
        } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 2-1-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
          trialBindings.bind(this.varSlot, type);
          b = trialBindings;
        }
      } else if (v == this.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 2-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#apply2 2-3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        trialBindings.bind(this.varSlot, type);
        b = trialBindings;
      }
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

  public PTypeSkel join(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel tt;
    if (bindings.isBound(this.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      tt = this.resolveBindings(bindings).join(type, bindings);
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      PTypeSkel t = type.resolveBindings(bindings);
      if (t instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join 3-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        tt = this;
      } else {
        tt = this.join2(t, bindings);
      }
    }
    return tt;
  }

  public PTypeSkel join2(PTypeSkel type, PTypeSkelBindings bindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel t;
    if (bindings.isGivenTVar(this.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      if (type instanceof PTypeVarSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        PTypeVarSkel tv = (PTypeVarSkel)type;
        if (tv.varSlot == this.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1-1-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
          t = this;
        } else if (bindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1-1-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
          t = null;
        } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1-1-3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
          bindings.bind(tv.varSlot, this);
          t = this;
        }
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        t = null;
      }
    } else if (type.getVarSlot() == this.varSlot) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 1a "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      t = this;
    } else if (type.includesVar(this.varSlot, bindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      t = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, type);
      t = type;
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
    return MTypeVar.create(index);
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

  public void collectTconInfo(List<PDefDict.TconInfo> list) {}

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    PTypeSkel t;
    return ((t = bindings.lookup(this.varSlot)) != null)? t: this;
  }

  public String repr() {
    return this.varSlot.repr();
  }
}
