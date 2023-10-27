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

class PExtendStmt extends PDefaultProgObj implements PDataDef {
  Module.Availability availability;
  Module.Access acc;
  String baseModId;
  String baseTcon;
  String tcon;
  PDataStmt.Param[] tparams;
  PDataConstrDef[] constrs;
  PFeatureImplDef[] featureImpls;
  PTypeRef sig;
  PDefDict.TconProps baseTconProps;

  PExtendStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
    this.scope.startDef();
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.acc = Module.ACC_PRIVATE;  // default
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("extend[src=");
    buf.append(this.srcInfo);
    buf.append(",basemod=");
    buf.append(this.baseModId);
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
    List<PDataStmt.Param> paramList;
    PTypeId baseTcon;
    String rename;
    List<PDataConstrDef> constrList;
    List<PFeatureImplDef> featureImplList;
    Set<String> nameSet;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.ext = new PExtendStmt(srcInfo, outerScope);
      this.paramList = new ArrayList<PDataStmt.Param>();
      this.constrList = new ArrayList<PDataConstrDef>();
      this.featureImplList = new ArrayList<PFeatureImplDef>();
      this.nameSet = new HashSet<String>();
    }

    PScope getDefScope() { return this.ext.scope; }

    void setAvailability(Module.Availability availability) {
      this.ext.availability = availability;
    }

    void addParam(PDataStmt.Param param) {
      this.paramList.add(param);
    }

    void setBaseTcon(PTypeId baseTcon) {
      this.baseTcon = baseTcon;
    }

    void setRename(String rename) {
      this.rename = rename;
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
      this.ext.tparams = this.paramList.toArray(new PDataStmt.Param[this.paramList.size()]);
      this.ext.baseModId = (this.baseTcon.modId != null)? this.baseTcon.modId: PModule.MOD_ID_LANG;
      this.ext.baseTcon = this.baseTcon.name;
      this.ext.tcon = (this.rename != null)? this.rename: this.ext.baseTcon;
      PType[] ps = new PType[this.ext.tparams.length];
      for (int i = 0; i < this.ext.tparams.length; i++) {
        ps[i] = this.ext.tparams[i].varDef;
      }
      this.ext.sig = PTypeRef.create(this.ext.srcInfo, this.ext.scope,
        PTypeId.create(this.ext.srcInfo, this.ext.scope, null, this.ext.tcon, false), ps);
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
    PDataStmt.Param param;
    int spc = ParserA.SPACE_DO_NOT_CARE;
    while ((param = PDataStmt.Param.accept(reader, defScope)) != null) {
      builder.addParam(param);
      spc = ParserA.SPACE_NEEDED;
    }
    PTypeId tcon = PTypeId.accept(reader, defScope, Parser.QUAL_MAYBE, spc);
    if (tcon == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setBaseTcon(tcon);
    if (ParserA.acceptToken(reader, LToken.GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Base form incomplete at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }



    // PType base;
    // if ((base = PType.acceptSig(reader, defScope, true, Parser.QUAL_MAYBE)) == null) {
      // emsg = new StringBuffer();
      // emsg.append("Type description missing at ");
      // emsg.append(reader.getCurrentSrcInfo());
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }
    // builder.setBase(base);


    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) != null) {
      PTypeId rename;
      if ((rename = PTypeId.accept(reader, defScope, Parser.QUAL_INHIBITED, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("New name missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setRename(rename.name);
    }
    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND));
    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDataStmt.DataDefBody body;
    if ((body = PDataStmt.acceptDataDefBody(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
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
    PTypeId btconItem = PTypeId.create(elem.getSrcInfo(), defScope, elem.getAttrValueAsId("base-mid"), btcon, false);
    btconItem.setTcon();

    String renamed = elem.getAttrValueAsId("renamed-tcon");
    if (renamed != null) {
      builder.setRename(renamed);
    }

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
    builder.addConstrList(body.constrList);
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.baseModId);
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
    if (this.baseModId != null && this.scope.resolveModId(this.baseModId) == null) {  // refer base mod id in order to register foreign mod
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(this.baseModId);
      emsg.append("\" not defined at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    // /* DEBUG */ System.out.print("extend sig resolveid "); System.out.println(this.sig);
    this.sig = this.sig.resolve();
    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i].varDef = this.tparams[i].varDef.resolve();
    }
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i] = this.constrs[i].resolve();
      this.constrs[i].setDataType(this.sig);
    }
    for (int i = 0; i < this.featureImpls.length; i++) {
      this.featureImpls[i] = this.featureImpls[i].resolve();
    }
    // /* DEBUG */ System.out.println("resolve " + this.baseModId + "." + this.baseTcon);
    if ((this.baseTconProps = this.scope.resolveTcon(this.baseModId, this.baseTcon)) == null) {
      emsg = new StringBuffer();
      emsg.append("Base type constructor not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    // /* DEBUG */ System.out.println("extend base tcon info " + this.baseTconProps);
    if (this.baseTconProps.acc == Module.ACC_OPAQUE) {
      emsg = new StringBuffer();
      emsg.append("Cannot extend opaque data at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    if (this.baseTconProps.paramCount() >= 0 && this.tparams.length != this.baseTconProps.paramCount()) {
      emsg = new StringBuffer();
      emsg.append("Parameter count of 'extend' definition mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    if (this.baseTconProps.paramProps != null) {
      for (int i = 0; i < this.tparams.length; i++) {
        if (this.tparams[i].variance != this.baseTconProps.paramProps[i].variance) {
          emsg = new StringBuffer();
          emsg.append("Variance of *");
          emsg.append(this.tparams[i].varDef.name);
          emsg.append(" mismatch with that of base definition at ");
          emsg.append(this.srcInfo);
          emsg.append(". ");
          emsg.append(this.tparams[i].variance);
          emsg.append(" ");
          emsg.append(this.baseTconProps.paramProps[i].variance);
          throw new CompileException(emsg.toString()) ;
        }
      }
    }
    return this;
  }

  public String getFormalTcon() { return this.tcon; }

  public PDefDict.IdKey getBaseTconKey() { return this.baseTconProps.key; }

  public int getParamCount() { return this.tparams.length; }

  public PTypeRefSkel getTypeSig() {
    return (PTypeRefSkel)this.sig.toSkel();
  }

  public Module.Variance getParamVarianceAt(int pos) { return this.tparams[pos].variance; }

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
      this.tparams[i].varDef.excludePrivateAcc();
    }
    if (this.acc != Module.ACC_OPAQUE) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].excludePrivateAcc();
      }
    }
  }

  void collectTconProps() throws CompileException {
    List<PDefDict.TconProps> tps = new ArrayList<PDefDict.TconProps>();
    for (int i = 0; i < this.constrs.length; i++) {
      for (int j = 0; j < constrs[i].attrs.length; j++) {
        constrs[i].attrs[j].getNormalizedType().collectTconProps(tps);
      }
    }
    this.scope.addReferredTcons(tps);
  }

  public void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    g.addExtension(this.baseTconProps.key, PDefDict.IdKey.create(this.scope.myModName(), this.tcon));
  }

  public void checkConcreteness() throws CompileException {
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].checkConcreteness();
    }
  }

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    List<PEvalStmt> es;
    if ((e = this.generateCallHashFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateCallDebugReprFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateInFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateNarrowFun(mod)) != null) { funs.add(e); }
    if ((es = this.generateAttrFuns(mod)) != null) { funs.addAll(es); }
    // if ((es = this.generateFeatureImplFuns(mod)) != null) { funs.addAll(es); }  HERE
    return funs;
  }

  PEvalStmt generateCallHashFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_hash_TCON @private -> <int> {
    //   X _hash_TCON
    // }
    if (!mod.funOfficialDict.containsKey("_hash_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_hash");
    PScope modScope = this.scope.theMod.scope;
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    PScope defScope = evalStmtBuilder.getDefScope();
    // PScope retScope = evalStmtBuilder.getRetScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_call_hash_" + this.tcon);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      paramTypeBuilder.addItem(PTypeVarDef.create(si, defScope, paramNames[i], false, /* null, */ null));
    }
    paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTypeId.create(si, retScope, PModule.MOD_ID_LANG, "int", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "_hash_" + this.tcon)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateCallDebugReprFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_debug_repr_TCON @private -> <cstr> {
    //   X _debug_repr_TCON
    // }
    if (!mod.funOfficialDict.containsKey("_debug_repr_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_debug_repr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setOfficial("_call_debug_repr_" + this.tcon);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      paramTypeBuilder.addItem(PTypeVarDef.create(si, defScope, paramNames[i], false, /* null, */ null));
    }
    paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTypeId.create(si, retScope, PModule.MOD_ID_LANG, "cstr", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "_debug_repr_" + this.tcon)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(callEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateInFun(PModule mod) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON+> *X _in_TCON? | TCON? @public -> <bool> {
    //   X case {
    //   ; *** DCON0 -> true$
    //   ; *** DCON1 -> true$
    //   ;  :
    //   ; ** -> X base._in_TCON?
    //   }
    // }
    if (this.acc != Module.ACC_PUBLIC && this.acc != Module.ACC_PROTECTED) { return null; }
    String[] names = PModule.generateInFunNames(this.tcon);  // official name and aliases
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = srcInfo.appendPostfix("_in");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].varDef.unresolvedCopy(si, defScope,
        PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP);
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, true));
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTypeId.create(si, retScope, PModule.MOD_ID_LANG, "bool", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder caseEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    caseEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    PCaseBlock.Builder caseBlockBuilder = PCaseBlock.Builder.newInstance(si, bodyScope);
    for (int i = 0; i < this.constrs.length; i++) {
      PDataDef.Constr constr = this.constrs[i];
      PCaseClause.Builder caseClauseBuilder = PCaseClause.Builder.newInstance(si, bodyScope);
      PScope caseClauseScope = caseClauseBuilder.getScope();
      PCasePtnMatch.Builder casePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si, caseClauseScope);
      PPtn.Builder ptnBuilder = PPtn.Builder.newInstance(si, caseClauseScope);
      ptnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
      ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, PWildCards.create(si, caseClauseScope)));
      ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, caseClauseScope, null, constr.getDcon())));
      casePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, ptnBuilder.create()));
      caseClauseBuilder.addPtnMatch(casePtnMatchBuilder.create());
      PEval.Builder trueTermEvalBuilder = PEval.Builder.newInstance(si, caseClauseScope);
      trueTermEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, caseClauseScope, PModule.MOD_ID_LANG, "true$")));
      List<PExpr> aes = new ArrayList<PExpr>();
      aes.add(PExpr.create(trueTermEvalBuilder.create()));
      caseClauseBuilder.setAction(PExprList.Seq.create(si, caseClauseScope, aes));
      caseBlockBuilder.addClause(caseClauseBuilder.create());
    }
    PCaseClause.Builder otherwiseCaseClauseBuilder = PCaseClause.Builder.newInstance(si, bodyScope);
    PScope otherwiseScope = otherwiseCaseClauseBuilder.getScope();
    PCasePtnMatch.Builder otherwisePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si, otherwiseScope);
    PPtn.Builder otherwisePtnBuilder = PPtn.Builder.newInstance(si, otherwiseScope);
    otherwisePtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    otherwisePtnBuilder.addItem(PPtnItem.create(si, otherwiseScope, PPtnMatch.CONTEXT_TRIAL, null, PWildCard.create(si, otherwiseScope)));
    otherwisePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, otherwiseScope, PPtnMatch.CONTEXT_TRIAL, null, otherwisePtnBuilder.create()));
    otherwiseCaseClauseBuilder.addPtnMatch(otherwisePtnMatchBuilder.create());
    String[] baseNames = PModule.generateInFunNames(this.baseTcon);
    PEval.Builder forwardingEvalBuilder = PEval.Builder.newInstance(si, otherwiseScope);
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, otherwiseScope, null, "X")));
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, otherwiseScope, this.baseModId, baseNames[0])));
    List<PExpr> aes = new ArrayList<PExpr>();
    aes.add(PExpr.create(forwardingEvalBuilder.create()));
    otherwiseCaseClauseBuilder.setAction(PExprList.Seq.create(si, otherwiseScope, aes));
    caseBlockBuilder.addClause(otherwiseCaseClauseBuilder.create());
    caseEvalBuilder.addItem(PEvalItem.create(caseBlockBuilder.create()));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(caseEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateNarrowFun(PModule mod) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON+> *X _narrow_TCON | narrow @public -> <<T0 T1 .. TCON> maybe> {
    //   X case {
    //   ; *V0 *V1 .. DCON0 -> (V0 V1 .. DCON0) value$
    //   ; *V0 *V1 .. DCON1 -> (V0 V1 .. DCON1) value$
    //   ;  :
    //   ; ** -> X base._narrow_TCON
    //   }
    // }
    if (this.acc != Module.ACC_PUBLIC && this.acc != Module.ACC_PROTECTED) { return null; }
    String[] names = PModule.generateNarrowFunNames(this.tcon);
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = srcInfo.appendPostfix("_narrow");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].varDef.unresolvedCopy(si, defScope,
        PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP);
      paramNames[i] = p.name;
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, true));
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retMaybeTypeBuilder = PType.Builder.newInstance(si, retScope);
    PType.Builder retDataTypeBuilder = PType.Builder.newInstance(si, retScope);
    for (int i = 0; i < paramNames.length; i++) {
      retDataTypeBuilder.addItem(PTypeId.createVar(si, retScope, paramNames[i]));
    }
    retDataTypeBuilder.addItem(PTypeId.create(si, retScope, null, this.tcon, false));
    retMaybeTypeBuilder.addItem(retDataTypeBuilder.create());
    retMaybeTypeBuilder.addItem(PTypeId.create(si, retScope, PModule.MOD_ID_LANG, "maybe", false));
    retDefBuilder.setType(retMaybeTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder caseEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    caseEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    PCaseBlock.Builder caseBlockBuilder = PCaseBlock.Builder.newInstance(si, bodyScope);
    for (int i = 0; i < this.constrs.length; i++) {
      PDataDef.Constr constr = this.constrs[i];
      PCaseClause.Builder caseClauseBuilder = PCaseClause.Builder.newInstance(si, bodyScope);
      PScope caseClauseScope = caseClauseBuilder.getScope();
      PCasePtnMatch.Builder casePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si, caseClauseScope);
      PPtn.Builder ptnBuilder = PPtn.Builder.newInstance(si, caseClauseScope);
      ptnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
      String[] attrs = PModule.generateIds("V", constr.getAttrCount());
      for (int j = 0; j < constr.getAttrCount(); j++) {
        ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, PExprVarDef.create(si, caseClauseScope, PExprVarDef.CAT_LOCAL_VAR, null, attrs[j])));
      }
      ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, caseClauseScope, null, constr.getDcon())));
      casePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, ptnBuilder.create()));
      caseClauseBuilder.addPtnMatch(casePtnMatchBuilder.create());
      PEval.Builder narrowedResBuilder = PEval.Builder.newInstance(si, caseClauseScope);
      PEval.Builder narrowedValueBuilder = PEval.Builder.newInstance(si, caseClauseScope);
      for (int j = 0; j < constr.getAttrCount(); j++) {
        narrowedValueBuilder.addItem(PEvalItem.create(PExprId.create(si, caseClauseScope, null, attrs[j])));
      }
      narrowedValueBuilder.addItem(PEvalItem.create(PExprId.create(si, caseClauseScope, null, constr.getDcon())));
      narrowedResBuilder.addItem(PEvalItem.create(narrowedValueBuilder.create()));
      narrowedResBuilder.addItem(PEvalItem.create(PExprId.create(si, caseClauseScope, PModule.MOD_ID_LANG, "value$")));
      List<PExpr> aes = new ArrayList<PExpr>();
      aes.add(PExpr.create(narrowedResBuilder.create()));
      caseClauseBuilder.setAction(PExprList.Seq.create(si, caseClauseScope, aes));
      caseBlockBuilder.addClause(caseClauseBuilder.create());
    }
    PCaseClause.Builder otherwiseCaseClauseBuilder = PCaseClause.Builder.newInstance(si, bodyScope);
    PScope otherwiseScope = otherwiseCaseClauseBuilder.getScope();
    PCasePtnMatch.Builder otherwisePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si, otherwiseScope);
    PPtn.Builder otherwisePtnBuilder = PPtn.Builder.newInstance(si, otherwiseScope);
    otherwisePtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    otherwisePtnBuilder.addItem(PPtnItem.create(si, otherwiseScope, PPtnMatch.CONTEXT_TRIAL, null, PWildCard.create(si, otherwiseScope)));
    otherwisePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, otherwiseScope, PPtnMatch.CONTEXT_TRIAL, null, otherwisePtnBuilder.create()));
    otherwiseCaseClauseBuilder.addPtnMatch(otherwisePtnMatchBuilder.create());
    String[] baseNames = PModule.generateNarrowFunNames(this.baseTcon);
    PEval.Builder forwardingEvalBuilder = PEval.Builder.newInstance(si, otherwiseScope);
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, otherwiseScope, null, "X")));
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, otherwiseScope, this.baseModId, baseNames[0])));
    List<PExpr> aes = new ArrayList<PExpr>();
    aes.add(PExpr.create(forwardingEvalBuilder.create()));
    otherwiseCaseClauseBuilder.setAction(PExprList.Seq.create(si, otherwiseScope, aes));
    caseBlockBuilder.addClause(otherwiseCaseClauseBuilder.create());
    caseEvalBuilder.addItem(PEvalItem.create(caseBlockBuilder.create()));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(caseEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  List<PEvalStmt> generateAttrFuns(PModule mod) throws CompileException {
    List<PEvalStmt> es = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    if (this.constrs.length == 1) {
      PDataConstrDef constr = this.constrs[0];
      for (int i = 0; i < constr.attrs.length; i++) {
        PDataAttrDef attr = constr.attrs[i];
        if ((e = this.generateAttrFun1(mod, constr, attr)) != null) { es.add(e); }
      }
    } else {
      for (int i = 0; i < this.constrs.length; i++) {
        PDataConstrDef constr = this.constrs[i];
        for (int j = 0; j < constr.attrs.length; j++) {
          PDataAttrDef attr = constr.attrs[j];
          if ((e = this.generateAttrFun2(mod, constr, attr)) != null) { es.add(e); }
        }
      }
    }
    return es;
  }

  PEvalStmt generateAttrFun1(PModule mod, PDataConstrDef constr, PDataAttrDef attr) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON> *X _attr_TCON_A | A @xxx -> <A's type> {
    //   X = A: *V *** dcon$,
    //   V
    // }
    if (attr.name == null) { return null; }
    String[] names = PModule.generateAttrFunNames(this.tcon, attr.name);
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_attr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc((this.acc == Module.ACC_PUBLIC || this.acc == Module.ACC_PROTECTED)? Module.ACC_PUBLIC: Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    for (int i = 0; i < this.tparams.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].varDef.unresolvedCopy(si, defScope,
        PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP);
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(attr.type.unresolvedCopy(si, retScope,
      PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_OFF));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder matchEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    matchEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    PPtn.Builder matchPtnBuilder = PPtn.Builder.newInstance(si, bodyScope);
    matchPtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    matchPtnBuilder.addItem(PPtnItem.create(si, bodyScope, PPtnMatch.CONTEXT_TRIAL, attr.name, PExprVarDef.create(si, bodyScope, PExprVarDef.CAT_LOCAL_VAR, null, "V")));
    matchPtnBuilder.addItem(PPtnItem.create(si, bodyScope, PPtnMatch.CONTEXT_TRIAL, null, PWildCards.create(si, bodyScope)));
    matchPtnBuilder.addItem(PPtnItem.create(si, bodyScope, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, bodyScope, null, constr.dcon)));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(si, bodyScope, matchEvalBuilder.create(), PPtnMatch.create(si, bodyScope, PPtnMatch.CONTEXT_TRIAL, null, matchPtnBuilder.create())));
    PEval.Builder retEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    retEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "V")));
    ies.add(PExpr.create(retEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateAttrFun2(PModule mod, PDataConstrDef constr, PDataAttrDef attr) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON> *X _maybe_attr_TCON_A | maybe_A @xxx -> <<A's type> maybe> {
    //   X case {
    //   ; A: *V *** DCON -> V value$
    //   ; ** -> none$
    //   }
    // }
    if (attr.name == null) { return null; }
    String[] names = PModule.generateMaybeAttrFunNames(this.tcon, attr.name);
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = srcInfo.appendPostfix("_maybe_attr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope.theMod.scope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    PScope retScope = retDefBuilder.getScope();
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc((this.acc == Module.ACC_PUBLIC || this.acc == Module.ACC_PROTECTED)? Module.ACC_PUBLIC: Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    for (int i = 0; i < this.tparams.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].varDef.unresolvedCopy(si, defScope,
        PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP);
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, defScope);
    retTypeBuilder.addItem(attr.type.unresolvedCopy(si, retScope,
      PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_OFF));
    retTypeBuilder.addItem(PTypeId.create(si, retScope, PModule.MOD_ID_LANG, "maybe", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    PEval.Builder caseEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    caseEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    PCaseBlock.Builder caseBlockBuilder = PCaseBlock.Builder.newInstance(si, bodyScope);
    PCaseClause.Builder caseClauseBuilder = PCaseClause.Builder.newInstance(si, bodyScope);
    PScope caseClauseScope = caseClauseBuilder.getScope();
    PCasePtnMatch.Builder casePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si, caseClauseScope);
    PPtn.Builder ptnBuilder = PPtn.Builder.newInstance(si, caseClauseScope);
    ptnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, attr.name, PExprVarDef.create(si, caseClauseScope, PExprVarDef.CAT_LOCAL_VAR, null, "V")));
    ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, PWildCards.create(si, caseClauseScope)));
    ptnBuilder.addItem(PPtnItem.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, caseClauseScope, null, constr.dcon)));
    casePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, caseClauseScope, PPtnMatch.CONTEXT_TRIAL, null, ptnBuilder.create()));
    caseClauseBuilder.addPtnMatch(casePtnMatchBuilder.create());
    PEval.Builder valueEvalBuilder = PEval.Builder.newInstance(si, caseClauseScope);
    valueEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, caseClauseScope, null, "V")));
    valueEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, caseClauseScope, PModule.MOD_ID_LANG, "value$")));
    List<PExpr> aes = new ArrayList<PExpr>();
    aes.add(PExpr.create(valueEvalBuilder.create()));
    caseClauseBuilder.setAction(PExprList.Seq.create(si, caseClauseScope, aes));
    caseBlockBuilder.addClause(caseClauseBuilder.create());
    PCaseClause.Builder otherwiseCaseClauseBuilder = PCaseClause.Builder.newInstance(si, bodyScope);
    PScope otherwiseScope = otherwiseCaseClauseBuilder.getScope();
    PCasePtnMatch.Builder otherwisePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si, otherwiseScope);
    PPtn.Builder otherwisePtnBuilder = PPtn.Builder.newInstance(si, otherwiseScope);
    otherwisePtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    otherwisePtnBuilder.addItem(PPtnItem.create(si, otherwiseScope, PPtnMatch.CONTEXT_TRIAL, null, PWildCard.create(si, otherwiseScope)));
    otherwisePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, otherwiseScope, PPtnMatch.CONTEXT_TRIAL, null, otherwisePtnBuilder.create()));
    otherwiseCaseClauseBuilder.addPtnMatch(otherwisePtnMatchBuilder.create());
    PEval.Builder noneEvalBuilder = PEval.Builder.newInstance(si, otherwiseScope);
    noneEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, otherwiseScope, PModule.MOD_ID_LANG, "none$")));
    List<PExpr> aes2 = new ArrayList<PExpr>();
    aes2.add(PExpr.create(noneEvalBuilder.create()));
    otherwiseCaseClauseBuilder.setAction(PExprList.Seq.create(si, otherwiseScope, aes2));
    caseBlockBuilder.addClause(otherwiseCaseClauseBuilder.create());
    caseEvalBuilder.addItem(PEvalItem.create(caseBlockBuilder.create()));
    List<PExpr> ies = new ArrayList<PExpr>();
    ies.add(PExpr.create(caseEvalBuilder.create()));
    evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    return evalStmtBuilder.create();
  }
}
