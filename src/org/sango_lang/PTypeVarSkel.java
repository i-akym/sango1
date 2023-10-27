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
  // PTypeSkel constraint;  // PTypeRefSkel.ANY if not constrained
  PFeatureSkel.List features;

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Parser.SrcInfo srcInfo, String name, PTypeVarSlot varSlot, /* PTypeSkel constraint, */ PFeatureSkel.List features) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = srcInfo;
    var.name = ((name != null)? name + ".": "$") + Integer.toString(varSlot.id);
    var.varSlot = varSlot;
    // var.constraint = (constraint != null)? constraint: PTypeRefSkel.ANY;
    var.features = features;  // maybe null temprally, set later
    return var;
  }

  void castRequiresConcrete(PTypeSkelBindings bindings) {
    if (this.varSlot.requiresConcrete) { throw new RuntimeException("Already concreate. " + this.toString()); }
    PTypeVarSlot s = PTypeVarSlot.createInternal(true);
    String n = this.name /* + "." + s.id */ ;
    PTypeVarSkel v = create(this.srcInfo, n, s, this.features);
    bindings.bind(this.varSlot, v);
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
    // buf.append(",constraint=");
    // buf.append(this.constraint);
    if (this.features.features.length > 0) {
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

  // public void checkConstraint(boolean isArg, List<PTypeVarSlot> checked) throws CompileException {
    // StringBuffer emsg;
    // if (checked.contains(this.varSlot)) { return; }
    // if (PTypeRefSkel.isAny(this.constraint)) {
      // checked.add(this.varSlot);
    // } else if (!isArg) {
      // emsg = new StringBuffer();
      // emsg.append("Constraint not allowed for ");
      // emsg.append(this.name);
      // emsg.append(" at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // } else {
      // this.constraint.checkConstraint(false, checked);
      // checked.add(this.varSlot);
    // }
  // }

  public boolean isLiteralNaked() { return false; }

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

  public PTypeSkel resolveBindings(PTypeSkelBindings bindings) {
/* DEBUG */ if (this.features == null) { throw new IllegalArgumentException("Null features " + this.toString()); }
    return (bindings.isBound(this.varSlot))?
      bindings.lookup(this.varSlot).resolveBindings(bindings):
      this;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationContext context) {
/* DEBUG */ if (this.features == null) { throw new IllegalArgumentException("Null features " + this.toString()); }
    PTypeSkel t;
    if (context.isGivenTVar(this.varSlot)) {
      t = this;
    } else if (context.isBound(this.varSlot)) {
      t = context.lookup(this.varSlot);  // created before
    // } else if (context.isInFeatureImpl(this.varSlot)) {  // needed?
      // t = this;
    } else {  // create new var for free
      PTypeVarSkel v = new PTypeVarSkel();
      v.srcInfo = this.srcInfo;
      v.varSlot = PTypeVarSlot.createInternal(this.varSlot.requiresConcrete);
      v.name = this.name /* + "." + v.varSlot.id */ ;
      // v.constraint = this.constraint.instanciate(context);
      v.features = this.features.instanciate(context);
      context.bind(this.varSlot, v);
      t = v;
    }
// /* DEBUG */ System.out.print("INSTANCIATE ! "); System.out.print(this); System.out.print(" => "); System.out.println(t);
    return t;
  }

  public boolean accept(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
/* DEBUG */ if (this.features == null) { throw new IllegalArgumentException("Null features " + this.toString()); }
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
      b = tt.accept(width, bindsRef, ttt, bindings);  // forward
    } else if (ttt.getVarSlot() == this.varSlot) {  // me?
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(ttt); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1(width, bindsRef, ttt, bindings);
    }
    return b;
  }

  boolean accept1(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
    // type != this
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(this.varSlot) || bindings.getGivenBound(this.varSlot) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1 A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1Given(width, bindsRef, type, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1 B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1Free(width, bindsRef, type, bindings);
    }
    return b;
  }

  boolean accept1Given(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
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
      b = this.accept1GivenVar(width, bindsRef, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean accept1GivenVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // tv: GIVEN or FREE
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.equals(bindings.getGivenBound(tv.varSlot))) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (bindings.isGivenTVar(tv.varSlot) || bindings.getGivenBound(tv.varSlot) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenGiven(width, bindsRef, tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenFree(width, bindsRef, tv, bindings);
    }
    return b;
  }

  boolean accept1GivenGiven(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this != tv
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenGiven "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    b = false;
    return b;
  }

  boolean accept1GivenFree(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1GivenFree "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (!this.features.accept(false, tv, bindings)) {
      b = false;
    } else if (!this.varSlot.requiresConcrete && tv.varSlot.requiresConcrete) {
      b = false;
    } else {
      bindings.bindGiven(tv.varSlot, this);
      b = true;
    }
    return b;
  }

  boolean accept1Free(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
    // type != this
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, type);
      b = true;
    // } else if (cat == PTypeSkel.CAT_ANY) {
// /* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  // System.out.print("PTypeVarSkel#accept1Free B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
// }
      // throw new RuntimeException("Attempt to accept ANY by var. " + this.toString());
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeSome(width, bindsRef, (PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1FreeVar(width, bindsRef, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean accept1FreeSome(int width, boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (!bindsRef) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.varSlot.requiresConcrete && !tr.isConcrete(bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    // } else if (!this.constraint.accept(width, bindsRef, tr, bindings)) {
// /* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  // System.out.print("PTypeVarSkel#accept1FreeSome C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
// }
      // b = false;
    } else if (tr.includesVar(this.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (/* !bindings.isInFeatureImpl(this.varSlot) && */ !this.features.accept(bindsRef, tr, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome E "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
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

  boolean accept1FreeVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (!bindsRef || bindings.getGivenBound(this.varSlot) != null) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = this.accept1GivenVar(width, bindsRef, tv, bindings);  // same meaning
    // } else if (!this.constraint.accept(width, bindsRef, tv.constraint, bindings)) {
// /* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  // System.out.print("PTypeVarSkel#accept1FreeVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
// }
      // b = false;

    } else if (!this.features.accept(bindsRef, tv, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.varSlot.requiresConcrete && tv.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, tv);
      b = true;
    } else if (this.varSlot.requiresConcrete && !tv.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar E "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      tv.castRequiresConcrete(bindings);
      bindings.bind(this.varSlot, tv);
      b = true;
    } else if (!this.varSlot.requiresConcrete && tv.varSlot.requiresConcrete) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar F "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, tv);
      b = true;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      bindings.bind(this.varSlot, tv);
      b = true;
    }
    return b;
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    if (this.varSlot == varSlot) {
      b = true;
    } else if (b = this.features.includesVar(varSlot, bindings)) {
      ;
    } else if (bindings.isBound(this.varSlot)) {
      b = bindings.lookup(this.varSlot).includesVar(varSlot, bindings);
    }
    return b;
  }

  public PTypeVarSlot getVarSlot() { return this.varSlot; }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) {
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

  public PTypeSkel.JoinResult join2(int width, /* boolean bindsRef, */ PTypeSkel type, PTypeSkelBindings bindings) {
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

  PTypeSkel.JoinResult join2Free(int width, /* boolean bindsRef, */ PTypeSkel type, PTypeSkelBindings bindings) {
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

  PTypeSkel.JoinResult join2FreeTypeRef(int width, /* boolean bindsRef, */ PTypeRefSkel tr, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    // if (!bindsRef) {
      // r = null;
    /* } else */ if (tr.includesVar(this.varSlot, bindings)) {
      r = null;
    } else if (!this.features.accept(true, tr, bindings)) {
    // } else if (!this.features.accept(bindsRef, tr, bindings)) {
      r = null;
    } else {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tr);
      r = PTypeSkel.JoinResult.create(tr, b);
    }
    return r;
  }

  PTypeSkel.JoinResult join2FreeFree(int width, /* boolean bindsRef, */ PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.JoinResult r;
    PFeatureSkel.JoinResult fr;
    // if (!bindsRef) {
      // r = null;
    // /* } else */ if (this.features.features.length > 0) {
      // throw new RuntimeException("Sorry, joining var with feature(s) is not supported. " + this.toString());  // HERE
    // } else if (tv.features.features.length > 0) {
      // throw new RuntimeException("Sorry, joining var with feature(s) is not supported. " + tv.toString());  // HERE
    // } else {
    PTypeSkelBindings b = bindings.copy();
    if ((fr = this.features.joinList(tv.features, b)) == null) {
      r = null;
    } else {
      PTypeVarSlot s = PTypeVarSlot.createInternal(this.varSlot.requiresConcrete | tv.varSlot.requiresConcrete);
      String n = this.name /* + "." + s.id */;
      PTypeVarSkel v = create(fr.srcInfo, n, s, fr.pack());
      PTypeSkelBindings bb = fr.bindings.copy();
      bb.bind(this.varSlot, v);
      bb.bind(tv.varSlot, v);
      r = PTypeSkel.JoinResult.create(v, bb);
    // } else if (this.varSlot.requiresConcrete == tv.varSlot.requiresConcrete) {
      // PTypeSkelBindings b = bindings.copy();
      // b.bind(this.varSlot, tv);
      // r = PTypeSkel.JoinResult.create(tv, b);
    // } else if (this.varSlot.requiresConcrete) {
      // PTypeSkelBindings b = bindings.copy();
      // b.bind(tv.varSlot, this);
      // r = PTypeSkel.JoinResult.create(this, b);
    // } else if (tv.varSlot.requiresConcrete) {
      // PTypeSkelBindings b = bindings.copy();
      // b.bind(this.varSlot, tv);
      // r = PTypeSkel.JoinResult.create(tv, b);
    // } else {
      // r = null;
    }
    return r;
  }

  public MType toMType(PModule mod, List<PTypeVarSlot> slotList) {
    MTypeVar tv;
    int index = slotList.indexOf(this.varSlot);
    if (index < 0) {  // definition
      index = slotList.size();
      slotList.add(this.varSlot);
      // MType c = (!PTypeRefSkel.isAny(this.constraint))? this.constraint.toMType(mod, slotList): null;
      MFeature.List fs = (this.features.features.length > 0)? this.features.toMType(mod, slotList): null;
      tv = MTypeVar.create(index, this.varSlot.requiresConcrete, /* c, */ fs);
    } else {  // reference
      tv = MTypeVar.create(index, false, /* null, */ null);  // do not care attributes
    }
    return tv;
  }

  public void extractVars(List<PTypeVarSlot> extracted) {
    if (!extracted.contains(this.varSlot)) {
      extracted.add(this.varSlot);
    }
    // if (this.constraint != null) {
      // this.constraint.extractVars(extracted);
    // }
    this.features.extractVars(extracted);
  }

  public void collectTconProps(List<PDefDict.TconProps> list) {}

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    PTypeSkel t;
    return ((t = bindings.lookup(this.varSlot)) != null)? t: this;
  }

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    // if (!PTypeRefSkel.isAny(this.constraint)) {
      // r.append(this.constraint.repr());
      // r.add("=");
    // }
    StringBuffer buf = new StringBuffer();
    buf.append(name);
    if (this.varSlot.requiresConcrete) {
      buf.append("!");
    }
    if (!this.features.isEmpty()) {
      buf.append(this.features.repr());
    }
    r.add(buf.toString());
    return r;
  }
}
