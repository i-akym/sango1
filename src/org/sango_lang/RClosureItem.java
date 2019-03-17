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

public class RClosureItem extends RObjItem {
  RClosureImpl impl;
  RObjItem[] env;

  RClosureItem(RuntimeEngine e) { super(e); }

  public static RClosureItem create(RuntimeEngine e, RClosureImpl impl, RObjItem[] env) {
/* DEBUG */ if (impl == null) { throw new IllegalArgumentException("null impl"); }
    RClosureItem c = new RClosureItem(e);
    c.impl = impl;
    c.env = env;
    return c;
  }

  public boolean objEquals(RFrame frame, RObjItem item) {
    boolean b;
    if (item == this) {
      b = true;
    } else if (!(item instanceof RClosureItem)) {
      b = false;
    } else {
      RClosureItem c = (RClosureItem)item;
      b = c.impl.implEquals(this.impl);
      if (b) { 
        if (c.env.length != this.env.length) {
          b = false;
        } else {
          for (int i = 0; b && i < c.env.length; i++) {
            b = c.env[i].objEquals(frame, this.env[i]);
          }
        }
      }
    }
    return b;
  }

  public RType.Sig getTsig() {
    return RType.createTsig(Module.MOD_LANG, Module.TCON_FUN, this.impl.paramCount + 1);
  }

  int getParamCount() { return this.impl.paramCount; }

  public Cstr debugReprOfContents() {
    Cstr s = new Cstr();
    s.append(this.impl.mod.name.repr());
    s.append('.');
    s.append(this.impl.name);
    if (this.env.length > 0) {
      s.append(';');
      String sep = "";
      for (int i = 0; i < this.env.length; i++) {
        s.append(sep);
        s = s.append(this.env[i].debugRepr());
        sep = ",";
      }
    }
    return s;
  }
}
