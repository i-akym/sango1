/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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

public class MFeatureDef implements Module.Elem {
  String name;
  int availability;
  int acc;
  MTypeVar[] params;
  MTypeVar obj;
  MTypeRef impl;

  private MFeatureDef() {}

  static class Builder {
    MFeatureDef featureDef;
    List<MTypeVar> paramList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.featureDef = new MFeatureDef();
      this.paramList = new ArrayList<MTypeVar>();
    }

    void setName(String name) {
      this.featureDef.name = name;
    }

    void setAvailability(int availability) {
      this.featureDef.availability = availability;
    }

    void setAcc(int acc) {
      this.featureDef.acc = acc;
    }

    void addParam(MTypeVar param) {
      this.paramList.add(param);
    }

    void setObjType(MTypeVar v) {
      this.featureDef.obj = v;
    }

    void setImplType(MTypeRef t) {
      this.featureDef.impl = t;
    }

    MFeatureDef create() {
      this.featureDef.params = this.paramList.toArray(new MTypeVar[this.paramList.size()]);
      return this.featureDef;
    }
  }

  public Element externalize(Document doc) {
    Element featureDefNode = doc.createElement(Module.TAG_FEATURE_DEF);
    featureDefNode.setAttribute(Module.ATTR_NAME, this.name);
    if (this.availability != Module.AVAILABILITY_GENERAL) {
      featureDefNode.setAttribute(Module.ATTR_AVAILABILITY, Module.reprOfAvailability(this.availability));
    }
    if (this.acc != Module.ACC_PRIVATE) {
      featureDefNode.setAttribute(Module.ATTR_ACC, Module.reprOfAcc(this.acc));
    }
    if (this.params.length > 0) {
      Element paramsNode = doc.createElement(Module.TAG_PARAMS);
      for (int i = 0; i < this.params.length; i++) {
        paramsNode.appendChild(this.params[i].externalize(doc));
      }
      featureDefNode.appendChild(paramsNode);
    }

    // HERE
    Element objNode = doc.createElement(Module.TAG_OBJ);
    featureDefNode.appendChild(objNode);
    Element implNode = doc.createElement(Module.TAG_IMPL);
    featureDefNode.appendChild(implNode);

    return featureDefNode;
  }

  void checkCompat(Module.ModTab modTab, MFeatureDef fd, Module.ModTab defModTab) throws FormatException {
    StringBuffer emsg;
    if (Module.equalOrMoreOpenAcc(fd.acc, this.acc)) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible access mode - type: ");
      emsg.append(fd.tcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    if (fd.params == null && this.params == null) {
      ;
    } else if (fd.params != null && this.params != null && fd.params.length == this.params.length) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible parameter count - type: ");
      emsg.append(fd.tcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    for (int i = 0; i < this.constrs.length; i++) {
      MConstrDef cd = this.constrs[i];
      cd.checkCompat(modTab, fd, defModTab);
    }
  }
}
