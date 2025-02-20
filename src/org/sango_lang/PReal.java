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

class PReal extends PDefaultExprObj {
   double value;

  private PReal(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("real[");
    buf.append(this.value);
    buf.append("]");
    return buf.toString();
  }

  static PReal create(Parser.SrcInfo srcInfo, PScope outerScope, double value) {
    PReal r = new PReal(srcInfo, outerScope);
    r.value = value;
    return r;
  }

  static PReal accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    ParserA.Token token = ParserA.acceptToken(reader, LToken.REAL, spc);
    return (token != null)? create(token.getSrcInfo(), outerScope, token.value.realValue): null;
  }

  static PReal acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("real")) { return null; }
    String val = elem.getAttrValue("value");
    if (val == null) {
      emsg = new StringBuffer();
      emsg.append("Value missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    double d;
    try {
      d = Double.parseDouble(val);
    } catch (Exception Ex) {
      emsg = new StringBuffer();
      emsg.append("Invalid real specification at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(val);
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), outerScope, d);
  }

  public void collectModRefs() throws CompileException {}

  public PReal resolve() throws CompileException {
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this._normalized_typeSkel = this.scope.getLangPrimitiveType(this.srcInfo, "real").toSkel();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createDetNode(this);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForReal(this.srcInfo, this.value);
  }
}
