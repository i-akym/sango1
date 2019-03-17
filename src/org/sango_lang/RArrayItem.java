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

  public Cstr debugReprOfContents() {
    boolean allChar = true;
    for (int i = 0; allChar && i < this.items.length; i++) {
      allChar = this.items[i] instanceof RIntItem.CharObj;
    }
    Cstr s = new Cstr();
    if (this.items.length > 0 && allChar) {
      s.append('\"');
      for (int i = 0; i < this.items.length; i++) {
        s.append(Cstr.codePointToRawRepr(((RIntItem.CharObj)this.items[i]).value, true));
      }
      s.append('\"');
    } else {
      String sep = "";
      for (int i = 0; i < this.items.length; i++) {
        s.append(sep);
        s = s.append(this.items[i].debugReprOfContents());
        sep = ",";
      }
    }
    return s;
  }
}
