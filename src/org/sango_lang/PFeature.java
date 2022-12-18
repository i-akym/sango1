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
  // String modId;
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

  static PFeature accept(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) { return null; }
    PFeature f = acceptDesc(reader, scope);
    if ((t = ParserA.acceptToken(reader, LToken.RBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("] missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return f;
  }

  static PFeature acceptDesc(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    Builder builder = Builder.newInstance(reader.getCurrentSrcInfo(), scope);
    int state = 0;
    int spc = ParserA.SPACE_DO_NOT_CARE;
    while (state >= 0) {
      PProgObj item;
      if ((item = PTypeId.accept(reader, scope, Parser.QUAL_MAYBE, spc)) != null) {
        builder.addItem(item);
        spc = ParserA.SPACE_NEEDED;
      } else if ((item = PTypeVarDef.accept(reader, scope)) != null) {
        builder.addItem(item);
        spc = ParserA.SPACE_NEEDED;
      } else if ((item = PType.accept(reader, scope, spc, true)) != null) {
        builder.addItem(item);
        spc = ParserA.SPACE_NEEDED;
      } else {
        state = -1;
      }
    }
    return builder.create();
  }

  static PFeature acceptSig(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    SigBuilder builder = SigBuilder.newInstance(t.getSrcInfo(), scope);
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
    this.scope.referredModId(this.srcInfo, this.fname.modId);
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
  }

  public PFeature resolve() throws CompileException {
    StringBuffer emsg;
    if (this.fname.modId != null) {
      this.modName = this.scope.resolveModId(this.fname.modId);
      if (this.modName == null) {
        emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(this.fname.modId);
        emsg.append("\" not defined at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    for (int i = 0; i < this.params.length; i++) {
      PType p = (PType)this.params[i].resolve();
      this.params[i] = p;
    }
    return this;
  }

  public void normalizeTypes() {
    throw new RuntimeException("PFeature#normalizeTypes not implemented.");
  }

  static class SigBuilder {
    PFeature feature;
    java.util.List<PType> params;

    static SigBuilder newInstance(Parser.SrcInfo srcInfo, PScope scope) {
      return new SigBuilder(srcInfo, scope);
    }

    SigBuilder(Parser.SrcInfo srcInfo, PScope scope) {
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

  static class Builder {
    PFeature feature;
    java.util.List<PProgObj> items;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope scope) {
      return new Builder(srcInfo, scope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope scope) {
      this.feature = new PFeature(srcInfo, scope);
      this.items = new ArrayList<PProgObj>();
    }

    void addItem(PProgObj item) {
      this.items.add(item);
    }

    PFeature create() throws CompileException {
      StringBuffer emsg;
      if (this.items.size() == 0) {
        emsg = new StringBuffer();
        emsg.append("Feature description missing at ");
        emsg.append(this.feature.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }

      PProgObj a = this.items.get(this.items.size() - 1);  // anchor item
      if (!(a instanceof PTypeId)) {
        emsg = new StringBuffer();
        emsg.append("Feature name missing at ");
        emsg.append(this.feature.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.feature.fname = (PTypeId)a;

      this.feature.params = new PType[this.items.size() - 1];
      for (int i = 0; i < this.items.size() - 1; i++) {
        PProgObj p = this.items.get(i);
        PType t = null;
        if (p instanceof PType) {
          t = (PType)p;
        } else if (p instanceof PTypeId) {
          t = PType.Undet.create((PTypeId)p);
        } else {
          emsg = new StringBuffer();
          emsg.append("Invalid feature parameter at ");
          emsg.append(p.getSrcInfo());
          emsg.append(". - ");
          emsg.append(p);
          throw new CompileException(emsg.toString());
        }
        this.feature.params[i] = t;
      }

      return this.feature;
    }
  }

  static class List{}
}
