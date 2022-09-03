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

public class MConstrDef {
  MDataDef theDataDef;
  String dcon;
  MAttrDef [] attrs;

  private MConstrDef() {}

  void setDataDef(MDataDef dataDef) {
    this.theDataDef = dataDef;
  }

  static class Builder {
    MConstrDef constrDef;
    List<MAttrDef> attrList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.constrDef = new MConstrDef();
      this.attrList = new ArrayList<MAttrDef>();
    }

    void setDcon(String dcon) {
      this.constrDef.dcon = dcon;
    }

    void addAttrDef(MAttrDef attrDef) {
      attrDef.setConstrDef(this.constrDef);
      this.attrList.add(attrDef);
    }

    MConstrDef create() {
      this.constrDef.attrs = this.attrList.toArray(new MAttrDef[this.attrList.size()]);
      return this.constrDef;
    }
  }

  public Element externalize(Document doc) {
    Element constrDefNode = doc.createElement(Module.TAG_CONSTR);
    constrDefNode.setAttribute(Module.ATTR_DCON, this.dcon);
    for (int i = 0; i < this.attrs.length; i++) {
      constrDefNode.appendChild(this.externalizeAttrDef(doc, this.attrs[i]));
    }
    return constrDefNode;
  }

  Element externalizeAttrDef(Document doc, MAttrDef attrDef) {
    return attrDef.externalize(doc);
  }

  void checkCompat(Module.ModTab modTab, MDataDef dd, Module.ModTab defModTab) throws FormatException {
    StringBuffer emsg;
    MConstrDef cd = null;
    for (int i = 0; cd == null && i < dd.constrs.length; i++) {
      if (dd.constrs[i].dcon.equals(this.dcon)) {
        cd = dd.constrs[i];
      }
    }
    if (cd != null) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Data constructor missing - data constructor: ");
      emsg.append(this.dcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    if (cd.attrs.length == this.attrs.length) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Attribute count mismatch - data constructor: ");
      emsg.append(this.dcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].checkCompat(modTab, cd.attrs[i], defModTab);
    }
  }
}
