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

public class RArrayItem extends RObjItem {
  RObjItem[] items;
  
  static RArrayItem create(RuntimeEngine e, int size) {
    return e.memMgr.createArrayItem(size);
  }

  RArrayItem(RuntimeEngine e, int size) {
    super(e);
    this.items = new RObjItem[size];
    for (int i = 0; i < size; i++) {
      this.items[i] = null;
    }
  }

  public boolean objEquals(RFrame frame, RObjItem item) {
    boolean b;
    if (item == this) {
      b = true;
    } else if (!(item instanceof RArrayItem)) {
      b = false;
    } else {
      RArrayItem a = (RArrayItem)item;
      if (a.items.length != this.items.length) {
        b = false;
      } else {
        b = true;
        for (int i = 0; b && i < this.items.length; i++) {
          b = a.items[i].objEquals(frame, this.items[i]);
        }
      }
    }
    return b;
  }

  public RType.Sig getTsig() {
    return RType.createTsig(Module.MOD_LANG, "string", 1);
  }

  public int getElemCount() { return this.items.length; }

  public RObjItem getElemAt(int index) { return this.items[index]; }

  public void setElemAt(int index, RObjItem elem) { this.items[index] = elem; }

  public void setCharElemAt(int index, int c) {
    this.setElemAt(index, this.theEngine.memMgr.getCharItem(c));
  }

  public boolean equalsToCstr(Cstr cstr) {
    boolean b = cstr.getLength() == this.items.length;
    for (int i = 0; b && i < this.items.length; i++) {
      b = cstr.getCharAt(i) == ((RIntItem)this.items[i]).getValue();
    }
    return b;
  }

  public void doHash(RNativeImplHelper helper, RClosureItem self) {
    Object[] ris = (Object[])helper.getAndClearResumeInfo();
    if (ris == null) {
      int h = -1;
      if (this.items.length > 0) {
        helper.scheduleHash(this.items[0], new Object[] { 0, h });
      } else {
        helper.setReturnValue(helper.getIntItem(h));
      }
    } else {
      RIntItem hx = (RIntItem)helper.getInvocationResult().getReturnValue();
      int current = (Integer)ris[0];
      int h = (Integer)ris[1] * 31;  // multiply by prime num
      h ^= hx.getValue();
      int next = current + 1;
      if (next < this.items.length) {
        helper.scheduleHash(this.items[next], new Object[] { next, h });
      } else {
        helper.setReturnValue(helper.getIntItem(h));
      }
    }
  }

  public Cstr dumpInside() {
    // TODO: improve performance ; many copying of chars
    Cstr s = new Cstr();
    String sep = "";
    for (int i = 0; i < this.items.length; i++) {
      s.append(sep);
      s.append(this.items[i].dump());
      sep = ",";
    }
    return s;
  }

  public void doDebugRepr(RNativeImplHelper helper, RClosureItem self) {
    Object[] ris = (Object[])helper.getAndClearResumeInfo();
    if (ris == null) {
      if (this.items.length == 0) {
        Cstr r = this.createDumpHeader();
        r.append(this.createDumpTrailer());
        helper.setReturnValue(helper.cstrToArrayItem(r));
      } else {
        boolean allChar = true;
        for (int i = 0; allChar && i < this.items.length; i++) {
          allChar = this.items[i] instanceof RIntItem.CharObj;
        }
        if (allChar) {
          Cstr r = this.createDumpHeader();
          r.append('\"');
          for (int i = 0; i < this.items.length; i++) {
            r.append(Cstr.codePointToRawRepr(((RIntItem.CharObj)this.items[i]).value, true));
          }
          r.append('\"');
          r.append(this.createDumpTrailer());
          helper.setReturnValue(helper.cstrToArrayItem(r));
        } else {
          Cstr r = this.createDumpHeader();
          helper.scheduleDebugRepr(this.items[0], new Object[] { 0, r });
        }
      }
    } else {
      RArrayItem rx = (RArrayItem)helper.getInvocationResult().getReturnValue();
      int current = (Integer)ris[0];
      Cstr r = (Cstr)ris[1];
      r.append(helper.arrayItemToCstr(rx));
      int next = current + 1;
      if (next < this.items.length) {
        r.append(',');
        helper.scheduleDebugRepr(this.items[next], new Object[] { next, r });
      } else {
        r.append(this.createDumpTrailer());
        helper.setReturnValue(helper.cstrToArrayItem(r));
      }
    }
  }
}
