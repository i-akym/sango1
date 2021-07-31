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

class PUndetPtn extends PDefaultPtnElem {
  PExprId anchor;

  private PUndetPtn() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("undet_ptn[src=");
    buf.append(this.srcInfo);
    buf.append(",anchor=");
    buf.append(this.anchor);
    buf.append("]");
    return buf.toString();
  }

  static PUndetPtn create(Parser.SrcInfo srcInfo, PExprId anchor) {
    PUndetPtn p = new PUndetPtn();
    p.srcInfo = srcInfo;
    p.anchor = anchor;
    return p;
  }

  static PUndetPtn create(PExprId anchor) {
    return create(anchor.srcInfo, anchor);
  }

  public PPtnElem setupScope(PScope scope) throws CompileException {
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    PPtnElem a = this.anchor.setupScope(scope);
    PPtnElem p;
    if (a instanceof PEVarRef) {
      p = a;
    } else {
      PExprId dcon = (PExprId)a;
      dcon.setCat(PExprId.CAT_DCON_PTN);
      p = PDataConstrPtn.create(
        this.srcInfo, dcon, new PPtnElem[0], new PPtnItem[0], false).setupScope(this.scope);
    }
    return p;
  }

  public PUndetPtn resolveId() throws CompileException {
    throw new RuntimeException("PUndetPtn#resolveId() called. - " + this.toString());
  }

  public void normalizeTypes() {
    throw new IllegalStateException("PUndetPtn#normalizeTypes should not be called.");
  }
}
