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

public class PTypeVarSlot {
  static int idValue = 0;

  int id;
  int variance;
  boolean requiresConcrete;

  private PTypeVarSlot() {}

  static PTypeVarSlot create(PTypeVarDef varDef) {
    return createInternal(varDef.variance, varDef.requiresConcrete);
  }

  public static PTypeVarSlot createInternal(int variance, boolean requiresConcrete) {
    PTypeVarSlot s = new PTypeVarSlot();
    s.id = idValue++;
    s.variance = variance;
    s.requiresConcrete = requiresConcrete;
    return s;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    switch (this.variance) {
    case Module.COVARIANT:
      buf.append("+");
      break;
    case Module.CONTRAVARIANT:
      buf.append("-");
      break;
    default:
      break;
    }
    buf.append("$");
    buf.append(this.id);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    return buf.toString();
  }

  String repr() {
    StringBuffer buf = new StringBuffer();
    switch (this.variance) {
    case Module.COVARIANT:
      buf.append("+");
      break;
    case Module.CONTRAVARIANT:
      buf.append("-");
      break;
    default:
      break;
    }
    buf.append("$");
    buf.append(this.id);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    return buf.toString();
  }
}
