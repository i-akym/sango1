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

abstract public class RType {

  public static Sig createTsig(Cstr mod, String name, int paramCount) {
    return new Sig(mod, new Cstr(name), paramCount);
  }

  public static class Sig {
    public Cstr mod;
    public Cstr name;
    public int paramCount;

    Sig(Cstr mod, Cstr name, int paramCount) {
      this.mod = mod;
      this.name = name;
      this.paramCount = paramCount;
    }

    public boolean equals(RFrame frame, RArrayItem aMod, RArrayItem aName, RIntItem iParamCount) {
      boolean b = aMod.equalsToCstr(this.mod);
      if (b) { b = aName.equalsToCstr(this.name); }
      if (b) { b = iParamCount.getValue() == this.paramCount; }
      return b;
    }
  }
}
