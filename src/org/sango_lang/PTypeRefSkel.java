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

public class PTypeRefSkel implements PTypeSkel {
  Compiler theCompiler;
  Parser.SrcInfo srcInfo;
  PDefDict.IdKey tconKey;
  boolean ext;
  PTypeSkel[] params;  // empty array if no params
  private PFeatureSkel.List features;

  private PTypeRefSkel() {}

  public static PTypeRefSkel create(Compiler theCompiler, Parser.SrcInfo srcInfo, PDefDict.IdKey tconKey, boolean ext, PTypeSkel[] params) {
    PTypeRefSkel t = new PTypeRefSkel();
    t.theCompiler = theCompiler;
    t.srcInfo = srcInfo;
    t.tconKey = tconKey;
    t.ext = ext;
    t.params = params;
    return t;
  }

  PFeatureSkel.List getFeatures() throws CompileException {
    if (this.features == null) {
      this.calcFeatures();
    }
    return this.features;
  }

  private void calcFeatures() throws CompileException {
    PDataDef dd = this.theCompiler.defDict.getDataDef(null, this.tconKey);
    PTypeRefSkel sig = dd.getTypeSig();
    PFeatureSkel[] fs = new PFeatureSkel[dd.getFeatureImplCount()];
    for (int i = 0; i < fs.length; i++) {
      PTypeSkelBindings bindings = PTypeSkelBindings.create(new ArrayList<PTypeVarSlot>());
      PFeatureSkel f = dd.getFeatureImplAt(i).getImpl();
      for (int j = 0; j < sig.params.length; j++) {
        bindings.bind(((PTypeVarSkel)sig.params[j]).varSlot, this.params[j]);
      }
      fs[i] = f.resolveBindings(bindings).instanciate(PTypeSkel.InstanciationContext.create(bindings));
    }
    this.features = PFeatureSkel.List.create(this.srcInfo, fs);
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof PTypeRefSkel)) {
      b = false;
    } else {
      PTypeRefSkel t = (PTypeRefSkel)o;
      b = t.tconKey.equals(this.tconKey) && t.ext == this.ext && t.params.length == this.params.length;
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
    buf.append(this.tconKey.repr());
    if (this.ext) {
      buf.append("+");
    }
    if (this.features != null) {
      buf.append(this.features.toString());
    }
    buf.append(">");
    if (this.srcInfo != null) {
      buf.append("]");
    }
    return buf.toString();
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  public int getCat() {
    int c;
    if (isBottom(this)) {
      c = PTypeSkel.CAT_BOTTOM;
    } else {
      c = PTypeSkel.CAT_SOME;
    }
    return c;
  }

  public boolean isLiteralNaked() {
    return this.tconKey.modName.equals(Module.MOD_LANG) && 
      this.tconKey.idName.equals(Module.TCON_EXPOSED) ;
  }

  public boolean isConcrete() {
    boolean b = true;
    for (int i = 0; b & i < this.params.length; i++) {
      b &= this.params[i].isConcrete();
    }
    return b;
  }

  public PTypeSkel extractAnyInconcreteVar(PTypeSkel type) {
    if (!(type instanceof PTypeRefSkel)) { throw new IllegalArgumentException("Not typeref"); }
    PTypeRefSkel tt = (PTypeRefSkel)type;
    PTypeSkel t = null;
    for (int i = 0; t == null && i < this.params.length; i++) {
      t = this.params[i].extractAnyInconcreteVar(tt.params[i]);
    }
    return t;
  }

  // public PDefDict.TconProps getTconInfo() {
    // if (this.tconProps == null) {
      // throw new IllegalStateException("Tcon info not set up.");
    // }
    // return this.tconProps;
  // }

  Module.Variance[] paramVariances() throws CompileException {
    Module.Variance[] vv = new Module.Variance[this.params.length] ;
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
      PDataDef dd = this.theCompiler.defDict.getDataDef(null, this.tconKey);
      for (int i = 0; i < this.params.length; i++) {
        vv[i] = dd.getParamPropss()[i].variance;
      }
    }
    return vv;
  }

  static int[] paramWidths(int width, Module.Variance[] variances) {
    int[] ww = new int[variances.length] ;
    for (int i = 0; i < variances.length; i++) {
      ww[i] = PTypeSkel.calcWidth(width, variances[i]);
    }
    return ww;
  }

  public PTypeRefSkel normalize() throws CompileException {
    PTypeRefSkel n;
    PDefDict.TidProps tp = this.theCompiler.defDict.resolveTcon(null, this.tconKey);
    if ((tp.cat & PDefDict.TID_CAT_TCON_ALIAS) > 0) {
      PAliasTypeDef ad = this.theCompiler.defDict.getAliasTypeDef(null, this.tconKey);
      n = ad.unalias(this.params);
    } else {
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      for (int i = 0; i < ps.length; i++) {
        ps[i] = this.params[i].normalize();
      }
      n = create(this.theCompiler, this.srcInfo, this.tconKey, this.ext, ps);
    }
    return n;
  }

  public PTypeRefSkel resolveBindings(PTypeSkelBindings bindings) {
    PTypeRefSkel tr;
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].resolveBindings(bindings);
    }
    return create(this.theCompiler, this.srcInfo, this.tconKey, this.ext, ps);
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationContext context) {
// /* DEBUG */ System.out.print("INSTANCIATE R "); System.out.print(this); System.out.print(" "); System.out.print(context.applBindings); System.out.print(" "); System.out.println(context.bindingDict);
    PTypeRefSkel tr;
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].instanciate(context);
    }
    return create(this.theCompiler, this.srcInfo, this.tconKey, this.ext, ps);
  }

  public boolean accept(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    PTypeSkel t = type.resolveBindings(bindings);
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
      b = this.acceptBottom(width, t, bindings);
    } else {
      b = this.acceptSome(width, t, bindings);
    }
    return b;
  }

  boolean acceptBottom(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = width == PTypeSkel.WIDER;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptGenericVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean acceptSome(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = width == PTypeSkel.NARROWER;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptSomeSome(width, (PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptGenericVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean acceptSomeSome(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.params.length != tr.params.length) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptTypeRef 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (!this.canAcceptOnTcon(width, tr)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptTypeRef 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptTypeRef 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = true;
      int[] ww = paramWidths(width, this.paramVariances());
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].accept(ww[i], tr.params[i], bindings);
      }
    }
    return b;
  }

  boolean acceptGenericVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.includesVar(tv.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 3 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (tv.features != null) {
      throw new RuntimeException("Oops, var with features not supported for casting. " + tv.toString());  // HERE
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 5 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      this.castVarToMe(tv, bindings);
      b = this.accept(width, tv, bindings);
    }
    return b;
  }

  public boolean require(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    PTypeSkel t = type.resolveBindings(bindings);
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
      b = this.requireBottom(width, t, bindings);
    } else {
      b = this.requireSome(width, t, bindings);
    }
    return b;
  }

  boolean requireBottom(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = width == PTypeSkel.WIDER;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.requireGenericVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean requireSome(int width, PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireSome 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = width == PTypeSkel.NARROWER;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireSome 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.requireSomeSome(width, (PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireSome 4 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.requireGenericVar(width, (PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean requireSomeSome(int width, PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (this.params.length != tr.params.length) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (!this.canAcceptOnTcon(width, tr)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef 3 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = true;
      int[] ww = paramWidths(width, this.paramVariances());
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].require(ww[i], tr.params[i], bindings);
      }
    }
    return b;
  }

  boolean requireGenericVar(int width, PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireGenericVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv.varSlot)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireGenericVar 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.includesVar(tv.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireGenericVar 3 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (tv.features != null) {
      throw new RuntimeException("Oops, var with features not supported for casting. " + tv.toString());  // HERE
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireGenericVar 5 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      this.castVarToMe(tv, bindings);
      b = this.require(width, tv, bindings);
    }
    return b;
  }

  boolean canAcceptOnTcon(int width, PTypeRefSkel tr) throws CompileException {
    boolean b;
    switch (width) {
    case PTypeSkel.NARROWER:
      b = this.canAcceptNarrowerOnTcon(tr);
      break;
    case PTypeSkel.WIDER:
      b = this.canAcceptWiderOnTcon(tr);
      break;
    case PTypeSkel.EQUAL:
      b = this.canAcceptEqualOnTcon(tr);
      break;
    default:
      throw new IllegalArgumentException("Unknown width. " + width);
    }
    return b;
  }

  boolean canAcceptNarrowerOnTcon(PTypeRefSkel tr) throws CompileException {
    // BOTTOM is not supported here
    boolean b;
    if (this.tconKey.equals(tr.tconKey)) {
      if (this.ext && tr.ext) {
        b = true;
      } else if (this.ext && !tr.ext) {
        b = true;
      } else if (!this.ext && tr.ext) {
        b = false;
      } else {
        b = true;
      }
    } else if (this.theCompiler.defDict.isBaseOf(tr.tconKey, this.tconKey)) {
      if (this.ext && tr.ext) {
        b = false;
      } else if (this.ext && !tr.ext) {
        b = false;
      } else if (!this.ext && tr.ext) {
        b = true;
      } else {
        b = true;
      }
    } else if (this.theCompiler.defDict.isBaseOf(this.tconKey, tr.tconKey)) {
      if (this.ext && tr.ext) {
        b = true;
      } else if (this.ext && !tr.ext) {
        b = true;
      } else if (!this.ext && tr.ext) {
        b = false;
      } else {
        b = false;
      }
    } else {
      b = false;
    }
    return b;
  }

  boolean canAcceptWiderOnTcon(PTypeRefSkel tr) throws CompileException {
    // BOTTOM is not supported here
    boolean b;
    if (this.tconKey.equals(tr.tconKey)) {
      if (this.ext && tr.ext) {
        b = true;
      } else if (this.ext && !tr.ext) {
        b = false;
      } else if (!this.ext && tr.ext) {
        b = true;
      } else {
        b = true;
      }
    } else if (this.theCompiler.defDict.isBaseOf(tr.tconKey, this.tconKey)) {
      if (this.ext && tr.ext) {
        b = true;
      } else if (this.ext && !tr.ext) {
        b = false;
      } else if (!this.ext && tr.ext) {
        b = true;
      } else {
        b = false;
      }
    } else if (this.theCompiler.defDict.isBaseOf(this.tconKey, tr.tconKey)) {
      if (this.ext && tr.ext) {
        b = false;
      } else if (this.ext && !tr.ext) {
        b = false;
      } else if (!this.ext && tr.ext) {
        b = true;
      } else {
        b = true;
      }
    } else {
      b = false;
    }
    return b;
  }

  boolean canAcceptEqualOnTcon(PTypeRefSkel tr) {
    // BOTTOM is not supported here
    boolean b;
    b = this.tconKey.equals(tr.tconKey) && (this.ext == tr.ext);
    return b;
  }

  void castVarToMe(PTypeVarSkel var, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#castVarToMe "); System.out.print(this); System.out.print(" "); System.out.print(var); System.out.print(" "); System.out.println(bindings);
}
    PTypeRefSkel t = new PTypeRefSkel();
    t.theCompiler = this.theCompiler;
    t.srcInfo = this.srcInfo;
    t.tconKey = this.tconKey;
    t.ext = this.ext;
    t.params = new PTypeSkel[this.params.length];
    for (int i = 0; i < t.params.length; i++) {
      PTypeVarSkel v;
        PTypeVarSlot s = PTypeVarSlot.createInternal(var.varSlot.requiresConcrete);
        v = PTypeVarSkel.create(this.theCompiler, this.srcInfo, null, s, null);
      t.params[i] = v;
    }
    bindings.bind(var.varSlot, t);
  }

  // static PDefDict.TconProps resolveTcon(PDefDict.IdKey tconKey, PDefDict.DefDictGetter defDictGetter) throws CompileException {
    // Option.Set<Module.Access> as = (new Option.Set<Module.Access>())
      // .add(Module.ACC_PUBLIC).add(Module.ACC_PROTECTED)
      // .add(Module.ACC_OPAQUE).add(Module.ACC_PRIVATE);
    // return defDictGetter.getReferredDefDict(tconKey.modName).resolveTcon(
      // tconKey.idName, PDefDict.TID_CAT_TCON, as);
  // }

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

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList)  throws CompileException{
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type);
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

  public PTypeSkel.JoinResult join2(int width, /* boolean bindsRef, */ PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#join2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(type, bindings);
    } else if (type.getCat() == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(this, bindings);
    } else if (type instanceof PTypeVarSkel) {
      r = ((PTypeVarSkel)type).join2(width, /* bindsRef, */ this, bindings);  // forward
    } else if (type instanceof PTypeRefSkel) {
      r = this.join2TypeRef(width, /* bindsRef, */ (PTypeRefSkel)type, bindings);
    } else {
      throw new IllegalArgumentException("Unknown type. " + type.toString());
    }
    return r;
  }

  PTypeSkel.JoinResult join2TypeRef(int width, /* boolean bindsRef, */ PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 0 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
    PTypeSkel.JoinResult r;
    if (this.params.length != tr.params.length) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      r = null;
    } else if (!this.canAcceptOnTcon(width, tr)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      r = null;
    } else if (this.params.length == 0) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      PTypeSkel[] ps = new PTypeSkel[0];
      PTypeRefSkel joined = create(tr.theCompiler, tr.srcInfo, tr.tconKey, tr.ext, ps);
      r = PTypeSkel.JoinResult.create(joined, bindings);
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 4 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      int[] ww = paramWidths(width, this.paramVariances());
      PTypeSkelBindings b = bindings;
      boolean c = true;
      for (int i = 0; c && i < this.params.length; i++) {
        PTypeSkel.JoinResult r2;
        if ((r2 = this.params[i].join2(ww[i], /* bindsRef, */ tr.params[i], b.copy())) != null) {
          ps[i] = r2.joined;
          b = r2.bindings;
        } else if ((r2 = tr.params[i].join2(ww[i], /* bindsRef, */ this.params[i], b.copy())) != null) {
          ps[i] = r2.joined;
          b = r2.bindings;
        } else {
          c = false;
        }
      }
      if (c) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 4-1 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
        PTypeRefSkel joined = create(tr.theCompiler, tr.srcInfo, tr.tconKey, tr.ext, ps);
        r = PTypeSkel.JoinResult.create(joined, b);
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2TypeRef 4-2 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
        r = null;
      }
    }
    return r;
  }

  // static boolean isAny(PTypeSkel type) {
    // return isLangType(type, "@ANY");
  // }

  static boolean isBottom(PTypeSkel type) {
    return isLangType(type, Module.TCON_BOTTOM);
  }

  static boolean willNotReturn(PTypeSkel type) {
    return isBottom(type);
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
      if (tr.tconKey == null) { throw new IllegalArgumentException("Tcon is null."); }
      b = tr.tconKey.modName.equals(Module.MOD_LANG) && tr.tconKey.idName.equals(tcon);
    } else {
      b = false;
    }
    return b;
  }

  public PTypeSkel[] getParams() { return this.params; }

  public MType toMType(PModule mod, List<PTypeVarSlot> slotList) {
// /* DEBUG */ System.out.println(this);
    MTypeRef.Builder b = MTypeRef.Builder.newInstance();
    if (!this.tconKey.modName.equals(mod.name)) {
      b.setModIndex(mod.modNameToModRefIndex(this.tconKey.modName));
    }
    b.setTcon(this.tconKey.idName);
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

  public void collectVarVariances(PTypeVarSlot slot, Module.Variance contextVariance, List<Module.Variance> variances) throws CompileException {
    Module.Variance[] vs = this.paramVariances();
    for (int i = 0; i < this.params.length; i++) {
      if (contextVariance == null) {
        this.params[i].collectVarVariances(slot, vs[i], variances);
      } else if (contextVariance == Module.INVARIANT) {
        this.params[i].collectVarVariances(slot, Module.INVARIANT, variances);
      } else if (contextVariance == vs[i]) {
        this.params[i].collectVarVariances(slot, contextVariance, variances);
      } else {
        this.params[i].collectVarVariances(slot, Module.INVARIANT, variances);
      }
    }
  }

  // public void collectTconProps(List<PDefDict.TconProps> list) {
    // for (int i = 0; i < this.params.length; i++) {
      // this.params[i].collectTconProps(list);
    // }
    // if (!list.contains(this.tconProps)) {
      // list.add(this.tconProps);
    // }
  // }

  public PTypeSkel unalias(PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel ps[] = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].unalias(bindings);
    }
    PTypeSkel tr;
    PAliasTypeDef ad;
    if ((ad = this.theCompiler.defDict.getAliasTypeDef(null, this.tconKey)) != null) {
      tr = ad.unalias(ps);
    } else {
      tr = create(this.theCompiler, this.srcInfo, this.tconKey, this.ext, ps);
    }
    return tr;
  }

  PTypeVarSkel varIncompatVariance(PTypeSkel.VarianceTab vt) throws CompileException {
    PTypeVarSkel x = null;
    Module.Variance[] vs = this.paramVariances();
    for (int i = 0; x == null && i < this.params.length; i++) {
      if (this.params[i] instanceof PTypeVarSkel) {
        PTypeVarSkel v = (PTypeVarSkel)this.params[i];
        if (!vt.isCompatible(v.varSlot, vs[i])) {
          x = v;
        }
      } else if (this.params[i] instanceof PTypeRefSkel) {
        PTypeRefSkel r = (PTypeRefSkel)this.params[i];
        x = r.varIncompatVariance(vt.forContext(vs[i]));
      } else {
        throw new RuntimeException("Unexpected type. " + this.params[i]);
      }
    }
    return x;
  }

  public void collectTconKeys(Set<PDefDict.IdKey> keys) {
    keys.add(this.tconKey);
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectTconKeys(keys);
    }
  }

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    for (int i = 0; i < this.params.length; i++) {
      r.add(this.params[i].repr());
    }
    r.add(this.tconKey.repr() + ((this.ext)? "+": ""));
    return r;
  }
}
