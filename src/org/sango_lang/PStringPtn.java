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

class PStringPtn extends PDefaultExprObj {
  boolean isFromCstr;
  PPtnMatch[] elems;

  PStringPtn(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("string_ptn[src=");
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
    int context;  // PPtnMatch.CONTEXT_*
    PStringPtn string;
    List<PPtnMatch> elemList;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.string = new PStringPtn(srcInfo, outerScope);
      this.elemList = new ArrayList<PPtnMatch>();
    }

    void setContext(int context) {
      this.context = context;
    }

    void setFromCstr() {
      this.string.isFromCstr = true;
    }

    void addElem(PPtnMatch elem) {
      this.elemList.add(elem);
    }

    void addElemSeq(List<PPtnMatch> elemSeq) {
      for (int i = 0; i < elemSeq.size(); i++) {
        this.addElem(elemSeq.get(i));
      }
    }

    PStringPtn create() {
      this.string.elems = this.elemList.toArray(new PPtnMatch[this.elemList.size()]);
      return this.string;
    }
  }

  static PStringPtn accept(ParserA.TokenReader reader, PScope outerScope, int spc, int context) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.CSTR, spc)) != null) {
      return fromCstr(t, outerScope, context);
    } else if ((t = ParserA.acceptToken(reader, LToken.LBRACKET_VBAR, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    builder.addElemSeq(PPtnMatch.acceptPosdSeq(reader, outerScope, 0, context));
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

  static PStringPtn acceptX(ParserB.Elem elem, PScope outerScope, int context) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("string")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PPtnMatch pm = PPtnMatch.acceptX(e, outerScope, context);
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

  static PStringPtn fromCstr(ParserA.Token cstrToken, PScope outerScope, int context) {
    Builder builder = new Builder(cstrToken.getSrcInfo(), outerScope);
    builder.setContext(context);
    builder.setFromCstr();
    Cstr cstr = cstrToken.value.cstrValue;
    Parser.SrcInfo si = cstrToken.getSrcInfo();
    for (int i = 0; i < cstr.getLength(); i++) {
      builder.addElem(PPtnMatch.create(cstrToken.getSrcInfo(), outerScope, context, null, PChar.create(si, outerScope, cstr.getCharAt(i))));
    }
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i].collectModRefs();
    }
  }

  public PStringPtn resolve() throws CompileException {
    // if (this.idResolved) { return this; }
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i] = this.elems[i].resolve();
    }
    // this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i].normalizeTypes();
    }
    if (this.isFromCstr) {
      this.nTypeSkel = this.scope.getCharStringType(this.srcInfo).getSkel();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    if (this.nTypeSkel != null) {
      this.typeGraphNode = graph.createDetNode(this);
    } else {  // one or more elements
      this.typeGraphNode = graph.createStringPtnNode(this);
    }
    PTypeGraph.StringPtnElemNode n = graph.createStringPtnElemNode(this);
    n.setInNode(this.typeGraphNode);
    for (int i = 0; i < this.elems.length; i++) {
      this.elems[i].setupTypeGraph(graph).setInNode(n);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.StringPtnNode node = flow.createNodeForStringPtn(this.srcInfo);
    for (int i = 0; i < this.elems.length; i++) {
      node.addChild(this.elems[i].setupFlow(flow));
    }
    return node;
  }
}
