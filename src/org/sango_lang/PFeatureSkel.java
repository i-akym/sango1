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
  PDefDict.DefDictGetter defDictGetter;
  Parser.SrcInfo srcInfo;
  PDefDict.FeatureProps featureProps;
  PTypeSkel[] params;  // empty array if no params

  private PFeatureSkel() {}

  public static PFeatureSkel create(PDefDict.DefDictGetter defDictGetter, Parser.SrcInfo srcInfo, PDefDict.FeatureProps featureProps, PTypeSkel[] params) {
    PFeatureSkel f = new PFeatureSkel();
    f.defDictGetter = defDictGetter;
    f.srcInfo = srcInfo;
    f.featureProps = featureProps;
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
      b = f.featureProps.key.equals(this.featureProps.key) && f.params.length == this.params.length;
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
    buf.append(this.featureProps.key.repr());
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
    buf.append(this.featureProps.key.repr());
    return buf.toString();
  }

  String reprSolo() {
    return "[ " + this.repr() + " ]";
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  void extractVars(java.util.List<PTypeVarSlot> extracted) {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].extractVars(extracted);
    }
  }

  PTypeSkel extractAnyInconcreteVar(java.util.List<PTypeVarSlot> givenTVarList) {
    PTypeSkel t = null;
// TODO
    // for (int i = 0; t == null && i < this.params.length; i++) {
      // t = this.params[i].extractAnyInconcreteVar(givenTVarList);
    // }
    return t;
  }

  boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings) {
    boolean b = false;
    for (int i = 0; !b && i < this.params.length; i++) {
      b = this.params[i].includesVar(varSlot, bindings);
    }
    return b;
  }

  PFeatureSkel resolveBindings(PTypeSkelBindings bindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      ps[i] = this.params[i].resolveBindings(bindings);
    }
    return create(this.defDictGetter, this.srcInfo, this.featureProps, ps);
  }

  PFeatureSkel instanciate(PTypeSkel.InstanciationContext context) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      ps[i] = this.params[i].instanciate(context);
    }
    return create(this.defDictGetter, this.srcInfo, this.featureProps, ps);
  }

  boolean accept(boolean bindsRef, PFeatureSkel feature, PTypeSkelBindings bindings) {
    boolean b = true;
    if (!(b = this.featureProps.key.equals(feature.featureProps.key))) {
      ;
    } else {
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].accept(PTypeSkel.EQUAL, bindsRef, feature.params[i], bindings);
      }
    }
    return b;
  }

  boolean acceptOne(/* int width, */ boolean bindsRef, PTypeSkel obj, PTypeSkelBindings bindings) {
    boolean b = false;
    if (obj instanceof PTypeRefSkel) {
      b = this.acceptOneTypeRef(/* width, */ bindsRef, (PTypeRefSkel)obj, bindings);
    } else if (obj instanceof PTypeVarSkel) {
      b = this.acceptOneVar(/* width, */ bindsRef, (PTypeVarSkel)obj, bindings);
    } else {
      throw new IllegalArgumentException("Unexpected type. " + obj);
    }
    return b;
  }

  boolean acceptOneTypeRef(/* int width, */ boolean bindsRef, PTypeRefSkel tr, PTypeSkelBindings bindings) {
    List features = tr.getFeatures();
    boolean b = false;
    for (int i = 0; i < features.features.length; i++) {  // search target
      PFeatureSkel f = features.features[i];
      if (f.featureProps.key.equals(this.featureProps.key)) {
        b = this.acceptOneTypeRefOne(/* width, */ bindsRef, f, bindings);
        break;
      }
    }
    return b;
  }

  boolean acceptOneTypeRefOne(/* int width, */ boolean bindsRef, PFeatureSkel feature, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PFeatureSkel#acceptOneTypeRefOne 1 "); /* System.out.print(width); System.out.print(" "); */ System.out.print(this); System.out.print(" "); System.out.print(feature); System.out.print(" "); System.out.println(bindings);
}
    return this.accept(bindsRef, feature, bindings);
  }

  boolean acceptOneVar(/* int width, */ boolean bindsRef, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PFeatureSkel[] features = tv.features.features;
    boolean b = false;
    for (int i = 0; i < features.length; i++) {  // search target
      PFeatureSkel f = features[i];
      if (f.featureProps.key.equals(this.featureProps.key)) {
        b = this.accept(bindsRef, f, bindings);
        break;
      }
    }
    return b;
  }

  // PTypeRefSkel calcTypeRefFeatureImpl(PTypeRefSkel tr, PTypeSkelBindings bindings) {
// /* DEBUG */ System.out.print("calc tr fxi "); System.out.print(this); System.out.print(tr); System.out.println(bindings);
    // PTypeSkelBindings b = bindings.copy();
    // PFeatureDef fd = this.featureProps.defGetter.getFeatureDef();
    // PTypeVarSkel obj = fd.getObjType();
// /* DEBUG */ System.out.print("obj "); System.out.println(obj);
    // PTypeVarSkel[] ps = fd.getParams();
// /* DEBUG */ System.out.print("params "); System.out.println(ps);
    // PTypeRefSkel impl = fd.getImplType();
// /* DEBUG */ System.out.print("impl "); System.out.println(impl);
    // b.bind(obj.varSlot, tr);
    // for (int i = 0; i < this.params.length; i++) {
      // b.bind(ps[i].varSlot, this.params[i]);
    // }
    // return (PTypeRefSkel)impl.instanciate(PTypeSkel.InstanciationContext.create(b));
  // }

  // PTypeRefSkel calcVarFeatureImpl(PTypeVarSkel tv, PTypeSkelBindings bindings) {
    // PTypeSkelBindings b = bindings.copy();
    // PFeatureDef fd = this.featureProps.defGetter.getFeatureDef();
    // PTypeVarSkel obj = fd.getObjType();
    // PTypeVarSkel[] ps = fd.getParams();
    // PTypeRefSkel impl = fd.getImplType();
    // b.bind(obj.varSlot, tv);
    // b.givenTVarList.add(tv.varSlot);
    // for (int i = 0; i < this.params.length; i++) {
      // b.bind(ps[i].varSlot, this.params[i]);
    // }
    // return (PTypeRefSkel)impl.instanciate(PTypeSkel.InstanciationContext.create(b));
  // }

  JoinResult join(/* int width, boolean bindsRef, */ PFeatureSkel f2, JoinResult res) {
    PTypeSkelBindings b = res.bindings;
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    boolean cont = true;
    for (int i = 0; cont && i < this.params.length; i++) {
      PTypeSkel.JoinResult sr = this.params[i].join2(PTypeSkel.EQUAL, /* bindsRef, */ f2.params[i], b);
      if (sr != null) {
        ps[i] = sr.joined;
        b = sr.bindings;
      } else {
        cont = false;
      }
    }
    return cont? res.add(create(this.defDictGetter, this.srcInfo, this.featureProps, ps), b): null;
  }

  MFeature toMType(PModule mod, java.util.List<PTypeVarSlot> slotList) {
    MFeature.Builder builder = MFeature.Builder.newInstance();
    builder.setModIndex(mod.modNameToModRefIndex(this.featureProps.key.modName));
    builder.setName(this.featureProps.key.idName);
    for (int i = 0; i < this.params.length; i++) {
      builder.addParam(this.params[i].toMType(mod, slotList));
    }
    return builder.create();
  }

  static class List {
    Parser.SrcInfo srcInfo;
    PFeatureSkel[] features;

    List() {}

    static List create(Parser.SrcInfo srcInfo, PFeatureSkel[] features) {
      List L = new List();
      L.srcInfo = srcInfo;
      L.features = features;
      return L;
    }

    static List createEmpty(Parser.SrcInfo srcInfo) {
      return create(srcInfo, new PFeatureSkel[0]);
    }

    boolean isEmpty() { return this.features.length == 0; }

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

    void extractVars(java.util.List<PTypeVarSlot> extracted) {
      for (int i = 0; i < this.features.length; i++) {
        this.features[i].extractVars(extracted);
      }
    }

    PTypeSkel extractAnyInconcreteVar(java.util.List<PTypeVarSlot> givenTVarList) {
      PTypeSkel t = null;
// TODO
      // for (int i = 0; null == null && i < this.features.length; i++) {
        // t = this.features[i].extractAnyInconcreteVar(givenTVarList);
      // }
      return t;
    }

    boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings) {
      boolean b = false;
      for (int i = 0; !b && i < this.features.length; i++) {
        b = this.features[i].includesVar(varSlot, bindings);
      }
      return b;
    }

    List instanciate(PTypeSkel.InstanciationContext context) {
      PFeatureSkel[] fs = new PFeatureSkel[this.features.length];
      for (int i = 0; i < this.features.length; i++) {
        fs[i] = this.features[i].instanciate(context);
      }
      return create(this.srcInfo, fs);
    }

    boolean accept(/* int width, */ boolean bindsRef, PTypeSkel obj, PTypeSkelBindings bindings) {
/* DEBUG */ if (PTypeGraph.DEBUG > 1) {
  System.out.print("PFeatureSkel.List#accept 1 "); /* System.out.print(width); System.out.print(" "); */ System.out.print(this); System.out.print(" "); System.out.print(obj); System.out.print(" "); System.out.println(bindings);
}
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].acceptOne(/* width, */ bindsRef, obj, bindings);
      }
      return b;
    }

    List merge(boolean bindsRef, List fs2, PTypeSkelBindings bindings) {
      java.util.List<PFeatureSkel> ff = new ArrayList<PFeatureSkel>();
      for (int i = 0; i < fs2.features.length; i++) {
        ff.add(fs2.features[i]);
      }
      ListBuilder builder = ListBuilder.newInstance(this.srcInfo);
      for (int i = 0; bindings != null && i < this.features.length; i++) {
        PFeatureSkel f = this.features[i];
        boolean stop = false;
        for (int j = 0; !stop && j < ff.size(); j++) {  // search target
          PFeatureSkel f2 = ff.get(j);
          if (f.featureProps.key.equals(f2.featureProps.key)) {
            if (f.accept(bindsRef, f2, bindings)) {
              builder.add(f);
              ff.remove(j);
            } else {
              bindings = null;
            }
            stop = true;
          }
        }
        if (bindings != null && !stop) {
          builder.add(f);
        }
      }
      if (bindings != null) {
        for (int j = 0; j < ff.size(); j++) {
          builder.add(ff.get(j));
        }
      }
      return (bindings != null)? builder.create(): null;
    }

    JoinResult joinList(/* int width, boolean bindsRef, */ List fs2, PTypeSkelBindings bindings) {
      JoinResult r = JoinResult.create(this.srcInfo, bindings);
      for (int i = 0; r != null && i < this.features.length; i++) {
        PFeatureSkel f = this.features[i];
        boolean found = false;
        for (int j = 0; !found && j < fs2.features.length; j++) {  // search target
          PFeatureSkel f2 = fs2.features[j];
          if (f.featureProps.key.equals(f2.featureProps.key)) {
            r = f.join(/* width, bindsRef, */ f2, r);
            found = true;
          }
        }
      }
      return r;
    }

    MFeature.List toMType(PModule mod, java.util.List<PTypeVarSlot> slotList) {
      java.util.List<MFeature> fs = new ArrayList<MFeature>();
      for (int i = 0; i < this.features.length; i++) {
        fs.add(this.features[i].toMType(mod, slotList));
      }
      return MFeature.List.create(fs);
    }
  }

  static class JoinResult {
    Parser.SrcInfo srcInfo;
    java.util.List<PFeatureSkel> staged;
    PTypeSkelBindings bindings;

    static JoinResult create(Parser.SrcInfo srcInfo, PTypeSkelBindings bindings) {
      JoinResult r = new JoinResult();
      r.srcInfo = srcInfo;
      r.staged = new java.util.ArrayList<PFeatureSkel>();
      r.bindings = bindings;
      return r;
    }

    private JoinResult() {}

    JoinResult add(PFeatureSkel f, PTypeSkelBindings bindings) {
      this.staged.add(f);
      this.bindings = bindings;
      return this;
    }

    List pack() {
      return List.create(this.srcInfo, this.staged.toArray(new PFeatureSkel[this.staged.size()]));
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
