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

class PRetDef extends PDefaultTypedElem {
  static final int TYPE_MAYBE_SPECIFIED = 1;
  static final int TYPE_NEEDED = 2;

  private PRetDef() {}

  static PRetDef create(PTypeDesc type) {
    if (type == null) {
      throw new IllegalArgumentException("Type is null.");
    }
    return create(type.getSrcInfo(), type);
  }

  static PRetDef create(Parser.SrcInfo srcInfo, PTypeDesc type) {
    PRetDef ret = new PRetDef();
    ret.srcInfo = srcInfo;
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

  static PRetDef accept(ParserA.TokenReader reader, int typeSpec) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    PTypeDesc type = PType.accept(reader, ParserA.SPACE_DO_NOT_CARE);
    if (typeSpec == TYPE_NEEDED && type == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
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
    PTypeDesc type = PType.acceptX(e, PType.ACCEPTABLE_VARDEF);
    if (type == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), type);
  }

  public PRetDef setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    if (this.type != null) {
      this.type = (PTypeDesc)this.type.setupScope(scope);
    }
    return this;
  }

  public PRetDef resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    if (this.type != null) {
      this.type = (PTypeDesc)this.type.resolveId();
    }
    this.idResolved = true;
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    if (this.type != null) {
      this.type.excludePrivateAcc();
    }
  }

  public void normalizeTypes() {
    if (this.type != null) {
// /* DEBUG */ System.out.print("normalizing retdef at " + this.srcInfo);
// /* DEBUG */ System.out.print(" ");
// /* DEBUG */ System.out.println(this.type);
      this.nTypeSkel = this.type.normalize();
// /* DEBUG */ System.out.println(" -> " + this.nTypeSkel);
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createRetNode(this);
    return this.typeGraphNode;
  }
}
