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

class PUndetEval extends PDefaultExprObj implements PEval {
  PExprId anchor;
  PEvalItem.ObjItem params[];

  private PUndetEval(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

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

  static PUndetEval create(Parser.SrcInfo srcInfo, PScope outerScope, PExprId anchor, PEvalItem.ObjItem[] params) {
    PUndetEval e = new PUndetEval(srcInfo, outerScope);
    e.anchor = anchor;
    e.params = params;
    if (params.length > 0) {
      e.anchor.cutOffVar();
    }
    return e;
  }

  // public void setupScope(PScope scope) {
    // StringBuffer emsg;
    // if (scope == this.scope) { return; }
    // this.scope = scope;
    // this.idResolved = false;
    // for (int i = 0; i < this.params.length; i++) {
      // this.params[i].setupScope(scope);
    // }
    // this.anchor.setupScope(scope);
  // }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    this.anchor.collectModRefs();
  }

  public PEval resolve() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolve();
    }
    PExprObj a = this.anchor.resolve();
    PEval e;
    if (a instanceof PExprVarRef) {
      if (this.params.length > 0) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Neither function name nor data constructor found at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      e = (PExprVarRef)a;
    } else {
      PExprId anc = (PExprId)a;
      if (anc.maybeDcon()) {
        e = PDataConstrEval.convertFromResolvedUndet(this.srcInfo, this.scope, anc, this.params);
        // e = PDataConstrEval.create(this.srcInfo, this.scope, anc, this.params, new PEvalItem.ObjItem[0], null);
      } else {
        e = PStaticInvEval.create(this.srcInfo, this.scope, anc, this.params);
      }
      // this.anchor = (PExprId)a;
      // e = this;
    }
    // if (e == this) {
      // if (this.anchor.maybeDcon()) {
        // this.anchor.setCat(PExprId.CAT_DCON_EVAL);
        // e = PDataConstrEval.create(this.srcInfo, this.scope, this.anchor, this.params, new PEvalItem.ObjItem[0], null);
        // // e = e.resolve();
      // } else {
        // e = PStaticInvEval.create(this.srcInfo, this.scope, this.anchor, this.params);
        // // e = e.resolve();
      // }
    // }
    return e;
  }

  public void normalizeTypes() {
    throw new IllegalStateException("PUndetEval#normalizeTypes should not be called.");
  }
}
