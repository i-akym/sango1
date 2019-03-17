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

class PTuplePtn extends PDefaultPtnElem {
  PPtnMatch[] elems;

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("tupleptn[src=");
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
    PTuplePtn tuple;
    List<PPtnMatch> elemList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.tuple = new PTuplePtn();
      this.elemList = new ArrayList<PPtnMatch>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.tuple.srcInfo = si;
    }

    void addElem(PPtnMatch elem) {
      elem.parent = this.tuple;
      this.elemList.add(elem);
    }

    void addElemList(List<PPtnMatch> elemList) {
      for (int i = 0; i < elemList.size(); i++) {
        this.addElem(elemList.get(i));
      }
    }

    PTuplePtn create() throws CompileException {
      StringBuffer emsg;
      if (this.elemList.size() < 2) {
        emsg = new StringBuffer();
        emsg.append("Insufficient patterns at ");
        emsg.append(this.tuple.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.tuple.elems = this.elemList.toArray(new PPtnMatch[this.elemList.size()]);
      return this.tuple;
    }
  }

  static PTuplePtn accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LPAR_VBAR, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    builder.addElemList(PPtnMatch.acceptPosdSeq(reader, 2));
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

  static PTuplePtn acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("tuple")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PPtnMatch pm = PPtnMatch.acceptX(e);
      if (pm == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
        }
      builder.addElem(pm);
      e = e.getNextSibling();
    }
    return builder.create();
  }

  public PTuplePtn setupScope(PScope scope) throws CompileException {
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i] = this.elems[i].setupScope(scope);
    }
    return this;
  }

  public PTuplePtn resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i] = this.elems[i].resolveId();
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
    this.typeGraphNode = graph.createTuplePtnNode(this, this.elems.length);
    for (int i = 0; i < this.elems.length; i++) {
      PTypeGraph.Node en = this.elems[i].setupTypeGraph(graph);
      PTypeGraph.TuplePtnElemNode en2 = graph.createTuplePtnElemNode(this, i);
      en2.setInNode(this.typeGraphNode);
      en.setInNode(en2);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.TuplePtnNode node = flow.createNodeForTuplePtn(this.srcInfo);
    for (int i = 0; i < this.elems.length; i++) {
      node.addChild(this.elems[i].setupFlow(flow));
    }
    return node;
  }
}
