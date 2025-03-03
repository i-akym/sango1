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
  PEid anchor;
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

  static PUndetEval create(Parser.SrcInfo srcInfo, PScope outerScope, PEid anchor, PEvalItem.ObjItem[] params) {
    PUndetEval e = new PUndetEval(srcInfo, outerScope);
    e.anchor = anchor;
    e.params = params;
    if (params.length > 0) {
      e.anchor.cutOffVar();
    }
    return e;
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    this.anchor.collectModRefs();
  }

  public PEval resolve() throws CompileException {
    PEval e;
    PDefDict.EidProps ep = this.scope.resolveEid(this.anchor);
    if (ep == null) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("\"");
      emsg.append(this.anchor.repr());
      emsg.append("\" is not defined at ");
      emsg.append(this.anchor.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    } else if (ep.cat == PDefDict.EID_CAT_VAR) {
      if (this.params.length > 0) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Variable \"");
        emsg.append(this.anchor.name);
        emsg.append("\" is not allowed at ");
        emsg.append(this.anchor.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.anchor.setVar();
      PExprVarDef v = this.scope.lookupEVar(this.anchor.name);
      e = PExprVarRef.create(this.anchor.srcInfo, this.scope, this.anchor.name, v._resolved_varSlot);
    } else if ((ep.cat & PDefDict.EID_CAT_DCON_EVAL) > 0) {
      this.anchor.setDconEval();
      e = PDataConstrEval.convertFromResolvedUndet(this.srcInfo, this.scope, this.anchor, this.params).resolve();
    } else if ((ep.cat & PDefDict.EID_CAT_FUN) > 0) {
      this.anchor.setFun();
      e = PStaticInvEval.create(this.srcInfo, this.scope, this.anchor, this.params).resolve();
    } else {
      throw new RuntimeException("Unknown item. " + this.anchor);
    }

    // PExprObj a = this.anchor.resolve();
    // PEval e;
    // if (a instanceof PExprVarRef) {
      // if (this.params.length > 0) {
        // StringBuffer emsg = new StringBuffer();
        // emsg.append("Neither function name nor data constructor found at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      // e = (PExprVarRef)a;
    // } else {
      // PEid anc = (PEid)a;
      // if (anc.maybeDcon()) {
        // e = PDataConstrEval.convertFromResolvedUndet(this.srcInfo, this.scope, anc, this.params);
      // } else {
        // e = PStaticInvEval.create(this.srcInfo, this.scope, anc, this.params);
      // }
    // }
    return e;
  }

  public void normalizeTypes() throws CompileException {
    throw new RuntimeException("PUndetEval#normalizeTypes is called.");
  }
}
