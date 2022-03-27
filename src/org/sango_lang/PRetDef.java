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
  private PRetDef(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  static PRetDef create(PType type) {
    if (type == null) {
      throw new IllegalArgumentException("Type is null.");
    }
    return create(type.getSrcInfo(), type);
  }

  static PRetDef create(Parser.SrcInfo srcInfo, PType type) {
    PRetDef ret = new PRetDef(srcInfo);
    ret.type = type;
    return ret;
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

  static PRetDef accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    PType type = PType.accept(reader, ParserA.SPACE_DO_NOT_CARE);
    if (type == null) {
      type = PType.voidType(si);
    }
    return create(si, type);
  }

  static PRetDef acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("ret")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType type = PType.acceptX(e);
    if (type == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), type);
  }

  public void setupScope(PScope scope) {
    StringBuffer emsg;
    if (scope == this.scope) { return; }
    this.scope = scope;
    this.idResolved = false;
    this.type.setupScope(scope);
  }

  public void collectModRefs() throws CompileException {
    this.type.collectModRefs();
  }

  public PRetDef resolve() throws CompileException {
    if (this.idResolved) { return this; }
    this.type = (PType)this.type.resolve();
    this.idResolved = true;
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    this.type.excludePrivateAcc();
  }

  public void normalizeTypes() {
    this.nTypeSkel = this.type.normalize();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createRetNode(this);
    return this.typeGraphNode;
  }
}
