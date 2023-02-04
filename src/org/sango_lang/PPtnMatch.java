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

class PPtnMatch extends PDefaultExprObj {
  static final int CONTEXT_FIXED = 1;
  static final int CONTEXT_TRIAL = 2;

  int context;
  PImpose impose;  // maybe null
  PExprObj ptn;

  private PPtnMatch(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("ptnmatch[src=");
    buf.append(this.srcInfo);
    if (this.impose != null) {
      buf.append(",impose=");
      buf.append(this.impose);
    }
    buf.append(",ptn=");
    buf.append(this.ptn);
    buf.append("]");
    return buf.toString();
  }

  static PPtnMatch create(Parser.SrcInfo srcInfo, PScope outerScope, int context, PImpose impose, PExprObj ptn) {
    PPtnMatch pm = new PPtnMatch(srcInfo, outerScope);
    pm.context = context;
    pm.impose = impose;
    pm.ptn = ptn;
    return pm;
  }

  static PPtnMatch accept(ParserA.TokenReader reader, PScope outerScope, int context) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    PType type;
    PType imposingType = null;
    PType cast = null;
    if ((type = PType.accept(reader, outerScope, ParserA.SPACE_DO_NOT_CARE)) != null) {
      if (ParserA.acceptToken(reader, LToken.EQ_EQ, ParserA.SPACE_DO_NOT_CARE) != null) {
        imposingType = type;
      } else {
        cast = type;
      }
    }
    PExprObj ptn = PPtn.accept(reader, outerScope, context, cast);
    if (type != null && ptn == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern missing or invalid at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return (ptn != null)?
      create(si, outerScope, context, (imposingType != null)? PImpose.create(si, outerScope, imposingType): null, ptn):
      null;
  }

  static PPtnMatch acceptX(ParserB.Elem elem, PScope outerScope, int context) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("ptn-match")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    Parser.SrcInfo impsi = null;
    PType t = null;
    if (e.getName().equals("impose")) {
      impsi = e.getSrcInfo();
      ParserB.Elem ee = e.getFirstChild();
      if (ee == null) {
        emsg = new StringBuffer();
        emsg.append("Type missing at ");
        emsg.append(e.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      t = PType.acceptX(ee, outerScope);
      if (t == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      e = e.getNextSibling();
    }
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("ptn")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern missing at ");
      emsg.append(e.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PExprObj p = PPtn.acceptX(ee, outerScope, context);
    if (p == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(ee.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    return create(
      elem.getSrcInfo(), outerScope, context,
      (t != null)? PImpose.create(impsi, outerScope, t): null,
      p);
  }

  static PPtnMatch acceptEnclosed(ParserA.TokenReader reader, PScope outerScope, int spc, int context) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token lpar;
    if ((lpar = ParserA.acceptToken(reader, LToken.LPAR, spc)) == null) { return null; }
    PPtnMatch ptnMatch;
    if ((ptnMatch = accept(reader, outerScope, context)) == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern missing at ");
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
    ptnMatch.srcInfo = lpar.getSrcInfo();  // set source info to lpar's
    ptnMatch.context = context;
    return ptnMatch;
  }

  static List<PPtnMatch> acceptPosdSeq(ParserA.TokenReader reader, PScope outerScope, int min, int context) throws CompileException, IOException {
    StringBuffer emsg;
    List<PPtnMatch> ptnMatchList = new ArrayList<PPtnMatch>();
    PPtnMatch ptnMatch = null;
    Parser.SrcInfo srcInfo = null;  // dummy
    int state = 0;
    while (state >= 0)  {
      srcInfo = reader.getCurrentSrcInfo();
      switch (state) {
      case 0:
        if ((ptnMatch = accept(reader, outerScope, context)) != null) {
          ptnMatchList.add(ptnMatch);
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
        if ((ptnMatch = accept(reader, outerScope, context)) != null) {
          ptnMatchList.add(ptnMatch);
          state = 1;
        } else {
          emsg = new StringBuffer();
          emsg.append("Pattern match missing at ");
          emsg.append(srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      }
    }
    if (ptnMatchList.size() < min) {
      emsg = new StringBuffer();
      emsg.append("Insufficient pattern matches at ");
      emsg.append(srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return ptnMatchList;
  }

  public void collectModRefs() throws CompileException {
    if (this.impose != null) {
      this.impose.collectModRefs();
    }
    this.ptn.collectModRefs();
  }

  public PPtnMatch resolve() throws CompileException {
    if (this.impose != null) {
      this.impose = this.impose.resolve();
    }
    this.ptn = this.ptn.resolve();
    return this;
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    if (this.impose != null) {
      this.nTypeSkel = this.scope.getLangPrimitiveType(this.impose.srcInfo, Module.TCON_EXPOSED).toSkel();
      this.typeGraphNode = graph.createDetNode(this);
      this.ptn.setupTypeGraph(graph).setInNode(this.impose.setupTypeGraph(graph));
    } else {
      this.typeGraphNode = this.ptn.setupTypeGraph(graph);
    }
    return this.typeGraphNode;
  }

  void setTypeGraphInNode(PTypeGraph.Node node) {
    this.typeGraphNode.setInNode(node);
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForPtnMatch(this.srcInfo);
    if (this.impose != null) {
      node.addChild(this.impose.setupFlow(flow));
    }
    node.addChild(this.ptn.setupFlow(flow));
    return node;
  }
}
