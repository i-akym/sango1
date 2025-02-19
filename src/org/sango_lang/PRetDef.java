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

class PRetDef extends PDefaultExprObj {
  private PRetDef(Parser.SrcInfo srcInfo, PScope defScope) {
    super(srcInfo, defScope);
    // super(srcInfo, defScope.enterInner());
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("ret[");
    buf.append("src=");
    buf.append(this.srcInfo);
    if (this.type != null) {
      buf.append(",type=");
      buf.append(this.type);
    }
    buf.append("]");
    return buf.toString();
  }

  static PRetDef accept(ParserA.TokenReader reader, PScope defScope) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    Builder builder = Builder.newInstance(si, defScope);
    PScope scope = builder.getScope();
    PType type = PType.accept(reader, scope, ParserA.SPACE_DO_NOT_CARE);
    if (type == null) {
      type = PType.voidType(si, scope);
    }
    builder.setType(type);
    return builder.create();
  }

  static PRetDef acceptX(ParserB.Elem elem, PScope defScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("ret")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), defScope);
    PScope scope = builder.getScope();
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType type = PType.acceptX(e, scope);
    if (type == null) {
      type = PType.voidType(e.getSrcInfo(), scope);
    }
    builder.setType(type);
    return builder.create();
  }

  static class Builder {
    PRetDef ret;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope defScope) {
      return new Builder(srcInfo, defScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope defScope) {
      this.ret = new PRetDef(srcInfo, defScope);
    }

    PScope getScope() { return this.ret.scope; }

    void setType(PType type) {
      this.ret.type = type;
    }

    PRetDef create() throws CompileException {
      return this.ret;
    }
  }

  public void collectModRefs() throws CompileException {
    this.type.collectModRefs();
  }

  public PRetDef resolve() throws CompileException {
    this.type = (PType)this.type.resolve();
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    this.type.excludePrivateAcc();
  }

  public void normalizeTypes() throws CompileException {
    if (this._normalized_typeSkel == null) {
      this._normalized_typeSkel = this.type.toSkel().normalize();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createRetNode(this);
    return this.typeGraphNode;
  }
}
