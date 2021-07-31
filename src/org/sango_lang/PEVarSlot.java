/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2021 AKIYAMA Isao                                         *
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

public class PEVarSlot {
  static int hashValue = 0;

  int hash;
  PEVarDef varDef;

  private PEVarSlot() {}

  static PEVarSlot create(PEVarDef varDef) {
    PEVarSlot s = createInternal();
    s.varDef = varDef;
    return s;
  }

  public static PEVarSlot createInternal() {
    PEVarSlot s = new PEVarSlot();
    s.hash = hashValue++;
    return s;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    if (this.varDef != null) {
      buf.append(this.varDef.name);
      buf.append(":VE");
    } else {
      buf.append("PSEUDO");
    }
    buf.append(this.hash);
    return buf.toString();
  }

  String repr() {
    return (this.varDef != null)? this.varDef.name: "$" + this.hash;
  }
}
