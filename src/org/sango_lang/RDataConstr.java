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
  public static final RDataConstr pseudoOfTuple = new RDataConstr(Module.MOD_LANG, Module.MOD_LANG, "@TUPLE", -1, "tuple", -1, null);

  Cstr modName;  // type origin def
  Cstr constrModName;  // actually constructing module
  String name;  // dcon name
  int attrCount;
  String tcon;
  int tparamCount;
  String callbackFunNameKey;  // maybe null

  RDataConstr(Cstr modName, Cstr constrModName, String name, int attrCount, String tcon, int tparamCount, String callbackFunNameKey) {
    this.modName = modName;
    this.constrModName = constrModName;
    this.name = name;
    this.attrCount = attrCount;
    this.tcon = tcon;
    this.tparamCount = tparamCount;
    this.callbackFunNameKey = callbackFunNameKey;
  }

  static RDataConstr create(Cstr modName, Cstr constrModName, String name, int attrCount, String tcon, int tparamCount, String callbackFunNameKey) {
    return new RDataConstr(modName, constrModName, name, attrCount, tcon, tparamCount, callbackFunNameKey);
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof RDataConstr)) {
      b = false;
    } else {
      RDataConstr d = (RDataConstr)o;
      b = d.modName.equals(this.modName) && d.constrModName.equals(this.constrModName) && d.name.equals(this.name);
    }
    return b;
  }

  public RType.Sig getTsig(RuntimeEngine e) {
    return RType.createTsig(this.modName, this.tcon, this.tparamCount);
  }

  public Cstr getModName() { return this.modName; }

  public String getName() { return this.name; }
}
