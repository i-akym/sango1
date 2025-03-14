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

class PByte extends PDefaultExprObj {
   int value;

  private PByte(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("byte[");
    buf.append(this.value);
    buf.append("]");
    return buf.toString();
  }

  static PByte create(Parser.SrcInfo srcInfo, PScope outerScope, int value) {
    PByte b = new PByte(srcInfo, outerScope);
    b.value = value;
    return b;
  }

  static PByte accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    ParserA.Token token = ParserA.acceptToken(reader, LToken.BYTE, spc);
    return (token != null)? create(token.getSrcInfo(), outerScope, token.value.intValue): null;
  }

  static PByte acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("byte")) { return null; }
    String val = elem.getAttrValue("value");
    if (val == null) {
      emsg = new StringBuffer();
      emsg.append("Value missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    String r = elem.getAttrValue("radix");
    int radix;
    if (r == null) {
      radix = 10;
    } else if (r.equals("10")) {
      radix = 10;
    } else if (r.equals("8")) {
      radix = 8;
    } else if (r.equals("16")) {
      radix = 16;
    } else {
      emsg = new StringBuffer();
      emsg.append("Invalid radix at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(r);
      throw new CompileException(emsg.toString());
    }
    int i;
    try {
      i = Integer.parseInt(val, radix);
    } catch (Exception Ex) {
      emsg = new StringBuffer();
      emsg.append("Invalid integer specification at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(val);
      throw new CompileException(emsg.toString());
    }
    if (i < 0 || i > 255) {
      emsg = new StringBuffer();
      emsg.append("Byte value out of range at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(val);
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), outerScope, i);
  }

  public void collectModRefs() throws CompileException {}

  public PByte resolve() throws CompileException {
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this._normalized_typeSkel = this.scope.getLangPrimitiveType(this.srcInfo, "byte").toSkel();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createDetNode(this);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForByte(this.srcInfo, this.value);
  }
}
