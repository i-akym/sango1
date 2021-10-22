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
      PTVarDef d = var.varSlot.varDef;
      PTypeVarSkel v;
      if (d != null) {
        PTVarSlot s = PTVarSlot.create(d);
        v = PTypeVarSkel.create(d.getSrcInfo(), d.scope, s);
      } else {
        PTVarSlot s = PTVarSlot.createInternal(vv[i], var.varSlot.requiresConcrete);
        v = PTypeVarSkel.create(null, null, s);
      }
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
        PTypeVarSkel tv = (PTypeVarSkel)tr.params[i];
        vv[i] = tv.varSlot.variance;
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

  public PTypeSkelBindings applyTo(int width, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyTo "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    PTypeSkel t = type.resolveBindings(trialBindings);
    if (t instanceof PNoRetSkel) {
      b = this.applyToNoRet(width, (PNoRetSkel)t, trialBindings);
    } else if (t instanceof PTypeRefSkel) {
      b = this.applyToTypeRef(width, (PTypeRefSkel)t, trialBindings);
    } else {
      b = this.applyToVar(width, (PTypeVarSkel)t, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings applyToNoRet(int width, PNoRetSkel nr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNoRet "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(nr); System.out.print(" "); System.out.println(trialBindings);
}
    return trialBindings;
  }

  PTypeSkelBindings applyToTypeRef(int width, PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToTypeRef "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.params.length != tr.params.length) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToTypeRef 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else if (width == PTypeSkel.EQUAL) {
      b = this.applyToEqualTypeRef(tr, trialBindings);
    } else if (width == PTypeSkel.NARROWER) {
      b = this.applyToNarrowerTypeRef(tr, trialBindings);
    } else {  // WIDER
      b = this.applyToWiderTypeRef(tr, trialBindings);
    }
    return b;
  }

  PTypeSkelBindings applyToEqualTypeRef(PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToEqualTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;

    if (this.tconInfo.key.equals(tr.tconInfo.key) && this.ext == tr.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToEqualTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;  // continue
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToEqualTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;  // stop
    }
    if (b != null) {
      int[] ww = paramWidths(PTypeRefSkel.EQUAL, this.paramVariances());
      for (int i = 0; b != null && i < this.params.length; i++) {
        b = this.params[i].applyTo(ww[i], tr.params[i], b);
      }
    }
    return b;
  }

  PTypeSkelBindings applyToNarrowerTypeRef(PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      if (this.ext == tr.ext || this.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;  // continue
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;  // stop
      }
    } else if (this.defDictGetter.getGlobalDefDict().isBaseOf(tr.tconInfo.key, this.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else if (this.ext && this.defDictGetter.getGlobalDefDict().isBaseOf(this.tconInfo.key, tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToNarrowerTypeRef 4 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;  // stop
    }
    if (b != null) {
      int[] ww = paramWidths(PTypeRefSkel.NARROWER, this.paramVariances());
      for (int i = 0; b != null && i < this.params.length; i++) {
        b = this.params[i].applyTo(ww[i], tr.params[i], b);
      }
    }
    return b;
  }

  PTypeSkelBindings applyToWiderTypeRef(PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef 1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      if (this.ext == tr.ext || tr.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;  // continue
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;  // stop
      }
    } else if (this.defDictGetter.getGlobalDefDict().isBaseOf(this.tconInfo.key, tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef 2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else if (tr.ext && this.defDictGetter.getGlobalDefDict().isBaseOf(tr.tconInfo.key, this.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef 3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;  // continue
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToWiderTypeRef 4 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;  // stop
    }
    if (b != null) {
      int[] ww = paramWidths(PTypeRefSkel.WIDER, this.paramVariances());
      for (int i = 0; b != null && i < this.params.length; i++) {
        b = this.params[i].applyTo(ww[i], tr.params[i], b);
      }
    }
    return b;
  }

  PTypeSkelBindings applyToVar(int width, PTypeVarSkel tv, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToVar "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (trialBindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToVar 1 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#applyToVar 2 "); System.out.print(this); System.out.print(" "); System.out.print(tv); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.applyTo(width, tv.castTo(this, trialBindings), trialBindings);
    }
    return b;
  }

  static PDefDict.TconInfo resolveTcon(PDefDict.TconKey key, PDefDict.DefDictGetter defDictGetter) throws CompileException {
    return defDictGetter.getReferredDefDict(key.modName).resolveTcon(
      key.tcon,
      PTypeId.SUBCAT_DATA + PTypeId.SUBCAT_EXTEND + PTypeId.SUBCAT_ALIAS,
      Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE);
  }

  public boolean includesVar(PTVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    for (int i = 0; !b && i < this.params.length; i++) {
      b = this.params[i].includesVar(varSlot, bindings);
    }
    return b;
  }

  public PTVarSlot getVarSlot() { return null; }

  public PTypeSkel join(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel t;
    PTypeSkel tt = type.resolveBindings(bindings);
    if (tt instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      t = tt.join2(this, bindings);  // forward to PNoRetSkel
    } else {
      t = this.join2(tt, bindings);
    }
    return t;
  }

  public PTypeSkel join2(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel t;
    if (type instanceof PTypeRefSkel) {
      t = this.join2TypeRef((PTypeRefSkel)type, bindings);
    } else {
      t = this.join2Var((PTypeVarSkel)type, bindings);
    }
    return t;
  }

  PTypeSkel join2TypeRef(PTypeRefSkel tr, PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel t;
    if (this.params.length != tr.params.length) {
      t = null;
    } else if (this.tconInfo.key.equals(tr.tconInfo.key)) {
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      boolean cont = true;
      for (int i = 0; cont && i < this.params.length; i++) {
        ps[i] = this.params[i].join(tr.params[i], bindings);
        cont = ps[i] != null;
      }
      t = cont? create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext | tr.ext, ps): null;
    } else if (this.defDictGetter.getGlobalDefDict().isBaseOf(tr.tconInfo.key, this.tconInfo.key)) {
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      boolean cont = true;
      for (int i = 0; cont && i < this.params.length; i++) {
        ps[i] = this.params[i].join(tr.params[i], bindings);
        cont = ps[i] != null;
      }
      t = cont? create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps): null;
    } else if (this.defDictGetter.getGlobalDefDict().isBaseOf(this.tconInfo.key, tr.tconInfo.key)) {
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      boolean cont = true;
      for (int i = 0; cont && i < this.params.length; i++) {
        ps[i] = this.params[i].join(tr.params[i], bindings);
        cont = ps[i] != null;
      }
      t = cont? create(this.defDictGetter, tr.srcInfo, tr.tconInfo, tr.ext, ps): null;
    } else {
      t = null;
    }
    return t;
  }

  PTypeSkel join2Var(PTypeVarSkel tv, PTypeSkelBindings bindings) throws CompileException {
    PTypeSkel t;
    if (bindings.isGivenTVar(tv.varSlot)) {
      t = null;
    } else if (this.includesVar(tv.varSlot, bindings)) {
      t = null;
    } else {
      bindings.bind(tv.varSlot, this);
      t = this;
    }
    return t;
  }

  static boolean willNotReturn(PTypeSkel type) {
    return isLangType(type, Module.TCON_NORET);
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

  public MType toMType(PModule mod, List<PTVarSlot> slotList) {
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

  public List<PTVarSlot> extractVars(List<PTVarSlot> alreadyExtracted) {
    List<PTVarSlot> newlyExtracted = new ArrayList<PTVarSlot>();
    for (int i = 0; i < this.params.length; i++) {
      List<PTVarSlot> justExtracted = this.params[i].extractVars(alreadyExtracted);
      if (justExtracted != null) {
        newlyExtracted.addAll(justExtracted);
      }
    }
    return newlyExtracted;
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
    PAliasDef ad;
    if ((ad = this.tconInfo.props.defGetter.getAliasDef()) != null) {
      tr = ad.unalias(ps);
    } else {
      tr = create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
    }
    return tr;
  }

  public String repr() {
    StringBuffer buf = new StringBuffer();
    if (this.params.length > 0) {
      buf.append("<");
    }
    String sep = "";
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i].repr());
      sep = " ";
    }
    buf.append(sep);
    buf.append(this.tconInfo.key.toRepr());
    if (this.ext) {
      buf.append("+");
    }
    if (this.params.length > 0) {
      buf.append(">");
    }
    return buf.toString();
  }
}
