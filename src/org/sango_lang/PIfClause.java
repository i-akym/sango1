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

class PIfClause extends PDefaultExprObj {
  PExprList.Seq guard;
  PExprList.Seq action;

  private PIfClause(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("ifclause[src=");
    buf.append(this.srcInfo);
    buf.append(",guard=[");
    buf.append(this.guard);
    buf.append("],action=[");
    buf.append(this.action);
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PIfClause clause;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.clause = new PIfClause(srcInfo, outerScope);
    }

    PScope getScope() { return this.clause.scope; }

    void setGuard(PExprList.Seq seq) {
      this.clause.guard = seq;
    }

    void setAction(PExprList.Seq seq) {
      this.clause.action = seq;
    }

    PIfClause create() throws CompileException {
      StringBuffer emsg;
      if (this.clause.guard.exprs.length == 0) {
        emsg = new StringBuffer();
        emsg.append("Condition missing at ");
        emsg.append(this.clause.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return this.clause;
    }
  }

  static PIfClause accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    Builder builder = Builder.newInstance(si, outerScope);
    PScope scope = builder.getScope();
    PExprList.Seq guardSeq = PExprList.acceptSeq(reader, si, scope, false);
    if (guardSeq == null || guardSeq.exprs.length == 0) {
      return null;
    }
    builder.setGuard(guardSeq);
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setAction(PExprList.acceptSeq(reader, reader.getCurrentSrcInfo(), scope, true));
    return builder.create();
  }

  static PIfClause acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("if-clause")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope scope = builder.getScope();
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Condition missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("cond")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    Parser.SrcInfo si = e.getSrcInfo();
    List<PExpr> ges = new ArrayList<PExpr>();
    ParserB.Elem ee = e.getFirstChild();
    while (ee != null) {
      PExpr expr = PExpr.acceptX(ee, scope);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      ges.add(expr);
      ee = ee.getNextSibling();
    }
    builder.setGuard(PExprList.Seq.create(si, scope, ges));
    e = e.getNextSibling();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Action missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("action")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    si = e.getSrcInfo();
    ee = e.getFirstChild();
    List<PExpr> aes = new ArrayList<PExpr>();
    if (ee == null) {
      aes.add(PExpr.createDummyVoidExpr(e.getSrcInfo(), scope));
    } else {
      while (ee != null) {
        PExpr expr = PExpr.acceptX(ee, scope);
        if (expr == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        aes.add(expr);
        ee = ee.getNextSibling();
      }
    }
    builder.setAction(PExprList.Seq.create(si, scope, aes));
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    this.guard.collectModRefs();
    this.action.collectModRefs();
  }

  public PIfClause resolve() throws CompileException {
    this.guard = this.guard.resolve();
    this.action = this.action.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.guard.normalizeTypes();
    this.action.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    graph.createCondNode(this.guard).setInNode(this.guard.setupTypeGraph(graph));  // null guard needed??
    this.typeGraphNode = graph.createRefNode(this);
    this.typeGraphNode.setInNode(this.action.setupTypeGraph(graph));
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.BranchNode node = flow.createNodeForIfClause(this.srcInfo);
    GFlow.CondNode cn = flow.createNodeForCond(this.srcInfo);
    cn.addChild(this.guard.setupFlow(flow));
    node.addChild(flow.createTrialNodeInBranch(this.srcInfo, cn, node));
    node.addChild(this.action.setupFlow(flow));
    return node;
  }
}
