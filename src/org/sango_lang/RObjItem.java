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

abstract public class RObjItem extends RVMItem {

  public RObjItem(RuntimeEngine e) { super(e); }

  abstract public boolean objEquals(RFrame frame, RObjItem item);

  abstract public RType.Sig getTsig();

  public Cstr dump() {
    Cstr s = this.createDumpHeader();
    s.append(this.dumpInside());
    s.append(this.createDumpTrailer());
    return s;
  }

  public Cstr createDumpHeader() {
    Cstr r = new Cstr();
    RType.Sig tsig = this.getTsig();
    r.append('{');
    r.append(tsig.mod.repr());
    r.append('.');
    r.append(tsig.name.toJavaString());
    r.append('|');
    return r;
  }

  abstract public Cstr dumpInside();

  public Cstr createDumpTrailer() {
    Cstr r = new Cstr();
    r.append('}');
    return r;
  }

  public void debugRepr(RNativeImplHelper helper, RClosureItem self) {
    // default implementation; override this for contained RObjItems
    helper.setReturnValue(helper.cstrToArrayItem(this.dump()));
  }
}
