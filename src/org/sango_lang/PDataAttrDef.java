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

class PDataAttrDef extends PDefaultTypedElem implements PDataDef.Attr {
  String name;
  PVarDef var;

  private PDataAttrDef() {}

  static PDataAttrDef create(Parser.SrcInfo srcInfo, String name, PTypeDesc type, PVarDef var) {
    PDataAttrDef attr = new PDataAttrDef();
    attr.srcInfo = srcInfo;
    attr.name = name;
    // type.parent = attr;
    attr.type = type;
    if (var != null) {
      var.parent = attr;
    }
    attr.var = var;
    return attr;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("attr[src=");
    buf.append(this.srcInfo);
    buf.append(",name=");
    buf.append(this.name);
    buf.append(",type=");
    buf.append(this.type);
    buf.append(",var=");
    buf.append(this.var);
    buf.append("]");
    return buf.toString();
  }

  static List<PDataAttrDef> acceptList(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    List<PDataAttrDef> attrList = new ArrayList<PDataAttrDef>();
    Parser.SrcInfo si = null;
    String name = null;
    PTypeDesc type = null;
    PVarDef var = null;
    int spc = ParserA.SPACE_DO_NOT_CARE;
    int state = 0;
    DataAttrName aname;
    PTypeDesc type2;
    while (state >= 0) {
      switch (state) {
      case 0:  // (empty)
        if ((aname = acceptDataAttrName(reader, spc)) != null) {
          si = aname.srcInfo;
          name = aname.name;
          spc = ParserA.SPACE_DO_NOT_CARE;
          state = 1;
        } else if ((type = PType.acceptRO(reader, ParserA.SPACE_DO_NOT_CARE)) != null) {
          si = type.getSrcInfo();
          type = type;
          spc = ParserA.SPACE_DO_NOT_CARE;
          state = 2;
        } else {
          state = -1;
        }
        break;
      case 1:  // name:
        if ((type = PType.acceptRO(reader, ParserA.SPACE_DO_NOT_CARE)) != null) {
          spc = ParserA.SPACE_DO_NOT_CARE;
          state = 2;
        } else {
          emsg = new StringBuffer();
          emsg.append("Data type missing at ");
          emsg.append(reader.getCurrentSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      case 2:  // [name:] <type>
        // if ((var = PVarDef.accept(reader, PVarDef.CAT_ATTR, PVarDef.TYPE_NOT_ALLOWED)) != null) {
          // attrList.add(create(si, name, type, var));
          // si = null;
          // name = null;
          // type = null;
          // var = null;
          // spc = ParserA.SPACE_NEEDED;
          // state = 0;
        // } else
	if ((aname = acceptDataAttrName(reader, ParserA.SPACE_NEEDED)) != null) {
          attrList.add(create(si, name, type, var));
          si = aname.srcInfo;
          name = aname.name;
          type = null;
          var = null;
          spc = ParserA.SPACE_DO_NOT_CARE;
          state = 1;
        } else if ((type2 = PType.acceptRO(reader, ParserA.SPACE_NEEDED)) != null) {
          attrList.add(create(si, name, type, var));
          si = type.getSrcInfo();
          name = null;
          type = type2;
          var = null;
          spc = ParserA.SPACE_DO_NOT_CARE;
          state = 2;
        } else  {
          attrList.add(create(si, name, type, var));
          state = -1;
        }
        break;
      }
    }
    return attrList;
  }

  private static DataAttrName acceptDataAttrName(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token next = reader.getNextToken();
    ParserA.Token name;
    if (next.tagEquals(LToken.COL) && (name = ParserA.acceptNormalWord(reader, spc)) != null) {
      DataAttrName aname = new DataAttrName();
      aname.srcInfo = name.getSrcInfo();
      aname.name = name.value.token;
      ParserA.acceptToken(reader, LToken.COL, ParserA.SPACE_DO_NOT_CARE);
      return aname;
    } else {
      return null;
    }
  }

  static PDataAttrDef acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("attr")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Data type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeDesc t = PType.acceptXRO(e);
    if (t == null) {
      emsg = new StringBuffer();
      emsg.append("Data type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), elem.getAttrValueAsId("name"), t, null);
  }

  private static class DataAttrName {
    Parser.SrcInfo srcInfo;
    String name;
  }

  public String getName() { return this.name; }

  public PDataAttrDef setupScope(PScope scope) throws CompileException {
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    this.type = (PTypeDesc)this.type.setupScope(scope);
    // HERE:  var
    return this;
  }

  public PDataAttrDef resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    this.type = (PTypeDesc)this.type.resolveId();
    // HERE:  var
    this.idResolved = true;
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    this.type.excludePrivateAcc();
  }

  public void normalizeTypes() {
    this.nTypeSkel = this.type.normalize();
  }
}
