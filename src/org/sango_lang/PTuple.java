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

class PTuple extends PDefaultExprObj {
  PExprList.Elems elems;

  PTuple(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("tuple[src=");
    buf.append(this.srcInfo);
    buf.append(",elems=[");
    buf.append(this.elems);
    buf.append("]]");
    return buf.toString();
  }

  static PTuple create(Parser.SrcInfo srcInfo, PScope outerScope, PExprList.Elems elems) {
    PTuple t =  new PTuple(srcInfo, outerScope);
    t.elems = elems;
    return t;
  }

  static PTuple accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LPAR_VBAR, spc)) == null) {
      return null;
    }
    PExprList.Elems es = PExprList.acceptElems(reader, reader.getCurrentSrcInfo(), outerScope, 2);
    if (es == null) {
      emsg = new StringBuffer();
      emsg.append("Invalid tuple specification at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (ParserA.acceptToken(reader, LToken.VBAR_RPAR, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Missing end of tuple at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(t.getSrcInfo(), outerScope, es);
  }

  static PTuple acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("tuple")) { return null; }
    Parser.SrcInfo si = elem.getSrcInfo();
    List<PExpr> es = new ArrayList<PExpr>();
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PExpr expr = PExpr.acceptX(e, outerScope);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
        }
      es.add(expr);
      e = e.getNextSibling();
    }
    return create(si, outerScope, PExprList.Elems.create(si, outerScope, es));
  }

  public void collectModRefs() throws CompileException {
    this.elems.collectModRefs();
  }

  public PTuple resolve() throws CompileException {
    this.elems = this.elems.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.elems.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createTupleNode(this, this.elems.exprs.length);
    for (int i = 0; i < this.elems.exprs.length; i++) {
      ((PTypeGraph.TupleNode)this.typeGraphNode).setElemNode(i, this.elems.exprs[i].setupTypeGraph(graph));
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.TupleNode node = flow.createNodeForTuple(this.srcInfo);
    for (int i = 0; i < this.elems.exprs.length; i++) {
      node.addChild(this.elems.exprs[i].setupFlow(flow));
    }
    return node;
  }
}
