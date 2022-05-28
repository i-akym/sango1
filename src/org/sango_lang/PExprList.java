/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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

abstract class PExprList extends PDefaultExprObj {
  PExpr[] exprs;

  PExprList(Parser.SrcInfo srcInfo, PScope outerScope, List<PExpr> list) {
    super(srcInfo, outerScope);
    this.exprs = list.toArray(new PExpr[list.size()]);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("exprlist[exprs=");
    String sep = "";
    for (int i = 0; i < this.exprs.length; i++) {
      buf.append(this.exprs[i]);
      sep = ",";
    }
    buf.append("]");
    return buf.toString();
  }

  static Seq acceptSeq(ParserA.TokenReader reader, Parser.SrcInfo srcInfo, PScope outerScope, boolean voidAssumedWhenEmpty) throws CompileException, IOException {
    List<PExpr> exprList = new ArrayList<PExpr>();
    PExpr expr = null;
    int state = 0;
    while (state >= 0)  {
      if (state == 0 && (expr = PExpr.accept(reader, outerScope)) != null) {
        exprList.add(expr);
        state = 1;
      } else if (ParserA.acceptToken(reader, LToken.COMMA, ParserA.SPACE_DO_NOT_CARE) != null) {
        state = 0;
      } else {
        state = -1;
      }
    }
    if (exprList.size() == 0 && voidAssumedWhenEmpty) {
      exprList.add(PExpr.createDummyVoidExpr(reader.getCurrentSrcInfo(), outerScope));
    }
    return Seq.create(srcInfo, outerScope, exprList);
  }

  static Elems acceptElems(ParserA.TokenReader reader, Parser.SrcInfo srcInfo, PScope outerScope, int min) throws CompileException, IOException {
    StringBuffer emsg;
    List<PExpr> exprList = new ArrayList<PExpr>();
    PExpr expr = null;
    int state = 0;
    while (state >= 0)  {
      switch (state) {
      case 0:
        if ((expr = PExpr.accept(reader, outerScope)) != null) {
          exprList.add(expr);
          state = 1;
        } else {
          state = -1;
        }
        break;
      case 1:
        if (ParserA.acceptToken(reader, LToken.COMMA, ParserA.SPACE_DO_NOT_CARE) != null) {
          state = 2;
        } else {
          state = -1;
        }
        break;
      default:  // case 2
        if ((expr = PExpr.accept(reader, outerScope)) != null) {
          exprList.add(expr);
          state = 1;
        } else {
          emsg = new StringBuffer();
          emsg.append("Expression missing at ");
          emsg.append(reader.getCurrentSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      }
    }
    if (exprList.size() < min) {
      emsg = new StringBuffer();
      emsg.append("Insufficient expressions at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return Elems.create(srcInfo, outerScope, exprList);
  }

  // public void setupScope(PScope scope) {
    // if (scope == this.scope) { return; }
    // this.scope = scope;
    // for (int i = 0; i < this.exprs.length; i++) {
      // this.exprs[i].setupScope(scope);
    // }
    // this.idResolved = false;
  // }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.exprs.length; i++) {
      this.exprs[i].collectModRefs();
    }
  }

  public PExprList resolve() throws CompileException {
    // if (this.idResolved) { return this; }
    for (int i = 0; i < this.exprs.length; i++) {
      this.exprs[i] = this.exprs[i].resolve();
    }
    // this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.exprs.length; i++) {
      this.exprs[i].normalizeTypes();
    }
  }

  static class Seq extends PExprList /* implements PEval */ {
    static Seq create(Parser.SrcInfo srcInfo, PScope outerScope, List<PExpr> list) {
      return new Seq(srcInfo, outerScope, list);
    }

    Seq(Parser.SrcInfo srcInfo, PScope outerScope, List<PExpr> list) {
      super(srcInfo, outerScope, list);
    }

    public Seq resolve() throws CompileException { return (Seq)super.resolve(); }

    public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
      this.typeGraphNode = graph.createRefNode(this);
      PTypeGraph.Node n = null;
      for (int i = 0; i < this.exprs.length; i++) {
        if (n == null) {
          n = this.exprs[i].setupTypeGraph(graph);
        } else {
          PTypeGraph.SeqNode s = graph.createSeqNode(this.exprs[i]);
          s.setLeadingTypeNode(n);
          s.setInNode(this.exprs[i].setupTypeGraph(graph));
          n = s;
        }
      }
      this.typeGraphNode.setInNode(n);
      return this.typeGraphNode;
    }

    public GFlow.Node setupFlow(GFlow flow) {
      GFlow.SeqNode node = flow.createNodeForAction(this.exprs[0].getSrcInfo());
      for (int i = 0; i < this.exprs.length - 1; i++) {
        node.addChild(this.exprs[i].setupFlow(flow));
        node.addChild(flow.createSinkNode(this.exprs[i].getSrcInfo()));
      }
      node.addChild(this.exprs[this.exprs.length - 1].setupFlow(flow));
      return node;
    }
  }

  static class Elems extends PExprList {
    static Elems create(Parser.SrcInfo srcInfo, PScope outerScope, List<PExpr> list) {
      return new Elems(srcInfo, outerScope, list);
    }

    Elems(Parser.SrcInfo srcInfo, PScope outerScope, List<PExpr> list) {
      super(srcInfo, outerScope, list);
    }

    public Elems resolve() throws CompileException { return (Elems)super.resolve(); }

    public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
      throw new RuntimeException("PExprList.Elems#setupTypeGraph is called. " + this.toString());
    }

    public GFlow.Node setupFlow(GFlow flow) {
      throw new RuntimeException("PExprList.Elems#setupTypeGraph is called. " + this.toString());
    }
  }
}
