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

import java.lang.ref.WeakReference;

public class RWrefItem extends RObjItem {
  WeakReference<RErefItem> entityWref;

  RWrefItem(RuntimeEngine e) { super(e); }

  public static RWrefItem create(RuntimeEngine e, WeakReference<RErefItem> wr) {
    RWrefItem wref = new RWrefItem(e);
    wref.entityWref = wr;
    return wref;
  }

  public boolean objEquals(RFrame frame, RObjItem item) {
    boolean eq;
    if (item == this) {
      eq = true;
    } else if (!(item instanceof RWrefItem)) {
      eq = false;
    } else {
      RWrefItem w = (RWrefItem)item;
      eq = w.entityWref == this.entityWref;
    }
    return eq;
  }

  public RType.Sig getTsig() {
    return RType.createTsig(new Cstr("sango.entity"), "wref", 0);
  }

  public void doHash(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getIntItem(this.hashCode()));
  }

  public Cstr dumpInside() {
    return new Cstr(this.toString());
  }

  public RErefItem get() {
    return this.entityWref.get();
  }

  public void clear() {
    this.entityWref.clear();
  }
}
