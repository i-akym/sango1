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

class PListConstr extends PList {
  PExprObj elem;
  PExprObj tail;

  private PListConstr(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("list[src=");
    buf.append(this.srcInfo);
    buf.append(",elem=");
    buf.append(this.elem);
    buf.append(",tail=");
    buf.append(this.tail);
    buf.append("]");
    return buf.toString();
  }

  static PListConstr create(Parser.SrcInfo srcInfo, PScope outerScope, PExprObj elem, PExprObj tail) {
    PListConstr c = new PListConstr(srcInfo, outerScope);
    c.elem = elem;
    c.tail = tail;
    return c;
  }

  public void collectModRefs() throws CompileException {
    this.elem.collectModRefs();
    this.tail.collectModRefs();
  }

  public PListConstr resolve() throws CompileException {
    this.elem = this.elem.resolve();
    this.tail = this.tail.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.elem.normalizeTypes();
    this.tail.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createListNode(this);
    ((PTypeGraph.ListNode)this.typeGraphNode).setElemNode(this.elem.setupTypeGraph(graph));
    ((PTypeGraph.ListNode)this.typeGraphNode).setTailNode(this.tail.setupTypeGraph(graph));
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.ListNode node = flow.createNodeForList(this.srcInfo);
    node.addChild(this.elem.setupFlow(flow));
    node.addChild(this.tail.setupFlow(flow));
    return node;
  }
}
