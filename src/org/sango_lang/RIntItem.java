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

abstract public class RIntItem extends RObjItem {
  int value;

  RIntItem(RuntimeEngine e, int value) {
    super(e);
    this.value = value;
  }

  // public static RIntItem get(RuntimeEngine e, int cat, int value) {
    // return e.memMgr.getIntItem(cat, value);
  // }

  static RIntItem create(RuntimeEngine e, int cat, int value) {
    RIntItem i;
    switch (cat) {
    case MInstruction.INT_OBJ_INT:
      i = new IntObj(e, value);
      break;
    case MInstruction.INT_OBJ_BYTE:
      i = new ByteObj(e, value);
      break;
    case MInstruction.INT_OBJ_CHAR:
      i = new CharObj(e, value);
      break;
    default:
      throw new IllegalArgumentException("Illegal intitem category.");
    }
    return i;
  }

  public int getValue() { return this.value; }

  static class IntObj extends RIntItem {
    IntObj(RuntimeEngine e, int value) { super(e, value); }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof IntObj)) {
        eq = false;
      } else {
        IntObj i = (IntObj)item;
        eq = i.value == this.value;
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(Module.MOD_LANG, "int", 0);
    }

    public Cstr debugReprOfContents() {
      return new Cstr(Integer.toString(this.value));
    }
  }

  static class ByteObj extends RIntItem {
    ByteObj(RuntimeEngine e, int value) { super(e, value); }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof ByteObj)) {
        eq = false;
      } else {
        ByteObj i = (ByteObj)item;
        eq = i.value == this.value;
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(Module.MOD_LANG, "byte", 0);
    }

    public Cstr debugReprOfContents() {
      Cstr s = new Cstr(Integer.toString(this.value));
      s.append('~');
      return s;
    }
  }

  static class CharObj extends RIntItem {
    CharObj(RuntimeEngine e, int value) { super(e, value); }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof CharObj)) {
        eq = false;
      } else {
        CharObj i = (CharObj)item;
        eq = i.value == this.value;
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(Module.MOD_LANG, "char", 0);
    }

    public Cstr debugReprOfContents() {
      Cstr s = new Cstr();
      s.append('\'');
      s.append(Cstr.codePointToRawRepr(this.value, false));
      s.append('\'');
      return s;
    }
  }
}
