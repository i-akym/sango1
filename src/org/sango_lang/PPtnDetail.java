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

class PPtnDetail extends PExpr {
  // included in case ptn match

  // this.eval will become to be varref

  private PPtnDetail(Parser.SrcInfo srcInfo, PScope outerScope, PEval eval, PPtnMatch ptnMatch) {
    super(srcInfo, outerScope, eval, ptnMatch);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("ptn_detail[src=");
    buf.append(this.srcInfo);
    buf.append(",var=");
    buf.append(this.eval);
    buf.append(",ptnmatch=");
    buf.append(this.ptnMatch);
    buf.append("]");
    return buf.toString();
  }

  static PPtnDetail create(Parser.SrcInfo srcInfo, PScope outerScope, PEval eval, PPtnMatch ptnMatch) {
    return new PPtnDetail(srcInfo, outerScope, eval, ptnMatch);
  }

  static PPtnDetail accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    PExpr e = PExpr.accept(reader, outerScope);
    if (e == null) { return null; }
    if (e.ptnMatch == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(e.eval.getSrcInfo(), outerScope, e.eval, e.ptnMatch);
  }

  static PPtnDetail acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("ptn-detail")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Variable missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PEval ev = PEval.acceptX(e, outerScope);
    if (ev == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    e = e.getNextSibling();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern match missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PPtnMatch pm = PPtnMatch.acceptX(e, outerScope, PPtnMatch.CONTEXT_TRIAL);
    if (pm == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), outerScope, ev, pm);
  }

  static List<PPtnDetail> acceptSeq(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    List<PPtnDetail> detailList = new ArrayList<PPtnDetail>();
    PPtnDetail pd;
    int state = 0;
    while (state >= 0) {
      switch (state) {
      case 0:
        if ((pd = accept(reader, outerScope)) != null) {
          detailList.add(pd);
          state = 1;
        } else if (ParserA.acceptToken(reader, LToken.COMMA, ParserA.SPACE_DO_NOT_CARE) != null) {
          state = 0;
        } else {
          state = -1;
        }
        break;
      case 1:
        if (ParserA.acceptToken(reader, LToken.COMMA, ParserA.SPACE_DO_NOT_CARE) != null) {
          state = 0;
        } else {
          state = -1;
        }
      }
    }
    return detailList;
  }

  public PPtnDetail resolve() throws CompileException {
    StringBuffer emsg;
    PPtnDetail pd = (PPtnDetail)super.resolve();
    if (!(pd.eval instanceof PExprVarRef)) {
      emsg = new StringBuffer();
      emsg.append("Only variable can be specified for matching at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()); 
    }
    PExprVarRef var = (PExprVarRef)this.eval;
    if (this.scope.lookupEVar(var.name).getScope() != this.scope) {
      emsg = new StringBuffer();
      emsg.append("Variable \"");
      emsg.append(var.name);
      emsg.append("\" must be defined in case pattern or pattern details at ");
      emsg.append(var.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return pd;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    throw new RuntimeException("PPtnDetail#setupFlow with one parameter should not be called.");
  }

  GFlow.Node setupFlow(GFlow flow, GFlow.BranchNode b) {
    GFlow.SeqNode node = flow.createNodeForPtnMatchDetail(this.srcInfo);
    node.addChild(this.eval.setupFlow(flow));
    node.addChild(
      flow.createTrialNodeInBranch(
        this.srcInfo,
        flow.createMatchingRootNode(this.srcInfo, (GFlow.PtnMatchNode)this.ptnMatch.setupFlow(flow)),
        b));
    return node;
  }
}
