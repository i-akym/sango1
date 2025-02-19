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
  PExprList.Seq guard;  // maybe null
  PExprList.Seq action;  // void$ included if no expressions
  PScope outerScope;

  private PCaseClause(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
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
    if (this.guard != null) {
      buf.append(",guard=[");
      buf.append(this.guard);
      buf.append("]");
    }
    buf.append(",action=[");
    buf.append(this.action);
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PCaseClause clause;
    List<PCasePtnMatch> ptnMatchList;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.clause = new PCaseClause(srcInfo, outerScope);
      this.ptnMatchList = new ArrayList<PCasePtnMatch>();
    }

    PScope getScope() { return this.clause.scope; }

    // void setSrcInfo(Parser.SrcInfo si) {
      // this.clause.srcInfo = si;
    // }

    void addPtnMatch(PCasePtnMatch ptnMatch) {
      this.ptnMatchList.add(ptnMatch);
    }

    void addPtnMatchList(List<PCasePtnMatch> ptnMatchList) {
      for (int i = 0; i < ptnMatchList.size(); i++) {
        this.addPtnMatch(ptnMatchList.get(i));
      }
    }

    void setGuard(PExprList.Seq seq) {
      this.clause.guard = seq;
    }

    void setAction(PExprList.Seq seq) {
      this.clause.action = seq;
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
      return this.clause;
    }
  }

  static PCaseClause accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    Builder builder = Builder.newInstance(si, outerScope);
    PScope scope = builder.getScope();
    List<PCasePtnMatch> ptnMatchList = PCasePtnMatch.acceptSeq(reader, scope);
    if (ptnMatchList.isEmpty()) {
      return null;
    }
    builder.addPtnMatchList(ptnMatchList);
    if (ParserA.acceptToken(reader, LToken.VBAR_VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
      PExprList.Seq guardSeq = PExprList.acceptSeq(reader, reader.getCurrentSrcInfo(), scope, false);
      if (guardSeq == null || guardSeq.exprs.length == 0) {
        emsg = new StringBuffer();
        emsg.append("Condition missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setGuard(guardSeq);
    }
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

  static PCaseClause acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("case-clause")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope scope = builder.getScope();
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
      PCasePtnMatch pm = PCasePtnMatch.acceptX(ee, scope);
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
      Parser.SrcInfo si = e.getSrcInfo();
      ee = e.getFirstChild();
      List<PExpr> ges = new ArrayList<PExpr>();
      while (ee != null) {
        PExpr ex = PExpr.acceptX(ee, scope);
        if (ex == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        ges.add(ex);
      }
      builder.setGuard(PExprList.Seq.create(si, scope, ges));
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
    Parser.SrcInfo si = e.getSrcInfo();
    ee = e.getFirstChild();
    List<PExpr> aes = new ArrayList<PExpr>();
    if (ee == null) {
      aes.add(PExpr.createDummyVoidExpr(e.getSrcInfo(), scope));
    } else {
      while (ee != null) {
        PExpr ex = PExpr.acceptX(ee, scope);
        if (ex == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        aes.add(ex);
        ee = ee.getNextSibling();
      }
    }
    builder.setAction(PExprList.Seq.create(si, scope, aes));
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      ptnMatches[i].collectModRefs();
    }
    if (this.guard != null) {
      this.guard.collectModRefs();
    }
    this.action.collectModRefs();
  }

  public PCaseClause resolve() throws CompileException {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i] = this.ptnMatches[i].resolve();
    }
    if (this.guard != null) {
      this.guard = this.guard.resolve();
    }
    this.action = this.action.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i].normalizeTypes();
    }
    if (this.guard != null) {
      this.guard.normalizeTypes();
    }
    this.action.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    for (int i = 0; i < this.ptnMatches.length; i++) {
      this.ptnMatches[i].setupTypeGraph(graph);
    }
    if (this.guard != null) {
      graph.createCondNode(this.guard).setInNode(this.guard.setupTypeGraph(graph));
    }
    this.typeGraphNode = graph.createRefNode(this);
    this.typeGraphNode.setInNode(this.action.setupTypeGraph(graph));
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
    if (this.guard != null) {
      GFlow.CondNode cn = flow.createNodeForCond(this.guard.getSrcInfo());
      cn.addChild(this.guard.setupFlow(flow));
      gn.addChild(cn);
    }
    node.addChild(flow.createTrialNodeInBranch(this.srcInfo, gn, node));
    node.addChild(this.action.setupFlow(flow));
    return node;
  }
}
