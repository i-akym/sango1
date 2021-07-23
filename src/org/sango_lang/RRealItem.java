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

public class RRealItem extends RObjItem {
  double value;

  RRealItem(RuntimeEngine e, double value) {
    super(e);
    this.value = value;
    this.setHashValue(Double.hashCode(this.value));
  }

  // public static RRealItem get(RuntimeEngine e, double value) {
    // return e.memMgr.getRealItem(value);
  // }

  public boolean objEquals(RFrame frame, RObjItem item) {
    // NaN always not equal to NaN, so object ref comparison does not supply right result
    boolean eq;
    if (!(item instanceof RRealItem)) {
      eq = false;
    } else {
      RRealItem r = (RRealItem)item;
      eq = r.value == this.value;
    }
    return eq;
  }

  public RType.Sig getTsig() {
    return RType.createTsig(Module.MOD_LANG, "real", 0);
  }

  public double getValue() { return this.value; }

  public void doHash(RNativeImplHelper helper, RClosureItem self) {
    throw new RuntimeException("Hash value is already set.");
    // helper.setReturnValue(helper.getIntItem(Double.hashCode(this.value)));
  }

  public Cstr dumpInside() {
    return new Cstr(Double.toString(this.value));
  }
}
