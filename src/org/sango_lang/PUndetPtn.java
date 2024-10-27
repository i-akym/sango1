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

class PUndetPtn extends PDefaultExprObj {
  int context;
  PExprId anchor;

  private PUndetPtn(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("undet_ptn[src=");
    buf.append(this.srcInfo);
    buf.append(",anchor=");
    buf.append(this.anchor);
    buf.append("]");
    return buf.toString();
  }

  static PUndetPtn create(Parser.SrcInfo srcInfo, PScope outerScope, int context, PExprId anchor) {
    PUndetPtn p = new PUndetPtn(srcInfo, outerScope);
    p.context = context;
    p.anchor = anchor;
    return p;
  }

  public void collectModRefs() throws CompileException {
    this.anchor.collectModRefs();
  }

  public PExprObj resolve() throws CompileException {
    PExprObj p = this.anchor.resolve();
    if (p instanceof PExprVarRef) {
      ;
    } else {
      PExprId dcon = (PExprId)this.anchor;
      dcon.setCat(PDefDict.EID_CAT_DCON_PTN);
      p = PDataConstrPtn.convertFromResolvedUndet(this.srcInfo, this.scope, this.context, dcon);
    }
    return p;
  }
}
