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
  PTid fname;
  PType[] params;
  PDefDict.TidProps _resolved_featureProps;

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
    buf.append(PTid.repr(this.fname.modId, this.fname.name, false));
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
      if ((item = PTid.accept(reader, scope, Parser.QUAL_MAYBE, spc)) != null) {
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
      PTid n;
      if (state == 0 && (p = PTypeVarDef.accept(reader, scope)) != null) {
        builder.addParam(p);
      } else if (state == 0 && (n = PTid.accept(reader, scope, Parser.QUAL_INHIBITED, ParserA.SPACE_NEEDED)) != null) {
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

    if ((this._resolved_featureProps = this.scope.resolveFeature(this.fname)) == null) {
      emsg = new StringBuffer();
      emsg.append("Feature name \"");
      emsg.append(this.fname.repr());
      emsg.append("\" not defined at ");
      emsg.append(this.fname.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PFeatureDef def = this.scope.theMod.theCompiler.defDict.getFeatureDef(this.scope.theMod.actualName, this._resolved_featureProps.key);
    int pc = def.getParamCount();
    // PDefDict.TparamProps[] pss = def.getParamPropss();
    if (pc != this.params.length) {
    // if (pss.length != this.params.length) {
      emsg = new StringBuffer();
      emsg.append("Parameter count of \"");
      emsg.append(this.fname.repr());
      emsg.append("\" mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }

    for (int i = 0; i < this.params.length; i++) {
      PType p = (PType)this.params[i].resolve();
      this.params[i] = p;
    }
    return this;
  }

  void excludePrivateAcc() throws CompileException {
    if (this._resolved_featureProps.acc == Module.ACC_PRIVATE) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("\"");
      emsg.append(this.fname.repr());
      emsg.append("\" should not be private at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].excludePrivateAcc();
    }
  }

  PFeatureSkel toSkel() {
    PTypeSkel ps[] = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].toSkel();
    }
    return PFeatureSkel.create(this.scope.getCompiler(), this.srcInfo, this._resolved_featureProps.key, ps);
  }

  // PFeatureSkel getNormalizedSkel() throws CompileException {
    // PTypeSkel ps[] = new PTypeSkel[this.params.length];
    // for (int i = 0; i < ps.length; i++) {
      // ps[i] = this.params[i].getNormalizedSkel();
    // }
    // return PFeatureSkel.create(this.scope.getCompiler(), this.srcInfo, this._resolved_featureProps.key, ps);
  // }

  PFeature unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
    Builder builder = Builder.newInstance(srcInfo, scope);
    for (int i = 0; i < this.params.length; i++) {
      builder.addItem(this.params[i].unresolvedCopy(srcInfo, scope, extOpt, concreteOpt));
    }
    builder.addItem(this.fname.copy(srcInfo, scope, extOpt, concreteOpt));
    PFeature f = null;
    try {
      f = builder.create();
    } catch (Exception ex) {
      throw new RuntimeException("Internal error. " + ex.toString());
    }
    return f;
  }

  static class List extends PDefaultProgObj {
    PFeature[] features;

    private List(Parser.SrcInfo srcInfo, PScope scope) {
      super(srcInfo, scope);
    }

    static List accept(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
      StringBuffer emsg;
      ParserA.Token t;
      if ((t = ParserA.acceptToken(reader, LToken.LBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) { return null; }
      ListBuilder builder = ListBuilder.newInstance(t.getSrcInfo(), scope);
      PFeature f;
      int state = 0;
      while (state >= 0) {
        if (state == 0 && (f = acceptDesc(reader, scope)) != null) {
          builder.addFeature(f);
          state = 1;
        } else if (state == 1 && (ParserA.acceptToken(reader, LToken.COMMA, ParserA.SPACE_DO_NOT_CARE)) != null) {
          state = 0;
        } else {
          state = -1;
        }
      }
      if ((t = ParserA.acceptToken(reader, LToken.RBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("] missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return builder.create();
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("feature.list[");
      if (this.srcInfo != null) {
        buf.append("src=");
        buf.append(this.srcInfo);
        buf.append(",");
      }
      buf.append("features=[");
      String sep = "";
      for (int i = 0; i < this.features.length; i++) {
        buf.append(sep);
        buf.append(this.features[i]);
        sep = ",";
      }
      buf.append("]]");
      return buf.toString();
    }

    public void collectModRefs() throws CompileException {
      for (int i = 0; i < this.features.length; i++) {
        this.features[i].collectModRefs();
      }
    }

    public List resolve() throws CompileException {
      for (int i = 0; i < this.features.length; i++) {
        this.features[i] = this.features[i].resolve();
      }
      return this;
    }

    void excludePrivateAcc() throws CompileException {
      for (int i = 0; i < this.features.length; i++) {
        this.features[i].excludePrivateAcc();
      }
    }

    PFeatureSkel.List toSkel() {
      PFeatureSkel[] fss = new PFeatureSkel[this.features.length];
      for (int i = 0; i < fss.length; i++) {
        fss[i] = this.features[i].toSkel();
      }
      return PFeatureSkel.List.create(this.srcInfo, fss);
    }

    // PFeatureSkel.List getNormalizedSkel() throws CompileException {
      // PFeatureSkel[] fss = new PFeatureSkel[this.features.length];
      // for (int i = 0; i < fss.length; i++) {
        // fss[i] = this.features[i].getNormalizedSkel();
      // }
      // return PFeatureSkel.List.create(this.srcInfo, fss);
    // }

    List unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
      ListBuilder builder = ListBuilder.newInstance(srcInfo, scope);
      for (int i = 0; i < this.features.length; i++) {
        builder.addFeature(this.features[i].unresolvedCopy(srcInfo, scope, extOpt, concreteOpt));
      }
      List L = null;
      try {
        L = builder.create();
      } catch (Exception ex) {
        throw new RuntimeException("Internal error. " + ex.toString());
      }
      return L;
    }
  }

  static class SigBuilder {
    PFeature feature;
    java.util.List<PProgObj> params;

    static SigBuilder newInstance(Parser.SrcInfo srcInfo, PScope scope) {
      return new SigBuilder(srcInfo, scope);
    }

    SigBuilder(Parser.SrcInfo srcInfo, PScope scope) {
      this.feature = new PFeature(srcInfo, scope);
      this.params = new ArrayList<PProgObj>();
    }

    void addParam(PProgObj p) {
      this.params.add(p);
    }

    void setName(PTid n) {
      this.feature.fname = n;
    }

    PFeature create() throws CompileException {
      this.feature.params = new PType[this.params.size()];
      for (int i = 0; i < this.params.size(); i++) {
        PType.Builder tb = PType.Builder.newInstance(this.feature.srcInfo, this.feature.scope);
        tb.addItem(this.params.get(i));
        this.feature.params[i] = tb.create();
      }
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
      if (!(a instanceof PTid)) {
        emsg = new StringBuffer();
        emsg.append("Feature name missing at ");
        emsg.append(this.feature.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      // this.feature.modName = this.feature.scope.myModName();
      this.feature.fname = (PTid)a;

      this.feature.params = new PType[this.items.size() - 1];
      for (int i = 0; i < this.items.size() - 1; i++) {
        PProgObj p = this.items.get(i);
        PType t = null;
        if (p instanceof PType) {
          t = (PType)p;
        } else if (p instanceof PTid) {
          t = PType.Undet.create((PTid)p);
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

  static class ListBuilder {
    List list;
    java.util.List<PFeature> features;

    static ListBuilder newInstance(Parser.SrcInfo srcInfo, PScope scope) {
      return new ListBuilder(srcInfo, scope);
    }

    ListBuilder(Parser.SrcInfo srcInfo, PScope scope) {
      this.list = new List(srcInfo, scope);
      this.features = new ArrayList<PFeature>();
    }

    void addFeature(PFeature feature) {
      this.features.add(feature);
    }

    List create() throws CompileException {
      // StringBuffer emsg;
      // if (this.features.size() == 0) {
        // emsg = new StringBuffer();
        // emsg.append("Empty feature list at ");
        // emsg.append(this.list.getSrcInfo());
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      this.list.features = this.features.toArray(new PFeature[this.features.size()]);
      return this.list;
    }
  }
}
