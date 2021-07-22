/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2019 Isao Akiyama                                         *
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

abstract public class RListItem extends RObjItem {

  RListItem(RuntimeEngine e) { super(e); }

  public RType.Sig getTsig() {
    return RType.createTsig(Module.MOD_LANG, "list", 1);
  }

  public static class Cell extends RListItem {
    public RObjItem head;
    public RListItem tail;

    Cell(RuntimeEngine e) { super(e); }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean b;
      if (item == this) {
        b = true;
      } else if (!(item instanceof Cell)) {
        b = false;
      } else {
        Cell c = (Cell)item;
        b = true;
        frame.os.push(this.tail);
        frame.os.push(c.tail);
        frame.os.push(this.head);
        frame.os.push(c.head);
      }
      return b;
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      Object[] ris = (Object[])helper.getAndClearResumeInfo();
      if (ris == null) {
        Cell c = this;
        RListItem L = this.tail;
        Integer h;
        while ((h = L.peekHashValue()) == null) {
          helper.pushOstack(c);
          c = (Cell)L;  // NIL's hash value is already set, so L is a cell
          L = c.tail;
        }
        helper.scheduleHash(c.head, new Object[] { c, h });  // the cell, tail's hash value
      } else {
        RIntItem hh = (RIntItem)helper.getInvocationResult().getReturnValue();
        Cell c = (Cell)ris[0];
        int h = (Integer)ris[1] * 31;  // multiple tail's hash value by primary num
        h ^= hh.getValue();
        if (c == this) {
          helper.setReturnValue(helper.getIntItem(h));
        } else {
          c.setHashValue(h);
          c = (RListItem.Cell)helper.popOstack();
          helper.scheduleHash(c.head, new Object[] { c, h });
        }
      }
    }

    public Cstr dumpInside() {
      Cstr s = new Cstr();
      String sep = "";
      RListItem L = this;
      while (L instanceof Cell) {
        Cell c = (Cell)L;
        s.append(sep);
        s.append(c.head.dump());
        L = c.tail;
        sep = ",";
      }
      return s;
    }

    public void doDebugRepr(RNativeImplHelper helper, RClosureItem self) {
      Object[] ris = (Object[])helper.getAndClearResumeInfo();
      if (ris == null) {
        Cstr r = this.createDumpHeader();
        helper.scheduleDebugRepr(this.head, new Object[] { this.tail, r });
      } else {
        RArrayItem rx = (RArrayItem)helper.getInvocationResult().getReturnValue();
        Cstr r = (Cstr)ris[1];
        r.append(helper.arrayItemToCstr(rx));
        if (ris[0] instanceof Nil) {
          r.append(this.createDumpTrailer());
          helper.setReturnValue(helper.cstrToArrayItem(r));
        } else {
          Cell c = (Cell)ris[0];
          r.append(',');
          helper.scheduleDebugRepr(c.head, new Object[] { c.tail, r });
        }
      }
    }
  }

  public static class Nil extends RListItem {

    Nil(RuntimeEngine e) {
      super(e);
      this.setHashValue(-1);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return (item == this) || (item instanceof Nil);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      throw new RuntimeException("Hash value is already set.");
      // helper.setReturnValue(helper.getIntItem(-1));
    }

    public Cstr dumpInside() {
      return new Cstr("NIL");
    }
  }
}
