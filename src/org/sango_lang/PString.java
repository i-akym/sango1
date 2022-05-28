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
  PExprList.Elems elems;

  PString(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("string[src=");
    buf.append(this.srcInfo);
    buf.append(",elems=[");
    buf.append(this.elems);
    buf.append("]]");
    return buf.toString();
  }

  static PString create(Parser.SrcInfo srcInfo, PScope outerScope, boolean isFromCstr, PExprList.Elems elems) {
    PString s = new PString(srcInfo, outerScope);
    s.isFromCstr = isFromCstr;
    s.elems = elems;
    return s;
  }

  static PString accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.CSTR, spc)) != null) {
      return fromCstr(t, outerScope);
    } else if ((t = ParserA.acceptToken(reader, LToken.LBRACKET_VBAR, spc)) == null) {
      return null;
    }
    Parser.SrcInfo si = t.getSrcInfo();
    PExprList.Elems elems = PExprList.acceptElems(reader, reader.getCurrentSrcInfo(), outerScope, 0);
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
    return create(si, outerScope, false, elems);
  }

  static PString acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("string")) { return null; }
    Parser.SrcInfo si = elem.getSrcInfo();
    ParserB.Elem e = elem.getFirstChild();
    List<PExpr> es = new ArrayList<PExpr>();
    while (e != null) {
      PExpr expr = PExpr.acceptX(e, outerScope);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
        }
      es.add(expr);
      e = e.getNextSibling();
    }
    return create(si, outerScope, false, PExprList.Elems.create(si, outerScope, es));
  }

  static PString fromCstr(ParserA.Token cstrToken, PScope outerScope) throws CompileException {
    Cstr cstr = cstrToken.value.cstrValue;
    Parser.SrcInfo si = cstrToken.getSrcInfo();
    List<PExpr> es = new ArrayList<PExpr>();
    for (int i = 0; i < cstr.getLength(); i++) {
      PEval.Builder eb = PEval.Builder.newInstance(si, outerScope);
      // eb.setSrcInfo(si);
      eb.addItem(PEvalItem.ObjItem.create(si, outerScope, null, PChar.create(si, outerScope, cstr.getCharAt(i))));
      es.add(PExpr.create(eb.create()));
    }
    return create(si, outerScope, true, PExprList.Elems.create(si, outerScope, es));
  }

  // public void setupScope(PScope scope) {
    // if (scope == this.scope) { return; }
    // this.scope = scope;
    // this.idResolved = false;
    // this.elems.setupScope(scope);
  // }

  public void collectModRefs() throws CompileException {
    this.elems.collectModRefs();
  }

  public PString resolve() throws CompileException {
    // if (this.idResolved) { return this; }
    this.elems = this.elems.resolve();
    // this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.elems.normalizeTypes();
    if (this.isFromCstr) {
      this.nTypeSkel = this.scope.getCharStringType(this.srcInfo).getSkel();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    if (this.nTypeSkel != null) {
      this.typeGraphNode = graph.createDetNode(this);
    } else {  // one or more elements
      PTypeGraph.StringNode node = graph.createStringNode(this, this.elems.exprs.length);
      for (int i = 0; i < this.elems.exprs.length; i++) {
        node.setElemNode(i, this.elems.exprs[i].setupTypeGraph(graph));
      }
      this.typeGraphNode = node;
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.Node node;
    if (this.isFromCstr) {
      Cstr cstrValue = new Cstr();
      for (int i = 0; i < this.elems.exprs.length; i++) {
        PObjEval oe = (PObjEval)this.elems.exprs[i].eval;
        cstrValue.append(((PChar)oe.obj).value);
      }
      node = flow.createNodeForCstr(this.srcInfo, cstrValue);
    } else {
      GFlow.StringNode n = flow.createNodeForString(this.srcInfo);
      for (int i = 0; i < this.elems.exprs.length; i++) {
        n.addChild(this.elems.exprs[i].setupFlow(flow));
      }
      node = n;
    }
    return node;
  }
}
