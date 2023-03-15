/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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

import java.util.List;
import java.util.ArrayList;

abstract public class Option {
  int sn;  // sequence number in each class

  Option() {
    this.sn = this.nextSN();
  }

  abstract int nextSN();

  // immutable set of options
  public static class Set<T extends Option> {
    List<T> options;

    private Set(Set<T> s) {
      this.options = new ArrayList<T>(s.options);
    }

    public Set() {
      this.options = new ArrayList<T>();
    }

    public String toString() { return this.options.toString(); }

    public boolean contains(T o) {
      return o == ((o.sn < this.options.size())? this.options.get(o.sn): null);
    }

    public List<T> list() {
      List<T> L = new ArrayList<T>();
      for (int i = 0; i < this.options.size(); i++) {
        T o;
        if ((o = this.options.get(i)) != null) {
          L.add(o);
        }
      }
      return L;
    }
  
    public Set<T> add(T o) {
      Set<T> s = new Set<T>(this);
      if (o.sn < s.options.size()) {
        s.options.set(o.sn, o);
      } else {
        for (int i = s.options.size(); i < o.sn; i++) {
          s.options.add(null);
        }
        s.options.add(o);
      }
      return s;
    }

    public Set<T> intersection(Set<T> s) {
      Set<T> t = new Set<T>();
      for (int i = 0; i < s.options.size(); i++) {
        T o = s.options.get(i);
        if (o != null && this.options.get(i) == o) {
          t = t.add(o);
        }
      }
    return t;
    }
  }

  // immutable switch
  public static class Switch<T extends Option> {
    T sw;  // on = non-null, off = null

    public Switch() {}

    public Switch(T o) {
      this.sw = o;
    }

    public T getSwich() { return this.sw; }

    public boolean isOn() { return this.sw != null; }

    public boolean isOff() { return this.sw == null; }

    public Switch<T> setOn(T o) {
      return new Switch<T>(o);
    }

    public Switch<T> setOff() {
      return new Switch<T>();
    }
  }
}
