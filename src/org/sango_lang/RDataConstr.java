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

public class RDataConstr {
  public static final RDataConstr pseudoOfTuple = new RDataConstr(Module.MOD_LANG, "@TUPLE", -1, "tuple", -1);

  Cstr modName;
  String name;
  int attrCount;
  String tcon;
  int tparamCount;

  RDataConstr(Cstr modName, String name, int attrCount, String tcon, int tparamCount) {
    this.modName = modName;
    this.name = name;
    this.attrCount = attrCount;
    this.tcon = tcon;
    this.tparamCount = tparamCount;
  }

  static RDataConstr create(Cstr modName, String name, int attrCount, String tcon, int tparamCount) {
    return new RDataConstr(modName, name, attrCount, tcon, tparamCount);
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof RDataConstr)) {
      b = false;
    } else {
      RDataConstr d = (RDataConstr)o;
      b = d.modName.equals(this.modName) && d.name.equals(this.name);
    }
    return b;
  }

  public RType.Sig getTsig(RuntimeEngine e) {
    return RType.createTsig(this.modName, this.tcon, this.tparamCount);
  }

  public Cstr getModName() { return this.modName; }

  public String getName() { return this.name; }
}
