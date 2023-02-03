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

class PImpose extends PDefaultExprObj {

  private PImpose(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  static PImpose create(Parser.SrcInfo srcInfo, PScope outerScope, PType type) {
    PImpose i = new PImpose(srcInfo, outerScope);
    i.type = type;
    return i;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("impose[");
    buf.append("src=");
    buf.append(this.srcInfo);
    buf.append(",type=");
    buf.append(this.type);
    buf.append("]");
    return buf.toString();
  }

  public void collectModRefs() throws CompileException {
    this.type.collectModRefs();
  }

  public PImpose resolve() throws CompileException {
    this.type = (PType)this.type.resolve();
    return this;
  }

  // public void normalizeTypes() throws CompileException {
    // StringBuffer emsg;
    // this.nTypeSkel = this.type.getNormalizedSkel();
    // if (!(this.nTypeSkel instanceof PTypeRefSkel)) {
      // emsg = new StringBuffer();
      // emsg.append("Non-concrete imposing at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // PTypeRefSkel tr = (PTypeRefSkel)this.nTypeSkel;
    // if (tr.ext) {
      // emsg = new StringBuffer();
      // emsg.append("Extended type not allowed for imposing at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // for (int i = 0; i < tr.params.length; i++) {
      // if (!tr.params[i].isLiteralNaked()) {
        // emsg = new StringBuffer();
        // emsg.append("Invalid imposing parameter at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
    // }
  // }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    StringBuffer emsg;
    PTypeSkel t = this.getNormalizedType();
    if (!(t instanceof PTypeRefSkel)) {
      emsg = new StringBuffer();
      emsg.append("Non-concrete imposing at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeRefSkel tr = (PTypeRefSkel)t;
    if (tr.ext) {
      emsg = new StringBuffer();
      emsg.append("Extended type not allowed for imposing at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < tr.params.length; i++) {
      if (!tr.params[i].isLiteralNaked()) {
        emsg = new StringBuffer();
        emsg.append("Invalid imposing parameter at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
// /* DEBUG */ System.out.println(this.getNormalizedType());
    this.typeGraphNode = graph.createDetNode(this);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForImpose(this.srcInfo, this.typeGraphNode.getFixedType());
  }
}
