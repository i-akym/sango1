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

class PExpr extends PDefaultExprObj implements PEval {
  PEval eval;
  PPtnMatch ptnMatch;

  PExpr(Parser.SrcInfo srcInfo, PEval eval, PPtnMatch ptnMatch) {
    super(srcInfo);
    this.srcInfo = srcInfo;
    this.eval = eval;
    this.ptnMatch = ptnMatch;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("expr[eval=");
    buf.append(this.eval);
    if (this.ptnMatch != null) {
      buf.append(",ptnmatch=");
      buf.append(this.ptnMatch);
    }
    buf.append("]");
    return buf.toString();
  }

  static PExpr create(Parser.SrcInfo srcInfo, PEval eval, PPtnMatch ptnMatch) {
    return new PExpr(srcInfo, eval, ptnMatch);
  }

  static PExpr create(PEval eval) {
    return create(eval.getSrcInfo(), eval, null);
  }

  static PExpr accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    PEval eval;
    if ((eval = PEval.accept(reader)) == null) { return null; }
    PPtnMatch ptnMatch = null;
    if (ParserA.acceptToken(reader, LToken.EQ, ParserA.SPACE_DO_NOT_CARE) != null) {
      if ((ptnMatch = PPtnMatch.accept(reader, PPtnMatch.CONTEXT_FIXED)) == null) {
        emsg = new StringBuffer();
        emsg.append("Pattern matching missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    return create(eval.getSrcInfo(), eval, ptnMatch);
  }

  static PExpr acceptEnclosed(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token lpar;
    if ((lpar = ParserA.acceptToken(reader, LToken.LPAR, spc)) == null) { return null; }
    PExpr expr;
    if ((expr = accept(reader)) == null) {
      emsg = new StringBuffer();
      emsg.append("Expression missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((ParserA.acceptToken(reader, LToken.RPAR, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("\")\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    expr.srcInfo = lpar.getSrcInfo();  // set source info to lpar's
    return expr;
  }

  static List<PExprObj> acceptPosdSeq(ParserA.TokenReader reader, int min) throws CompileException, IOException {
    StringBuffer emsg;
    List<PExprObj> evalList = new ArrayList<PExprObj>();
    PExprObj expr = null;
    int state = 0;
    while (state >= 0)  {
      switch (state) {
      case 0:
        if ((expr = accept(reader)) != null) {
          evalList.add(expr);
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
        if ((expr = accept(reader)) != null) {
          evalList.add(expr);
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
    if (evalList.size() < min) {
      emsg = new StringBuffer();
      emsg.append("Insufficient expressions at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return evalList;
  }
  static PExpr acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("expr")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Evaluation missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("eval")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      emsg = new StringBuffer();
      emsg.append("Evaluation missing at ");
      emsg.append(e.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PEval eval = PEval.acceptX(ee);
    if (eval == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(ee.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    e = e.getNextSibling();

    PPtnMatch pm = null;
    if (e != null) {
      pm = PPtnMatch.acceptX(e, PPtnMatch.CONTEXT_FIXED);
      if (pm == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
    }
    return create(elem.getSrcInfo(), eval, pm);
  }

  static List<PExpr> acceptSeq(ParserA.TokenReader reader, boolean voidAssumedWhenEmpty) throws CompileException, IOException {
    List<PExpr> exprList = new ArrayList<PExpr>();
    PExpr expr = null;
    int state = 0;
    while (state >= 0)  {
      if (state == 0 && (expr = PExpr.accept(reader)) != null) {
        exprList.add(expr);
        state = 1;
      } else if (ParserA.acceptToken(reader, LToken.COMMA, ParserA.SPACE_DO_NOT_CARE) != null) {
        state = 0;
      } else {
        state = -1;
      }
    }
    if (exprList.size() == 0 && voidAssumedWhenEmpty) {
      exprList.add(createDummyVoidExpr(reader.getCurrentSrcInfo()));
    }
    return exprList;
  }

  static PExpr createDummyVoidExpr(Parser.SrcInfo si) {
    try {
      PEval.Builder voidEvalBuilder = PEval.Builder.newInstance();
      voidEvalBuilder.setSrcInfo(si);
      voidEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, PModule.MOD_ID_LANG, "void$")));
      return create(si, voidEvalBuilder.create() , null);
    } catch (CompileException ex) {
      throw new RuntimeException(ex.toString());
    }
  }

  public void setupScope(PScope scope) {
    if (scope == this.scope) { return; }
    this.scope = scope;
    this.idResolved = false;
    this.eval.setupScope(scope);
    if (this.ptnMatch != null) {
      this.ptnMatch.setupScope(scope);
    }
  }

  public void collectModRefs() throws CompileException {
    this.eval.collectModRefs();
    if (this.ptnMatch != null) {
      this.ptnMatch.collectModRefs();
    }
  }

  public PExpr resolve() throws CompileException {
    if (this.idResolved) { return this; }
    this.eval = this.eval.resolve();
    if (this.ptnMatch != null) {
      this.ptnMatch = this.ptnMatch.resolve();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.eval.normalizeTypes();
    if (this.ptnMatch != null) {
      this.ptnMatch.normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = this.eval.setupTypeGraph(graph);
    if (this.ptnMatch != null) {
      this.ptnMatch.setupTypeGraph(graph).setInNode(this.typeGraphNode);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.Node node;
    if (this.ptnMatch != null) {
      GFlow.ForceMatchingNode fm = flow.createNodeForMatchingExpr(this.srcInfo);
      fm.addChild(this.eval.setupFlow(flow));
      fm.addChild(flow.createCopyNode(this.eval.getSrcInfo()));
      GFlow.MatchingRootNode mr = flow.createMatchingRootNode(this.srcInfo, (GFlow.PtnMatchNode)this.ptnMatch.setupFlow(flow));
      fm.addChild(flow.createTrialNodeInExpr(this.srcInfo, mr, fm));
      node = fm;
    } else {
      node = this.eval.setupFlow(flow);
    }
    return node;
  }
}
