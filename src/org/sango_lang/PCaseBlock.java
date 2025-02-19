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

class PCaseBlock extends PDefaultExprObj {
  PCaseClause[] clauses;

  private PCaseBlock(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("case_block[src=");
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
    PCaseBlock caseBlock;
    List<PCaseClause> clauseList;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.caseBlock = new PCaseBlock(srcInfo, outerScope);
      this.clauseList = new ArrayList<PCaseClause>();
    }

    PScope getScope() { return this.caseBlock.scope; }

    void addClause(PCaseClause clause) {
      this.clauseList.add(clause);
    }

    PCaseBlock create() {
      this.caseBlock.clauses = this.clauseList.toArray(new PCaseClause[this.clauseList.size()]);
      return this.caseBlock;
    }
  }

  static PCaseBlock accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    ParserA.Token next = reader.getNextToken();
    if (!next.tagEquals(LToken.LBRACE) || ParserA.acceptSpecifiedWord(reader, "case", spc) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(si, outerScope);
    PScope scope = builder.getScope();
    ParserA.acceptToken(reader, LToken.LBRACE, ParserA.SPACE_DO_NOT_CARE);
    PCaseClause clause = null;
    int state = 0;
    while (state >= 0) {
      if (state == 0 && (clause = PCaseClause.accept(reader, scope)) != null) {
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

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i].collectModRefs();
    }
  }

  public PCaseBlock resolve() throws CompileException {
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i] = this.clauses[i].resolve();
    }
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createJoinNode(this, this.clauses.length);
    for (int i = 0; i < this.clauses.length; i++) {
      ((PTypeGraph.JoinNode)this.typeGraphNode).setBranchNode(i, this.clauses[i].setupTypeGraph(graph));
    }
    return this.typeGraphNode;
  }

  void setTypeGraphInNode(PTypeGraph.Node node) {
    for (int i = 0; i < this.clauses.length; i++) {
      this.clauses[i].setTypeGraphInNode(node);
    }
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.ForkNode node = flow.createNodeForCaseBlock(this.srcInfo);
    for (int i = 0; i < this.clauses.length; i++) {
      node.addBranch((GFlow.BranchNode)this.clauses[i].setupFlow(flow));
    }
    return node;
  }
}
