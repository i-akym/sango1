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

class PDataAttrDef extends PDefaultTypedObj implements PDataDef.Attr {
  String name;

  private PDataAttrDef(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
  }

  static PDataAttrDef create(Parser.SrcInfo srcInfo, PScope outerScope, String name, PType type) {
    PDataAttrDef attr = new PDataAttrDef(srcInfo, outerScope);
    attr.name = name;
    attr.type = type;
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
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PDataAttrDef attr;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.attr= new PDataAttrDef(srcInfo, outerScope);
    }

    PScope getScope() { return this.attr.scope; }

    void setName(String name) {
      this.attr.name = name;
    }

    void setType(PType type) {
      this.attr.type = type;
    }

    PDataAttrDef create() {
      return this.attr;
    }
  }

  static List<PDataAttrDef> acceptList(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    List<PDataAttrDef> attrList = new ArrayList<PDataAttrDef>();
    PType type = null;
    int spc = ParserA.SPACE_DO_NOT_CARE;
    int state = 0;
    Builder builder = null;
    while (state >= 0) {
      switch (state) {
      case 0:  // (empty)
        builder = Builder.newInstance(reader.getCurrentSrcInfo(), outerScope);
        ParserA.Token name;
        ParserA.Token next = reader.getNextToken();
        if (next.tagEquals(LToken.COL) && (name = ParserA.acceptNormalWord(reader, spc)) != null) {
          builder.setName(name.value.token);
          ParserA.acceptToken(reader, LToken.COL, ParserA.SPACE_DO_NOT_CARE);
          spc = ParserA.SPACE_DO_NOT_CARE;
          state = 1;
        } else if ((type = PType.accept(reader, builder.getScope(), ParserA.SPACE_DO_NOT_CARE)) != null) {
          builder.setType(type);
          attrList.add(builder.create());
          builder = null;
          spc = ParserA.SPACE_NEEDED;
          state = 0;
        } else {
          state = -1;
        }
        break;
      case 1:  // name:
        if ((type = PType.accept(reader, builder.getScope(), ParserA.SPACE_DO_NOT_CARE)) != null) {
          builder.setType(type);
          attrList.add(builder.create());
          builder = null;
          spc = ParserA.SPACE_NEEDED;
          state = 0;
        } else {
          emsg = new StringBuffer();
          emsg.append("Attribute type missing at ");
          emsg.append(reader.getCurrentSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      }
    }
    return attrList;
  }

  static PDataAttrDef acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
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
    PType t = PType.acceptX(e, outerScope);
    if (t == null) {
      emsg = new StringBuffer();
      emsg.append("Data type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    builder.setName(elem.getAttrValueAsId("name"));
    builder.setType(t);
    return builder.create();
  }

  public String getName() { return this.name; }

  public void collectModRefs() throws CompileException {
    this.type.collectModRefs();
  }

  public PDataAttrDef resolve() throws CompileException {
    this.type = (PType)this.type.resolve();
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    this.type.excludePrivateAcc();
  }

  public void normalizeTypes() {
    this.nTypeSkel = this.type.normalize();
  }

  public void checkConcreteness() throws CompileException {
    if (this.nTypeSkel instanceof PTypeRefSkel) {
      PTypeRefSkel t = (PTypeRefSkel)this.nTypeSkel;
      if (t.tconInfo.key.equals(new PDefDict.TconKey(Module.MOD_LANG, Module.TCON_FUN))) {
        // formally OK...
      } else if (t.tconInfo.key.equals(new PDefDict.TconKey(Module.MOD_LANG, Module.TCON_TUPLE))) {
        // formally OK...
      } else {
        PDefDict.TparamProps[] ps = t.tconInfo.props.paramProps;
        for (int i = 0; i < ps.length; i++) {
          if (ps[i].concrete & !t.params[i].isConcrete()) {
            StringBuffer emsg = new StringBuffer();
            emsg.append("Non-concrete type definition for attribute at ");
            emsg.append(this.getSrcInfo());
            emsg.append(".");
            throw new CompileException(emsg.toString());
          }
        }
      }
    }
  }
}
