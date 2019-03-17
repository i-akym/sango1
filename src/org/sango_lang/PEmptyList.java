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

class PEmptyList extends PList {

  private PEmptyList() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("NIL[src=");
    buf.append(this.srcInfo);
    buf.append("]");
    return buf.toString();
  }

  static PEmptyList create(Parser.SrcInfo srcInfo) {
    PEmptyList L = new PEmptyList();
    L.srcInfo = srcInfo;
    return L;
  }

  static PEmptyList acceptX(ParserB.Elem elem) throws CompileException {
    if (!elem.getName().equals("nil")) { return null; }
    return create(elem.getSrcInfo());
  }

  public PEmptyList setupScope(PScope scope) throws CompileException {
    this.scope = scope;
    this.idResolved = false;
    return this;
  }

  public PEmptyList resolveId() throws CompileException {
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() {
    // this.nTypeSkel = this.scope.getEmptyListType(this.srcInfo).getSkel();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createEmptyListNode(this);
    // this.typeGraphNode = graph.createDetNode(this);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForEmptyListBody(this.srcInfo);
    // GFlow.SeqNode n = flow.createNodeForEmptyList(this.srcInfo);
    // n.addChild(flow.createNodeForNewTvar(this.srcInfo, this.typeGraphNode.getNewvarList().get(0), this.scope));
    // n.addChild(flow.createNodeForEmptyListBody(this.srcInfo));
    // return n;
  }
}
