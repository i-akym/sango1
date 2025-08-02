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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PFeatureStmt extends PDefaultProgObj implements PFeatureDef {
  Module.Availability availability;
  Module.Access acc;
  PTypeVarDef obj;
  PType.ParamDef[] params;  // variance is used internally
  String fname;
  PFeature sig;
  PType impl;  // guaranteed to be PTypeRef later
  PTypeVarSkel objSkel;
  PTypeVarSkel[] paramSkels;
  PTypeRefSkel _normalized_implSkel;

  PFeatureStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.startFeature());
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.acc = Module.ACC_PRIVATE;  // default
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("feature[src=");
    buf.append(this.srcInfo);
    buf.append(",acc=");
    buf.append(this.acc);
    buf.append(",obj=");
    buf.append(this.obj);
    buf.append(",sig=");
    buf.append("[");
    String sep = " ";
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i]);
    }
    buf.append(sep);
    buf.append(fname);
    buf.append(" ]");
    // buf.append(this.sig);
    buf.append(",impl=");
    buf.append(this.impl);
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PFeatureStmt feature;
    List<PType.ParamDef> paramList;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.feature = new PFeatureStmt(srcInfo, outerScope);
      this.paramList = new ArrayList<PType.ParamDef>();
    }

    PScope getDefScope() { return this.feature.scope; }

    void setAvailability(Module.Availability availability) {
      this.feature.availability = availability;
    }

    void setAcc(Module.Access acc) {
      this.feature.acc = acc;
    }

    void setObjType(PTypeVarDef t) {
      this.feature.obj = t;
    }

    void addParam(PType.ParamDef param) {
      this.paramList.add(param);
    }

    void setFname(String fname) {
      this.feature.fname = fname;
    }

    void setImplType(PType tr) {
      this.feature.impl = tr;
    }

    PFeatureStmt create() throws CompileException {
      this.feature.params = this.paramList.toArray(new PType.ParamDef[this.paramList.size()]);
      PFeature.SigBuilder sb = PFeature.SigBuilder.newInstance(this.feature.getSrcInfo(), this.feature.getScope());
      for (int i = 0; i < this.feature.params.length; i++) {
        sb.addParam(this.feature.params[i].getItem());
      }
      sb.setName(PTid.createFeature(this.feature.getSrcInfo(), this.feature.getScope(), this.feature.fname));
      this.feature.sig = sb.create();
      return this.feature;
    }
  }

  static PFeatureStmt accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "feature", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    builder.setAvailability(PModule.acceptAvailability(reader));

    PTypeVarDef objType;
    if ((t = ParserA.acceptToken(reader, LToken.LT, ParserA.SPACE_NEEDED)) == null) {
      emsg = new StringBuffer();
      emsg.append("Object type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((objType = PTypeVarDef.accept(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Invalid object type at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((t = ParserA.acceptToken(reader, LToken.GT, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Systax error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setObjType(objType);

    if (ParserA.acceptToken(reader, LToken.LBRACKET, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Definition form missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType.SigSpec sig = PType.acceptSig(reader, defScope);
    if (sig == null) {
      emsg = new StringBuffer();
      emsg.append("Syntex error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < sig.params.size(); i++) {
      builder.addParam(sig.params.get(i));
    }
    if (sig.anchor.modId != null) {
      emsg = new StringBuffer();
      emsg.append("Module id not allowed at ");
      emsg.append(sig.anchor.srcInfo);
      emsg.append(". - ");
      emsg.append(sig.anchor.repr());
      throw new CompileException(emsg.toString());
    }
    builder.setFname(sig.anchor.name);

    // PType.ParamDef param;
    // int spc = ParserA.SPACE_DO_NOT_CARE;
    // while ((param = PType.ParamDef.accept(reader, defScope)) != null) {
      // if (param.variance != Module.INVARIANT) {
        // emsg = new StringBuffer();
        // emsg.append("Variance specification not allowed at ");
        // emsg.append(reader.getCurrentSrcInfo());
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      // builder.addParam(param);
      // spc = ParserA.SPACE_NEEDED;
    // }
    // PTid fname = PTid.accept(reader, defScope, Parser.QUAL_INHIBITED, spc);
    // if (fname == null) {
      // emsg = new StringBuffer();
      // emsg.append("Feature name missing at ");
      // emsg.append(reader.getCurrentSrcInfo());
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // builder.setFname(fname.name);

    if (ParserA.acceptToken(reader, LToken.RBRACKET, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Definition form incomplete at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_FEATURE, PModule.ACC_DEFAULT_FOR_FEATURE));

    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    PScope implScope = defScope.startFeatureImpl();
    PType implType;
    if ((implType = PType.accept(reader, implScope, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Implementation type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setImplType(implType);

    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    return builder.create();
  }

  static PFeatureStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("feature-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    Module.Access acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND);
    builder.setAcc(acc);

    PType.Builder tb = PType.Builder.newInstance(elem.getSrcInfo(), defScope);
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PTypeVarDef var = PTypeVarDef.acceptX(ee, defScope);
        if (var == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        tb.addItem(var);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }

// HERE
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    this.sig.collectModRefs();
    this.impl.collectModRefs();
  }

  public PFeatureStmt resolve() throws CompileException {
    StringBuffer emsg;

    this.obj = this.obj.resolve();
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].setupResolved();
    }
    this.sig = this.sig.resolve();
    this.impl = this.impl.resolve();

    this.objSkel = this.obj.toSkel();
    this.paramSkels = new PTypeVarSkel[this.params.length];
    for (int i = 0; i < this.paramSkels.length; i++) {
      this.paramSkels[i] = (PTypeVarSkel)this.params[i].getTypeSkel();
    }

    // following check will be done for variance's compatibility
    // PTypeSkelBindings bindings = PTypeSkelBindings.create();  // dummy
    // for (int i = 0; i < this.params.length; i++) {
      // if (!this.impl.toSkel().includesVar(this.params[i].varDef._resolved_varSlot, bindings)) {
        // emsg = new StringBuffer();
        // emsg.append("Insufficient variable references in feature implementation type at ");
        // emsg.append(this.impl.getSrcInfo());
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
    // }
    return this;
  }

  public Module.Availability getAvailability() { return this.availability; }

  public Module.Access getAcc() { return this.acc; }

  public PDefDict.IdKey getNameKey() {
    return PDefDict.IdKey.create(this.scope.myModName(), this.fname);
  }

  public int getParamCount() { return this.params.length; }

  public PDefDict.TparamProps[] getParamPropss() {
    PDefDict.TparamProps[] tps = new PDefDict.TparamProps[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      tps[i] = this.params[i].getProps();
      // tps[i] = PDefDict.TparamProps.create(this.params[i].variance, this.params[i].getVarSlot().requiresConcrete);
    }
    return tps;
  }

  public PTypeVarSkel getObjType() { return this.objSkel; }

  public PFeatureSkel getFeatureSig() { return this.sig.toSkel(); }

  public PTypeRefSkel getImplType() { return this._normalized_implSkel; }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].excludePrivateAcc();
    }
    this.impl.excludePrivateAcc();
    return;
  }

  // public void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {}

  void checkVariance() throws CompileException {
    for(int i = 0; i < this.params.length; i++) {
      this.checkVariance1(this.params[i]);
    }
  }

  void checkVariance1(PType.ParamDef p) throws CompileException {
    StringBuffer emsg;
    List<Module.Variance> vs = new ArrayList<Module.Variance>();
    this._normalized_implSkel.collectVarVariances(p.getVarSlot(), null, vs);
    if (vs.size() == 0) {
      emsg = new StringBuffer();
      emsg.append("Type variable \"");
      emsg.append(p.getVarName());
      emsg.append("\" not referred in feature object at ");
      emsg.append(p.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (vs.contains(null)) { throw new RuntimeException("Unexptected variance."); }
    boolean iv = vs.contains(Module.INVARIANT);
    boolean cv = vs.contains(Module.COVARIANT);
    boolean xv = vs.contains(Module.CONTRAVARIANT);
    if (p.variance == Module.INVARIANT) {
      ;  // pass
    } else if (p.variance == Module.COVARIANT && cv && !iv && !xv) {
      ;  // pass
    } else if (p.variance == Module.CONTRAVARIANT && xv && !iv && !cv) {
      ;  // pass
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible variance \"");
      emsg.append(p.variance);
      emsg.append("\" for type variable \"");
      emsg.append(p.getVarName());
      emsg.append("\" at ");
      emsg.append(p.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
  }

  void normalizeTypes() throws CompileException {
    this._normalized_implSkel = (PTypeRefSkel)this.impl.toSkel().normalize();
  }

  List<PAliasTypeStmt> generateAliases(PModule mod) throws CompileException {
    List<PAliasTypeStmt> aliases = new ArrayList<PAliasTypeStmt>();
    aliases.add(this.generateFeatureAlias(mod));
    return aliases;
  }

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();
    funs.add(this.generateFeatureFun(mod));
    funs.add(this.generateFeatureBuiltinFun(mod));
    return funs;
  }

  PAliasTypeStmt generateFeatureAlias(PModule mod) throws CompileException {
    // alias type <*T *A .. _feature_impl_FEAT> @xxx := < IMPL >
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_feature");
    PScope modScope = this.scope.theMod.scope;
    PAliasTypeStmt.Builder aliasTypeStmtBuilder = PAliasTypeStmt.Builder.newInstance(si, modScope);
    PScope defScope = aliasTypeStmtBuilder.getDefScope();
    PScope bodyScope = aliasTypeStmtBuilder.getBodyScope();
    PType.Builder sigBuilder = PType.Builder.newInstance(si, defScope);
    sigBuilder.addItem(this.obj.unresolvedCopy(si, defScope, PType.COPY_EXT_OFF, PType.COPY_CONCRETE_OFF));
    for (int i = 0; i < this.params.length; i++) {
      sigBuilder.addItem(PTypeVarDef.create(si, defScope, this.params[i].getVarName(), false, null));
      // sigBuilder.addItem(this.params[i].varDef.unresolvedCopy(si, defScope, PType.COPY_EXT_OFF, PType.COPY_CONCRETE_OFF));
    }
    sigBuilder.addItem(PTid.create(si, defScope, null, "_feature_impl_" + this.fname, false));
    aliasTypeStmtBuilder.setSig(sigBuilder.create());
    aliasTypeStmtBuilder.setAcc(this.acc);
    aliasTypeStmtBuilder.setBody(this.impl.unresolvedCopy(si, bodyScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));
// /* DEBUG */ PAliasTypeStmt a = aliasTypeStmtBuilder.create();
// /* DEBUG */ System.out.println(a);
    return aliasTypeStmtBuilder.create();
  }

  PEvalStmt generateFeatureFun(PModule mod) throws CompileException {
    // eval <*T(!)[ FEAT ]> *X _feature_FEAT @xxx -> < IMPL > {
    //   X _builtin_feature_get_FEAT
    // }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_feature");
    PScope modScope = this.scope.theMod.scope;
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_feature_" + this.fname);
    evalStmtBuilder.setAcc(this.acc);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    PFeature.ListBuilder paramFeaturesBuilder = PFeature.ListBuilder.newInstance(si, defScope);
    paramFeaturesBuilder.addFeature(
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));  // HERE
    paramTypeBuilder.addItem(
      PTypeVarDef.create(si, defScope, "T", this.obj.requiresConcrete, /* null, */ paramFeaturesBuilder.create()));
    evalStmtBuilder.addParam(
      PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    retDefBuilder.setType(this.impl.unresolvedCopy(si, retScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "_builtin_feature_get_" + this.fname)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateFeatureBuiltinFun(PModule mod) throws CompileException {
    // eval <*T[ FEAT ]> *X _builtin_feature_get_FEAT @private -> < IMPL > @native
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_feature");
    PScope modScope = this.scope.theMod.scope;
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_builtin_feature_get_" + this.fname);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    PFeature.ListBuilder paramFeaturesBuilder = PFeature.ListBuilder.newInstance(si, defScope);
    paramFeaturesBuilder.addFeature(
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));  // HERE
    paramTypeBuilder.addItem(
      PTypeVarDef.create(si, defScope, "T", false, /* null, */ paramFeaturesBuilder.create()));
    evalStmtBuilder.addParam(
      PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    retDefBuilder.setType(this.impl.unresolvedCopy(si, retScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    return evalStmtBuilder.create();
  }
}
