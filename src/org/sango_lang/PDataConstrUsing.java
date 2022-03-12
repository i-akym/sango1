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

class PDataConstrUsing extends PDefaultProgElem {
  private PDataConstrUsing() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("data_constr_using[src=");
    buf.append(this.srcInfo);
    buf.append("]");
    return buf.toString();
  }

  static PDataConstrUsing create(Parser.SrcInfo srcInfo) {
    PDataConstrUsing dcu = new PDataConstrUsing();
    dcu.srcInfo = srcInfo;
    return dcu;
  }

  static PDataConstrUsing accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    ParserA.Token token = ParserA.acceptToken(reader, LToken.COL_COL, spc);
    return (token != null)? create(token.getSrcInfo()): null;
  }

  public void setupScope(PScope scope) {
    throw new RuntimeException("PDataConstrUsing#setupScope() called. - " + this.toString());
  }

  public void collectModRefs() throws CompileException {
    throw new RuntimeException("PDataConstrUsing#collectModRefs() called. - " + this.toString());
  }

  public PDataConstrUsing resolve() throws CompileException {
    throw new RuntimeException("PDataConstrUsing#resolveId() called. - " + this.toString());
  }

  public void normalizeTypes() {
    throw new IllegalStateException("PDataConstrUsing#normalizeTypes should not be called.");
  }
}
