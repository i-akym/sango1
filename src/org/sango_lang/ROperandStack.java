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

class ROperandStack {
  private static final int SEG_SIZE = 100;

  RFrame theFrame;
  Seg activeSeg;
  int index;

  ROperandStack(RFrame f) {
    this.theFrame = f;
    this.index = -1;
  }

  void reuse() {
    this.activeSeg.reuse();
    this.index = -1;
  }

  Pointer getPointer() {
    return new Pointer(this.theFrame.theTaskControl.theMgr.theEngine);
  }

  void push(RVMItem item) {
    if (this.index < 0) {  // empty
      if (this.activeSeg == null) {
        this.activeSeg = new Seg(null);
      }
      this.index = 0;
    } else {
      this.index++;
      if (this.index >= SEG_SIZE) {  // seg full
        if (this.activeSeg.child == null) {
          this.activeSeg.child = new Seg(this.activeSeg);
        }
        this.activeSeg = this.activeSeg.child;
        this.index = 0;
      }
    }
    this.activeSeg.items[this.index] = item;
  }

  RVMItem getTop() {
    if (this.index < 0) {
      throw new RuntimeException("Operand stack empty.");
    }
    return this.activeSeg.items[this.index];
  }

  RVMItem pop() {
    if (this.index < 0) {
      throw new RuntimeException("Operand stack empty.");
    }
    RVMItem item = this.activeSeg.items[this.index];
    this.activeSeg.items[this.index] = null;
    this.index--;
    if (this.index < 0) {
      this.activeSeg.child = null;  // release child seg
      if (this.activeSeg.parent != null) {
        this.activeSeg = this.activeSeg.parent;  // switch active seg and keep last seg
        this.index = SEG_SIZE - 1;
      }
    }
    return item;
  }

  RObjItem[] popMultipleObjItemsPushOrder(int count) {
    RObjItem[] a = new RObjItem[count];
    for (int i = count - 1; i >= 0; i--) {
      a[i] = (RObjItem)this.pop();
    }
    return a;
  }

  boolean isPointerEqualTo(Pointer p) {
    if (p.theStack != this) {
      throw new IllegalArgumentException("Unknown stack.");
    }
    return this.activeSeg == p.activeSeg && this.index == p.index;
  }

  void rewindTo(Pointer p) {
    if (p.theStack != this) {
      throw new IllegalArgumentException("Unknown stack.");
    }
    if (p.index < 0) {
      while (this.index >= 0) {
        this.pop();
      }
    } else {
      if (p.activeSeg == this.activeSeg && p.index > this.index) {
        throw new IllegalArgumentException("Cannot rewind.");
      }
      Seg s = this.activeSeg;
      while (s != null && s != p.activeSeg) {
        s = s.parent;
      }
      if (s == null) {
        throw new IllegalArgumentException("Cannot rewind.");
      }
      while (this.activeSeg != p.activeSeg || this.index != p.index) {
        this.pop();
      }
    }
  }

  class Pointer extends RVMItem {
    ROperandStack theStack;
    Seg activeSeg;
    int index;

    Pointer(RuntimeEngine e) {
      super(e);
      this.theStack = ROperandStack.this;
      this.activeSeg = ROperandStack.this.activeSeg;
      this.index = ROperandStack.this.index;
    }
  }

  private static class Seg {
    Seg parent;
    Seg child;
    RVMItem[] items;

    Seg(Seg parent) {
      this.parent = parent;
      this.items = new RVMItem[SEG_SIZE];
      this.clear();
    }

    void clear() {
      for (int i = 0; i < SEG_SIZE; i++) {
        this.items[i] = null;
      }
    }

    void reuse() {
      this.parent = null;
      this.clear();
    }
  }
}
