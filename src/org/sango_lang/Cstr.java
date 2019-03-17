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

import java.util.ArrayList;
import java.util.List;

public class Cstr {
  List<Integer> value;
  boolean hashed;
  int hashCodeValue;

  public Cstr() {
    this.value = new ArrayList<Integer>();
  }

  public Cstr(String s) {
    this();
    int len = s.length();
    for (int i = 0; i < len; i = s.offsetByCodePoints(i, 1)) {
      this.append(s.codePointAt(i));
    }
  }

  public String toString() {
    return this.value.toString();
  }

  public String repr() {
    StringBuffer buf = new StringBuffer();
    buf.append("\"");
    for (int i = 0; i < this.value.size(); i++) {
      buf.append(codePointToRawRepr(this.value.get(i), true));
    }
    buf.append("\"");
    return buf.toString();
  }

  public int hashCode() {
    if (!this.hashed) {
      this.hashCodeValue = 0;
      for (int i = 0; i < this.value.size(); i++) {
        this.hashCodeValue ^= this.value.get(i).intValue();
      }
      this.hashed = true;
    }
    return this.hashCodeValue;
  }

  public boolean equalsToString(String s) {
    // naive impl
    return this.toJavaString().equals(s);
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (o instanceof Cstr) {
      List<Integer> ov = ((Cstr)o).value;
      b = this.value.equals(ov);
    } else {
      b = false;
    }
    return b;
  }

  public int getLength() { return this.value.size(); }

  public int getCharAt(int i) {
    return this.value.get(i).intValue();
  }

  public int lastIndexOf(int c) {
    int idx = -1;
    for (int i = this.value.size() - 1; idx < 0 && i >= 0; i--) {
      if (this.value.get(i) == c) {
        idx = i;
      }
    }
    return idx;
  }

  public void append(int c) {
    this.value.add(new Integer(c));
    this.hashed = false;
  }

  public void append(char c) {
    this.append((int)c);
  }

  public void append(String s) {
    int len = s.length();
    for (int i = 0; i < len; i = s.offsetByCodePoints(i, 1)) {
      this.append(s.codePointAt(i));
    }
  }

  public String toJavaString() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < this.value.size(); i++) {
      buf.appendCodePoint(this.value.get(i).intValue());
    }
    return buf.toString();
  }

  public Cstr substring(int start, int end) {
    Cstr s = new Cstr();
    for (int i = start; i < end; i++) {
      s.append(this.getCharAt(i));
    }
    return s;
  }

  public Cstr append(Cstr s) {
    Cstr ss = new Cstr();
    for (int i = 0; i < this.value.size(); i++) {
      ss.value.add(this.value.get(i));
    }
    for (int i = 0; i < s.value.size(); i++) {
      ss.value.add(s.value.get(i));
    }
    return ss;
  }

  public static String codePointToJavaString(int c) {
    StringBuffer buf = new StringBuffer();
    buf.appendCodePoint(c);
    return buf.toString();
  }

  public static String codePointToRawRepr(int c, boolean inString) {
    String s;
    switch (c) {
    case '\\': s = "\\\\"; break;
    case '\n': s = "\\n"; break;
    case '\r': s = "\\r"; break;
    case '\t': s = "\\t"; break;
    case '\'': s = (!inString)? "\\\'": "\'"; break;
    case '\"': s = inString? "\\\"": "\""; break;
    case '`': s = "``"; break;
    default: s = codePointToJavaString(c); break;
    }
    return s;
  }
}
