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
package sni_sango.sni_entity;

import org.sango_lang.Cstr;
import org.sango_lang.RClosureItem;
import org.sango_lang.RErefItem;
import org.sango_lang.RMemMgr;
import org.sango_lang.RFrame;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RType;
import org.sango_lang.RuntimeEngine;
import org.sango_lang.RWrefItem;

public class SNIbox {
  public static SNIbox getInstance(RuntimeEngine e) {
    return new SNIbox();
  }

  public void sni_read(RNativeImplHelper helper, RClosureItem self, RObjItem box) {
    RStructItem b = (RStructItem)box;
    RMemMgr.ExistenceItem ex = (RMemMgr.ExistenceItem)b.getFieldAt(0);
    helper.setReturnValue(ex.read());
  }

  public void sni_write(RNativeImplHelper helper, RClosureItem self, RObjItem box, RObjItem x) {
    RStructItem b = (RStructItem)box;
    RMemMgr.ExistenceItem ex = (RMemMgr.ExistenceItem)b.getFieldAt(0);
    helper.setReturnValue(ex.write(x));
  }
}
