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

class PCaseClause extends PDefaultExprObj {
  PCasePtnMatch[] ptnMatches;
  PExpr[] guardExprs;  // maybe null
  PExpr[] actionExprs;  // void$ included if no expressions
  PScope outerScope;

  private PCaseClause(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("caseclause[src=");
    buf.append(this.srcInfo);
    buf.append(",ptnmatches=[");
    for (int i = 0; i < this.ptnMatches.length; i++) {
      buf.append(this.ptnMatches[i]);
      buf.append(",");
    }
    buf.append("]");
    if (this.guardExprs != null) {
      buf.append(",guard=[");
      for (int i = 0; i < this.guardExprs.length; i++) {
        buf.append(this.guardExprs[i]);
        buf.append(",");
      }
      buf.append("]");
    }
    buf.append(",exprs=[");
    for (int i = 0; i < this.actionExprs.length; i++) {
      buf.append(this.actionExprs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PCaseClause clause;
    List<PCasePtnMatch> ptnMatchList;
    List<PExpr> guardExprList;
    List<PExpr> actionExprList;

    static Builder newInstance(Parser.SrcInfo srcInfo) {
      return new Builder(srcInfo);
    }

    Builder(Parser.SrcInfo srcInfo) {
      this.clause = new PCaseClause(srcInfo);
      this.ptnMatchList = new ArrayList<PCasePtnMatch>();
      this.guardExprList = new ArrayList<PExpr>();
      this.actionExprList = new ArrayList<PExpr>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.clause.srcInfo = si;
    }

    void addPtnMatch(PCasePtnMatch ptnMatch) {
      this.ptnMatchList.add(ptnMatch);
    }

    void addPtnMatchList(List<PCasePtnMatch> ptnMatchList) {
      for (int i = 0; i < ptnMatchList.size(); i++) {
        this.addPtnMatch(ptnMatchList.get(i));
      }
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

    PCaseClause create() throws CompileException {
      StringBuffer emsg;
      if (this.ptnMatchList.size() == 0) {
        emsg = new StringBuffer();
        emsg.append("Pattern match missing at ");
        emsg.append(this.clause.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.clause.ptnMatches = this.ptnMatchList.toArray(new PCasePtnMatch[this.ptnMatchList.size()]);
      this.clause.guardExprs = this.guardExprList.isEmpty()? null: this.guardExprList.toArray(new PExpr[this.guardExprList.size()]);
      this.clause.actionExprs = this.actionExprList.toArray(new PExpr[this.actionExprList.size()]);
      return this.clause;
    }
  }

  static PCaseClause accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    List<PCasePtnMatch> ptnMatchList = PCasePtnMatch.acceptSeq(reader);
    if (ptnMatchList.isEmpty()) {
      return null;
    }
    Builder builder = Builder.newInstance(si);
    builder.addPtnMatchList(ptnMatchList);
    if (ParserA.acceptToken(reader, LToken.VBAR_VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
      List<PExpr> guardExprList = PExpr.acceptSeq(reader, false);
      if (guardExprList == null || guardExprList.size() == 0) {
        emsg = new StringBuffer();
        emsg.append("Condition missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.addGuardExprList(guardExprList);
    }
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

  static PCaseClause acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("case-clause")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern matches missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("case-ptn-matches")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern match missing at ");
      emsg.append(e.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    while (ee != null) {
      PCasePtnMatch pm = PCasePtnMatch.acceptX(ee);
      if (pm == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      builder.addPtnMatch(pm);
      ee = ee.getNextSibling();
    }
    e = e.getNextSibling();
    if (e != null && e.getName().equals("cond")) {
      ee = e.getFirstChild();
      while (ee != null) {
        PExpr ex = PExpr.acceptX(ee);
        if (ex == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        builder.addGuardExpr(ex);
      }
      e = e.getNextSibling();
    }
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
        PExpr ex = PExpr.acceptX(ee);
        if (ex == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        builder.addActionExpr(ex);
        ee = ee.getNextSibling();
      }
    }
    return builder.create();
  }

  public void setupScope(PScope scope) {
    StringBuffer emsg;
    if (scope == this.outerScope) { return; }
    this.outerScope = scope;
    this.scope = scope.enterInner();
    this.idResolved = false;
    if (this.ptnMatches.length == 1) {
      this.ptnMatches[0].setupScope(this.scope);
    } else {
      this.scope.enableDefineEVar(false);
      for (int i = 0; i < this.ptnMatches.length; i++) {
        ptnMatches[i].setupScope(this.scope);
      }
      this.scope.enableDefineEVar(true);
    }
    if (this.guardExprs != null) {
      for (int i = 0; i < this.guardExprs.length; i++) {
        this.guardExprs[i].setupScope(this.scope);
      }
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i].setupScope(this.scope);
    }
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      ptnMatches[i].collectModRefs();
    }
    if (this.guardExprs != null) {
      for (int i = 0; i < this.guardExprs.length; i++) {
        this.guardExprs[i].collectModRefs();
      }
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i].collectModRefs();
    }
  }

  public PCaseClause resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i] = this.ptnMatches[i].resolve();
    }
    if (this.guardExprs != null) {
      for (int i = 0; i < this.guardExprs.length; i++) {
        this.guardExprs[i] = this.guardExprs[i].resolve();
      }
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i] = this.actionExprs[i].resolve();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i].normalizeTypes();
    }
    if (this.guardExprs != null) {
      for (int i = 0; i < this.guardExprs.length; i++) {
        this.guardExprs[i].normalizeTypes();
      }
    }
    for (int i = 0; i < this.actionExprs.length; i++) {
      this.actionExprs[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createRefNode(this);
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i].setupTypeGraph(graph);
    }
    PTypeGraph.Node n = null;
    PExpr e = null;
    if (this.guardExprs != null) {
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
      graph.createCondNode(e).setInNode(n);
    }
    e = null;
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

  void setTypeGraphInNode(PTypeGraph.Node node) {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i].setTypeGraphInNode(node);
    }
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.BranchNode node = flow.createNodeForCaseClause(this.srcInfo);
    GFlow.SeqNode gn = flow.createNodeForCaseGuard(this.srcInfo);
    if (this.ptnMatches.length == 1) {
      gn.addChild(this.ptnMatches[0].setupFlow(flow, node));
    } else {
      GFlow.ForkNode psn = flow.createNodeForCasePtnMatches(this.srcInfo);
      for (int i = 0; i < this.ptnMatches.length; i++) {
        psn.addBranch(flow.createNodeForCasePtnMatchOne(this.srcInfo, this.ptnMatches[i].setupFlow(flow, node), node));
      }
      gn.addChild(psn);
    }
    if (this.guardExprs != null) {
      GFlow.CondNode cn = flow.createNodeForCond(this.guardExprs[0].getSrcInfo());
      for (int i = 0; i < this.guardExprs.length; i++) {
        cn.addChild(this.guardExprs[i].setupFlow(flow));
      }
      gn.addChild(cn);
    }
    GFlow.TrialNode t = flow.createTrialNodeInBranch(this.srcInfo, gn, node);
    node.addChild(t);
    GFlow.SeqNode an = flow.createNodeForAction(this.actionExprs[0].getSrcInfo());
    for (int i = 0; i < this.actionExprs.length; i++) {
      an.addChild(flow.createSinkNode(this.actionExprs[i].getSrcInfo()));  // first 'sink' purges preserved input value of case block
      an.addChild(this.actionExprs[i].setupFlow(flow));
    }
    node.addChild(an);
    return node;
  }
}
