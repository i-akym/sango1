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

class PSelfInv extends PDefaultProgElem {
  private PSelfInv(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("self_inv[src=");
    buf.append(this.srcInfo);
    buf.append("]");
    return buf.toString();
  }

  static PSelfInv create(Parser.SrcInfo srcInfo) {
    return new PSelfInv(srcInfo);
  }

  static PSelfInv accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    ParserA.Token token = ParserA.acceptToken(reader, LToken.AMP_AMP, spc);
    return (token != null)? create(token.getSrcInfo()): null;
  }

  // public void setupScope(PScope scope) {
    // throw new RuntimeException("PSelfInv#setupScope() should not be called. - " + this.toString());
  // }

  public void collectModRefs() throws CompileException {
    throw new RuntimeException("PSelfInv#collectModRefs() should not be called. - " + this.toString());
  }

  public PSelfInv resolve() throws CompileException {
    throw new RuntimeException("PSelfInv#resolveId() called. - " + this.toString());
  }

  public void normalizeTypes() {
    throw new IllegalStateException("PSelfInv#normalizeTypes should not be called.");
  }
}
