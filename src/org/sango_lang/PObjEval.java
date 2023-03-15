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

class PObjEval extends PDefaultExprObj implements PEval {
  PExprObj obj;

  private PObjEval(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("obj[src=");
    buf.append(this.srcInfo);
    buf.append(",obj=");
    buf.append(this.obj);
    buf.append("]");
    return buf.toString();
  }

  static PObjEval create(Parser.SrcInfo srcInfo, PScope outerScope, PExprObj obj) {
    PObjEval e = new PObjEval(srcInfo, outerScope);
    e.obj = obj;
    return e;
  }

  public void collectModRefs() throws CompileException {
    this.obj.collectModRefs();
  }

  public PObjEval resolve() throws CompileException {
    this.obj = this.obj.resolve();
    return this;
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    return this.obj.setupTypeGraph(graph);
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return this.obj.setupFlow(flow);
  }
}
