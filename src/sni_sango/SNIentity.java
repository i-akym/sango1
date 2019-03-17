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
package sni_sango;

import org.sango_lang.Module;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RErefItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;
import org.sango_lang.RWrefItem;

public class SNIentity {
  public static SNIentity getInstance(RuntimeEngine e) {
    return new SNIentity();
  }

  public void sni_create_entity(RNativeImplHelper helper, RClosureItem self, RObjItem x, RObjItem invalidator) {
    RStructItem maybeInvalidator = (RStructItem)invalidator;
    RClosureItem inv = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeInvalidator);
    helper.setReturnValue(helper.getCore().createEntity(x, inv));
  }

  public void sni_read(RNativeImplHelper helper, RClosureItem self, RObjItem entity) {
    RErefItem eref = (RErefItem)entity;
    helper.setReturnValue(eref.read());
  }

  public void sni_write(RNativeImplHelper helper, RClosureItem self, RObjItem entity, RObjItem x) {
    RErefItem eref = (RErefItem)entity;
    helper.setReturnValue(eref.write(x));
  }

  public void sni_create_weak_holder(RNativeImplHelper helper, RClosureItem self, RObjItem entity, RObjItem listener) {
    RErefItem e = (RErefItem)entity;
    RStructItem maybeListener = (RStructItem)listener;
    RClosureItem lis = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeListener);
    helper.setReturnValue(helper.getCore().createWeakHolder(e, lis));
  }

  public void sni_get(RNativeImplHelper helper, RClosureItem self, RObjItem w) {
    RWrefItem wref = (RWrefItem)w;
    RObjItem e = wref.get();
    RObjItem ret = sni_sango.SNIlang.getMaybeItem(helper, e);
    helper.setReturnValue(ret);
  }

  public void sni_clear(RNativeImplHelper helper, RClosureItem self, RObjItem w) {
    RWrefItem wref = (RWrefItem)w;
    wref.clear();
  }
}
