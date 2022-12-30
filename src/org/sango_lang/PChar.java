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

class PChar extends PDefaultExprObj {
   int value;

  private PChar(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("char[");
    buf.append(this.value);
    buf.append("]");
    return buf.toString();
  }

  static PChar create(Parser.SrcInfo srcInfo, PScope outerScope, int value) {
    PChar c = new PChar(srcInfo, outerScope);
    c.value = value;
    return c;
  }

  static PChar accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    ParserA.Token token = ParserA.acceptToken(reader, LToken.CHAR, spc);
    return (token != null)? create(token.getSrcInfo(), outerScope, token.value.charValue): null;
  }

  static PChar acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("char")) { return null; }
    String code = elem.getAttrValue("code");
    if (code == null) {
      emsg = new StringBuffer();
      emsg.append("Code missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    int c = 0;
    try {
      c = Integer.parseUnsignedInt(code, 16);
    } catch (Exception Ex) {
      emsg = new StringBuffer();
      emsg.append("Invalid code specification at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(code);
      throw new CompileException(emsg.toString());
    }
    if (c > 0x10ffff) {
      emsg = new StringBuffer();
      emsg.append("Too large code at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(code);
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), outerScope, c);
  }

  public void collectModRefs() throws CompileException {}

  public PChar resolve() throws CompileException {
    return this;
  }

  public void normalizeTypes() {
    this.nTypeSkel = this.scope.getLangPrimitiveType(this.srcInfo, "char").getNormalizedSkel();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createDetNode(this);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForChar(this.srcInfo, this.value);
  }
}
