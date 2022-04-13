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
  PScope outerScope;

  private PIfClause(Parser.SrcInfo srcInfo) {
    super(srcInfo);
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
    // List<PExpr> guardExprList;

    static Builder newInstance(Parser.SrcInfo srcInfo) {
      return new Builder(srcInfo);
    }

    Builder(Parser.SrcInfo srcInfo) {
      this.clause = new PIfClause(srcInfo);
      // this.guardExprList = new ArrayList<PExpr>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.clause.srcInfo = si;
    }

    // void addGuardExpr(PExpr expr) {
      // this.guardExprList.add(expr);
    // }

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
      // this.clause.guardExprs = this.guardExprList.toArray(new PExpr[this.guardExprList.size()]);
      return this.clause;
    }
  }

  static PIfClause accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    Builder builder = Builder.newInstance(si);
    PExprList.Seq guardSeq = PExprList.acceptSeq(reader, si, false);
    if (guardSeq == null || guardSeq.exprs.length == 0) {
      return null;
    }
    builder.setSrcInfo(si);
    builder.setGuard(guardSeq);
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setAction(PExprList.acceptSeq(reader, reader.getCurrentSrcInfo(), true));
    // builder.addActionExprList(PExpr.acceptSeq(reader, true));
    return builder.create();
  }

  static PIfClause acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("if-clause")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo());
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
      PExpr expr = PExpr.acceptX(ee);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      ges.add(expr);
      // builder.addGuardExpr(expr);
      ee = ee.getNextSibling();
    }
    builder.setGuard(PExprList.Seq.create(si, ges));
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
      aes.add(PExpr.createDummyVoidExpr(e.getSrcInfo()));
      // builder.addActionExpr(PExpr.createDummyVoidExpr(e.getSrcInfo()));
    } else {
      while (ee != null) {
        PExpr expr = PExpr.acceptX(ee);
        if (expr == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        aes.add(expr);
        // builder.addActionExpr(expr);
        ee = ee.getNextSibling();
      }
    }
    builder.setAction(PExprList.Seq.create(si, aes));
    return builder.create();
  }

  public void setupScope(PScope scope) {
    if (scope == this.outerScope) { return; }
    this.outerScope = scope;
    this.scope = scope.enterInner();
    this.idResolved = false;
    this.guard.setupScope(this.scope);
    this.action.setupScope(this.scope);
  }

  public void collectModRefs() throws CompileException {
    this.guard.collectModRefs();
    this.action.collectModRefs();
  }

  public PIfClause resolve() throws CompileException {
    if (this.idResolved) { return this; }
    this.guard = this.guard.resolve();
    this.action = this.action.resolve();
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.guard.normalizeTypes();
    this.action.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    graph.createCondNode(this.guard).setInNode(this.guard.setupTypeGraph(graph));  // null guard needed??
    // PExpr e = null;
    // PTypeGraph.Node n = null;
    // for (int i = 0; i < this.guardExprs.length; i++) {
      // e = this.guardExprs[i];
      // if (n == null) {
        // n = e.setupTypeGraph(graph);
      // } else {
        // PTypeGraph.SeqNode s = graph.createSeqNode(e);
        // s.setLeadingTypeNode(n);
        // s.setInNode(e.setupTypeGraph(graph));
        // n = s;
      // }
    // }
    // if (e != null) {
      // graph.createCondNode(e).setInNode(n);
    // }
    this.typeGraphNode = graph.createRefNode(this);
    this.typeGraphNode.setInNode(this.action.setupTypeGraph(graph));
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.BranchNode node = flow.createNodeForIfClause(this.srcInfo);
    GFlow.CondNode cn = flow.createNodeForCond(this.srcInfo);
    cn.addChild(this.guard.setupFlow(flow));
    // for (int i = 0; i < this.guardExprs.length - 1; i++) {
      // cn.addChild(this.guardExprs[i].setupFlow(flow));
      // cn.addChild(flow.createSinkNode(this.guardExprs[i].getSrcInfo()));
    // }
    // cn.addChild(this.guardExprs[this.guardExprs.length - 1].setupFlow(flow));
    node.addChild(flow.createTrialNodeInBranch(this.srcInfo, cn, node));
    node.addChild(this.action.setupFlow(flow));
    return node;
  }
}
