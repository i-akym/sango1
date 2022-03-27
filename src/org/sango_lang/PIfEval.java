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

class PIfEval extends PDefaultExprObj implements PEval {
  PIfClause[] clauses;

  private PIfEval(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("if[src=");
    buf.append(this.srcInfo);
    buf.append(",clauses=[");
    for (int i = 0; i < this.clauses.length; i++) {
      buf.append(this.clauses[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PIfEval ifEval;
    List<PIfClause> clauseList;

    static Builder newInstance(Parser.SrcInfo srcInfo) {
      return new Builder(srcInfo);
    }

    Builder(Parser.SrcInfo srcInfo) {
      this.ifEval = new PIfEval(srcInfo);
      this.clauseList = new ArrayList<PIfClause>();
    }

    // void setSrcInfo(Parser.SrcInfo si) {
      // this.ifEval.srcInfo = si;
    // }

    void addClause(PIfClause clause) {
      this.clauseList.add(clause);
    }

    PIfEval create() {
      this.ifEval.clauses = this.clauseList.toArray(new PIfClause[this.clauseList.size()]);
      return this.ifEval;
    }
  }

  static PIfEval accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    ParserA.Token next = reader.getNextToken();
    if (!next.tagEquals(LToken.LBRACE) || ParserA.acceptSpecifiedWord(reader, "if", spc) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(si);
    ParserA.acceptToken(reader, LToken.LBRACE, ParserA.SPACE_DO_NOT_CARE);
    PIfClause clause = null;
    int state = 0;
    while (state >= 0) {
      if (state == 0 && (clause = PIfClause.accept(reader)) != null) {
        builder.addClause(clause);
        state = 1;
      } else if (ParserA.acceptToken(reader, LToken.SEM, ParserA.SPACE_DO_NOT_CARE) != null) {
        state = 0;
      } else {
        state = -1;
      }
    }
    if (ParserA.acceptToken(reader, LToken.RBRACE, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Syntax error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      ParserA.Token et = reader.getToken();
      if (et.value != null) {
        emsg.append(" - ");
        emsg.append(et.value.token);
      }
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PIfEval acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("if")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PIfClause c = PIfClause.acceptX(e);
      if (c == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      builder.addClause(c);
      e = e.getNextSibling();
    }
    return builder.create();
  }

  public void setupScope(PScope scope) {
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i].setupScope(scope);
    }
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i].collectModRefs();
    }
  }

  public PIfEval resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i] = this.clauses[i].resolve();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createJoinNode(this, this.clauses.length);
    for (int i = 0; i < this.clauses.length; i++) {
      ((PTypeGraph.JoinNode)this.typeGraphNode).setBranchNode(i, this.clauses[i].setupTypeGraph(graph));
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.ForkNode node = flow.createNodeForIfBlock(this.srcInfo);
    for (int i = 0; i < this.clauses.length; i++) {
      node.addBranch((GFlow.BranchNode)this.clauses[i].setupFlow(flow));
    }
    return node;
  }
}
