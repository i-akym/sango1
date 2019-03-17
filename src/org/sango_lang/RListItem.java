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

    public Cstr debugReprOfContents() {
      Cstr s = new Cstr();
      RObjItem o = this;
      String sep = "";
      while (o instanceof Cell) {
        s.append(sep);
        Cell c = (Cell)o;
        s = s.append(c.head.debugRepr());
        o = c.tail;
        sep = ",";
      }
      return s;
    }
  }

  public static class Nil extends RListItem {

    Nil(RuntimeEngine e) { super(e); }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return (item == this) || (item instanceof Nil);
    }

    public Cstr debugReprOfContents() {
      return new Cstr("NIL");
    }
  }
}
