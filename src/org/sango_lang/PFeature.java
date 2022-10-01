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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PFeature extends PDefaultProgObj {
  String mod;
  Cstr modName;
  String name;
  PType[] params;

  private PFeature(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.mod);
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
  }

  public PFeature resolve() throws CompileException {
    StringBuffer emsg;
    if (this.mod != null) {
      this.modName = this.scope.resolveModId(this.mod);
      if (this.modName == null) {
        emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(this.mod);
        emsg.append("\" not defined at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }

    // following code is a copy for referece...

    // if ((this.tconInfo = this.scope.resolveTcon(this.mod, this.tcon)) == null) {
      // emsg = new StringBuffer();
      // emsg.append("Type constructor \"");
      // emsg.append(PTypeId.repr(this.mod, this.tcon, false));
      // emsg.append("\" not defined at ");
      // emsg.append(this.tconSrcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // if (this.tconInfo.props.paramCount() >= 0 && this.params.length != this.tconInfo.props.paramCount()) {
      // emsg = new StringBuffer();
      // emsg.append("Parameter count of \"");
      // emsg.append(PTypeId.repr(this.mod, this.tcon, false));
      // emsg.append("\" mismatch at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString()) ;
    // }

    for (int i = 0; i < this.params.length; i++) {
      PType p = (PType)this.params[i].resolve();
      this.params[i] = p;
    }
    return this;
  }

  public void normalizeTypes() {
    throw new RuntimeException("PFeature#normalizeTypes not implemented.");
  }

  static class List{}
}
