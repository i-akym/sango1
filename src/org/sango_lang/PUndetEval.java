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

class PUndetEval extends PDefaultEvalElem {
  PExprId anchor;
  PEvalElem params[];

  private PUndetEval() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("undet[src=");
    buf.append(this.srcInfo);
    buf.append(",anchor=");
    buf.append(this.anchor);
    buf.append(",params=[");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(this.params[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static PUndetEval create(Parser.SrcInfo srcInfo, PExprId anchor, PEvalElem[] params) {
    PUndetEval e = new PUndetEval();
    e.srcInfo = srcInfo;
    e.anchor = anchor;
    e.params = params;
    if (params.length > 0) {
      e.anchor.cutOffVar();
    }
    return e;
  }

  public PEvalElem setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].setupScope(scope);
    }
    PEvalElem e;
    PEvalElem a = this.anchor.setupScope(scope);
    if (a instanceof PVarRef) {
      e = a;
    } else {
      this.anchor = (PExprId)a;
      e = this;
    }
    return e;
  }

  public PEvalElem resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolveId();
    }
    this.anchor = this.anchor.resolveId();
    this.idResolved = true;
    PEvalElem e;
    if (this.anchor.maybeDcon()) {
      this.anchor.setCat(PExprId.CAT_DCON_EVAL);
      e = PDataConstrEval.create(this.srcInfo, this.anchor, this.params, new PEvalItem[0], null).setupScope(this.scope).resolveId();
    } else {
      e = PStaticInvEval.create(this.srcInfo, this.anchor, this.params).setupScope(this.scope).resolveId();
    }
    return e;
    // return (this.anchor.maybeDcon())?  // TODO: Improve!
      // PDataConstrEval.create(this.srcInfo, this.anchor, this.params, new PEvalItem[0], null).setupScope(this.scope).resolveId():
      // PStaticInvEval.create(this.srcInfo, this.anchor, this.params).setupScope(this.scope).resolveId();
  }

  public void normalizeTypes() {
    throw new IllegalStateException("PUndetEval#normalizeTypes should not be called.");
  }
}
