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

class PTuple extends PDefaultEvalElem {
  PEvalElem[] elems;

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("tuple[src=");
    buf.append(this.srcInfo);
    buf.append(",elems=[");
    for (int i = 0; i < this.elems.length; i++) {
      buf.append(this.elems[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PTuple tuple;
    List<PEvalElem> elemList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.tuple = new PTuple();
      this.elemList = new ArrayList<PEvalElem>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.tuple.srcInfo = si;
    }

    void addElem(PEvalElem elem) {
      this.elemList.add(elem);
    }

    void addElemList(List<PEvalElem> elemList) {
      for (int i = 0; i < elemList.size(); i++) {
        this.addElem(elemList.get(i));
      }
    }

    PTuple create() throws CompileException {
      StringBuffer emsg;
      if (this.elemList.size() < 2) {
        emsg = new StringBuffer();
        emsg.append("Insufficient expressions at ");
        emsg.append(this.tuple.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.tuple.elems = this.elemList.toArray(new PEvalElem[this.elemList.size()]); 
      return this.tuple;
    }
  }

  static PTuple accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LPAR_VBAR, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    builder.addElemList(PExpr.acceptPosdSeq(reader, 2));
    if (ParserA.acceptToken(reader, LToken.VBAR_RPAR, ParserA.SPACE_DO_NOT_CARE) == null) {
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

  static PTuple acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("tuple")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PEvalElem expr = PExpr.acceptX(e);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
        }
      builder.addElem(expr);
      e = e.getNextSibling();
    }
    return builder.create();
  }

  public void setupScope(PScope scope) {
    if (scope == this.scope) { return; }
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i].setupScope(scope);
    }
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i].collectModRefs();
    }
  }

  public PTuple resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i] = this.elems[i].resolve();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createTupleNode(this, this.elems.length);
    for (int i = 0; i < this.elems.length; i++) {
      ((PTypeGraph.TupleNode)this.typeGraphNode).setElemNode(i, this.elems[i].setupTypeGraph(graph));
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.TupleNode node = flow.createNodeForTuple(this.srcInfo);
    for (int i = 0; i < this.elems.length; i++) {
      node.addChild(this.elems[i].setupFlow(flow));
    }
    return node;
  }
}
