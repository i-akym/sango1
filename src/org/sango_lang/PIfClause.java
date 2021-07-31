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

class PIfClause extends PDefaultTypedElem {
  PExpr[] guardExprs;
  PExpr[] actionExprs;
  PScope outerScope;

  private PIfClause() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("ifclause[src=");
    buf.append(this.srcInfo);
    buf.append(",guard=[");
    for (int i = 0; i < this.guardExprs.length; i++) {
      buf.append(this.guardExprs[i]);
      buf.append(",");
    }
    buf.append("],exprs=[");
    for (int i = 0; i < this.actionExprs.length; i++) {
      buf.append(this.actionExprs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PIfClause clause;
    List<PExpr> guardExprList;
    List<PExpr> actionExprList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.clause = new PIfClause();
      this.guardExprList = new ArrayList<PExpr>();
      this.actionExprList = new ArrayList<PExpr>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.clause.srcInfo = si;
    }

    void addGuardExpr(PExpr expr) {
      this.guardExprList.add(expr);
    }

    void addGuardExprList(List<PExpr> exprList) {
      for (int i = 0; i < exprList.size(); i++) {
        this.addGuardExpr(exprList.get(i));
      }
    }

    void addActionExpr(PExpr expr) {
      this.actionExprList.add(expr);
    }

    void addActionExprList(List<PExpr> exprList) {
      for (int i = 0; i < exprList.size(); i++) {
        this.addActionExpr(exprList.get(i));
      }
    }

    PIfClause create() throws CompileException {
      StringBuffer emsg;
      if (this.guardExprList.size() == 0) {
        emsg = new StringBuffer();
        emsg.append("Condition missing at ");
        emsg.append(this.clause.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.clause.guardExprs = this.guardExprList.toArray(new PExpr[this.guardExprList.size()]);
      this.clause.actionExprs = this.actionExprList.toArray(new PExpr[this.actionExprList.size()]);
      return this.clause;
    }
  }

  static PIfClause accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance();
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    List<PExpr> guardExprList = PExpr.acceptSeq(reader, false);
    if (guardExprList == null || guardExprList.size() == 0) {
      return null;
    }
    builder.setSrcInfo(si);
    builder.addGuardExprList(guardExprList);
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addActionExprList(PExpr.acceptSeq(reader, true));
    return builder.create();
  }

  static PIfClause acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("if-clause")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());
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
    ParserB.Elem ee = e.getFirstChild();
    while (ee != null) {
      PExpr expr = PExpr.acceptX(ee);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      builder.addGuardExpr(expr);
      ee = ee.getNextSibling();
    }
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
    ee = e.getFirstChild();
    if (ee == null) {
      builder.addActionExpr(PExpr.createDummyVoidExpr(e.getSrcInfo()));
    } else {
      while (ee != null) {
        PExpr expr = PExpr.acceptX(ee);
        if (expr == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        builder.addActionExpr(expr);
        ee = ee.getNextSibling();
      }
    }
    return builder.create();
  }

  public PIfClause setupScope(PScope scope) throws CompileException {
    if (scope == this.outerScope) { return this; }
    this.outerScope = scope;
    this.scope = scope.enterInner();
    this.idResolved = false;
    for (int i = 0; i < this.guardExprs.length; i++) {
      this.guardExprs[i] = this.guardExprs[i].setupScope(this.scope);
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i] = this.actionExprs[i].setupScope(this.scope);
    }
    return this;
  }

  public PIfClause resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.guardExprs.length; i++) {
      this.guardExprs[i] = this.guardExprs[i].resolveId();
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i] = this.actionExprs[i].resolveId();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.guardExprs.length; i++) {
      this.guardExprs[i].normalizeTypes();
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createRefNode(this);
    PExpr e = null;
    PTypeGraph.Node n = null;
    for (int i = 0; i < this.guardExprs.length; i++) {
      e = this.guardExprs[i];
      if (n == null) {
        n = e.setupTypeGraph(graph);
      } else {
        PTypeGraph.SeqNode s = graph.createSeqNode(e);
        s.setLeadingTypeNode(n);
        s.setInNode(e.setupTypeGraph(graph));
        n = s;
      }
    }
    if (e != null) {
      graph.createCondNode(e).setInNode(n);
    }
    n = null;
    for (int i = 0; i < this.actionExprs.length; i++) {
      e = this.actionExprs[i];
      if (n == null) {
        n = e.setupTypeGraph(graph);
      } else {
        PTypeGraph.SeqNode s = graph.createSeqNode(e);
        s.setLeadingTypeNode(n);
        s.setInNode(e.setupTypeGraph(graph));
        n = s;
      }
    }
    this.typeGraphNode.setInNode(n);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.BranchNode node = flow.createNodeForIfClause(this.srcInfo);
    GFlow.CondNode cn = flow.createNodeForCond(this.srcInfo);
    for (int i = 0; i < this.guardExprs.length - 1; i++) {
      cn.addChild(this.guardExprs[i].setupFlow(flow));
      cn.addChild(flow.createSinkNode(this.guardExprs[i].getSrcInfo()));
    }
    cn.addChild(this.guardExprs[this.guardExprs.length - 1].setupFlow(flow));
    node.addChild(flow.createTrialNodeInBranch(this.srcInfo, cn, node));
    GFlow.SeqNode an = flow.createNodeForAction(this.actionExprs[0].getSrcInfo());
    for (int i = 0; i < this.actionExprs.length - 1; i++) {
      an.addChild(this.actionExprs[i].setupFlow(flow));
      an.addChild(flow.createSinkNode(this.actionExprs[i].getSrcInfo()));
    }
    an.addChild(this.actionExprs[this.actionExprs.length - 1].setupFlow(flow));
    node.addChild(an);
    return node;
  }
}
