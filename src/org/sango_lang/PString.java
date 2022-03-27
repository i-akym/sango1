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

class PString extends PDefaultExprObj {
  boolean isFromCstr;
  PExprObj[] elems;

  PString(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("string[src=");
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
    PString string;
    List<PExprObj> elemList;

    static Builder newInstance(Parser.SrcInfo srcInfo) {
      return new Builder(srcInfo);
    }

    Builder(Parser.SrcInfo srcInfo) {
      this.string = new PString(srcInfo);
      this.elemList = new ArrayList<PExprObj>();
    }

    // void setSrcInfo(Parser.SrcInfo si) {
      // this.string.srcInfo = si;
    // }

    void setFromCstr() {
      this.string.isFromCstr = true;
    }

    void addElem(PExprObj elem) {
      this.elemList.add(elem);
    }

    void addElemSeq(List<PExprObj> elemSeq) {
      for (int i = 0; i < elemSeq.size(); i++) {
        this.addElem(elemSeq.get(i));
      }
    }

    PString create() {
      this.string.elems = this.elemList.toArray(new PExprObj[this.elemList.size()]);
      return this.string;
    }
  }

  static PString accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.CSTR, spc)) != null) {
      return fromCstr(t);
    } else if ((t = ParserA.acceptToken(reader, LToken.LBRACKET_VBAR, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo());
    builder.addElemSeq(PExpr.acceptPosdSeq(reader, 0));
    if (ParserA.acceptToken(reader, LToken.VBAR_RBRACKET, ParserA.SPACE_DO_NOT_CARE) == null) {
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

  static PString acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("string")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PExprObj expr = PExpr.acceptX(e);
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

  static PString fromCstr(ParserA.Token cstrToken) {
    Builder builder = new Builder(cstrToken.getSrcInfo());
    builder.setFromCstr();
    Cstr cstr = cstrToken.value.cstrValue;
    Parser.SrcInfo si = cstrToken.getSrcInfo();
    for (int i = 0; i < cstr.getLength(); i++) {
      builder.addElem(PChar.create(si, cstr.getCharAt(i)));
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

  public PString resolve() throws CompileException {
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
    if (this.isFromCstr) {
      this.nTypeSkel = this.scope.getCharStringType(this.srcInfo).getSkel();
    // } else if (this.elems.length == 0)  {
      // this.nTypeSkel = this.scope.getEmptyStringType(this.srcInfo).getSkel();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    if (this.nTypeSkel != null) {
      this.typeGraphNode = graph.createDetNode(this);
    } else {  // one or more elements
      PTypeGraph.StringNode node = graph.createStringNode(this, this.elems.length);
      for (int i = 0; i < this.elems.length; i++) {
        node.setElemNode(i, this.elems[i].setupTypeGraph(graph));
      }
      this.typeGraphNode = node;
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.Node node;
    if (this.isFromCstr) {
      Cstr cstrValue = new Cstr();
      for (int i = 0; i < this.elems.length; i++) {
        cstrValue.append(((PChar)this.elems[i]).value);
      }
      node = flow.createNodeForCstr(this.srcInfo, cstrValue);
    } else {
      GFlow.StringNode n = flow.createNodeForString(this.srcInfo);
      for (int i = 0; i < this.elems.length; i++) {
        n.addChild(this.elems[i].setupFlow(flow));
      }
      node = n;
    }
    return node;
  }
}
