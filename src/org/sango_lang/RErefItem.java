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

public class RErefItem extends RObjItem {
  RMemMgr.Entity entity;

  RErefItem(RuntimeEngine e) { super(e); }

  public static RErefItem create(RuntimeEngine e, RMemMgr.Entity entity) {
    RErefItem eref = new RErefItem(e);
    eref.entity = entity;
    return eref;
  }

  public boolean objEquals(RFrame frame, RObjItem item) {
    boolean eq;
    if (item == this) {
      eq = true;
    } else if (!(item instanceof RErefItem)) {
      eq = false;
    } else {
      RErefItem r = (RErefItem)item;
      eq = r.entity == this.entity;
    }
    return eq;
  }

  public RType.Sig getTsig() {
    return RType.createTsig(Module.MOD_LANG, "eref", 0);
  }

  public Cstr debugReprOfContents() {
    return new Cstr(this.toString() + "," + this.entity.toString() + "," + this.read().debugRepr().toJavaString());
  }

  public RObjItem read() { return this.entity.read(); }

  public RObjItem write(RObjItem item) { return this.entity.write(item); }
}
