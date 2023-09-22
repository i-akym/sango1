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
  PTypeSkel constraint;  // PTypeRefSkel.ANY if not constrained
  PFeatureSkel.List features;

  private PTypeVarSkel() {}

  public static PTypeVarSkel create(Parser.SrcInfo srcInfo, String name, PTypeVarSlot varSlot, PTypeSkel constraint, PFeatureSkel.List features) {
    PTypeVarSkel var = new PTypeVarSkel();
    var.srcInfo = srcInfo;
    var.name = ((name != null)? name + ".": "$") + Integer.toString(varSlot.id);
    var.varSlot = varSlot;
    var.constraint = (constraint != null)? constraint: PTypeRefSkel.ANY;
    var.features = features;  // maybe null temprally, set later
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
    buf.append(",features=");
    buf.append(this.features);
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
/* DEBUG */ if (this.features == null) { throw new IllegalArgumentException("Null features " + this.toString()); }
    PTypeSkel t;
// /* DEBUG */ System.out.print("INSTANCIATE V "); System.out.print(this); System.out.print(" "); System.out.print(iBindings.applBindings); System.out.print(" "); System.out.println(iBindings.bindingDict);
    if (iBindings.isBound(this.varSlot)) {
// /* DEBUG */ System.out.print("INSTANCIATE 1 "); System.out.println(this);
      t = iBindings.lookup(this.varSlot);  // created before
    } else if (iBindings.isBoundAppl(this.varSlot)) {
// /* DEBUG */ System.out.print("INSTANCIATE 2 "); System.out.println(this);
      t = iBindings.lookupAppl(this.varSlot).instanciate(iBindings);  // forward
    } else if (iBindings.isGivenTVar(this.varSlot)) {
// /* DEBUG */ System.out.print("INSTANCIATE 3 "); System.out.println(this);
      t = this;
    } else if (iBindings.isInFeatureImpl(this.varSlot)) {
// /* DEBUG */ System.out.print("INSTANCIATE 3a "); System.out.println(this);
      t = this;
    } else {  // create new var for free
// /* DEBUG */ System.out.print("INSTANCIATE 4 "); System.out.println(this);
      PTypeVarSkel v = new PTypeVarSkel();
      v.srcInfo = this.srcInfo;
      v.varSlot = PTypeVarSlot.createInternal(this.varSlot.requiresConcrete);
      v.name = this.name + "." + v.varSlot.id;
      v.constraint = this.constraint.instanciate(iBindings);
      // v.constraint = (this.constraint != null)? this.constraint.instanciate(iBindings): null;
      v.features = this.features.instanciate(iBindings);
      iBindings.bind(this.varSlot, v);
      t = v;
    }
// /* DEBUG */ System.out.print("INSTANCIATE ! "); System.out.print(this); System.out.print(" => "); System.out.println(t);
    return t;
  }

  public PTypeSkel resolveBindings(PTypeSkelBindings bindings) {
/* DEBUG */ if (this.features == null) { throw new IllegalArgumentException("Null features " + this.toString()); }
    return (bindings.isBound(this.varSlot))?
      bindings.lookup(this.varSlot).resolveBindings(bindings):
      this;
  }

  public boolean accept(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
/* DEBUG */ if (this.features == null) { throw new IllegalArgumentException("Null features " + this.toString()); }
    if (this.features.features.length > 0 && !PTypeRefSkel.isAny(this.constraint)) {
      throw new RuntimeException("Sorry, type var with both constraint and feature is not supported yet. " + this.toString());
    }
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
    if (bindings.isGivenTVar(this.varSlot)) {
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
    } else if (cat == PTypeSkel.CAT_ANY) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Given A "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      throw new RuntimeException("Attempt to accept ANY by var. " + this.toString());
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
    if (bindings.isGivenTVar(tv.varSlot)) {
      b = this.accept1GivenGiven(width, bindsRef, tv, bindings);
    } else {
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
    if (!PTypeRefSkel.isAny(this.constraint)) {
      throw new RuntimeException("Oops, constrained var is not supported. " + this.toString());
    }
    if (!PTypeRefSkel.isAny(tv.constraint)) {
      throw new RuntimeException("Oops, constrained var is not supported. " + tv.toString());
    }
    if (this.features.features.length > 0) {
      throw new RuntimeException("Oops, var with feature(s) is not supported. " + this.toString());
    }
    if (tv.features.features.length > 0) {
      throw new RuntimeException("Oops, var with feature(s) is not supported. " + tv.toString());
    }
    bindings.bind(tv.varSlot, this);  // HERE: casting needed
    b = true;
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
    } else if (cat == PTypeSkel.CAT_ANY) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1Free B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      throw new RuntimeException("Attempt to accept ANY by var. " + this.toString());
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
    } else if (!this.constraint.accept(width, bindsRef, tr, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (tr.includesVar(this.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeSome D "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (!bindings.isInFeatureImpl(this.varSlot) && !this.features.acceptVar(bindsRef, this, tr, bindings)) {
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
    if (!bindsRef) {
      if (bindings.isBound(tv.varSlot)) {
        b = false;
      } else if (bindings.isGivenTVar(tv.varSlot)) {
        b = false;
      } else if (this.varSlot.requiresConcrete != tv.varSlot.requiresConcrete) {
        b = false;
// HERE: check constraint
      } else {  // a kind of casting...
        PTypeVarSkel var = create(this.srcInfo, null,
          PTypeVarSlot.createInternal(this.varSlot.requiresConcrete), this.constraint, this.features);  // ok?
        bindings.bind(tv.varSlot, var);
        b = true;
      }
    } else if (!this.constraint.accept(width, bindsRef, tv.constraint, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar B "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#accept1FreeVar C "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}

    // } else if () {
/// features


    } else if (this.varSlot.requiresConcrete == tv.varSlot.requiresConcrete) {
      bindings.bind(this.varSlot, tv);
      b = true;
    } else if (tv.varSlot.requiresConcrete) {
      bindings.bind(this.varSlot, tv);
      b = true;
    } else {
      PTypeVarSkel var = create(this.srcInfo, null,
        PTypeVarSlot.createInternal(this.varSlot.requiresConcrete), this.constraint, this.features);  // ok?
      bindings.bind(tv.varSlot, var);
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

  // boolean bindConstraint(PTypeSkelBindings bindings) {
    // boolean bound;
    // if (this.constraint != null && !bindings.isBound(this.varSlot) && !bindings.isGivenTVar(this.varSlot)) {
      // bindings.bind(this.varSlot, this.constraint);
      // bound = true;
    // } else {
      // bound = false;
    // }
    // return bound;
  // }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    PTypeSkel.JoinResult r;
    if ((r = this.join2(PTypeSkel.WIDER, true, type, PTypeSkelBindings.create(givenTVarList))) != null) {
      t = r.joined.instanciate(PTypeSkel.InstanciationBindings.create(r.bindings));
    } else {
      t = null;
    }
    return t;
  }

  public PTypeSkel.JoinResult join2(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeVarSkel#join2 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel.JoinResult r;
    PTypeSkel t = this.resolveBindings(bindings);
    if (t != this) {
      r = t.join2(width, bindsRef, type, bindings);
    } else if (type.getCat() == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (bindings.isGivenTVar(this.varSlot)) {
      r = this.join2Given(width, bindsRef, type, bindings);
    } else {
      r = this.join2Free(width, bindsRef, type, bindings);
    }
    return r;
  }

  PTypeSkel.JoinResult join2Given(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    if (type instanceof PTypeRefSkel) {
      r = null;
    } else if (type instanceof PTypeVarSkel) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      PTypeSkel t = tv.resolveBindings(bindings);
      if (t != tv) {
        r = this.join2Given(width, bindsRef, t, bindings);
      } else if (this.varSlot == tv.varSlot)  {
        r = PTypeSkel.JoinResult.create(this, bindings);
      } else if (bindings.isGivenTVar(tv.varSlot))  {
        r = null;
      } else {
        r = this.join2GivenFree(width, bindsRef, tv, bindings);
      }
    } else {
      throw new IllegalArgumentException("Unknown type. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join2GivenFree(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    r = tv.join2FreeGiven(width, bindsRef, this, bindings);
    return r;
  }

  PTypeSkel.JoinResult join2Free(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    if (type instanceof PTypeRefSkel) {
      r = this.join2FreeTypeRef(width, bindsRef, (PTypeRefSkel)type, bindings);
    } else if (type instanceof PTypeVarSkel) {
      PTypeVarSkel tv = (PTypeVarSkel)type;
      PTypeSkel t = tv.resolveBindings(bindings);
      if (t != tv) {
        r = this.join2Free(width, bindsRef, t, bindings);
      } else if (this.varSlot == tv.varSlot)  {
        r = PTypeSkel.JoinResult.create(this, bindings);
      } else if (bindings.isGivenTVar(tv.varSlot))  {
        r = this.join2FreeGiven(width, bindsRef, tv, bindings);
      } else {
        r = this.join2FreeFree(width, bindsRef, tv, bindings);
      }
    } else {
      throw new IllegalArgumentException("Unknown type. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join2FreeGiven(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.JoinResult r;
    if (!bindsRef) {
      r = null;
    } else {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tv);
      r = PTypeSkel.JoinResult.create(this, b);
    }
    return r;
  }

  PTypeSkel.JoinResult join2FreeTypeRef(int width, boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings bindings) {
    PTypeSkel.JoinResult r;
    if (!bindsRef) {
      r = null;
    } else if (!PTypeRefSkel.isAny(this.constraint)) {
      throw new RuntimeException("Sorry, joining var with constraint is not supported. " + this.toString());  // HERE
    } else if (this.features.features.length > 0) {
      throw new RuntimeException("Sorry, joining var with feature(s) is not supported. " + this.toString());  // HERE
    } else if (tr.includesVar(this.varSlot, bindings)) {
      r = null;
    } else {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tr);
      r = PTypeSkel.JoinResult.create(tr, b);
    }
    return r;
  }

  PTypeSkel.JoinResult join2FreeFree(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // this.varSlot != tv.varSlot
    PTypeSkel.JoinResult r;
    if (!bindsRef) {
      r = null;
    } else if (!PTypeRefSkel.isAny(this.constraint)) {
      throw new RuntimeException("Sorry, joining var with constraint is not supported. " + this.toString());  // HERE
    } else if (!PTypeRefSkel.isAny(tv.constraint)) {
      throw new RuntimeException("Sorry, joining var with constraint is not supported. " + tv.toString());  // HERE
    } else if (this.features.features.length > 0) {
      throw new RuntimeException("Sorry, joining var with feature(s) is not supported. " + this.toString());  // HERE
    } else if (tv.features.features.length > 0) {
      throw new RuntimeException("Sorry, joining var with feature(s) is not supported. " + tv.toString());  // HERE
    } else if (this.varSlot.requiresConcrete == tv.varSlot.requiresConcrete) {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tv);
      r = PTypeSkel.JoinResult.create(tv, b);
    } else if ((width == PTypeSkel.WIDER) && this.varSlot.requiresConcrete) {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tv);
      r = PTypeSkel.JoinResult.create(tv, b);
    } else if ((width == PTypeSkel.WIDER) && tv.varSlot.requiresConcrete) {
      PTypeSkelBindings b = bindings.copy();
      b.bind(tv.varSlot, this);
      r = PTypeSkel.JoinResult.create(this, b);
    } else if ((width == PTypeSkel.NARROWER) && this.varSlot.requiresConcrete) {
      PTypeSkelBindings b = bindings.copy();
      b.bind(tv.varSlot, this);
      r = PTypeSkel.JoinResult.create(this, b);
    } else if ((width == PTypeSkel.NARROWER) && tv.varSlot.requiresConcrete) {
      PTypeSkelBindings b = bindings.copy();
      b.bind(this.varSlot, tv);
      r = PTypeSkel.JoinResult.create(tv, b);
    } else {
      r = null;
    }
    return r;
  }

  public MType toMType(PModule mod, List<PTypeVarSlot> slotList) {
    MTypeVar tv;
    int index = slotList.indexOf(this.varSlot);
    if (index < 0) {  // definition
      index = slotList.size();
      slotList.add(this.varSlot);
      MType c = (!PTypeRefSkel.isAny(this.constraint))? this.constraint.toMType(mod, slotList): null;
      MFeature.List fs = (this.features.features.length > 0)? this.features.toMType(mod, slotList): null;
      tv = MTypeVar.create(index, this.varSlot.requiresConcrete, c, fs);
    } else {  // reference
      tv = MTypeVar.create(index, false, null, null);  // do not care attributes
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
    this.features.extractVars(extracted);
  }

  PTypeRefSkel castTo(PTypeRefSkel tr, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeVarSkel#castTo "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    return tr.castFor(this, bindings);
  }

  public void collectTconProps(List<PDefDict.TconProps> list) {}

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    PTypeSkel t;
    return ((t = bindings.lookup(this.varSlot)) != null)? t: this;
  }

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    if (!PTypeRefSkel.isAny(this.constraint)) {
      r.append(this.constraint.repr());
      r.add("=");
    }
    StringBuffer buf = new StringBuffer();
    buf.append(name);
    if (this.varSlot.requiresConcrete) {
      buf.append("!");
    }
    r.add(buf.toString());
    return r;
  }
}
