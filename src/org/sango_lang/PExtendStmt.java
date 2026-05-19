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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PExtendStmt extends PDefaultProgObj implements PDataDef.ExtensionDef {
  Module.Availability availability;
  Module.Access acc;
  String defKey;
  PTid baseTcon;
  PType.DefHeaderParam[] tparams;
  PDataConstrDef[] constrs;
  PFeatureImplDef[] featureImpls;
  PTypeRef sig;
  PDefDict.IdKey _resolved_baseTconKey;
  PTypeRef _resolved_sig;  // null means variable params

  PExtendStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.startData());
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.acc = Module.ACC_PRIVATE;  // default
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("extend[src=");
    buf.append(this.srcInfo);
    buf.append(",basetcon=");
    buf.append(this.baseTcon);
    buf.append(",sig=");
    buf.append(this.sig);
    buf.append("],acc=");
    buf.append(this.acc);
    buf.append(",constrs=[");
    for (int i = 0; i < this.constrs.length; i++) {
      buf.append(this.constrs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PExtendStmt ext;
    List<PType.DefHeaderParam> paramList;
    List<PDataConstrDef> constrList;
    List<PFeatureImplDef> featureImplList;
    Set<String> nameSet;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.ext = new PExtendStmt(srcInfo, outerScope);
      this.paramList = new ArrayList<PType.DefHeaderParam>();
      this.constrList = new ArrayList<PDataConstrDef>();
      this.featureImplList = new ArrayList<PFeatureImplDef>();
      this.nameSet = new HashSet<String>();
    }

    PScope getDefScope() { return this.ext.scope; }

    void setAvailability(Module.Availability availability) {
      this.ext.availability = availability;
    }

    void addParam(PType.DefHeaderParam param) {
      this.paramList.add(param);
    }

    void setBaseTcon(PTid baseTcon) {
      baseTcon.setCat(PDefDict.TID_CAT_TCON_DATA);
      this.ext.baseTcon = baseTcon;
    }

    void setDefKey(String defKey) {
      this.ext.defKey = defKey;
    }

    void setAcc(Module.Access acc) {
      this.ext.acc = acc;
    }

    void addConstr(PDataConstrDef constr) throws CompileException {
      StringBuffer emsg;
      for (int i = 0; i < constr.attrs.length; i++) {
        String name = constr.attrs[i].name;
        if (name != null) {
          if (this.nameSet.contains(name)) {
            emsg = new StringBuffer();
            emsg.append("Attribute name duplicate at ");
            emsg.append(constr.attrs[i].getSrcInfo());
            emsg.append(". - ");
            emsg.append(name);
            throw new CompileException(emsg.toString());
          }
          this.nameSet.add(name);
        }
      }
      // constr.setDataDef(this.ext);
      this.constrList.add(constr);
    }

    void addConstrList(List<PDataConstrDef> constrList) throws CompileException {
      for (int i = 0; i < constrList.size(); i++) {
        this.addConstr(constrList.get(i));
      }
    }

    void addFeatureImpl(PFeatureImplDef f) throws CompileException {
      this.featureImplList.add(f);
    }

    PExtendStmt create() throws CompileException {
      StringBuffer emsg;
      this.ext.tparams = this.paramList.toArray(new PType.DefHeaderParam[this.paramList.size()]);
      // this.ext.baseModId = (this.baseTcon.modId != null)? this.baseTcon.modId: PModule.MOD_ID_LANG;
      // this.ext.baseTcon = this.baseTcon.name;
      // this.ext.tcon = this.ext.baseTcon;
      PType.Builder tb = PType.Builder.newInstance(this.ext.srcInfo, this.getDefScope());
      for (int i = 0; i < this.ext.tparams.length; i++) {
        tb.addItem(this.ext.tparams[i].getItem());
      }
      tb.addItem(this.ext.baseTcon);
      // tb.addItem(PTid.create(this.ext.srcInfo, this.getDefScope(), null, this.ext.baseTcon, false));
      this.ext.sig = PType.asRef(tb.create());
      this.ext.constrs = this.constrList.toArray(new PDataConstrDef[this.constrList.size()]);
      this.ext.featureImpls = this.featureImplList.toArray(new PFeatureImplDef[this.featureImplList.size()]);
      return this.ext;
    }
  }

  static PExtendStmt accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "extend", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    builder.setAvailability(PModule.acceptAvailability(reader));

    if (ParserA.acceptToken(reader, LToken.LT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Base form missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType.DefHeader dh = PType.acceptDefHeader(reader, defScope, true, Parser.QUAL_MAYBE);
    if (dh == null) {
      emsg = new StringBuffer();
      emsg.append("Syntex error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < dh.params.length; i++) {
      builder.addParam(dh.params[i]);
    }
    builder.setBaseTcon(dh.anchor);

    if (ParserA.acceptToken(reader, LToken.GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Base form incomplete at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) != null) {
      ParserA.Token defKey;
      if ((defKey = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("Definition key missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setDefKey(defKey.value.token);
    }
    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND));
    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    PDataStmt.DataDefBody body;
    if ((body = PDataStmt.acceptDataDefBody(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(si);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (body.extensible) {
      emsg = new StringBuffer();
      emsg.append("Additional extension not allowed at ");
      emsg.append(si);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);

    // PFeatureImplDef id;
    // while ((id = PFeatureImplDef.accept(reader, defScope)) != null) {
      // builder.addFeatureImpl(id);
    // }

    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PExtendStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("extend-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();

    String btcon = elem.getAttrValueAsId("base-tcon");
    if (btcon == null) {
      emsg = new StringBuffer();
      emsg.append("Base type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTid btconItem = PTid.create(elem.getSrcInfo(), defScope, elem.getAttrValueAsId("base-mid"), btcon, false);
    btconItem.setTcon();

    // String renamed = elem.getAttrValueAsId("renamed-tcon");
    // if (renamed != null) {
      // builder.setRename(renamed);
    // }

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    Module.Access acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND);
    builder.setAcc(acc);

// HERE: change for variance in param
    // PType.Builder tb = PType.Builder.newInstance(elem.getSrcInfo(), defScope);
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        // PTypeVarDef var = PTypeVarDef.acceptX(ee, defScope);
        // if (var == null) {
          // emsg = new StringBuffer();
          // emsg.append("Unexpected XML node. - ");
          // emsg.append(ee.getSrcInfo().toString());
          // throw new CompileException(emsg.toString());
        // }
        // tb.addItem(var);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    // tb.addItem(btconItem);
    // builder.setBase(tb.create());

    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDataStmt.DataDefBody body = PDataStmt.acceptXDataDefBody(e, builder.getDefScope());
    if (body == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (body.extensible) {
      emsg = new StringBuffer();
      emsg.append("More extension not allowed at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.baseTcon.modId);
    // this.sig.collectModRefs();
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].collectModRefs();
    }
    for (int i = 0; i < this.featureImpls.length; i++) {
      this.featureImpls[i].collectModRefs();
    }
  }

  public PExtendStmt resolve() throws CompileException {
    StringBuffer emsg;

    PDefDict.TidProps bp = this.scope.theMod.resolveTcon(this.baseTcon);
    if (bp == null || bp.cat != PDefDict.TID_CAT_TCON_DATA) {
      emsg = new StringBuffer();
      emsg.append("Base data definition ");
      emsg.append(this.baseTcon.repr());
      emsg.append(" not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    this._resolved_baseTconKey = bp.key;

    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i].setupResolved();
    }
    this._resolved_sig = this.sig.resolve();
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i] = this.constrs[i].resolve();
    }
    for (int i = 0; i < this.featureImpls.length; i++) {
      this.featureImpls[i] = this.featureImpls[i].resolve();
    }
    // if (baseDef.getAcc() == Module.ACC_OPAQUE) {
      // emsg = new StringBuffer();
      // emsg.append("Cannot extend opaque data at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString()) ;
    // }
    return this;
  }

  public Cstr getModName() { return this.scope.theMod.actualName; }

  public String getDefKey() { return this.defKey; }

  public String getTcon() { return this.baseTcon.name; }

  public Cstr getOriginModName() { return this._resolved_baseTconKey.modName; }
  // public PDefDict.IdKey getBaseTconKey() { return this._resolved_baseTconKey; }

  public int getParamCount() { return this.tparams.length; }

  public PDefDict.TparamProps[] getParamPropss() {
    PDefDict.TparamProps[] tps = new PDefDict.TparamProps[this.tparams.length];
    for (int i = 0; i < this.tparams.length; i++) {
      tps[i] = this.tparams[i].getProps();
    }
    return tps;
  }

  // public int getParamCount() { return this.tparams.length; }

  public PTypeRefSkel getTypeSig() {
    return (PTypeRefSkel)this._resolved_sig.toSkel();
  }

  // public Module.Variance getParamVarianceAt(int pos) { return this.tparams[pos].variance; }

  public Module.Availability getAvailability() { return this.availability; }

  public Module.Access getAcc() { return this.acc; }

  public int getConstrCount() { return this.constrs.length; }

  public PDataDef.Constr getConstr(String dcon) {
    PDataDef.Constr c = null;
    for (int i = 0; i < this.constrs.length; i++) {
      if (this.constrs[i].dcon.equals(dcon)) {
        c = this.constrs[i];
        break;
      }
    }
    return c;
  }

  public PDataDef.Constr getConstrAt(int index) { return this.constrs[index]; }

  public int getFeatureImplCount() {
    return this.featureImpls.length;
  }

  public PDataDef.FeatureImpl getFeatureImplAt(int index) {
    return this.featureImpls[index];
  }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i].excludePrivateAcc();
    }
    if (this.acc != Module.ACC_OPAQUE) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].excludePrivateAcc();
      }
    }
  }

  void checkExtension() throws CompileException {
    StringBuffer emsg;
    PDataDef.OriginDef baseDef = this.scope.theMod.theCompiler.defDict.getDataDef(this.scope.theMod.actualName, this._resolved_baseTconKey);
    if (baseDef == null) {
      emsg = new StringBuffer();
      emsg.append("Base data definition ");
      emsg.append(this._resolved_baseTconKey.repr());
      emsg.append(" not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    if (!baseDef.isExtensible()) {
      emsg = new StringBuffer();
      emsg.append("Extension of ");
      emsg.append(this._resolved_baseTconKey.repr());
      emsg.append(" not allowed at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    if (baseDef.getParamCount() != this.tparams.length) {
      emsg = new StringBuffer();
      emsg.append("Parameter count mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(". - ");
      emsg.append(this._resolved_baseTconKey.repr());
      throw new CompileException(emsg.toString()) ;
    }

    // PDefDict.TparamProps[] bpps = baseDef.getParamPropss();
    // if (bpps != null && this.tparams.length != bpps.length) {
      // emsg = new StringBuffer();
      // emsg.append("Parameter count of 'extend' definition mismatch at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString()) ;
    // }
    // if (bpps != null) {
      // for (int i = 0; i < this.tparams.length; i++) {
        // if (this.tparams[i].variance != bpps[i].variance) {
          // emsg = new StringBuffer();
          // emsg.append("Variance of *");
          // emsg.append(this.tparams[i].getVarName());
          // emsg.append(" mismatch with that of base definition at ");
          // emsg.append(this.srcInfo);
          // emsg.append(". ");
          // emsg.append(this.tparams[i].variance);
          // emsg.append(" ");
          // emsg.append(bpps[i].variance);
          // throw new CompileException(emsg.toString()) ;
        // }
      // }
    // }

    PTypeRefSkel bsigSkel = baseDef.getTypeSig();
    PTypeRefSkel sigSkel = this.getTypeSig();
    PTypeSkel.Bindings bindings = PTypeSkel.Bindings.create();
    for (int i = 0; i < sigSkel.params.length; i++) {
      if (!sigSkel.params[i].accept(PTypeSkel.NARROWER, bsigSkel.params[i], bindings)) {
        PTypeVarSkel p = (PTypeVarSkel)sigSkel.params[i];
        emsg = new StringBuffer();
        emsg.append("Type parameter \"");
        emsg.append(p.name);
        emsg.append("\" is not compatible at ");
        emsg.append(p.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
  }

  public void checkVariance() throws CompileException {
    PTypeVarSlot[] ss = new PTypeVarSlot[this.tparams.length];
    Module.Variance[] vs = new Module.Variance[this.tparams.length];
    for (int i = 0; i < this.tparams.length; i++) {
      // if (this.tparams[i].varDef._resolved_varSlot == null) { throw new RuntimeException("Null varSlot."); }
      ss[i] = this.tparams[i].getVarSlot();
      vs[i] = this.tparams[i].variance;
    }
    PTypeSkel.VarianceTab vt = PTypeSkel.VarianceTab.create(ss, vs);
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].checkVariance(vt);
    }
  }

  void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].normalizeTypes();
    }
  }

  void checkFeatureImpl() throws CompileException {
    // for (int i = 0; i < this.featureImpls.length; i++) {
      // this.checkFeatureImpl1(this.featureImpls[i], this.scope.theMod.theCompiler.defDict.getDataDef(this.scope.theMod.actualName, this.baseTconKey));
    // }
  }

  // void checkFeatureImpl1(PFeatureImplDef id, PDataDef.OriginDef base) throws CompileException {
    // List<PTypeVarSlot> vs = new ArrayList<PTypeVarSlot>();
    // for (int i = 0; i < this.tparams.length; i++) {
      // vs.add(this.tparams[i].getVarSlot());
    // }
    // PTypeSkel.Bindings b = PTypeSkel.Bindings.create(vs);

    // PTypeRefSkel bsig = base.getTypeSig();
    // for (int i = 0; i < bsig.params.length; i++) {
      // b.bind((PTypeVarSkel)bsig.params[i], this.tparams[i].getTypeSkel());
    // }

    // PFeatureSkel f = id.feature.toSkel();
    // boolean a = false;
    // for (int i = 0; !a && i < base.getFeatureImplCount(); i++) {
      // a = f.require(PTypeSkel.EQUAL /* HERE */, base.getFeatureImplAt(i).getImpl(), b.copy());
    // }
    // if (!a) {
      // StringBuffer emsg = new StringBuffer();
      // emsg.append("Feature implemetation ");
      // emsg.append(f.reprSolo().toString());
      // emsg.append(" is not defined in base at ");
      // emsg.append(id.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
  // }

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    List<PEvalStmt> es;
    if ((e = this.generateCallHashFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateCallDebugReprFun(mod)) != null) { funs.add(e); }
    return funs;
  }

  PEvalStmt generateCallHashFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_hash_DEFKEY @private -> <int> {
    //   X _hash_DEFKEY
    // }
    PDefDict.EidProps ep = this.scope.getCompiler().defDict.resolveFunOfficial(
      null,
      PDefDict.IdKey.create(mod.actualName, "_hash_" + this.defKey));
    if (ep == null) { return null; }
    // if (!mod.predefineFunOfficial("_hash_" + this.tcon, Module.ACC_PRIVATE)) { return null; }
    // if (!mod.funOfficialDict.containsKey("_hash_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_hash");
    PScope modScope = this.scope.theMod.scope;
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_call_hash_" + this.defKey);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM,
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP),
      "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTid.create(si, retScope, PModule.MOD_ID_LANG, "int", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "_hash_" + this.defKey)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateCallDebugReprFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_debug_repr_DEFKEY @private -> <cstr> {
    //   X _debug_repr_DEFKEY
    // }
    PDefDict.EidProps ep = this.scope.getCompiler().defDict.resolveFunOfficial(
      null,
      PDefDict.IdKey.create(mod.actualName, "_debug_repr_" + this.defKey));
    if (ep == null) { return null; }
    // if (!mod.predefineFunOfficial("_debug_repr_" + this.tcon, Module.ACC_PRIVATE)) { return null; }
    // if (!mod.funOfficialDict.containsKey("_debug_repr_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_debug_repr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_call_debug_repr_" + this.defKey);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM,
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP),
      "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTid.create(si, retScope, PModule.MOD_ID_LANG, "cstr", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "_debug_repr_" + this.defKey)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }
}
