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

class PCaseEval extends PDefaultExprObj implements PEval {
  PExprObj obj;
  PCaseBlock caseBlock;

  private PCaseEval(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("case[src=");
    buf.append(this.srcInfo);
    buf.append(",obj=");
    buf.append(this.obj);
    buf.append(",case_block=");
    buf.append(this.caseBlock);
    buf.append("]");
    return buf.toString();
  }

  static PCaseEval create(Parser.SrcInfo srcInfo, PScope outerScope, PExprObj obj, PCaseBlock caseBlock) {
    PCaseEval e = new PCaseEval(srcInfo, outerScope);
    e.obj = obj;
    e.caseBlock = caseBlock;
    return e;
  }

  static PCaseEval acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("case")) { return null; }
    PCaseBlock.Builder builder = PCaseBlock.Builder.newInstance(elem.getSrcInfo(), outerScope);
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Value missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("value")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      emsg = new StringBuffer();
      emsg.append("Value missing at ");
      emsg.append(e.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PExprObj obj = PEval.acceptX(ee, outerScope);
    if (obj == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    e = e.getNextSibling();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Clauses missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("case-clauses")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    ee = e.getFirstChild();
    while (ee != null) {
      PCaseClause c = PCaseClause.acceptX(ee, outerScope);
      if (c == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      builder.addClause(c);
      ee = ee.getNextSibling();
    }
    return create(elem.getSrcInfo(), outerScope, obj, builder.create());
  }

  public void collectModRefs() throws CompileException {
    this.obj.collectModRefs();
    this.caseBlock.collectModRefs();
  }

  public PCaseEval resolve() throws CompileException {
    this.obj = this.obj.resolve();
    this.caseBlock = this.caseBlock.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.obj.normalizeTypes();
    this.caseBlock.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    PTypeGraph.Node on = this.obj.setupTypeGraph(graph);
    PTypeGraph.Node bn = this.caseBlock.setupTypeGraph(graph);
    this.caseBlock.setTypeGraphInNode(on);
    return bn;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForCaseEval(this.srcInfo);
    node.addChild(this.obj.setupFlow(flow));
    node.addChild(this.caseBlock.setupFlow(flow));
    return node;
  }
}
