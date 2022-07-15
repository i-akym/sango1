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
  PTypeVarSlot varSlot;
  PTypeSkel constraint;  // maybe null

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Parser.SrcInfo srcInfo, String name, PTypeVarSlot varSlot, PTypeSkel constraint) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = srcInfo;
    var.name = ((name != null)? name + ".": "$") + Integer.toString(varSlot.id);
    var.varSlot = varSlot;
    var.constraint = constraint;
    return var;
  }

  PTypeVarSkel copy() {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = this.srcInfo;
    var.name = this.name;
    var.varSlot = this.varSlot;
    var.constraint = this.constraint;
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

  public int getCat() {
    return PTypeSkel.CAT_VAR;
  }

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
      PTypeVarSlot s = PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete);
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
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    PTypeSkel tt = this.resolveBindings(trialBindings);
    PTypeSkel ttt = type.resolveBindings(trialBindings);
    if (tt != this) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = tt.accept(width, bindsRef, ttt, trialBindings);  // forward
    } else if (ttt.getVarSlot() == this.varSlot) {  // me?
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (this.constraint != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrained(width, bindsRef, ttt, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimple(width, bindsRef, ttt, trialBindings);
    }
    return b;
  }

  public PTypeSkelBindings acceptConstrained(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrained "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b = this.constraint.accept(width, bindsRef, type, trialBindings);
    if (b == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrained A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(b);
}
      ;
    } else if (b.isGivenTVar(this.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrained B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(b);
}
      b = this.acceptConstrainedGiven(width, bindsRef, type, b);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrained C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(b);
}
      b = this.acceptConstrainedFree(width, bindsRef, type, b);
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedGiven(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGiven A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedGivenBottom(width, bindsRef, type, trialBindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGiven B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedGivenTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGiven C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedGivenVar(width, bindsRef, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedGivenBottom(int width, boolean bindsRef, PTypeSkel bot, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(bot); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings acceptConstrainedGivenTypeRef(int width, boolean bindsRef, PTypeSkel tr, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenTyperef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    return null;
  }

  PTypeSkelBindings acceptConstrainedGivenVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (tv.varSlot == this.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (tv.constraint != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedGivenVarConstrained(width, bindsRef, tv, trialBindings);
    }
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedGivenVarSimple(width, bindsRef, tv, trialBindings);
    return b;
  }

  PTypeSkelBindings acceptConstrainedGivenVarConstrained(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVarConstrained "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b = this.constraint.accept(width, bindsRef, tv.constraint, trialBindings);
    if (b == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVarConstrained A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      ;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVarConstrained B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      PTypeVarSkel var = create(this.srcInfo, null,
        PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
      trialBindings.bind(tv.varSlot, var);
      b = trialBindings;
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedGivenVarSimple(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedGivenVarSimple "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeVarSkel var = create(this.srcInfo, null,
      PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
    trialBindings.bind(tv.varSlot, var);
    return trialBindings;
  }

  PTypeSkelBindings acceptConstrainedFree(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedFreeBottom(width, bindsRef, type, trialBindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedFreeTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFree C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedFreeVar(width, bindsRef, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedFreeBottom(int width, boolean bindsRef, PTypeSkel bot, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptFreerainedFreeBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(bot); System.out.print(" "); System.out.println(trialBindings);
}
    trialBindings.bind(this.varSlot, bot);  // ok??
    return trialBindings;
  }

  PTypeSkelBindings acceptConstrainedFreeTypeRef(int width, boolean bindsRef, PTypeSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeTyperef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b = this.constraint.accept(width, bindsRef, tr, trialBindings);
    if (b == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeTyperef A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      ;
    } else if (tr.includesVar(this.varSlot, b)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeTyperef B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(b);
}
      b = null;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeTyperef C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(b);
}
      b.bind(this.varSlot, tr);
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedFreeVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (tv.varSlot == this.varSlot) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (tv.constraint != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedFreeVarConstrained(width, bindsRef, tv, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptConstrainedFreeVarSimple(width, bindsRef, tv, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedFreeVarConstrained(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVarConstrained "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b = this.constraint.accept(width, bindsRef, tv.constraint, trialBindings);
    if (b == null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVarConstrained A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      ;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVarConstrained B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(b);
}
      PTypeVarSkel var = create(this.srcInfo, null,
        PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
      b.bind(tv.varSlot, var);
    }
    return b;
  }

  PTypeSkelBindings acceptConstrainedFreeVarSimple(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVarSimple "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isGivenTVar(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVarSimple A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptConstrainedFreeVarSimple B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      PTypeVarSkel var = create(this.srcInfo, null,
        PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
      trialBindings.bind(tv.varSlot, var);
      b = trialBindings;
    }
    return b;
  }

  public PTypeSkelBindings acceptSimple(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimple "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isGivenTVar(this.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimple A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleGiven(width, bindsRef, type, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimple B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleFree(width, bindsRef, type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptSimpleGiven(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGiven A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleGivenBottom(width, bindsRef, type, trialBindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGiven B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleGivenTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGiven C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleGivenVar(width, bindsRef, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptSimpleGivenBottom(int width, boolean bindsRef, PTypeSkel bot, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(bot); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings acceptSimpleGivenTypeRef(int width, boolean bindsRef, PTypeSkel tr, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenTyperef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    return null;
  }

  PTypeSkelBindings acceptSimpleGivenVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (tv.constraint != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleGivenVarConstrained(width, bindsRef, tv, trialBindings);
    }
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleGivenVarSimple(width, bindsRef, tv, trialBindings);
    return b;
  }

  PTypeSkelBindings acceptSimpleGivenVarConstrained(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenVarConstrained "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeVarSkel var = create(this.srcInfo, null,
      PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
    trialBindings.bind(tv.varSlot, var);
    return trialBindings;
  }

  PTypeSkelBindings acceptSimpleGivenVarSimple(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleGivenVarSimple "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeVarSkel var = create(this.srcInfo, null,
      PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
    trialBindings.bind(tv.varSlot, var);
    return trialBindings;
  }

  PTypeSkelBindings acceptSimpleFree(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFree A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleFreeBottom(width, bindsRef, type, trialBindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFree B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleFreeTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFree C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleFreeVar(width, bindsRef, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptSimpleFreeBottom(int width, boolean bindsRef, PTypeSkel bot, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(bot); System.out.print(" "); System.out.println(trialBindings);
}
    trialBindings.bind(this.varSlot, bot);
    return trialBindings;
  }

  PTypeSkelBindings acceptSimpleFreeTypeRef(int width, boolean bindsRef, PTypeSkel tr, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeTyperef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.varSlot.requiresConcrete && !tr.isConcrete(trialBindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeTyperef A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (tr.includesVar(this.varSlot, trialBindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeTyperef B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeTyperef C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tr);
      b = trialBindings;
    }
    return b;
  }

  PTypeSkelBindings acceptSimpleFreeVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (tv.constraint != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleFreeVarConstrained(width, bindsRef, tv, trialBindings);
    }
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.acceptSimpleFreeVarSimple(width, bindsRef, tv, trialBindings);
    return b;
  }

  PTypeSkelBindings acceptSimpleFreeVarConstrained(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarConstrained "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isBound(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarConstrained A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarConstrained B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
      PTypeVarSkel var = create(this.srcInfo, null,
        PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
      trialBindings.bind(tv.varSlot, var);
      b = trialBindings;
    }
    return b;
  }

  PTypeSkelBindings acceptSimpleFreeVarSimple(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarSimple "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (tv.varSlot.requiresConcrete == this.varSlot.requiresConcrete || !this.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarSimple A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      trialBindings.bind(this.varSlot, tv);
      b = trialBindings;
    } else if (trialBindings.isGivenTVar(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarSimple B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#acceptSimpleFreeVarSimple C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      PTypeVarSkel var = create(this.srcInfo, null,
        PTypeVarSlot.createInternal(this.varSlot.variance, this.varSlot.requiresConcrete), this.constraint);  // ok?
      trialBindings.bind(tv.varSlot, var);
      b = trialBindings;
    }
    return b;
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    if (this.varSlot == varSlot) {
      b = true;
    } else if (bindings.isBound(this.varSlot)) {
      b = bindings.lookup(this.varSlot).includesVar(varSlot, bindings);
    }
    return b;
  }

  public PTypeVarSlot getVarSlot() { return this.varSlot; }

  boolean bindConstraint(PTypeSkelBindings bindings) {
    boolean bound;
    if (this.constraint != null && !bindings.isBound(this.varSlot) && !bindings.isGivenTVar(this.varSlot)) {
      bindings.bind(this.varSlot, this.constraint);
      bound = true;
    } else {
      bound = false;
    }
// /* DEBUG */ System.out.print("bindConstraint "); System.out.println(bound);
    return bound;
  }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      t = type.join(this, givenTVarList);  // forward to PTypeRefSkel
    } else if (cat == PTypeSkel.CAT_SOME) {
      t = type.join(this, givenTVarList);  // forward to PTypeRefSkel
    } else {
      t = this.join2(type, givenTVarList);
    }
    return t;
  }

  public PTypeSkel join2(PTypeSkel type, List<PTypeVarSlot> givenTVarList) {
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

  PTypeSkel join2GivenGiven(PTypeVarSkel tv, List<PTypeVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2GivenGiven "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
    return null;
  }

  PTypeSkel join2GivenFree(PTypeVarSkel tv, List<PTypeVarSlot> givenTVarList) {
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

  PTypeSkel join2FreeFree(PTypeVarSkel tv, List<PTypeVarSlot> givenTVarList) {
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

  public MType toMType(PModule mod, List<PTypeVarSlot> slotList) {
    MTypeVar tv;
    int index = slotList.indexOf(this.varSlot);
    if (index < 0) {  // definition
      index = slotList.size();
      slotList.add(this.varSlot);
      MType c = (this.constraint != null)? this.constraint.toMType(mod, slotList): null;
      tv = MTypeVar.create(index, this.varSlot.variance, this.varSlot.requiresConcrete, c);
    } else {  // reference
      tv = MTypeVar.create(index, Module.INVARIANT, false, null);  // do not care attributes
    }
    return tv;
  }

  public void extractVars(List<PTypeVarSlot> extracted) {
    if (!extracted.contains(this.varSlot)) {
      extracted.add(this.varSlot);
    }
    if (this.constraint != null) {
      this.constraint.extractVars(extracted);
    }
  }

  PTypeRefSkel castTo(PTypeRefSkel tr, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#castTo "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    return tr.castFor(this, bindings);
  }

  // PTypeVarSkel constrainAs(PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // PTypeVarSkel var = new PTypeVarSkel();
    // var.srcInfo = this.srcInfo;
    // var.varSlot = PTypeVarSlot.createInternal(tv.varSlot.variance, tv.varSlot.requiresConcrete);
    // bindings.bind(this.varSlot, var);
    // return var;
  // }

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
    StringBuffer buf = new StringBuffer();
    switch (this.varSlot.variance) {
    case Module.COVARIANT:
      buf.append("+");
      break;
    case Module.CONTRAVARIANT:
      buf.append("-");
      break;
    default:
      break;
    }
    buf.append(name);
    if (this.varSlot.requiresConcrete) {
      buf.append("!");
    }
    r.add(buf.toString());
    // r.add(this.varSlot.repr());
    return r;
  }
}
