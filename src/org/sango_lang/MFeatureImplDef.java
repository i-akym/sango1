/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2023 AKIYAMA Isao                                         *
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MFeatureImplDef {
  MDataDef theDataDef;
  int providerModIndex;  // needed?
  String providerFun;  // needed?
  String getter;
  MFeature provided;

  private MFeatureImplDef() {}

  void setDataDef(MDataDef dataDef) {
    this.theDataDef = dataDef;
  }

  static class Builder {
    MFeatureImplDef featureImplDef;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.featureImplDef = new MFeatureImplDef();
    }

    void setProvider(int modIndex, String fun, String getter) {
      this.featureImplDef.providerModIndex = modIndex;
      this.featureImplDef.providerFun = fun;
      this.featureImplDef.getter = getter;
    }

    void setProvided(MFeature provided) {
      this.featureImplDef.provided = provided;
    }

    MFeatureImplDef create() {
      return this.featureImplDef;
    }
  }

  public Element externalize(Document doc) {
    Element featureImplDefNode = doc.createElement(Module.TAG_FEATURE_IMPL);
    featureImplDefNode.setAttribute(Module.ATTR_MOD_INDEX, Integer.toString(this.providerModIndex));
    featureImplDefNode.setAttribute(Module.ATTR_NAME, this.providerFun);
    featureImplDefNode.setAttribute(Module.ATTR_GETTER, this.getter);
    featureImplDefNode.appendChild(this.provided.externalize(doc));
    return featureImplDefNode;
  }

  void checkCompat(Module.ModTab modTab, MFeatureImplDef fid, Module.ModTab defModTab) throws FormatException {
    StringBuffer emsg;
    if (!this.provided.isCompatible(modTab, fid.provided, defModTab)) {  // ok?
      emsg = new StringBuffer();
      emsg.append("Feature implementation mismatch - type: ");
      emsg.append(this.provided);  // temporal
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }

// HERE
  }
}
