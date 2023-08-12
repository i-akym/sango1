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

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  PFeatureSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      ps[i] = this.params[i].instanciate(iBindings);
    }
    return create(this.defDictGetter, this.srcInfo, this.featureProps, ps);
  }

  boolean acceptVarOne(int width, boolean bindsRef, PTypeVarSkel theVar, PTypeSkel obj, PTypeSkelBindings bindings) {
    boolean b = false;
    if (obj instanceof PTypeRefSkel) {
      b = this.acceptVarOneTypeRef(width, bindsRef, theVar, (PTypeRefSkel)obj, bindings);
    } else if (obj instanceof PTypeVarSkel) {
      b = this.acceptVarOneVar(width, bindsRef, theVar, (PTypeVarSkel)obj, bindings);
    } else {
      throw new IllegalArgumentException("Unexpected type. " + obj);
    }
    return b;
  }

  boolean acceptVarOneTypeRef(int width, boolean bindsRef, PTypeVarSkel theVar, PTypeRefSkel tr, PTypeSkelBindings bindings) {
    List features = tr.getFeatures();
    boolean b = false;
    for (int i = 0; i < features.features.length; i++) {  // search target
      PFeatureSkel f = features.features[i];
      if (f.featureProps.key.equals(this.featureProps.key)) {
        b = this.acceptVarOneTypeRefOne(width, bindsRef, theVar, tr, f, bindings);
        break;
      }
    }
    return b;
  }

  boolean acceptVarOneTypeRefOne(int width, boolean bindsRef, PTypeVarSkel theVar, PTypeRefSkel tr, PFeatureSkel feature, PTypeSkelBindings bindings) {
    return this.calcVarFeatureImpl(theVar, bindings).
      accept(width, bindsRef, feature.calcTypeRefFeatureImpl(tr, bindings), bindings);
  }

  boolean acceptVarOneVar(int width, boolean bindsRef, PTypeVarSkel theVar, PTypeVarSkel tv, PTypeSkelBindings bindings) {
    throw new RuntimeException("PFeatureSkel#acceptVarOneVar not implemented.");
  }

  PTypeRefSkel calcTypeRefFeatureImpl(PTypeRefSkel tr, PTypeSkelBindings bindings) {
    PFeatureDef fd = this.featureProps.defGetter.getFeatureDef();
    PTypeVarSkel obj = fd.getObjType();
    PTypeVarSkel[] ps = fd.getParams();
    PTypeRefSkel impl = fd.getImplType();
    bindings.bind(obj.varSlot, tr);
    for (int i = 0; i < this.params.length; i++) {
      bindings.bind(ps[i].varSlot, this.params[i]);
    }
    return (PTypeRefSkel)impl.instanciate(PTypeSkel.InstanciationBindings.create(bindings));
  }

  PTypeRefSkel calcVarFeatureImpl(PTypeVarSkel tv, PTypeSkelBindings bindings) {
    PFeatureDef fd = this.featureProps.defGetter.getFeatureDef();
    PTypeVarSkel obj = fd.getObjType();
    PTypeVarSkel[] ps = fd.getParams();
    PTypeRefSkel impl = fd.getImplType();
    bindings.bind(obj.varSlot, tv);
    for (int i = 0; i < this.params.length; i++) {
      bindings.bind(ps[i].varSlot, this.params[i]);
    }
    return (PTypeRefSkel)impl.instanciate(PTypeSkel.InstanciationBindings.create(bindings));
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

    List instanciate(PTypeSkel.InstanciationBindings iBindings) {
      PFeatureSkel[] fs = new PFeatureSkel[this.features.length];
      for (int i = 0; i < this.features.length; i++) {
        fs[i] = this.features[i].instanciate(iBindings);
      }
      return create(this.srcInfo, fs);
    }

    boolean acceptVar(int width, boolean bindsRef, PTypeVarSkel theVar, PTypeSkel obj, PTypeSkelBindings bindings) {
      boolean b = true;
      for (int i = 0; b && i < this.features.length; i++) {
        b = this.features[i].acceptVarOne(width, bindsRef, theVar, obj, bindings);
      }
      return b;
    }

    MFeature.List toMType(PModule mod, java.util.List<PTypeVarSlot> slotList) {
      java.util.List<MFeature> fs = new ArrayList<MFeature>();
      for (int i = 0; i < this.features.length; i++) {
        fs.add(this.features[i].toMType(mod, slotList));
      }
      return MFeature.List.create(fs);
    }
  }
}
