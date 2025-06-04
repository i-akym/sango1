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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MDataDef implements Module.Elem {
  String tcon;
  Module.Availability availability;
  Module.Access acc;
  MType.ParamDef[] params;
  int baseModIndex;  //  = 0 -> org def,  > 0 -> ext def
  String baseTcon;
  MConstrDef[] constrs;
  MFeatureImplDef[] featureImpls;

  private MDataDef() {}

  static class Builder {
    MDataDef dataDef;
    List<MType.ParamDef> paramList;
    List<MConstrDef> constrList;
    List<MFeatureImplDef> featureImplList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.dataDef = new MDataDef();
      this.dataDef.availability = Module.AVAILABILITY_GENERAL;
      this.constrList = new ArrayList<MConstrDef>();
      this.featureImplList = new ArrayList<MFeatureImplDef>();
    }

    void prepareForParams() {
      this.paramList = new ArrayList<MType.ParamDef>();
    }

    void setTcon(String tcon) {
      this.dataDef.tcon = tcon;
    }

    void setAvailability(Module.Availability availability) {
      this.dataDef.availability = availability;
    }

    void setAcc(Module.Access acc) {
      this.dataDef.acc = acc;
    }

    void addParam(MType.ParamDef param) {
      this.paramList.add(param);
    }

    void setBaseModIndex(int baseModIndex) {
      this.dataDef.baseModIndex = baseModIndex;
    }

    void setBaseTcon(String baseTcon) {
      this.dataDef.baseTcon = baseTcon;
    }

    void addConstrDef(MConstrDef constrDef) {
      constrDef.setDataDef(this.dataDef);
      this.constrList.add(constrDef);
    }

    void addFeatureImplDef(MFeatureImplDef featureImplDef) {
      featureImplDef.setDataDef(this.dataDef);
      this.featureImplList.add(featureImplDef);
    }

    MDataDef create() {
      this.dataDef.params = (this.paramList != null)?
        this.paramList.toArray(new MType.ParamDef[this.paramList.size()]):
        null;
      this.dataDef.constrs = this.constrList.toArray(new MConstrDef[this.constrList.size()]);
      this.dataDef.featureImpls = this.featureImplList.toArray(new MFeatureImplDef[this.featureImplList.size()]);
      return this.dataDef;
    }
  }

  public Element externalize(Document doc) {
    Element dataDefNode = doc.createElement(Module.TAG_DATA_DEF);
    dataDefNode.setAttribute(Module.ATTR_TCON, this.tcon);
    if (this.availability != Module.AVAILABILITY_GENERAL) {
      dataDefNode.setAttribute(Module.ATTR_AVAILABILITY, Module.reprOfAvailability(this.availability));
    }
    if (this.acc != Module.ACC_PRIVATE) {
      dataDefNode.setAttribute(Module.ATTR_ACC, Module.reprOfAcc(this.acc));
    }
    if (this.baseModIndex > 0) {
      dataDefNode.setAttribute(Module.ATTR_BASE_MOD_INDEX, Integer.toString(this.baseModIndex));
    }
    if (this.baseTcon != null) {
      dataDefNode.setAttribute(Module.ATTR_BASE_TCON, this.baseTcon);
    }
    if (this.params.length > 0) {
      Element paramsNode = doc.createElement(Module.TAG_PARAMS);
      for (int i = 0; i < this.params.length; i++) {
        paramsNode.appendChild(this.params[i].externalize(doc));
      }
      dataDefNode.appendChild(paramsNode);
    }
    if (this.constrs != null) {  // needed?
      for (int i = 0; i < this.constrs.length; i++) {
        dataDefNode.appendChild(this.externalizeConstrDef(doc, this.constrs[i]));
      }
    }
    for (int i = 0; i < this.featureImpls.length; i++) {
      dataDefNode.appendChild(this.externalizeFeatureImplDef(doc, this.featureImpls[i]));
    }
    return dataDefNode;
  }

  Element externalizeConstrDef(Document doc, MConstrDef constrDef) {
    return constrDef.externalize(doc);
  }

  Element externalizeFeatureImplDef(Document doc, MFeatureImplDef featureImplDef) {
    return featureImplDef.externalize(doc);
  }

  void checkCompat(Module.ModTab modTab, MDataDef dd, Module.ModTab defModTab) throws FormatException {
    StringBuffer emsg;
    if (Module.equalOrMoreOpenAcc(dd.acc, this.acc)) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible access mode - type: ");
      emsg.append(dd.tcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    if (dd.params == null && this.params == null) {
      ;
    } else if (dd.params != null && this.params != null && dd.params.length == this.params.length) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible parameter count - type: ");
      emsg.append(dd.tcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    for (int i = 0; i < this.constrs.length; i++) {
      MConstrDef cd = this.constrs[i];
      cd.checkCompat(modTab, dd, defModTab);
    }
  }
}
