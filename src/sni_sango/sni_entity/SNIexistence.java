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
import org.sango_lang.Module;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RMemMgr;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;

public class SNIexistence {
  public static SNIexistence getInstance(RuntimeEngine e) {
    return new SNIexistence();
  }

  public void sni_create_immutable_existence(RNativeImplHelper helper, RClosureItem self, RObjItem assoc, RObjItem invalidator) {
    RStructItem maybeAssoc = (RStructItem)assoc;
    RObjItem a = sni_sango.SNIlang.unwrapMaybeItem(helper, maybeAssoc);
    RStructItem maybeInvalidator = (RStructItem)invalidator;
    RClosureItem inv = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeInvalidator);
    RObjItem[] es = helper.getCore().createImmutableExistence(a, inv);
    RStructItem maybeRoSlot = sni_sango.SNIlang.wrapMaybeItem(helper, es[1]);
    helper.setReturnValue(helper.getTupleItem(new RObjItem[] { es[0], maybeRoSlot }));
  }

  public void sni_create_mutable_existence(RNativeImplHelper helper, RClosureItem self, RObjItem assoc, RObjItem invalidator) {
    RStructItem maybeInvalidator = (RStructItem)invalidator;
    RClosureItem inv = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeInvalidator);
    RObjItem[] es = helper.getCore().createMutableExistence(assoc, inv);
    helper.setReturnValue(helper.getTupleItem(new RObjItem[] { es[0], es[1] }));
  }

  public void sni_imut_peek_assoc(RNativeImplHelper helper, RClosureItem self, RObjItem existence, RObjItem roSlot) {
    RMemMgr.ExistenceItem e = (RMemMgr.ExistenceItem)existence;
    RMemMgr.SlotItem s = (RMemMgr.SlotItem)roSlot;
    try {
      helper.setReturnValue(s.peekAssoc(e));
    } catch (IllegalArgumentException ex) {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_mut_peek_assoc(RNativeImplHelper helper, RClosureItem self, RObjItem existence, RObjItem rwSlot) {
    RMemMgr.ExistenceItem e = (RMemMgr.ExistenceItem)existence;
    RMemMgr.SlotItem s = (RMemMgr.SlotItem)rwSlot;
    try {
      helper.setReturnValue(s.peekAssoc(e));
    } catch (IllegalArgumentException ex) {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_mut_replace_assoc(RNativeImplHelper helper, RClosureItem self, RObjItem existence, RObjItem rwSlot, RObjItem newAssoc) {
    RMemMgr.ExistenceItem e = (RMemMgr.ExistenceItem)existence;
    RMemMgr.SlotItem s = (RMemMgr.SlotItem)rwSlot;
    try {
      helper.setReturnValue(s.replaceAssoc(e, newAssoc));
    } catch (IllegalArgumentException ex) {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_create_weak_ref(RNativeImplHelper helper, RClosureItem self, RObjItem existence, RObjItem listener) {
    RMemMgr.ExistenceItem e = (RMemMgr.ExistenceItem)existence;
    RStructItem maybeListener = (RStructItem)listener;
    RClosureItem lis = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeListener);
    helper.setReturnValue(helper.getCore().createWeakRef(e, lis));
  }

  public void sni_get(RNativeImplHelper helper, RClosureItem self, RObjItem w) {
    RMemMgr.WeakRefItem wr = (RMemMgr.WeakRefItem)w;
    RObjItem e = wr.get();
    RObjItem ret = sni_sango.SNIlang.getMaybeItem(helper, e);
    helper.setReturnValue(ret);
  }

  public void sni_clear(RNativeImplHelper helper, RClosureItem self, RObjItem w) {
    RMemMgr.WeakRefItem wr = (RMemMgr.WeakRefItem)w;
    wr.clear();
  }
}
