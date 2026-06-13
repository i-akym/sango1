/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2023  AKIYAMA Isao                                        *
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

public class PFeatureSkel {
  Compiler theCompiler;
  Parser.SrcInfo srcInfo;
  PDefDict.IdKey fnameKey;
  PTypeSkel[] params;  // empty array if no params

  private PFeatureSkel() {}

  public static PFeatureSkel create(Compiler theCompiler, Parser.SrcInfo srcInfo, PDefDict.IdKey fnameKey, PTypeSkel[] params) {
    PFeatureSkel f = new PFeatureSkel();
    f.theCompiler = theCompiler;
    f.srcInfo = srcInfo;
    f.fnameKey = fnameKey;
    f.params = params;
    return f;
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof PFeatureSkel)) {
      b = false;
    } else {
      PFeatureSkel f = (PFeatureSkel)o;
      b = f.fnameKey.equals(this.fnameKey) && f.params.length == this.params.length;
      for (int i = 0; b && i < f.params.length; i++) {
        b = f.params[i].equals(this.params[i]);
      }
    }
    return b;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    if (this.srcInfo != null) {
      buf.append("featureskel[src=");
      buf.append(this.srcInfo);
      buf.append(",");
    }
    String sep = "";
    buf.append("[");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i]);
      sep = " ";
    }
    buf.append(sep);
    buf.append(this.fnameKey.repr());
    buf.append("]");
    if (this.srcInfo != null) {
      buf.append("]");
    }
    return buf.toString();
  }

  String repr() {
    StringBuffer buf = new StringBuffer();
    String sep = "";
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i].repr());
      sep = " ";
    }
    buf.append(sep);
    buf.append(this.fnameKey.repr());
    return buf.toString();
  }

  String reprSolo() {
    return "[ " + this.repr() + " ]";
  }

  PFeatureSkel normalize() throws CompileException {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].normalize();
    }
    return create(this.theCompiler, this.srcInfo, this.fnameKey, ps);
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  PFeatureDef getMyFeatureDef() {
    PFeatureDef fd = null;
    try {
      fd = this.theCompiler.defDict.getFeatureDef(null, this.fnameKey);
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpected error. " + ex.toString());
    }
    return fd;
  }

  PTypeRefSkel getImplTypeSkel(PTypeSkel obj, PTypeSkel.Bindings bindings) {
    PFeatureDef fd = this.getMyFeatureDef();
    PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(bindings);
    bindings.bind((PTypeVarSkel)fd.getObjType().instanciate(ic), obj);
    PFeatureSkel sig = fd.getFeatureSig();
    for (int i = 0; i < sig.params.length; i++) {
      bindings.bind((PTypeVarSkel)sig.params[i].instanciate(ic), this.params[i]);
    }
    return (PTypeRefSkel)fd.getImplType().resolveBindings(bindings);
  }

  void extractVars(java.util.List<PTypeVarSlot> extracted) {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].extractVars(extracted);
    }
  }

  boolean isConcrete() {
    boolean b = true;
    for (int i = 0; b && i < this.params.length; i++) {
      b = this.params[i].isConcrete();
    }
    return b;
  }

  // PTypeSkel extractAnyInconcreteVar(/* java.util.List<PTypeVarSlot> givenTVarList */) {
    // PTypeSkel t = null;
// // TODO
    // // for (int i = 0; t == null && i < this.params.length; i++) {
      // // t = this.params[i].extractAnyInconcreteVar(givenTVarList);
    // // }
    // return t;
  // }

  boolean includesVar(PTypeVarSlot varSlot, PTypeSkel.Bindings bindings) {
    boolean b = false;
    for (int i = 0; !b && i < this.params.length; i++) {
      b = this.params[i].includesVar(varSlot, bindings);
    }
    return b;
  }

  PFeatureSkel resolveBindings(PTypeSkel.Bindings bindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      ps[i] = this.params[i].resolveBindings(bindings);
    }
    return create(this.theCompiler, this.srcInfo, this.fnameKey, ps);
  }

  PFeatureSkel instanciate(PTypeSkel.InstanciationContext context) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      ps[i] = this.params[i].instanciate(context);
    }
    return create(this.theCompiler, this.srcInfo, this.fnameKey, ps);
  }

  boolean accept(PFeatureSkel feature, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b = true;
    if (!(b = this.fnameKey.equals(feature.fnameKey))) {
      ;
    } else {
      PFeatureDef fd = this.getMyFeatureDef();
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].accept(feature.params[i], bindings);
      }
    }
    return b;
  }

  boolean acceptList(List fs, PTypeSkel.Bindings bindings) throws CompileException {
    int len = (fs == null)? 0: fs.features.length;
    boolean b = false;
    for (int i = 0; !b && i < len; i++) {
      b = this.accept(fs.features[i], bindings);
    }
    return b;
  }

  boolean acceptObj(PTypeSkel obj, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b = false;
    if (obj instanceof PTypeRefSkel) {
      b = this.acceptTypeRef((PTypeRefSkel)obj, bindings);
    } else if (obj instanceof PTypeVarSkel) {
      b = this.acceptVar((PTypeVarSkel)obj, bindings);
    } else {
      throw new IllegalArgumentException("Unexpected type. " + obj);
    }
    return b;
  }

  boolean acceptTypeRef(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b;
    List fs = tr.getFeatures();
    b = this.acceptList(fs, bindings);
    return b;
  }

  boolean acceptVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    return this.acceptList(tv.features, bindings);
  }

  boolean require(PFeatureSkel feature, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b = true;
    if (!(b = this.fnameKey.equals(feature.fnameKey))) {
      ;
    } else {
      PFeatureDef fd = this.getMyFeatureDef();
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].require(feature.params[i], bindings);
      }
    }
    return b;
  }

  boolean requireList(List fs, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b = false;
    for (int i = 0; !b && i < fs.features.length; i++) {
      b = this.require(fs.features[i], bindings);
    }
    return b;
  }

  boolean requireObj(PTypeSkel obj, PTypeSkel.Bindings bindings) throws CompileException {
    boolean b = false;
    if (obj instanceof PTypeRefSkel) {
      b = this.requireTypeRef((PTypeRefSkel)obj, bindings);
    } else if (obj instanceof PTypeVarSkel) {
      b = this.requireVar((PTypeVarSkel)obj, bindings);
    } else {
      throw new IllegalArgumentException("Unexpected type. " + obj);
    }
    return b;
  }

  boolean requireTypeRef(PTypeRefSkel tr, PTypeSkel.Bindings bindings) throws CompileException {
    List fs = tr.getFeatures();
    boolean b = this.requireList(fs, bindings);
    return b;
  }

  boolean requireVar(PTypeVarSkel tv, PTypeSkel.Bindings bindings) throws CompileException {
    return this.requireList(tv.features, bindings);
  }

  JoinResult join(PFeatureSkel f2, JoinResult res) throws CompileException {
    PTypeSkel.Bindings b = res.bindings;
    PFeatureDef fd = this.theCompiler.defDict.getFeatureDef(null, this.fnameKey);
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    boolean cont = true;
    for (int i = 0; cont && i < this.params.length; i++) {
      PTypeSkel.JoinResult sr = this.params[i].join2(f2.params[i], b);
      if (sr != null) {
        ps[i] = sr.joined;
        b = sr.bindings;
      } else {
        cont = false;
      }
    }
    return cont? res.add(create(this.theCompiler, this.srcInfo, this.fnameKey, ps), b): null;
  }

  MFeature toMType(PModule mod, Module.Builder modBuilder, boolean inReferredDef, java.util.List<PTypeVarSlot> slotList) {
    MFeature.Builder builder = MFeature.Builder.newInstance();
    builder.setModIndex(modBuilder.modNameToModIndex(this.fnameKey.modName));
    // builder.setModIndex(mod.modNameToModRefIndex(inReferredDef, this.fnameKey.modName));
    builder.setName(this.fnameKey.idName);
    for (int i = 0; i < this.params.length; i++) {
      builder.addParam(this.params[i].toMType(mod, modBuilder, inReferredDef, slotList));
    }
    return builder.create();
  }

  static class List {
    Parser.SrcInfo srcInfo;
    PFeatureSkel[] features;

    List() {}

    static List create(Parser.SrcInfo srcInfo, PFeatureSkel[] features) {
// /* DEBUG TRAP */ if (features.length == 0) { throw new RuntimeException("empty feature list"); }
      List L = new List();
      L.srcInfo = srcInfo;
      L.features = features;
      return L;
    }

    // static List createEmpty(Parser.SrcInfo srcInfo) {
      // return create(srcInfo, new PFeatureSkel[0]);
    // }

    // boolean isEmpty() { return this.features.length == 0; }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      if (this.srcInfo != null) {
        buf.append("featureskel[src=");
        buf.append(this.srcInfo);
        buf.append(",");
      }
      String sep = "";
      buf.append("[");
      for (int i = 0; i < this.features.length; i++) {
        buf.append(sep);
        buf.append(this.features[i]);
        sep = " ";
      }
      buf.append("]");
      if (this.srcInfo != null) {
        buf.append("]");
      }
      return buf.toString();
    }

    PTypeSkel.Repr repr() {
      PTypeSkel.Repr r = PTypeSkel.Repr.create();
      StringBuffer buf = new StringBuffer();
      String sep = "";
      buf.append("[ ");
      for (int i = 0; i < this.features.length; i++) {
        buf.append(sep);
        buf.append(this.features[i].repr());
        sep = ", ";
      }
      buf.append(" ]");
      r.add(buf.toString());
      return r;
    }

    List normalize() throws CompileException {
      PFeatureSkel[] L = new PFeatureSkel[this.features.length];
      for (int i = 0; i < L.length; i++) {
        L[i] = this.features[i].normalize();
      }
      return create(this.srcInfo, L);
    }

    void extractVars(java.util.List<PTypeVarSlot> extracted) {
      for (int i = 0; i < this.features.length; i++) {
        this.features[i].extractVars(extracted);
      }
    }

    boolean isConcrete() {
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].isConcrete();
      }
      return b;
    }

    // PTypeSkel extractAnyInconcreteVar(/* java.util.List<PTypeVarSlot> givenTVarList */) {
      // PTypeSkel t = null;
// // TODO
      // // for (int i = 0; null == null && i < this.features.length; i++) {
        // // t = this.features[i].extractAnyInconcreteVar(givenTVarList);
      // // }
      // return t;
    // }

    boolean includesVar(PTypeVarSlot varSlot, PTypeSkel.Bindings bindings) {
      boolean b = false;
      for (int i = 0; !b && i < this.features.length; i++) {
        b = this.features[i].includesVar(varSlot, bindings);
      }
      return b;
    }

    List resolveBindings(PTypeSkel.Bindings bindings) {
      PFeatureSkel[] fs = new PFeatureSkel[this.features.length];
      for (int i = 0; i < this.features.length; i++) {
        fs[i] = this.features[i].resolveBindings(bindings);
      }
      return create(this.srcInfo, fs);
    }

    List instanciate(PTypeSkel.InstanciationContext context) {
      PFeatureSkel[] fs = new PFeatureSkel[this.features.length];
      for (int i = 0; i < this.features.length; i++) {
        fs[i] = this.features[i].instanciate(context);
      }
      return create(this.srcInfo, fs);
    }

    boolean acceptList(List fs, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PFeatureSkel.List#acceptList A "); System.out.print(this); System.out.print(" "); System.out.print(fs); System.out.print(" "); System.out.println(bindings);
}
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].acceptList(fs, bindings);
      }
      return b;
    }

    boolean acceptObj(PTypeSkel obj, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PFeatureSkel.List#acceptObj A "); System.out.print(this); System.out.print(" "); System.out.print(obj); System.out.print(" "); System.out.println(bindings);
}
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].acceptObj(obj, bindings);
      }
      return b;
    }

    boolean requireList(List fs, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PFeatureSkel.List#requireList A "); System.out.print(this); System.out.print(" "); System.out.print(fs); System.out.print(" "); System.out.println(bindings);
}
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].requireList(fs, bindings);
      }
      return b;
    }

    boolean requireObj(PTypeSkel obj, PTypeSkel.Bindings bindings) throws CompileException {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PFeatureSkel.List#requireObj 1 "); System.out.print(this); System.out.print(" "); System.out.print(obj); System.out.print(" "); System.out.println(bindings);
}
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].requireObj(obj, bindings);
      }
      return b;
    }

    JoinResult joinList(List fs2, PTypeSkel.Bindings bindings) throws CompileException {
      JoinResult r = JoinResult.create(this.srcInfo, bindings);
      for (int i = 0; r != null && i < this.features.length; i++) {
        PFeatureSkel f = this.features[i];
        boolean found = false;
        for (int j = 0; !found && j < fs2.features.length; j++) {  // search target
          PFeatureSkel f2 = fs2.features[j];
          if (f.fnameKey.equals(f2.fnameKey)) {
            r = f.join(f2, r);
            found = true;
          }
        }
      }
      return r;
    }

    MFeature.List toMType(PModule mod, Module.Builder modBuilder, boolean inReferredDef, java.util.List<PTypeVarSlot> slotList) {
      java.util.List<MFeature> fs = new ArrayList<MFeature>();
      for (int i = 0; i < this.features.length; i++) {
        fs.add(this.features[i].toMType(mod, modBuilder, inReferredDef, slotList));
      }
      return MFeature.List.create(fs);
    }
  }

  static class JoinResult {
    Parser.SrcInfo srcInfo;
    java.util.List<PFeatureSkel> staged;
    PTypeSkel.Bindings bindings;

    static JoinResult create(Parser.SrcInfo srcInfo, PTypeSkel.Bindings bindings) {
      JoinResult r = new JoinResult();
      r.srcInfo = srcInfo;
      r.staged = new java.util.ArrayList<PFeatureSkel>();
      r.bindings = bindings;
      return r;
    }

    private JoinResult() {}

    JoinResult add(PFeatureSkel f, PTypeSkel.Bindings bindings) {
      this.staged.add(f);
      this.bindings = bindings;
      return this;
    }

    List pack() {
      return (this.staged.size() > 0)? 
        List.create(this.srcInfo, this.staged.toArray(new PFeatureSkel[this.staged.size()])):
        null;

      // return List.create(this.srcInfo, this.staged.toArray(new PFeatureSkel[this.staged.size()]));
    }
  }

  static class ListBuilder {
    Parser.SrcInfo srcInfo;
    java.util.List<PFeatureSkel> staged;

    static ListBuilder newInstance(Parser.SrcInfo srcInfo) {
      ListBuilder b = new ListBuilder();
      b.srcInfo = srcInfo;
      b.staged = new ArrayList<PFeatureSkel>();
      return b;
    }

    void add(PFeatureSkel feature) {
      this.staged.add(feature);
    }

    List create() {
      return List.create(this.srcInfo, this.staged.toArray(new PFeatureSkel[this.staged.size()]));
    }
  }
}
