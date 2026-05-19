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

class PDataStmt extends PDefaultProgObj implements PDataDef.OriginDef {
  Module.Availability availability;
  Module.Access acc;
  boolean extensible;
  PType.DefHeaderParam[] tparams;  // null means variable params
  String tcon;
  PDataConstrDef[] constrs;  // null means native impl
  PFeatureImplDef[] featureImpls;
  PTypeRef sig;  // null means variable params
  PTypeRef _resolved_sig;  // null means variable params

  PDataStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.startData());
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.acc = Module.ACC_PRIVATE;  // default
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("data[src=");
    buf.append(this.srcInfo);
    if (this.sig != null) {
      buf.append(",sig=");
      buf.append(this.sig);
    } else {
      buf.append(",tcon=");
      buf.append(this.tcon);
    }
    buf.append("],acc=");
    buf.append(this.acc);
    if (this.constrs != null) {
      buf.append(",constrs=");
      buf.append("[");
      for (int i = 0; i < this.constrs.length; i++) {
        buf.append(this.constrs[i]);
        buf.append(",");
      }
      buf.append("]");
    } else {
      buf.append(",NATIVE_IMPL");
    }
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PDataStmt dat;
    List<PType.DefHeaderParam> paramList;
    PTid tcon;
    List<PDataConstrDef> constrList;
    List<PFeatureImplDef> featureImplList;
    Set<String> nameSet;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.dat = new PDataStmt(srcInfo, outerScope);
      this.paramList = new ArrayList<PType.DefHeaderParam>();
      this.constrList = new ArrayList<PDataConstrDef>();
      this.featureImplList = new ArrayList<PFeatureImplDef>();
      this.nameSet = new HashSet<String>();
    }

    PScope getDefScope() { return this.dat.scope; }

    void setAvailability(Module.Availability availability) {
      this.dat.availability = availability;
    }

    void addParam(PType.DefHeaderParam param) {
      this.paramList.add(param);
    }

    void setTcon(PTid tcon) {
      tcon.setCat(PDefDict.TID_CAT_TCON_DATA);
      this.tcon = tcon;
    }

    void setAcc(Module.Access acc) {
      this.dat.acc = acc;
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
      // constr.setDataDef(this.dat);
      this.constrList.add(constr);
    }

    void addConstrList(List<PDataConstrDef> constrList) throws CompileException {
      if (constrList != null) {
        for (int i = 0; i < constrList.size(); i++) {
          this.addConstr(constrList.get(i));
        }
      }
    }

    void setExtensible(boolean b) {
      this.dat.extensible = b;
    }

    void addFeatureImpl(PFeatureImplDef f) throws CompileException {
      this.featureImplList.add(f);
    }

    PDataStmt create() throws CompileException {
      StringBuffer emsg;
      this.dat.tparams = this.paramList.toArray(new PType.DefHeaderParam[this.paramList.size()]);
      this.dat.tcon = this.tcon.name;
      PType.Builder tb = PType.Builder.newInstance(this.dat.srcInfo, this.getDefScope());
      for (int i = 0; i < this.dat.tparams.length; i++) {
        tb.addItem(this.dat.tparams[i].getItem());
      }
      tb.addItem(PTid.create(this.dat.srcInfo, this.getDefScope(), null, this.tcon.name, false));
      this.dat.sig = PType.asRef(tb.create());
      if (!this.constrList.isEmpty() || this.dat.extensible) {
        this.dat.constrs = this.constrList.toArray(new PDataConstrDef[this.constrList.size()]);
      } else if (dat.acc == Module.ACC_OPAQUE || dat.acc == Module.ACC_PRIVATE) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Native data must be opaque or private at ");
        emsg.append(this.dat.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.dat.featureImpls = this.featureImplList.toArray(new PFeatureImplDef[this.featureImplList.size()]);
      return this.dat;
    }
  }

  static PDataStmt createForVariableParams(Parser.SrcInfo srcInfo, PScope scope, String tcon, Module.Access acc) {  // 'tuple', 'fun'
    PDataStmt dat = new PDataStmt(srcInfo, scope);
    dat.tcon = tcon;
    dat.acc = acc;
    return dat;
  }

  static PDataStmt accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "data", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    builder.setAvailability(PModule.acceptAvailability(reader));

    if (ParserA.acceptToken(reader, LToken.LT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Definition form missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType.DefHeader dh = PType.acceptDefHeader(reader, defScope, true, Parser.QUAL_PROHIBITED);
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
    if (dh.anchor.modId != null) {
      emsg = new StringBuffer();
      emsg.append("Module id not allowed at ");
      emsg.append(dh.anchor.srcInfo);
      emsg.append(". - ");
      emsg.append(dh.anchor.repr());
      throw new CompileException(emsg.toString());
    }
    builder.setTcon(dh.anchor);

    if (ParserA.acceptToken(reader, LToken.GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Definition form incomplete at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    Module.Access acc = PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_DATA, PModule.ACC_DEFAULT_FOR_DATA);
    builder.setAcc(acc);

    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    DataDefBody body;
    if ((body = acceptDataDefBody(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
    builder.setExtensible(body.extensible);

    PFeatureImplDef id;
    while ((id = PFeatureImplDef.accept(reader, defScope)) != null) {
      builder.addFeatureImpl(id);
    }

    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PDataStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("data-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();

    String tcon = elem.getAttrValueAsId("tcon");
    if (tcon == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTid tconItem = PTid.create(elem.getSrcInfo(), defScope, null, tcon, false);
    tconItem.setTcon();

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    Module.Access acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_DATA, PModule.ACC_DEFAULT_FOR_DATA);
    builder.setAcc(acc);

// HERE: change for variance in param
    // PType.Builder tb = PType.Builder.newInstance(elem.getSrcInfo(), defScope);
    ParserB.Elem e = elem.getFirstChild();
    // if (e != null && e.getName().equals("params")) {
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
    // }
    // tb.addItem(tconItem);
    // builder.setSig(tb.create());

    DataDefBody body = null;
    if (e != null) {
      if ((body = acceptXDataDefBody(e, defScope))!= null) {
        builder.addConstrList(body.constrList);
      }
      e = e.getNextSibling();
    }
    return builder.create();
  }

  static DataDefBody acceptDataDefBody(ParserA.TokenReader reader, PScope defScope) throws CompileException, IOException {
    DataDefBody b;
    if ((b = acceptDataDefBodyNative(reader)) != null) {
      return b;
    } else if ((b = acceptDataDefBodyConstructions(reader, defScope)) != null) {
      return b;
    } else {
      return null;
    }
  }

  static DataDefBody acceptXDataDefBody(ParserB.Elem elem, PScope defScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("constrs")) { return null; }
    DataDefBody body = new DataDefBody();
    body.constrList = new ArrayList<PDataConstrDef>();
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PDataConstrDef constr = PDataConstrDef.acceptX(e, defScope);
      if (constr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      body.constrList.add(constr);
      e = e.getNextSibling();
    }
    return body.constrList.isEmpty()? null: body;
  }

  private static DataDefBody acceptDataDefBodyNative(ParserA.TokenReader reader) throws CompileException, IOException {
    if (ParserA.acceptSpecifiedWord(reader, PModule.IMPL_WORD_NATIVE, ParserA.SPACE_DO_NOT_CARE) != null) {
      return new DataDefBody();
    } else {
      return null;
    }
  }

  private static DataDefBody acceptDataDefBodyConstructions(ParserA.TokenReader reader, PScope defScope) throws CompileException, IOException {
    DataDefBody body = new DataDefBody();
    body.constrList = new ArrayList<PDataConstrDef>();
    PDataConstrDef constr;
    int state = 0;  // 0: expect constr  1: constr ended  -1: stop
    while (state >= 0) {
      if ((state == 0) && (constr = PDataConstrDef.accept(reader, defScope)) != null) {
        body.constrList.add(constr);
        state = 1;
      } else if ((state == 0) && ParserA.acceptToken(reader, LToken.DOT_DOT_DOT, ParserA.SPACE_DO_NOT_CARE) != null) {
        body.extensible = true;
        state = -1;
      } else if (ParserA.acceptToken(reader, LToken.VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
        state = 0;
      } else {
        state = -1;
      }
    }
    return (!body.constrList.isEmpty() || body.extensible)? body: null;
  }

  static class DataDefBody {
    List<PDataConstrDef> constrList;
    boolean extensible;
  }

  public void collectModRefs() throws CompileException {
    // sig has no mod refs
    if (this.constrs != null) {  // skip if native impl
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].collectModRefs();
      }
    }
    if (this.featureImpls != null) {
      for (int i = 0; i < this.featureImpls.length; i++) {
        this.featureImpls[i].collectModRefs();
      }
    }
  }

  public PDataStmt resolve() throws CompileException {
    StringBuffer emsg;
    if (this.tparams != null) {
      for (int i = 0; i < this.tparams.length; i++) {
        this.tparams[i].setupResolved();
      }
    }
    if (this.sig != null) {
      this._resolved_sig = (PTypeRef)this.sig.resolve();
    }
    if (this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i] = this.constrs[i].resolve();
        // this.constrs[i].setDataType(this._resolved_sig);
      }
    }
    if (this.featureImpls != null) {
      for (int i = 0; i < this.featureImpls.length; i++) {
        this.featureImpls[i].scope.doCopy();
        this.featureImpls[i] = this.featureImpls[i].resolve();
        for (int j = 0; j < i; j++) {
          if (this.featureImpls[i].feature.fname.equals(
              this.featureImpls[j].feature.fname)) {
            emsg = new StringBuffer();
            emsg.append("Feature name duplicated at ");
            emsg.append(this.featureImpls[i].srcInfo);
            emsg.append(". ");
            emsg.append(this.featureImpls[i].feature.fname.repr());
            throw new CompileException(emsg.toString());
          }
        }
      }
    }
    return this;
  }

  public Cstr getModName() { return this.scope.theMod.actualName; }

  public String getTcon() { return this.tcon; }

  // public PDefDict.IdKey getBaseTconKey() { return null; }

  public int getParamCount() {
    return (this.tparams == null)? -1: this.tparams.length;
  }

  public PDefDict.TparamProps[] getParamPropss() {
    PDefDict.TparamProps[] tps;
    if (this.tparams == null) {  // for tuple, fun
      tps = null;
    } else {
      tps = new PDefDict.TparamProps[this.tparams.length];
      for (int i = 0; i < this.tparams.length; i++) {
        tps[i] = this.tparams[i].getProps();
        // tps[i] = PDefDict.TparamProps.create(this.tparams[i].variance, this.tparams[i].varDef.requiresConcrete);
      }
    }
    return tps;
  }

  // public int getParamCount() { return (this.tparams != null)? this.tparams.length: -1 ; }

  public PTypeRefSkel getTypeSig() {
    return (this._resolved_sig != null)? this._resolved_sig.toSkel(): null;
  }

  // public Module.Variance getParamVarianceAt(int pos) { return this.tparams[pos].variance; }
    // consider when tparams == null

  public Module.Availability getAvailability() { return this.availability; }

  public Module.Access getAcc() { return this.acc; }

  public boolean isExtensible() { return this.extensible; }

  public int getConstrCount() {
    return (this.constrs != null)? this.constrs.length: 0;
  }

  public PDataDef.Constr getConstr(String dcon) {
    PDataDef.Constr c = null;
    if (this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        if (this.constrs[i].dcon.equals(dcon)) {
          c = this.constrs[i];
          break;
        }
      }
    }
    return c;
  }

  public PDataDef.Constr getConstrAt(int index) {
    return (this.constrs != null)? this.constrs[index]: null;
  }

  public int getFeatureImplCount() {
    return this.featureImpls.length;
  }

  public PDataDef.FeatureImpl getFeatureImplAt(int index) {
    return this.featureImpls[index];
  }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    if (this.tparams != null) {
      for (int i = 0; i < this.tparams.length; i++) {
        this.tparams[i].excludePrivateAcc();
      }
    }
    if (this.acc != Module.ACC_OPAQUE && this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].excludePrivateAcc();
      }
    }
  }

  public void checkVariance() throws CompileException {
    if (this.tparams != null && this.constrs != null) {
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
  }

  void normalizeTypes() throws CompileException {
    if (this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].normalizeTypes();
      }
    }
  }

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    List<PEvalStmt> es;
    if ((e = this.generateCallHashFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateCallDebugReprFun(mod)) != null) { funs.add(e); }
    if ((es = this.generateAttrFuns(mod)) != null) { funs.addAll(es); }
    if ((es = this.generateFeatureImplFuns(mod)) != null) { funs.addAll(es); }
    return funs;
  }

  PEvalStmt generateCallHashFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_hash_TCON @private -> <int> {
    //   X _hash_TCON
    // }
    if (this.tparams == null) { return null; }
    if (this.constrs == null) { return null; }
    PDefDict.EidProps ep = this.scope.getCompiler().defDict.resolveFunOfficial(
      null,
      PDefDict.IdKey.create(mod.actualName, "_hash_" + this.tcon));
    if (ep == null) { return null; }
    // if (!mod.predefineFunOfficial("_hash_" + this.tcon, Module.ACC_PRIVATE)) { return null; }
    // // if (!mod.funOfficialDict.containsKey("_hash_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_hash");
    PScope modScope = this.scope.theMod.scope;
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_call_hash_" + this.tcon);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PScope evalDefScope = evalStmtBuilder.getDefScope();
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM,
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP),
      "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTid.create(si, retScope, PModule.MOD_ID_LANG, "int", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "_hash_" + this.tcon)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateCallDebugReprFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_debug_repr_TCON @private -> <cstr> {
    //   X _debug_repr_TCON
    // }
    if (this.tparams == null) { return null; }
    if (this.constrs == null) { return null; }
    PDefDict.EidProps ep = this.scope.getCompiler().defDict.resolveFunOfficial(
      null,
      PDefDict.IdKey.create(mod.actualName, "_debug_repr_" + this.tcon));
    if (ep == null) { return null; }
    // if (!mod.predefineFunOfficial("_debug_repr_" + this.tcon, Module.ACC_PRIVATE)) { return null; }
    // // if (!mod.funOfficialDict.containsKey("_debug_repr_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_debug_repr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_call_debug_repr_" + this.tcon);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM,
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP),
      "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTid.create(si, retScope, PModule.MOD_ID_LANG, "cstr", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "_debug_repr_" + this.tcon)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  List<PEvalStmt> generateAttrFuns(PModule mod) throws CompileException {
    if (this.tparams == null) { return null; }
    if (this.constrs == null) { return null; }
    List<PEvalStmt> es = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    if (!this.extensible && this.constrs.length == 1) {
      PDataConstrDef constr = this.constrs[0];
      for (int i = 0; i < constr.attrs.length; i++) {
        PDataAttrDef attr = constr.attrs[i];
        if ((e = this.generateAttrFun1(mod, constr, attr)) != null) { es.add(e); }
      }
    } // else {
      // for (int i = 0; i < this.constrs.length; i++) {
        // PDataConstrDef constr = this.constrs[i];
        // for (int j = 0; j < constr.attrs.length; j++) {
          // PDataAttrDef attr = constr.attrs[j];
          // if ((e = this.generateAttrFun2(mod, constr, attr)) != null) { es.add(e); }
        // }
      // }
    // }
    return es;
  }

  PEvalStmt generateAttrFun1(PModule mod, PDataConstrDef constr, PDataAttrDef attr) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON> *X _attr_TCON_A | A @xxx -> <A's type> {
    //   X = A: *V *** dcon$,
    //   V
    // }
    if (attr.name == null) { return null; }
    String[] names = PModule.generateAttrFunNames(this.tcon, attr.name);
    Module.Access a = (this.acc == Module.ACC_PUBLIC || this.acc == Module.ACC_PROTECTED)? Module.ACC_PUBLIC: Module.ACC_PRIVATE;
    // if (!mod.predefineFunOfficial(names[0], a)) { return null; }
    // // if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_attr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc(a);
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM,
      this.sig.unresolvedCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP),
      "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(attr.type.unresolvedCopy(si, retScope,
      PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_OFF));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder matchEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    matchEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "X")));
    PPtn.Builder matchPtnBuilder = PPtn.Builder.newInstance(si, bodyScope);
    matchPtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    matchPtnBuilder.addItem(PPtnItem.create(si, bodyScope, PPtnMatch.CONTEXT_FIXED, attr.name, PExprVarDef.create(si, bodyScope, PExprVarDef.CAT_LOCAL_VAR, null, "V")));
    matchPtnBuilder.addItem(PPtnItem.create(si, bodyScope, PPtnMatch.CONTEXT_FIXED, null, PWildCards.create(si, bodyScope)));
    matchPtnBuilder.addItem(PPtnItem.create(si, bodyScope, PPtnMatch.CONTEXT_FIXED, null, PEid.create(si, bodyScope, null, constr.dcon)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(si, bodyScope, matchEvalBuilder.create(), PPtnMatch.create(si, bodyScope, PPtnMatch.CONTEXT_TRIAL, null, matchPtnBuilder.create())));
    PEval.Builder retEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    retEvalBuilder.addItem(PEvalItem.create(PEid.create(si, bodyScope, null, "V")));
    ies.add(PExpr.create(retEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  List<PEvalStmt> generateFeatureImplFuns(PModule mod) throws CompileException {
    if (this.featureImpls == null || this.featureImpls.length == 0) return null;
    List<PEvalStmt> es = new ArrayList<PEvalStmt>();
    for (int i = 0; i < this.featureImpls.length; i++) {
// /* DEBUG */ System.out.println(this.generateFeatureImplFun(mod, this.featureImpls[i], this.implGetters[i]));
      es.add(this.generateFeatureImplFun(mod, this.featureImpls[i] /* , this.implGetters[i] */));
    }
    return es;
  }

  PEvalStmt generateFeatureImplFun(PModule mod, PFeatureImplDef impl) throws CompileException {
    // eval @availability GETTER @private -> <<*T0 *T1 .. TCON> ... feature impl type alias> {
    //   impl_provider
    // }
    Parser.SrcInfo si = srcInfo.appendPostfix("_feature_impl");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(impl.getter);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(this.sig.unresolvedCopy(si, retScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));
    for (int i = 0; i < impl.feature.params.length; i++) {
        retTypeBuilder.addItem(impl.feature.params[i].unresolvedCopy(si, retScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));
    }
    retTypeBuilder.addItem(
      PTid.create(si, retScope, 
        impl.feature.fname.modId, "_feature_impl_" + impl.feature.fname.name,
        false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(impl.provider));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    PEvalStmt evalStmt = evalStmtBuilder.create();
// /* DEBUG */ System.out.println(evalStmt);
    return evalStmt;
  }
}
