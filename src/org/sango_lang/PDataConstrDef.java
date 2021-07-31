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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PDataConstrDef extends PDefaultProgElem implements PDataDef.Constr {
  String dcon;
  PDataAttrDef[] attrs;
  PScope outerScope;
  PTypeDesc dataType;

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("constr[src=");
    buf.append(this.srcInfo);
    buf.append(",con=");
    buf.append(this.dcon);
    buf.append(",attrs=[");
    for (int i = 0; i < this.attrs.length; i++) {
      buf.append(this.attrs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PDataConstrDef constr;
    List<PDataAttrDef> attrList;
    List<String> attrNameList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.constr = new PDataConstrDef();
      this.attrList = new ArrayList<PDataAttrDef>();
      this.attrNameList = new ArrayList<String>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.constr.srcInfo = si;
    }

    void setDcon(String dcon) {
      this.constr.dcon = dcon;
    }

    void addAttr(PDataAttrDef attr) throws CompileException {
      StringBuffer emsg;
      String attrName = attr.name;
      if (attrName != null) {
        if (this.attrNameList.contains(attrName)) {
          emsg = new StringBuffer();
          emsg.append("Attribute name \"");
          emsg.append(attrName);
          emsg.append("\" duplicate at ");
          emsg.append(attr.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        attrNameList.add(attrName);
      } else if (attrNameList.size() > 0) {
        emsg = new StringBuffer();
        emsg.append("Attribute name missing at ");
        emsg.append(attr.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.attrList.add(attr);
    }

    void addAttrList(List<PDataAttrDef> attrList) throws CompileException {
      for (int i = 0; i < attrList.size(); i++) {
        this.addAttr(attrList.get(i));
      }
    }

    PDataConstrDef create() {
      this.constr.attrs = this.attrList.toArray(new PDataAttrDef[this.attrList.size()]);
      return this.constr;
    }
  }

  static PDataConstrDef accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    List<PDataAttrDef> attrList = PDataAttrDef.acceptList(reader);
    Builder builder = null;
    ParserA.Token dcon;
    if ((dcon = ParserA.acceptNormalWord(reader, ParserA.SPACE_NEEDED)) != null) {
      builder = Builder.newInstance();
      builder.setSrcInfo(si);
      builder.setDcon(dcon.value.token);
      builder.addAttrList(attrList);
    } else if (attrList.isEmpty()) {
      ;  // neither attributes nor dcon
    } else {
      emsg = new StringBuffer();
      emsg.append("Data constructor missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return (builder != null)? builder.create(): null;
  }

  static PDataConstrDef acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("constr")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());
    String dcon = elem.getAttrValueAsId("dcon");
    if (dcon == null) {
      emsg = new StringBuffer();
      emsg.append("Data constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setDcon(dcon);

    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PDataAttrDef attr = PDataAttrDef.acceptX(e);
      if (attr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      builder.addAttr(attr);
      e = e.getNextSibling();
    }
    return builder.create();
  }

  public String getDcon() { return this.dcon; }

  public int getAttrCount() { return this.attrs.length; }

  public PDataDef.Attr getAttrAt(int i) { return this.attrs[i]; }

  public int getAttrIndex(String name) {
    int i = this.attrs.length - 1;
    for (; i >= 0; i--) {
      if (name.equals(this.attrs[i].name)) {
        break;
      }
    }
    return i;
  }

  void setDataType(PTypeDesc type) {
    this.dataType = type;
  }

  public PTypeSkel getType(PTypeSkelBindings bindings) {
    PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(bindings);
    return this.dataType.getSkel().instanciate(ib);
  }

  public PDataConstrDef setupScope(PScope scope) throws CompileException {
    if (scope == this.outerScope) { return this; }
    this.outerScope = scope;
    this.scope = scope.enterInner();
    // this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i] = this.attrs[i].setupScope(this.scope);
    }
    return this;
  }

  public PDataConstrDef resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i] = this.attrs[i].resolveId();
    }
    this.idResolved = true;
    return this;
  }

  void excludePrivateAcc() throws CompileException {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].excludePrivateAcc();
    }
  }

  public void normalizeTypes() {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].normalizeTypes();
    }
  }
}
