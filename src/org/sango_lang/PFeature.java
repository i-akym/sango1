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
  String modId;
  Cstr modName;
  PTypeId fname;
  PType[] params;

  private PFeature(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("feature[");
    if (this.srcInfo != null) {
      buf.append("src=");
      buf.append(this.srcInfo);
      buf.append(",");
    }
    buf.append("name=");
    buf.append(PTypeId.repr(this.fname.modId, this.fname.name, false));
    buf.append(",params=[");
    String sep = "";
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i]);
      sep = ",";
    }
    buf.append("]]");
    return buf.toString();
  }

  static PFeature acceptSig(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), scope);
    int state = 0;
    while (state >= 0) {
      PTypeVarDef p;
      PTypeId n;
      if (state == 0 && (p = PTypeVarDef.accept(reader, scope)) != null) {
        builder.addParam(p);
      } else if (state == 0 && (n = PTypeId.accept(reader, scope, Parser.QUAL_INHIBITED, ParserA.SPACE_NEEDED)) != null) {
        builder.setName(n);
        state = -1;
      } else {
        emsg = new StringBuffer();
        emsg.append("Syntax error in feature signature at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    if (ParserA.acceptToken(reader, LToken.RBRACKET, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("] missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.modId);
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
  }

  public PFeature resolve() throws CompileException {
    StringBuffer emsg;
    if (this.modId != null) {
      this.modName = this.scope.resolveModId(this.modId);
      if (this.modName == null) {
        emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(this.modId);
        emsg.append("\" not defined at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }

    // following code is a copy for referece...

    // if ((this.tconInfo = this.scope.resolveTcon(this.modId, this.tcon)) == null) {
      // emsg = new StringBuffer();
      // emsg.append("Type constructor \"");
      // emsg.append(PTypeId.repr(this.modId, this.tcon, false));
      // emsg.append("\" not defined at ");
      // emsg.append(this.tconSrcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // if (this.tconInfo.props.paramCount() >= 0 && this.params.length != this.tconInfo.props.paramCount()) {
      // emsg = new StringBuffer();
      // emsg.append("Parameter count of \"");
      // emsg.append(PTypeId.repr(this.modId, this.tcon, false));
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

  static class Builder {
    PFeature feature;
    java.util.List<PType> params;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope scope) {
      return new Builder(srcInfo, scope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope scope) {
      this.feature = new PFeature(srcInfo, scope);
      this.params = new ArrayList<PType>();
    }

    void addParam(PTypeVarDef v) {
      this.params.add(v);
    }

    void setName(PTypeId n) {
      this.feature.fname = n;
    }

    PFeature create() {
      this.feature.params = this.params.toArray(new PType[this.params.size()]);
/* DEBUG */ System.out.println(this.feature);
      return this.feature;
    }
  }

  static class List{}
}
