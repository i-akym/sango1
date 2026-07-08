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
  PTypeSkel[] params;  // empty array if no params
  private PFeatureSkel.List features;

  private PTypeRefSkel() {}

  public static PTypeRefSkel create(Compiler theCompiler, Parser.SrcInfo srcInfo, PDefDict.IdKey tconKey, PTypeSkel[] params) {
    PTypeRefSkel t = new PTypeRefSkel();
    t.theCompiler = theCompiler;
    t.srcInfo = srcInfo;
    t.tconKey = tconKey;
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
    PDataDef.OriginDef dd = this.theCompiler.defDict.getDataDef(null, this.tconKey);
    PTypeRefSkel sig = dd.getTypeSig();
    PFeatureSkel[] fs = new PFeatureSkel[dd.getFeatureImplCount()];
    for (int i = 0; i < fs.length; i++) {
      PTypeSkel.Bindings bindings = PTypeSkel.Bindings.create(new ArrayList<PTypeVarSlot>());
      for (int j = 0; j < sig.params.length; j++) {
        // sig param does not include type ref, so width does not mean; only to cause binding of var actually
        if (!sig.params[j].accept(this.params[j], bindings)) {
          StringBuffer emsg = new StringBuffer();
          emsg.append("Does not suit type definition at ");
          emsg.append(this.params[j].getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
      PFeatureSkel f = dd.getFeatureImplAt(i).getImpl();
      fs[i] = f.resolveBindings(bindings);
      // fs[i] = f.resolveBindings(bindings).instanciate(PTypeSkel.InstanciationContext.create(bindings));
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
      b = t.tconKey.equals(this.tconKey) && t.params.length == this.params.length;
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
    boolean b;
    if (isBottom(this)) {
      b =  false;
    } else {
      b = true;
      for (int i = 0; b & i < this.params.length; i++) {
        b &= this.params[i].isConcrete();
      }
    }
    return b;
  }

  public PTypeSkel normalize() throws CompileException {
    PTypeSkel n;
    PDefDict.TidProps tp = this.theCompiler.defDict.resolveTcon(null, this.tconKey);
    if ((tp.cat & PDefDict.TID_CAT_TCON_ALIAS) > 0) {
      PAliasTypeDef ad = this.theCompiler.defDict.getAliasTypeDef(null, this.tconKey);
      n = ad.unalias(this.params);
    } else {
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      for (int i = 0; i < ps.length; i++) {
        ps[i] = this.params[i].normalize();
      }
      n = create(this.theCompiler, this.srcInfo, this.tconKey, ps);
    }

    // check occurrence of bottom
    if (n instanceof PTypeRefSkel) {
      PTypeRefSkel nn = (PTypeRefSkel)n;
      int p = isFun(nn)? nn.params.length - 1: nn.params.length;
      for (int i = 0; i < p; i++) {
        if (isBottom(nn.params[i])) {
          StringBuffer emsg = new StringBuffer();
          emsg.append("\"<_>\" not allowed at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
    return n;
  }

  public PTypeRefSkel resolveBindings(PTypeSkel.Bindings bindings) {
    PTypeRefSkel tr;
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].resolveBindings(bindings);
    }
    return create(this.theCompiler, this.srcInfo, this.tconKey, ps);
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationContext context) {
// /* DEBUG */ System.out.print("INSTANCIATE R "); System.out.print(this); System.out.print(" "); System.out.print(context.applBindings); System.out.print(" "); System.out.println(context.bindingDict);
    PTypeRefSkel tr;
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].instanciate(context);
    }
    return create(this.theCompiler, this.srcInfo, this.tconKey, ps);
  }

  public boolean accept(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    PTypeSkel t = type.resolveBindings(bindings);
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
      b = this.acceptBottom(t, bindings);
    } else {
      b = this.acceptSome(t, bindings);
    }
    return b;
  }

  boolean acceptBottom(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptGenericVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptBottom 5 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptGenericAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean acceptSome(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptSomeSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptGenericVar((PTypeVarSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_ANVAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSome 5 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.acceptGenericAnonym((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean acceptSomeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSomeSome "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (!this.tconKey.equals(tr.tconKey)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSomeSome 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.params.length != tr.params.length) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptSomeSome 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptTypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = true;
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].accept(tr.params[i], bindings);
      }
    }
    return b;
  }

  boolean acceptGenericVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.includesVar(tv.varSlot, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 3 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (tv.features == null || tv.features.acceptObj(this, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 4 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      this.castVarToMe(tv, bindings);
      b = this.accept(tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericVar 5 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  boolean acceptGenericAnonym(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericAnonym "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (tv.features == null || tv.features.acceptObj(this, bindings)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericAnonym 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      this.castVarToMe(tv, bindings);
      b = this.accept(tv, bindings);
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#acceptGenericAnonym 2 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    }
    return b;
  }

  public boolean require(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    PTypeSkel t = type.resolveBindings(bindings);
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
      b = this.requireBottom(t, bindings);
    } else {
      b = this.requireSome(t, bindings);
    }
    return b;
  }

  boolean requireBottom(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireBottom 4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.requireGenericVar((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean requireSome(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    int cat = type.getCat();
    if (cat == PTypeSkel.CAT_BOTTOM) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireSome 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = true;
    } else if (cat == PTypeSkel.CAT_SOME) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireSome 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.requireSomeSome((PTypeRefSkel)type, bindings);
    } else if (cat == PTypeSkel.CAT_VAR) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireSome 4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      b = this.requireGenericVar((PTypeVarSkel)type, bindings);
    } else {
      throw new RuntimeException("Unexspected type. " + type.toString());
    }
    return b;
  }

  boolean requireSomeSome(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (!this.tconKey.equals(tr.tconKey)) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else if (this.params.length != tr.params.length) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = false;
    } else {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireTypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(bindings);
}
      b = true;
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].require(tr.params[i], bindings);
      }
    }
    return b;
  }

  boolean requireGenericVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#requireGenericVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(bindings);
}
    boolean b;
    if (bindings.isGivenTVar(tv)) {
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
      b = this.require(tv, bindings);
    }
    return b;
  }

  void castVarToMe(PTypeVarSkel var, PTypeSkel.Bindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#castVarToMe "); System.out.print(this); System.out.print(" "); System.out.print(var); System.out.print(" "); System.out.println(bindings);
}
    PTypeRefSkel t = new PTypeRefSkel();
    t.theCompiler = this.theCompiler;
    t.srcInfo = this.srcInfo;
    t.tconKey = this.tconKey;
    t.params = new PTypeSkel[this.params.length];
    for (int i = 0; i < t.params.length; i++) {
      PTypeVarSkel v;
        PTypeVarSlot s = PTypeVarSlot.create();
        v = PTypeVarSkel.create(this.theCompiler, this.srcInfo, null, s, var.requiresConcrete, null);
      t.params[i] = v;
    }
    bindings.bind(var, t);
  }

  public boolean includesVar(PTypeVarSlot varSlot, PTypeSkel.Bindings bindings) {
    boolean b = false;
    for (int i = 0; !b && i < this.params.length; i++) {
      b = this.params[i].includesVar(varSlot, bindings);
    }
    return b;
  }

  public PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList)  throws CompileException{
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    PTypeSkel t;
    PTypeSkel.JoinResult r;
    if ((r = this.join2(type, PTypeSkel.Bindings.create(givenTVarList))) != null) {
      t = r.joined.instanciate(PTypeSkel.InstanciationContext.create(r.bindings));
    } else {
      t = null;
    }
    return t;
  }

  public PTypeSkel.JoinResult join2(PTypeSkel type, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PTypeRefSkel#join2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel.JoinResult r;
    if (this.getCat() == PTypeSkel.CAT_BOTTOM) {
      r = PTypeSkel.JoinResult.create(type, bindings);
    } else {
      int cat = type.getCat();
      if (cat == PTypeSkel.CAT_BOTTOM) {
        r = PTypeSkel.JoinResult.create(this, bindings);
      } else if (cat == PTypeSkel.CAT_SOME) {
        r = this.join2Some((PTypeRefSkel)type, bindings);
      } else if (cat == PTypeSkel.CAT_VAR || cat == PTypeSkel.CAT_ANVAR ) {
        r = ((PTypeVarSkel)type).join2(this, bindings);  // forward
      } else {
        throw new IllegalArgumentException("Unknown category. " + type.toString());
      }
    }
    return r;
  }

  PTypeSkel.JoinResult join2Some(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Some 0 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
    PTypeSkel.JoinResult r;
    if (!this.tconKey.equals(tr.tconKey)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Some 1 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      r = null;
    } else if (this.params.length != tr.params.length) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Some 2 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      r = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Some 3 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      PTypeSkel.Bindings b = bindings;
      boolean c = true;
      for (int i = 0; c && i < this.params.length; i++) {
        PTypeSkel.JoinResult r2;
        if ((r2 = this.params[i].join2(tr.params[i], b.copy())) != null) {
          ps[i] = r2.joined;
          b = r2.bindings;
        } else if ((r2 = tr.params[i].join2(this.params[i], b.copy())) != null) {
          ps[i] = r2.joined;
          b = r2.bindings;
        } else {
          c = false;
        }
      }
      if (c) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Some 3-1 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
        PTypeRefSkel joined = create(tr.theCompiler, tr.srcInfo, tr.tconKey, ps);
        r = PTypeSkel.JoinResult.create(joined, b);
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2Some 3-2 "); System.out.print(this); System.out.print(" "); System.out.println(tr);
}
        r = null;
      }
    }
    return r;
  }

  static boolean isBottom(PTypeSkel type) {
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
      if (tr.tconKey == null) { throw new IllegalArgumentException("Tcon is null."); }
      b = tr.tconKey.modName.equals(Module.MOD_LANG) && tr.tconKey.idName.equals(tcon);
    } else {
      b = false;
    }
    return b;
  }

  public PTypeSkel[] getParams() { return this.params; }

  public MType toMType(PModule mod, Module.Builder modBuilder, boolean inReferredDef, List<PTypeVarSlot> slotList) {
// /* DEBUG */ System.out.println(this);
    MTypeRef.Builder b = MTypeRef.Builder.newInstance();
    if (!this.tconKey.modName.equals(mod.actualName)) {
      b.setModIndex(modBuilder.modNameToModIndex(this.tconKey.modName));
    }
    b.setTcon(this.tconKey.idName);
    for (int i = 0; i < params.length; i++) {
      b.addParam(this.params[i].toMType(mod, modBuilder, inReferredDef, slotList));
    }
    return b.create();
  }

  public void extractVars(List<PTypeVarSlot> extracted) {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].extractVars(extracted);
    }
  }

  public PTypeSkel unalias(PTypeSkel.Bindings bindings) throws CompileException {
    PTypeSkel ps[] = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].unalias(bindings);
    }
    PTypeSkel tr;
    PAliasTypeDef ad;
    if ((ad = this.theCompiler.defDict.getAliasTypeDef(null, this.tconKey)) != null) {
      tr = ad.unalias(ps);
    } else {
      tr = create(this.theCompiler, this.srcInfo, this.tconKey, ps);
    }
    return tr;
  }

  public void excludeBareTVarAtRet(Parser.SrcInfo si, boolean atRet, List<PTypeVarSlot> checked) throws CompileException {
    if (isFun(this)) {
      for (int i = 0; i < this.params.length - 1; i++) {
        this.params[i].excludeBareTVarAtRet(si, false, checked);
      }
      this.params[this.params.length - 1].excludeBareTVarAtRet(si, atRet, checked);
    } else {
      for (int i = 0; i < this.params.length; i++) {
        this.params[i].excludeBareTVarAtRet(si, false, checked);
      }
    }
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
    r.add(this.tconKey.repr());
    return r;
  }
}
