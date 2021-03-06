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
    for (int i = 0; i < t.params.length; i++) {
      PVarDef d = var.varSlot.varDef;
      PVarSlot s = PVarSlot.create(d);
      t.params[i] = PTypeVarSkel.create((d != null)? d.getSrcInfo(): null, (d != null)? d.scope: null, s);
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

  public PDefDict.TconInfo getTconInfo() {
    if (this.tconInfo == null) {
      throw new IllegalStateException("Tcon info not set up.");
    }
    return this.tconInfo;
  }

  public PTypeSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].instanciate(iBindings);
    }
    return create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
    // PTypeRefSkel tr = new PTypeRefSkel();
    // tr.srcInfo = this.srcInfo;
    // tr.tconInfo = this.tconInfo;
    // tr.ext = this.ext;
    // tr.params = ps;
    // return tr;
  }

  public PTypeRefSkel resolveBindings(PTypeSkelBindings bindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].resolveBindings(bindings);
    }
    return create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
    // PTypeRefSkel tr = new PTypeRefSkel();
    // tr.srcInfo = this.srcInfo;
    // tr.tconInfo = this.tconInfo;
    // tr.ext = this.ext;
    // tr.params = ps;
    // return tr;
  }

  public PTypeSkelBindings applyTo(PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    PTypeSkel t = type.resolveBindings(trialBindings);
    if (t instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else {
      b = this.applyTo2(t, trialBindings);
    }
    return b;
  }

  public PTypeSkelBindings applyTo2(PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply2 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (type instanceof PTypeRefSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply2 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = this.applyTo3((PTypeRefSkel)type, trialBindings);
    } else {  // 'type' is guaranteed to be unbound by applyTo
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply2 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (trialBindings.isGivenTvar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply2 2-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply2 2-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        b = this.applyTo(tv.castTo(this, trialBindings), trialBindings);
      }
    }
    return b;
  }

  PTypeSkelBindings applyTo3(PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
    if (this.params.length != tr.params.length) { return null; }  // apparently differs; including fun, tuple, extended
    return (this.tconInfo.key.modName.equals(Module.MOD_LANG) && this.tconInfo.key.tcon.equals(Module.TCON_FUN))?
      this.applyTo3Fun(tr, trialBindings):
      this.applyTo3Other(tr, trialBindings);
  }

  PTypeSkelBindings applyTo3Fun(PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
     PTypeSkelBindings b;
    if (!this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3f-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      return null;
    }
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3f-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
    b = trialBindings;
    for (int i = 0; b != null && i < this.params.length -1; i++) {
      b = tr.params[i].applyTo(this.params[i], b);
    }
    if (b != null) {
      b = this.params[this.params.length -1 ].applyTo(tr.params[this.params.length -1 ], b);
    }
    return b;
  }

  PTypeSkelBindings applyTo3Other(PTypeRefSkel tr, PTypeSkelBindings trialBindings) throws CompileException {
     PTypeSkelBindings b;
    if (this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3o-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      if (this.ext || !tr.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3o-1-1 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;
        for (int i = 0; b != null && i < this.params.length; i++) {
          b = this.params[i].applyTo(tr.params[i], b);
        }
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3o-1-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
        b = null;
      }
    } else if (this.ext) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3o-2 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
       b = isTconOfExtensionOf(tr.tconInfo, this.tconInfo, this.defDictGetter)? trialBindings: null;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#apply 3o-3 "); System.out.print(this); System.out.print(" "); System.out.print(tr); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    }
    return b;
  }

  static boolean isTconOfExtensionOf(PDefDict.TconInfo ti0, PDefDict.TconInfo ti1, PDefDict.DefDictGetter defDictGetter) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
/* DEBUG */ if (ti0 == null) { System.out.println("no ti0 " + ti1); }
/* DEBUG */ if (ti0.props == null) { System.out.println("no tcon props " + ti0); }
}
    PDataDef dd = ti0.props.defGetter.getDataDef();
    PDefDict.TconKey btk = (dd != null)? dd.getBaseTconKey(): null;
    boolean b = false;
    while (!b && btk != null) {
      if (btk.equals(ti1.key)) {
        b = true;
      } else {
        PDefDict.TconInfo bti = resolveTcon(btk, defDictGetter);
        dd = bti.props.defGetter.getDataDef();
        btk = (dd != null)? dd.getBaseTconKey(): null;
      }
    }
    return b;
  }

  static PDefDict.TconInfo resolveTcon(PDefDict.TconKey key, PDefDict.DefDictGetter defDictGetter) throws CompileException {
    return defDictGetter.getReferredDefDict(key.modName).resolveTcon(
      key.tcon,
      PTypeId.SUBCAT_DATA + PTypeId.SUBCAT_EXTEND + PTypeId.SUBCAT_ALIAS,
      Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE);
  }

  public boolean includesVar(PVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    for (int i = 0; !b && i < this.params.length; i++) {
      b = this.params[i].includesVar(varSlot, bindings);
    }
    return b;
  }

  public PVarSlot getVarSlot() { return null; }

  public PTypeSkel join(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel tt;
    PTypeSkel t = type.resolveBindings(bindings);
    if (t instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      tt = t.join2(this, bindings);
    } else {
      tt = this.join2(t, bindings);
    }
    return tt;
  }

  public PTypeSkel join2(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
    PTypeSkel t;
    if (type instanceof PTypeVarSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      t = type.join2(this, bindings);
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
      PTypeRefSkel tr = (PTypeRefSkel)type;
      if (this.params.length != tr.params.length) { return null; }  // apparently differs; including fun, tuple, extended
      if (this.tconInfo.key.equals(tr.tconInfo.key)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 3-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        PTypeSkel[] ps = new PTypeSkel[this.params.length];
        for (int i = 0; i < this.params.length; i++) {
          ps[i] = this.params[i].join(tr.params[i], bindings);
          if (ps[i] == null) { return null; }
        }
        t = create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext | tr.ext, ps);
        // PTypeRefSkel r = new PTypeRefSkel();
        // r.tconInfo = this.tconInfo;
        // r.ext = this.ext | tr.ext;
        // r.params = ps;
        // t = r;
      } else if (isTconOfExtensionOf(this.tconInfo, tr.tconInfo, this.defDictGetter)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 3-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        PTypeSkel[] ps = new PTypeSkel[this.params.length];
        for (int i = 0; i < this.params.length; i++) {
          ps[i] = this.params[i].join(tr.params[i], bindings);
          if (ps[i] == null) { return null; }
        }
        t = create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
        // PTypeRefSkel r = new PTypeRefSkel();
        // r.tconInfo = this.tconInfo;
        // r.ext = this.ext;
        // r.params = ps;
        // t = r;
      } else if (isTconOfExtensionOf(tr.tconInfo, this.tconInfo, this.defDictGetter)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 3-3 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        PTypeSkel[] ps = new PTypeSkel[this.params.length];
        for (int i = 0; i < this.params.length; i++) {
          ps[i] = this.params[i].join(tr.params[i], bindings);
          if (ps[i] == null) { return null; }
        }
        t = create(this.defDictGetter, this.srcInfo, this.tconInfo, this.ext, ps);
        // PTypeRefSkel r = new PTypeRefSkel();
        // r.tconInfo = tr.tconInfo;
        // r.ext = tr.ext;
        // r.params = ps;
        // t = r;
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PTypeRefSkel#join2 3-4 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
}
        t = null;
      }
    }
    return t;
  }

  static boolean willNotReturn(PTypeSkel type) {
    return isLangType(type, "_");
  }

  static boolean isList(PTypeSkel type) {
    return isLangType(type, "list");
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

  public MType toMType(PModule mod, List<PVarSlot> slotList) {
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

  public List<PVarSlot> extractVars(List<PVarSlot> alreadyExtracted) {
    List<PVarSlot> newlyExtracted = new ArrayList<PVarSlot>();
    for (int i = 0; i < this.params.length; i++) {
      List<PVarSlot> justExtracted = this.params[i].extractVars(alreadyExtracted);
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

  public GFlow.Node setupFlow(GFlow flow, PScope scope, PTypeSkelBindings bindings) {
    GFlow.DataConstrNode node = flow.createNodeForDataConstrBody(
      this.srcInfo, scope.theMod.modNameToModRefIndex(Module.MOD_LANG), "type$", "type", 0);
    GFlow.Node n = flow.createNodeForEmptyListBody(this.srcInfo);
    for (int i = this.params.length - 1; i >= 0; i--) {
      GFlow.ListNode ln = flow.createNodeForList(this.srcInfo);
      ln.addChild(this.params[i].resolveBindings(bindings).setupFlow(flow, scope, bindings));
      ln.addChild(n);
      n = ln;
    }
    node.addChild(n);
    node.addChild(flow.createNodeForCstr(this.srcInfo, this.tconInfo.key.modName));
    node.addChild(flow.createNodeForCstr(this.srcInfo, this.tconInfo.key.tcon));
    return node;
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
