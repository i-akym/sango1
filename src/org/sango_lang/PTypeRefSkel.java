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

public class PTypeRefSkel implements PTypeSkel {
  PDefDict.DefDictGetter defDictGetter;
  Parser.SrcInfo srcInfo;
  PDefDict.TconInfo tconInfo;
  boolean ext;
  PTypeSkel[] params;  // empty array if no params

  private PTypeRefSkel() {}

  public static PTypeRefSkel create(PDefDict.DefDictGetter defDictGetter, Parser.SrcInfo srcInfo, PDefDict.TconInfo tconInfo, boolean ext, PTypeSkel[] params) {
/* DEBUG */ if (defDictGetter == null) {
/* DEBUG */   throw new IllegalArgumentException("nulll defDictGetter " + tconInfo.key.toRepr());
/* DEBUG */ }
    PTypeRefSkel t = new PTypeRefSkel();
    t.defDictGetter = defDictGetter;
    t.srcInfo = srcInfo;
    t.tconInfo = tconInfo;
    t.ext = ext;
    t.params = params;
    return t;
  }

  PTypeRefSkel castFor(PTypeVarSkel var, PTypeSkelBindings bindings) {
    PTypeRefSkel t = new PTypeRefSkel();
    t.defDictGetter = this.defDictGetter;
    t.srcInfo = this.srcInfo;
    t.tconInfo = this.tconInfo;
    t.ext = this.ext;
    t.params = new PTypeSkel[this.params.length];
    int vv[] = this.paramVariances();
    for (int i = 0; i < t.params.length; i++) {
      PTypeVarSkel v;
        PTypeVarSlot s = PTypeVarSlot.createInternal(vv[i], var.varSlot.requiresConcrete);
        v = PTypeVarSkel.create(this.srcInfo, null, s, null);  // constraint == null ok?
      t.params[i] = v;
    }
    bindings.bind(var.varSlot, t);
    return t;
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof PTypeRefSkel)) {
      b = false;
    } else {
      PTypeRefSkel t = (PTypeRefSkel)o;
      b = t.tconInfo.equals(this.tconInfo) && t.ext == this.ext && t.params.length == this.params.length;
      for (int i = 0; b && i < t.params.length; i++) {
        b = t.params[i].equals(this.params[i]);
      }
    }
    return b;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    if (this.srcInfo != null) {
      buf.append("typerefskel[src=");
      buf.append(this.srcInfo);
      buf.append(",");
    }
    String sep = "";
    buf.append("<");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i]);
      sep = " ";
    }
    buf.append(sep);
    buf.append(this.tconInfo.key.toRepr());
    if (this.ext) {
      buf.append("+");
    }
    buf.append(">");
    if (this.srcInfo != null) {
      buf.append("]");
    }
    return buf.toString();
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  public int getCat() {
    return isBottom(this)? PTypeSkel.CAT_BOTTOM: PTypeSkel.CAT_SOME;
  }

  public boolean isLiteralNaked() {
    return this.tconInfo.key.modName.equals(Module.MOD_LANG) && 
      this.tconInfo.key.tcon.equals(Module.TCON_EXPOSED) ;
  }

  public boolean isConcrete() {
    boolean b = true;
    for (int i = 0; b & i < this.params.length; i++) {
      b &= this.params[i].isConcrete();
    }
    return b;
  }

  public boolean isConcrete(PTypeSkelBindings bindings) {
    boolean b = true;
    for (int i = 0; b & i < this.params.length; i++) {
      b &= this.params[i].isConcrete(bindings);
    }
    return b;
  }

  public PDefDict.TconInfo getTconInfo() {
    if (this.tconInfo == null) {
      throw new IllegalStateException("Tcon info not set up.");
    }
    return this.tconInfo;
  }

  int[] paramVariances() {
    int[] vv = new int[this.params.length] ;
    if (isTuple(this)) {
      for (int i = 0; i < this.params.length; i++) {
        vv[i] = Module.COVARIANT;
      }
    } else if (isFun(this)) {
      for (int i = 0; i < this.params.length - 1; i++) {
        vv[i] = Module.CONTRAVARIANT;
      }
      vv[this.params.length - 1] = Module.COVARIANT;
    } else {
      PDataDef dd = this.tconInfo.props.defGetter.getDataDef();
      PTypeRefSkel tr = (PTypeRefSkel)dd.getTypeSig();
      for (int i = 0; i < this.params.length; i++) {
        vv[i] = tr.params[i].getVarSlot().variance;
      }
    }
    return vv;
  }

  static int[] paramWidths(int width, int[] variances) {
    int[] ww = new int[variances.length] ;
    if (width == PTypeSkel.EQUAL) {
      for (int i = 0; i < variances.length; i++) {
        ww[i] = PTypeSkel.EQUAL;
      }
    } else {
      for (int i = 0; i < variances.length; i++) {
        if (variances[i] == Module.INVARIANT) {
          ww[i] = PTypeSkel.EQUAL;
        } else if (variances[i] == Module.CONTRAVARIANT) {
          ww[i] = - width;
        } else {
          ww[i] = width;
        }
      }
    }
    return ww;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].instanciate(iBindings);
    }
    return create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
  }

  public PTypeRefSkel resolveBindings(PTypeSkelBindings bindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].resolveBindings(bindings);
    }
    return create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
  }

  public void checkVariance(int width) throws CompileException {
    int[] ww = paramWidths(width, this.paramVariances());
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].checkVariance(ww[i]);
    }
  }

  public PTypeSkelBindings accept(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
    return (this.getCat() == PTypeSkel.CAT_BOTTOM)?
      this.accept1(width, bindsRef, type.resolveBindings(trialBindings), trialBindings):
      this.accept2(width, bindsRef, type.resolveBindings(trialBindings), trialBindings);
  }

  public PTypeSkelBindings accept1(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#accept1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      b = trialBindings;
    } else if (cat == PTypeSkel.CAT_SOME) {
      b = (width == PTypeSkel.WIDER)? trialBindings: null;
    } else {
      b = this.acceptVar(width, bindsRef, (PTypeVarSkel)type, trialBindings);
    }
    return b;
  }

  public PTypeSkelBindings accept2(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#accept2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
      b = (width == PTypeSkel.NARROWER)? trialBindings: null;
      // b = this.acceptBottom(width, bindsRef, type, trialBindings);
    } else if (cat == PTypeSkel.CAT_SOME) {
      b = this.acceptTypeRef(width, bindsRef, (PTypeRefSkel)type, trialBindings);
    } else {
      PTypeVarSkel v = (PTypeVarSkel)type;
      if (v.bindConstraint(trialBindings)) {
        b = this.accept(width, bindsRef, v, trialBindings);  // retry
      } else {
        b = this.acceptVar(width, bindsRef, v, trialBindings);
      }
    }
    return b;
  }

  // PTypeSkelBindings acceptBottom(int width, boolean bindsRef, PTypeSkel bot, PTypeSkelBindings trialBindings) throws CompileException {
// if (PTypeGraph.DEBUG > 1) {
    // /* DEBUG */ System.out.print("PTypeRefSkel#acceptBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(bot); System.out.print(" "); System.out.println(trialBindings);
// }
  //}
  // }

  PTypeSkelBindings acceptTypeRef(int width, boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.params.length != tr.params.length) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptTypeRef 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (width == PTypeSkel.EQUAL) {
      b = this.acceptEqualTypeRef(bindsRef, tr, trialBindings);
    } else if (width == PTypeSkel.NARROWER) {
      b = this.acceptNarrowerTypeRef(bindsRef, tr, trialBindings);
    } else {  // WIDER
      b = this.acceptWiderTypeRef(bindsRef, tr, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings acceptEqualTypeRef(boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptEqualTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;

    if (this.tconInfo.key.equals(tr.tconInfo.key) && this.ext == tr.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptEqualTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;  // continue
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptEqualTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;  // stop
    }
    if (b != null) {
      int[] ww = paramWidths(PTypeRefSkel.EQUAL, this.paramVariances());
      for (int i = 0; b != null && i < this.params.length; i++) {
        b = this.params[i].accept(ww[i], bindsRef, tr.params[i], b);
      }
    }
    return b;
  }

  PTypeSkelBindings acceptNarrowerTypeRef(boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      if (this.ext == tr.ext || this.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;  // continue
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;  // stop
      }
    } else if (this.defDictGetter.getGlobalDefDict().isBaseOf(tr.tconInfo.key, this.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else if (this.ext && this.defDictGetter.getGlobalDefDict().isBaseOf(this.tconInfo.key, tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptNarrowerTypeRef 4 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;  // stop
    }
    if (b != null) {
      int[] ww = paramWidths(PTypeRefSkel.NARROWER, this.paramVariances());
      for (int i = 0; b != null && i < this.params.length; i++) {
        b = this.params[i].accept(ww[i], bindsRef, tr.params[i], b);
      }
    }
    return b;
  }

  PTypeSkelBindings acceptWiderTypeRef(boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      if (this.ext == tr.ext || tr.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;  // continue
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;  // stop
      }
    } else if (this.defDictGetter.getGlobalDefDict().isBaseOf(this.tconInfo.key, tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else if (tr.ext && this.defDictGetter.getGlobalDefDict().isBaseOf(tr.tconInfo.key, this.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptWiderTypeRef 4 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;  // stop
    }
    if (b != null) {
      int[] ww = paramWidths(PTypeRefSkel.WIDER, this.paramVariances());
      for (int i = 0; b != null && i < this.params.length; i++) {
        b = this.params[i].accept(ww[i], bindsRef, tr.params[i], b);
      }
    }
    return b;
  }

  PTypeSkelBindings acceptVar(int width, boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptVar 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (this.includesVar(tv.varSlot, trialBindings)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptVar 2 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#acceptVar 3 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.accept(width, bindsRef, tv.castTo(this, trialBindings), trialBindings);
    }
    return b;
  }

  static PDefDict.TconInfo resolveTcon(PDefDict.TconKey key, PDefDict.DefDictGetter defDictGetter) throws CompileException {
    return defDictGetter.getReferredDefDict(key.modName).resolveTcon(
      key.tcon,
      PTypeId.SUBCAT_DATA + PTypeId.SUBCAT_EXTEND + PTypeId.SUBCAT_ALIAS,
      Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE);
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    for (int i = 0; !b && i < this.params.length; i++) {
      b = this.params[i].includesVar(varSlot, bindings);
    }
    return b;
  }

  public PTypeVarSlot getVarSlot() {
    return null;
  }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0-1 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
      t = type;
    } else if (type.getCat() == PTypeSkel.CAT_BOTTOM) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0-2 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
      t = this;
    } else {
      t = this.join2(type, givenTVarList);
    }
    return t;
  }

  public PTypeSkel join2(PTypeSkel type, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 0 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_SOME) {
      t = this.join2TypeRef((PTypeRefSkel)type, givenTVarList);
    } else {
      t = this.join2Var((PTypeVarSkel)type, givenTVarList);
    }
    return t;
  }

  PTypeSkel join2TypeRef(PTypeRefSkel tr, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 0 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
    PTypeSkel t;
    if (this.params.length != tr.params.length) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
      t = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
      PTypeSkelBindings b = this.accept(PTypeSkel.NARROWER, true, tr, PTypeSkelBindings.create(givenTVarList));
      if (b != null) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 2-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
        t = this.instanciate(PTypeSkel.InstanciationBindings.create(b));
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 2-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
        b = tr.accept(PTypeSkel.NARROWER, true, this, PTypeSkelBindings.create(givenTVarList));
        if (b != null) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 2-2-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
          t = tr.instanciate(PTypeSkel.InstanciationBindings.create(b));
        } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 2-2-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr);
}
          t = null;
        }
      }
    }
    return t;
  }

  PTypeSkel join2Var(PTypeVarSkel tv, List<PTypeVarSlot> givenTVarList) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Var 0 "); System.out.print(this); System.out.print(" "); System.out.print(tv);
}
    PTypeSkel t;
    if (givenTVarList.contains(tv.varSlot)) {
      t = null;
    } else {
      t = this;
    }
    return t;
  }

  static boolean isBottom(PTypeSkel type) {
    return isLangType(type, Module.TCON_BOTTOM);
  }

  static boolean willNotReturn(PTypeSkel type) {
    return isLangType(type, Module.TCON_BOTTOM);
  }

  static boolean isTuple(PTypeSkel type) {
    return isLangType(type, Module.TCON_TUPLE);
  }

  static boolean isList(PTypeSkel type) {
    return isLangType(type, Module.TCON_LIST);
  }

  static boolean isFun(PTypeSkel type) {
    return isLangType(type, Module.TCON_FUN);
  }

  public static boolean isLangType(PTypeSkel type, String tcon) {
    boolean b;
    if (type instanceof PTypeRefSkel) {
      PTypeRefSkel tr = (PTypeRefSkel)type;
      if (tr.tconInfo == null) { throw new IllegalArgumentException("Tcon not resolved."); }
      b = tr.tconInfo.key.modName.equals(Module.MOD_LANG) && tr.tconInfo.key.tcon.equals(tcon);
    } else {
      b = false;
    }
    return b;
  }

  public PTypeSkel[] getParams() { return this.params; }

  public MType toMType(PModule mod, List<PTypeVarSlot> slotList) {
// /* DEBUG */ System.out.print("{MM "); System.out.print(this); System.out.print(slotList); System.out.println(" mm}");
    MTypeRef.Builder b = MTypeRef.Builder.newInstance();
    if (!this.tconInfo.key.modName.equals(mod.name)) {
      b.setModName(this.tconInfo.key.modName);
    }
    b.setTcon(this.tconInfo.key.tcon);
    b.setExt(this.ext);
    for (int i = 0; i < params.length; i++) {
      b.addParam(this.params[i].toMType(mod, slotList));
    }
    return b.create();
  }

  public void extractVars(List<PTypeVarSlot> extracted) {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].extractVars(extracted);
    }
  }

  public void collectTconInfo(List<PDefDict.TconInfo> list) {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectTconInfo(list);
    }
    if (!list.contains(this.tconInfo)) {
      list.add(this.tconInfo);
    }
  }

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    PTypeSkel ps[] = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].unalias(bindings);
    }
    PTypeSkel tr;
    PAliasTypeDef ad;
    if ((ad = this.tconInfo.props.defGetter.getAliasTypeDef()) != null) {
      tr = ad.unalias(ps);
    } else {
      tr = create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
    }
    return tr;
  }

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    for (int i = 0; i < this.params.length; i++) {
      r.add(this.params[i].repr());
    }
    r.add(this.tconInfo.key.toRepr() + ((this.ext)? "+": ""));
    return r;
  }
}
