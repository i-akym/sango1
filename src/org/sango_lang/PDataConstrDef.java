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

class PDataConstrDef extends PDefaultProgObj implements PDataDef.Constr {
  String dcon;
  PDataAttrDef[] attrs;
  PDataDef dataDef;
  // PType dataType;

  PDataConstrDef(Parser.SrcInfo srcInfo, PScope defScope) {
    super(srcInfo, defScope.enterInner());
  }

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

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope defScope) {
      return new Builder(srcInfo, defScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope defScope) {
      this.constr = new PDataConstrDef(srcInfo, defScope);
      this.attrList = new ArrayList<PDataAttrDef>();
      this.attrNameList = new ArrayList<String>();
    }

    PScope getScope() { return this.constr.scope; }

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

  static PDataConstrDef accept(ParserA.TokenReader reader, PScope defScope) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance(reader.getCurrentSrcInfo(), defScope);
    PScope scope = builder.getScope();
    List<PDataAttrDef> attrList = PDataAttrDef.acceptList(reader, scope);
    if (attrList == null) { return null; }
    ParserA.Token dcon;
    if ((dcon = ParserA.acceptNormalWord(reader, ParserA.SPACE_NEEDED)) != null) {
      builder.setDcon(dcon.value.token);
      builder.addAttrList(attrList);
    } else if (attrList.isEmpty()) {
      return null;
      // ;  // neither attributes nor dcon
    } else {
      emsg = new StringBuffer();
      emsg.append("Data constructor missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
    // return (builder != null)? builder.create(): null;
  }

  static PDataConstrDef acceptX(ParserB.Elem elem, PScope defScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("constr")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), defScope);
    PScope scope = builder.getScope();
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
      PDataAttrDef attr = PDataAttrDef.acceptX(e, scope);
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

  public PDataDef getDataDef() { return this.dataDef; }

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

  void setDataDef(PDataDef dataDef) {
    this.dataDef = dataDef;
  }

  // void setDataType(PType type) {
    // this.dataType = type;
  // }

  public PTypeSkel getType(PTypeSkelBindings bindings) {
    PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(bindings);
    return this.dataDef.getTypeSig().resolveBindings(bindings).instanciate(ic);
    // return this.dataType.toSkel().resolveBindings(bindings).instanciate(ic);
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].collectModRefs();
    }
  }

  public PDataConstrDef resolve() throws CompileException {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i] = this.attrs[i].resolve();
    }
    return this;
  }

  void excludePrivateAcc() throws CompileException {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].excludePrivateAcc();
    }
  }

  void checkVariance(PTypeSkel.VarianceTab vt) throws CompileException {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].checkVariance(vt);
    }
  }

  // public void checkConcreteness() throws CompileException {
    // for (int i = 0; i < this.attrs.length; i++) {
      // this.attrs[i].checkConcreteness();
    // }
  // }

  void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.attrs.length; i++) {
      this.attrs[i].normalizeTypes();
    }
  }
}
