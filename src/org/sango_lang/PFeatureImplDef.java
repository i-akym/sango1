/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2023 AKIYAMA Isao                                         *
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

class PFeatureImplDef extends PDefaultProgObj implements PDataDef.FeatureImpl {
  PEid provider;
  PFeature feature;
  String getter;
  PDefDict.EidProps _resolved_providerProps;

  PFeatureImplDef(Parser.SrcInfo srcInfo, PScope defScope) {
    super(srcInfo, defScope.enterInner());
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("feature_impl[src=");
    buf.append(this.srcInfo);
    buf.append(",provider=");
    buf.append(this.provider);
    buf.append(",feature=");
    buf.append(this.feature);
    buf.append(",getter=");
    buf.append(this.getter);
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PFeatureImplDef implDef;
    PFeature feature;
    String getter;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope defScope) {
      return new Builder(srcInfo, defScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope defScope) {
      this.implDef = new PFeatureImplDef(srcInfo, defScope);
      this.implDef.getter = "_feature_impl_get_" + defScope.generateId();
    }

    PScope getScope() { return this.implDef.scope; }

    void setProvider(PEid provider) {
      this.implDef.provider = provider;
      this.implDef.provider.setFun();
    }

    void setFeature(PFeature feature) {
      this.implDef.feature = feature;
    }

    PFeatureImplDef create() {
      return this.implDef;
    }
  }

  static PFeatureImplDef accept(ParserA.TokenReader reader, PScope defScope) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance(reader.getCurrentSrcInfo(), defScope);
    PScope scope = builder.getScope();

    if (ParserA.acceptToken(reader, LToken.PLUS_PLUS, ParserA.SPACE_DO_NOT_CARE) == null) { return null; }

    PEid provider;
    if ((provider = PEid.accept(reader, scope, Parser.QUAL_MAYBE, ParserA.SPACE_NEEDED)) == null) {
      emsg = new StringBuffer();
      emsg.append("Feature provider missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    provider.setCat(PDefDict.EID_CAT_FUN_OFFICIAL);
    builder.setProvider(provider);

    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    PFeature f;
    if ((f = PFeature.accept(reader, scope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Provided feature missing missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setFeature(f);

    // PFeatureImplDef id = builder.create();
// /* DEBUG */ System.out.println(id);
    // return id;
    return builder.create();
  }

  static PFeatureImplDef acceptX(ParserB.Elem elem, PScope defScope) throws CompileException {
    // StringBuffer emsg;
    // if (!elem.getName().equals("constr")) { return null; }
    // Builder builder = Builder.newInstance(elem.getSrcInfo(), defScope);
    // PScope scope = builder.getScope();
    // String dcon = elem.getAttrValueAsId("dcon");
    // if (dcon == null) {
      // emsg = new StringBuffer();
      // emsg.append("Data constructor missing at ");
      // emsg.append(elem.getSrcInfo().toString());
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // builder.setDcon(dcon);

    // ParserB.Elem e = elem.getFirstChild();
    // while (e != null) {
      // PDataAttrDef attr = PDataAttrDef.acceptX(e, scope);
      // if (attr == null) {
        // emsg = new StringBuffer();
        // emsg.append("Unexpected XML node. - ");
        // emsg.append(e.getSrcInfo().toString());
        // throw new CompileException(emsg.toString());
      // }
      // builder.addAttr(attr);
      // e = e.getNextSibling();
    // }
    // return builder.create();
    return null;
  }

  public void collectModRefs() throws CompileException {
    this.provider.collectModRefs();
    this.feature.collectModRefs();
  }

  public PFeatureImplDef resolve() throws CompileException {
    StringBuffer emsg;
    this.provider.setCat(PDefDict.EID_CAT_FUN_OFFICIAL);
    this._resolved_providerProps = this.scope.theMod.resolveFunOfficial(this.provider);
    if (this._resolved_providerProps == null) {
      emsg = new StringBuffer();
      emsg.append("Provider \"");
      emsg.append(this.provider.repr());
      emsg.append("\" is not defined at ");
      emsg.append(this.provider.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    this.feature = this.feature.resolve();
    return this;
  }

  void excludePrivateAcc() throws CompileException {
    // this.provider
    this.feature.excludePrivateAcc();
  }

  // public void checkConcreteness() throws CompileException {
    // // HERE
  // }

  public Cstr getProviderModName() throws CompileException {
    PDefDict.EidProps ep = this.scope.theMod.resolveFunOfficial(this.provider);
    if (ep == null) { throw new RuntimeException("Unknown provider. " + this.provider.repr()); }
    return ep.key.modName;
  }

  public String getProviderFunName() { return this.provider.name; }

  public String getGetter() { return this.getter; }

  public PFeatureSkel getImpl() {
    return this.feature.toSkel();
  }
}
