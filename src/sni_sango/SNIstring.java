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

import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RResult;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;

public class SNIstring {
  public static SNIstring getInstance(RuntimeEngine e) {
    return new SNIstring();
  }

  public void sni_new_string(RNativeImplHelper helper, RClosureItem self, RObjItem count, RObjItem iter) {
    this.new_string(helper, self, count, iter, 1);
  }

  public void sni_new_reverse_string(RNativeImplHelper helper, RClosureItem self, RObjItem count, RObjItem iter) {
    this.new_string(helper, self, count, iter, -1);
  }

  public void new_string(RNativeImplHelper helper, RClosureItem self, RObjItem count, RObjItem iter, int direction) {
    Object[] iterInfo = (Object[])helper.getAndClearResumeInfo();  // [0] array; [1] next index to fill
    if (iterInfo == null) {
      int c = ((RIntItem)count).getValue();
      if (c < 0) {
        helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr("Negative element count."), null));
        return;
      }
      RArrayItem a = helper.createArrayItem(c);
      if (c == 0) {
        helper.setReturnValue(a);
      } else {
        RClosureItem nextf = (RClosureItem)((RStructItem)iter).getFieldAt(0);
        helper.scheduleInvocation(
          nextf,
          new RObjItem[0],
          new Object[] { a, (direction > 0)? 0: c - 1 });
      }
    } else {
      RResult res = helper.getInvocationResult();
      RObjItem ret = res.getReturnValue();  // <<A <A iter> tuple> maybe>
      if (ret == null) {  // exception is not caught in this frame, so ret must not be null
        throw new RuntimeException("Unexpected exception.");
      }
      RObjItem r = sni_sango.SNIlang.unwrapMaybeItem(helper, ret);
      if (r == null) {
        RObjItem oNoElem = sni_sango.SNIlang.createNoElemException(helper, new Cstr("Insufficient elements."), null);
        helper.setException(oNoElem);
        return;
      }
      RStructItem generated = (RStructItem)r;  // <A <A iter> tuple>
      RObjItem value = generated.getFieldAt(0);
      RArrayItem a = (RArrayItem)iterInfo[0];
      int idx = (Integer)iterInfo[1];
      a.setElemAt(idx, value);
      idx += direction;
      if (idx >= a.getElemCount() || idx < 0) {
        helper.setReturnValue(a);
      } else {
        RStructItem newIter = (RStructItem)generated.getFieldAt(1);
        RClosureItem newNextf = (RClosureItem)newIter.getFieldAt(0);
        helper.scheduleInvocation(newNextf, new RObjItem[0], new Object[] { a, idx });
      }
    }
  }

  public void sni_length(RNativeImplHelper helper, RClosureItem self, RObjItem xz) {
    RArrayItem a = (RArrayItem)xz;
    helper.setReturnValue(helper.getIntItem(a.getElemCount()));
  }

  public void sni_elem(RNativeImplHelper helper, RClosureItem self, RObjItem xz, RObjItem pos) {
    RArrayItem a = (RArrayItem)xz;
    RIntItem i = (RIntItem)pos;
    helper.setReturnValue(a.getElemAt(i.getValue()));
  }

  public void sni_slice(RNativeImplHelper helper, RClosureItem self, RObjItem xz, RObjItem start, RObjItem count) {
    RArrayItem a = (RArrayItem)xz;
    int aL = a.getElemCount();
    int p = ((RIntItem)start).getValue();
    int c = ((RIntItem)count).getValue();
    if (c == 0) {
      ;
    } else if (p < 0 || aL <= p || c < 0 || aL < p + c) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Start position and/or element count is invalid. - string length ");
      emsg.append(aL);
      emsg.append(", start position ");
      emsg.append(p);
      emsg.append(", count ");
      emsg.append(c);
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr(emsg.toString()), null));
      return;
    }
    RArrayItem s = helper.createArrayItem(c);
    int i;
    int j;
    for (i = 0, j = p; i < c; i++, j++) {
      s.setElemAt(i, a.getElemAt(j));
    }
    helper.setReturnValue(s);
  }


  public void sni_concat(RNativeImplHelper helper, RClosureItem self, RObjItem xzs) {
    int count = 0;
    RObjItem o = xzs;
    while (o instanceof RListItem.Cell) {
      RListItem.Cell L = (RListItem.Cell)o;
      count += ((RArrayItem)L.head).getElemCount();
      o = L.tail;
    }
    RArrayItem a = helper.createArrayItem(count);
    o = xzs;
    int i = 0;
    RArrayItem sa = null;
    int j = 0;
    while (i < count) {
      if (sa == null) {
        RListItem.Cell L = (RListItem.Cell)o;
        sa = (RArrayItem)L.head;
        j = 0;
      }
      if (j >= sa.getElemCount()) {
        sa = null;
        o = ((RListItem.Cell)o).tail;
      } else {
        a.setElemAt(i, sa.getElemAt(j));
        i++;
        j++;
      }
    }
    helper.setReturnValue(a);
  }
}
