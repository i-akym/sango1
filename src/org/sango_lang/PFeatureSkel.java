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
  // PTypeId fname;
  PTypeSkel[] params;  // empty array if no params

  private PFeatureSkel() {}

  public static PFeatureSkel create(PDefDict.DefDictGetter defDictGetter, Parser.SrcInfo srcInfo, PDefDict.FeatureProps featureProps, PTypeSkel[] params) {
    PFeatureSkel f = new PFeatureSkel();
    f.defDictGetter = defDictGetter;
    f.srcInfo = srcInfo;
    f.featureProps = featureProps;
    // f.fname = fname;
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

    MFeature.List toMType(PModule mod, java.util.List<PTypeVarSlot> slotList) {
      java.util.List<MFeature> fs = new ArrayList<MFeature>();
      for (int i = 0; i < this.features.length; i++) {
        fs.add(this.features[i].toMType(mod, slotList));
      }
      return MFeature.List.create(fs);
    }
  }
}
