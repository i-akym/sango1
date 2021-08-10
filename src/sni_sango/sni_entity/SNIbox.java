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

  public void sni_create_box(RNativeImplHelper helper, RClosureItem self, RObjItem x, RObjItem invalidator) {
    RStructItem maybeInvalidator = (RStructItem)invalidator;
    RClosureItem inv = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeInvalidator);
    RErefItem eref = helper.getCore().createEntity(x, inv);
    helper.setReturnValue(new BoxHItem(helper.getRuntimeEngine(), eref));
  }

  public void sni_read(RNativeImplHelper helper, RClosureItem self, RObjItem box) {
    BoxHItem b = (BoxHItem)box;
    helper.setReturnValue(b.read());
  }

  public void sni_write(RNativeImplHelper helper, RClosureItem self, RObjItem box, RObjItem x) {
    BoxHItem b = (BoxHItem)box;
    helper.setReturnValue(b.write(x));
  }

  public void sni_create_weak_holder(RNativeImplHelper helper, RClosureItem self, RObjItem box, RObjItem listener) {
    BoxHItem b = (BoxHItem)box;
    RStructItem maybeListener = (RStructItem)listener;
    RClosureItem lis = (RClosureItem)sni_sango.SNIlang.unwrapMaybeItem(helper, maybeListener);
    RClosureItem myLis = null;
    if (lis != null) {
      try {
        ListenerWrapper lw = new ListenerWrapper(lis);
        myLis = helper.createClosureOfNativeImplHere(
          "listener_f", 1, lw,
          lw.getClass().getMethod(
            "wlis", 
            new Class[] { RNativeImplHelper.class, RClosureItem.class, RObjItem.class }));
      } catch (Exception ex) {
        new RuntimeException(ex.toString());
      }
    }
    RWrefItem w = helper.getCore().createWeakHolder(b.eref, myLis);
    helper.setReturnValue(new WBoxHItem(helper.getRuntimeEngine(), w));
  }

  public void sni_get(RNativeImplHelper helper, RClosureItem self, RObjItem w) {
    WBoxHItem wb = (WBoxHItem)w;
    RErefItem e = wb.get();
    RObjItem ret = sni_sango.SNIlang.getMaybeItem(helper,
      (e != null)?
        new BoxHItem(helper.getRuntimeEngine(), e):
        null
    );
    helper.setReturnValue(ret);
  }

  public void sni_clear(RNativeImplHelper helper, RClosureItem self, RObjItem w) {
    WBoxHItem wb = (WBoxHItem)w;
    wb.clear();
  }

  public static class BoxHItem extends RObjItem {
    RErefItem eref;

    BoxHItem(RuntimeEngine e, RErefItem eref) {
      super(e);
      this.eref = eref;
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof BoxHItem)) {
        eq = false;
      } else {
        BoxHItem b = (BoxHItem)item;
        eq = b.eref == this.eref;
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.entity.box"), "box_h", 1);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(this.eref.hashCode()));
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }

    public RObjItem read() { return this.eref.read(); }

    public RObjItem write(RObjItem item) { return this.eref.write(item); }
  }

  public static class WBoxHItem extends RObjItem {
    RWrefItem wref;

    WBoxHItem(RuntimeEngine e, RWrefItem wref) {
      super(e);
      this.wref = wref;
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof WBoxHItem)) {
        eq = false;
      } else {
        WBoxHItem w = (WBoxHItem)item;
        eq = w.wref == this.wref;
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.entity.box"), "wbox_h", 1);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(this.wref.hashCode()));
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }

    public RErefItem get() { return this.wref.get(); }

    public void clear() { this.wref.clear(); }
  }


  public static class ListenerWrapper {
    RClosureItem lis;

    ListenerWrapper(RClosureItem lis) {
      this.lis = lis;
    }

    public void wlis(RNativeImplHelper helper, RClosureItem self, RObjItem wref) {
      if (helper.getAndClearResumeInfo() == null) {
        WBoxHItem w = new WBoxHItem(helper.getRuntimeEngine(), (RWrefItem)wref);
        helper.scheduleInvocation(this.lis, new RObjItem[] { w }, this);
      } else {
        ;  // return
      }
    }
  }
}
