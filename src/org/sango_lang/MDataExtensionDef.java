/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2026 AKIYAMA Isao                                         *
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
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MDataExtensionDef implements Module.Elem {
  String defKey;
  Module.Availability availability;
  Module.Access acc;
  MType.ParamDef[] params;
  int baseModIndex;
  String baseTcon;
  MConstrDef[] constrs;

  private MDataExtensionDef() {}

  static class Builder {
    MDataExtensionDef dataExtensionDef;
    List<MType.ParamDef> paramList;
    List<MConstrDef> constrList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.dataExtensionDef = new MDataExtensionDef();
      this.dataExtensionDef.availability = Module.AVAILABILITY_GENERAL;
      this.constrList = new ArrayList<MConstrDef>();
    }

    void prepareForParams() {
      this.paramList = new ArrayList<MType.ParamDef>();
    }

    void setDefKey(String defKey) {
      this.dataExtensionDef.defKey = defKey;
    }

    void setAvailability(Module.Availability availability) {
      this.dataExtensionDef.availability = availability;
    }

    void setAcc(Module.Access acc) {
      this.dataExtensionDef.acc = acc;
    }

    void addParam(MType.ParamDef param) {
      this.paramList.add(param);
    }

    void setBaseModIndex(int baseModIndex) {
      this.dataExtensionDef.baseModIndex = baseModIndex;
    }

    void setBaseTcon(String baseTcon) {
      this.dataExtensionDef.baseTcon = baseTcon;
    }

    void addConstrDef(MConstrDef constrDef) {
      this.constrList.add(constrDef);
    }

    MDataExtensionDef create() {
      this.dataExtensionDef.params = (this.paramList != null)?
        this.paramList.toArray(new MType.ParamDef[this.paramList.size()]):
        null;
      this.dataExtensionDef.constrs = this.constrList.toArray(new MConstrDef[this.constrList.size()]);
      return this.dataExtensionDef;
    }
  }

  public Element externalize(Document doc) {
    Element dataExtDefNode = doc.createElement(Module.TAG_DATA_EXT_DEF);
    if (this.availability != Module.AVAILABILITY_GENERAL) {
      dataExtDefNode.setAttribute(Module.ATTR_AVAILABILITY, Module.reprOfAvailability(this.availability));
    }
    if (this.acc != Module.ACC_PRIVATE) {
      dataExtDefNode.setAttribute(Module.ATTR_ACC, Module.reprOfAcc(this.acc));
    }
    dataExtDefNode.setAttribute(Module.ATTR_DEF_KEY, this.defKey);
    dataExtDefNode.setAttribute(Module.ATTR_BASE_MOD_INDEX, Integer.toString(this.baseModIndex));
    dataExtDefNode.setAttribute(Module.ATTR_BASE_TCON, this.baseTcon);
    if (this.params.length > 0) {
      Element paramsNode = doc.createElement(Module.TAG_PARAMS);
      for (int i = 0; i < this.params.length; i++) {
        paramsNode.appendChild(this.params[i].externalize(doc));
      }
      dataExtDefNode.appendChild(paramsNode);
    }
    for (int i = 0; i < this.constrs.length; i++) {
      dataExtDefNode.appendChild(this.externalizeConstrDef(doc, this.constrs[i]));
    }
    return dataExtDefNode;
  }

  Element externalizeConstrDef(Document doc, MConstrDef constrDef) {
    return constrDef.externalize(doc);
  }

  void checkCompat(Module.ModTab modTab, Map<String, MDataExtensionDef> dconTab, Module.ModTab defModTab, Map<String, MConstrDef> constrDefDict) throws FormatException {
  
    StringBuffer emsg;

    for (int i = 0; i < this.constrs.length; i++) {
      MConstrDef cd = this.constrs[i];
      MDataExtensionDef xd = dconTab.get(cd.dcon);
      if (xd == null) {
        emsg = new StringBuffer();
        emsg.append("Data contructor \"");
        emsg.append(cd.dcon);
        emsg.append("\" not found, referred in: ");
        emsg.append(modTab.getMyModName().repr());
        emsg.append(" defined in: ");
        emsg.append(defModTab.getMyModName().repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
      if (Module.equalOrMoreOpenAcc(xd.acc, this.acc)) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Incompatible access mode on extension: ");
        emsg.append(xd.baseTcon);
        emsg.append(", referred in: ");
        emsg.append(modTab.getMyModName().repr());
        emsg.append(" defined in: ");
        emsg.append(defModTab.getMyModName().repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
      if (xd.params == null && this.params == null) {
        ;
      } else if (xd.params != null && this.params != null && xd.params.length == this.params.length) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Incompatible parameter count on extension: ");
        emsg.append(xd.baseTcon);
        emsg.append(", referred in: ");
        emsg.append(modTab.getMyModName().repr());
        emsg.append(" defined in: ");
        emsg.append(defModTab.getMyModName().repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
      MConstrDef dcd = constrDefDict.get(cd.dcon);
      if (dcd == null) {
        emsg = new StringBuffer();
        emsg.append("Data contructor \"");
        emsg.append(dcd.dcon);
        emsg.append("\" not found, referred in: ");
        emsg.append(modTab.getMyModName().repr());
        emsg.append(" defined in: ");
        emsg.append(defModTab.getMyModName().repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
      cd.checkCompat(modTab, new MConstrDef[] { dcd }, defModTab);
    }
  }
}
