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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PCompiledModule implements PModDecl {
  Compiler theCompiler;
  Module.Availability availability;
  Cstr name;
  Cstr[] foreignMods;

  private PCompiledModule() {}

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("module[COMPILED,name=");
    b.append(this.name);
    b.append("]");
    return b.toString();
  }

  static PCompiledModule create(Compiler theCompiler, Module mod) /* throws FormatException */ {
    PCompiledModule cm = new PCompiledModule();
    cm.theCompiler = theCompiler;
    cm.name = mod.actualName;
    cm.availability = mod.availability;
    cm.foreignMods = mod.getModTab().getForeignMods();

    MDataDef[] mdds = mod.getDataDefs();
    for (int i = 0; i < mdds.length; i++) {
      MDataDef mdd = mdds[i];
      DataDef dd = cm.convertDataDef(mod, mdd);
      PDefDict.IdKey ik = PDefDict.IdKey.create(mod.actualName, mdd.tcon);
      if (dd.getBaseTconKey() == null) {
        if (!theCompiler.defDict.predefineTconData(ik, dd.acc)) {
          throw new RuntimeException("Unexpected error occurred on prefining " + ik);
        }
      } else {
        if (!theCompiler.defDict.predefineTconExtend(ik, dd.acc)) {
          throw new RuntimeException("Unexpected error occurred on prefining " + ik);
        }
      }
      int cc = dd.getConstrCount();
      for (int j = 0; j < cc; j++) {
        PDataDef.Constr c = dd.getConstrAt(j);
        String dcon = c.getDcon();
        PDefDict.IdKey dk = PDefDict.IdKey.create(mod.actualName, dcon);
        if (!theCompiler.defDict.predefineDcon(dk, dd.acc)) {
          throw new RuntimeException("Unexpected error occurred on prefining " + dk);
        }
      }
      theCompiler.defDict.putDataDef(ik, dd);
    }

    MAliasTypeDef[] matds = mod.getAliasTypeDefs();
    for (int i = 0; i < matds.length; i++) {
      MAliasTypeDef matd = matds[i];
      AliasTypeDef atd = cm.convertAliasTypeDef(mod, matd);
      PDefDict.IdKey ik = PDefDict.IdKey.create(mod.actualName, matd.tcon);
      if (!theCompiler.defDict.predefineTconAliasType(ik, atd.acc)) {
        throw new RuntimeException("Unexpected error occurred on prefining " + ik);
      }
      theCompiler.defDict.putAliasTypeDef(ik, atd);
    }

    MFeatureDef[] mftds = mod.getFeatureDefs();
    for (int i = 0; i < mftds.length; i++) {
      MFeatureDef mftd = mftds[i];
      FeatureDef ftd = cm.convertFeatureDef(mod, mftd);
      PDefDict.IdKey ik = PDefDict.IdKey.create(mod.actualName, mftd.fname);
      if (!theCompiler.defDict.predefineFeature(ik, ftd.acc)) {
        throw new RuntimeException("Unexpected error occurred on prefining " + ik);
      }
      theCompiler.defDict.putFeatureDef(ik, ftd);
    }

    MFunDef[] mfds = mod.getFunDefs();
    for (int i = 0; i < mfds.length; i++) {
      MFunDef mfd = mfds[i];
      FunDef fd = cm.convertFunDef(mod, mfd);
      PDefDict.IdKey ik = PDefDict.IdKey.create(mod.actualName, fd.name);
      if (!theCompiler.defDict.predefineFunOfficial(ik, fd.acc)) {
        throw new RuntimeException("Unexpected error occurred on prefining " + ik);
      }
      for (int j = 0; j < fd.aliases.length; j++) {
        PDefDict.IdKey a = PDefDict.IdKey.create(mod.actualName, fd.aliases[j]);
        if (!theCompiler.defDict.predefineFunAlias(a, fd.acc)) {
          throw new RuntimeException("Unexpected error occurred on prefining " + a);
        }
      }
      theCompiler.defDict.putFunDef(ik, fd);

    }

    return cm;
  }

  PModDecl getModDecl() { return this; }

  public Module.Availability getAvailability() { return this.availability; }

  public Cstr getName() { return this.name; }

  public Cstr[] getForeignMods() {
    return this.foreignMods;
  }

  DataDef convertDataDef(Module mod, MDataDef dataDef) {
    DataDef dd = new DataDef();
    dd.availability = dataDef.availability;
    dd.modName = mod.actualName;
    dd.sigTcon = dataDef.tcon;
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    if (dataDef.params != null) {
      dd.paramVariances = new Module.Variance[dataDef.params.length];
      dd.sigParams = new PTypeVarSkel[dataDef.params.length];
      for (int i = 0; i < dataDef.params.length; i++) {
        dd.paramVariances[i] = dataDef.params[i].variance;
        PTypeVarSkel v = (PTypeVarSkel)this.convertType(dataDef.params[i].var, mod, varList /* , unresolvedTypeRefList, unresolvedFeatureList */);
        dd.sigParams[i] = v;
      }
    }
    dd.acc = dataDef.acc;
    for (int i = 0; i < dataDef.constrs.length; i++) {
      MConstrDef mcd = dataDef.constrs[i];
      ConstrDef cd = dd.addConstr(mcd.dcon);
      for (int j = 0; j < mcd.attrs.length; j++) {
        MAttrDef mad = mcd.attrs[j];
        AttrDef ad = cd.addAttr(mad.name, this.convertType(mad.type, mod, varList /* , unresolvedTypeRefList, unresolvedFeatureList */));
      }
    }
    if (dataDef.baseModIndex > 0) {
      dd.baseTconKey = PDefDict.IdKey.create(mod.getModAt(dataDef.baseModIndex), dataDef.baseTcon);
    }
    dd.featureImpls = new FeatureImpl[dataDef.featureImpls.length];
    for (int i = 0; i < dataDef.featureImpls.length; i++) {
      MFeatureImplDef mfi = dataDef.featureImpls[i];
      FeatureImpl fi = new FeatureImpl();
      fi.providerModName = mod.getModTab().get(mfi.providerModIndex);
      fi.providerFunName = mfi.providerFun;
      fi.getter = mfi.getter;
      fi.impl = convertFeature(mfi.provided, mod, varList /* , unresolvedTypeRefList, unresolvedFeatureList */);
      dd.featureImpls[i] = fi;
    }
    return dd;
  }

  class DataDef implements PDataDef {
    Module.Availability availability;
    Cstr modName;
    PTypeRefSkel sig;  // lazy setup
    String sigTcon;
    Module.Variance[] paramVariances;
    PTypeVarSkel[] sigParams;
    Module.Access acc;
    List<String> constrList;
    Map<String, ConstrDef> constrDict;
    PDefDict.IdKey baseTconKey;
    PDataDef.FeatureImpl[] featureImpls;

    DataDef() {
      this.constrList = new ArrayList<String>();
      this.constrDict = new HashMap<String, ConstrDef>();
    }

    public String getFormalTcon() { return this.sigTcon; }

    public PDefDict.IdKey getBaseTconKey() { return this.baseTconKey; }

    public int getParamCount() {
      return (this.sigParams == null)? -1: this.sigParams.length;
    }

    public PDefDict.TparamProps[] getParamPropss() {
      PDefDict.TparamProps[] tps;
      if (this.sigParams == null) {
        tps = null;
      } else {
        tps = new PDefDict.TparamProps[this.sigParams.length];
        for (int i = 0; i < this.sigParams.length; i++) {
          tps[i] = PDefDict.TparamProps.create(this.paramVariances[i], this.sigParams[i].varSlot.requiresConcrete);
        }
      }
      return tps;
    }

    // public int getParamCount() { return (this.sigParams != null)? this.sigParams.length: -1 ; }

    public PTypeRefSkel getTypeSig() {
      if (this.sig == null) {
        this.sig = PTypeRefSkel.create(
          PCompiledModule.this.theCompiler,
          null,
          PDefDict.IdKey.create(this.modName, this.sigTcon),
          false,
          this.sigParams);
      }
      return this.sig;
    }

    // public Module.Variance getParamVarianceAt(int pos) { return this.paramVariances[pos]; }

    public Module.Availability getAvailability() { return this.availability; }

    public Module.Access getAcc() { return this.acc; }

    public int getConstrCount() { return this.constrDict.size(); }

    public PDataDef.Constr getConstr(String dcon) { return this.constrDict.get(dcon); }

    public PDataDef.Constr getConstrAt(int index) { return this.constrDict.get(this.constrList.get(index)); }

    public int getFeatureImplCount() { return this.featureImpls.length; }

    public PDataDef.FeatureImpl getFeatureImplAt(int index) { return this.featureImpls[index] ; }

    ConstrDef addConstr(String dcon) {
      ConstrDef cd = new ConstrDef(dcon);
      this.constrList.add(dcon);
      this.constrDict.put(dcon, cd);
      cd.dataDef = this;
      return cd;
    }
  }

  AliasTypeDef convertAliasTypeDef(Module mod, MAliasTypeDef aliasTypeDef) {
    AliasTypeDef ad = new AliasTypeDef();
    PDefDict.IdKey ik = PDefDict.IdKey.create(mod.actualName, aliasTypeDef.tcon);
    // PDefDict.DataDefGetter g = createDataDefGetter(ad);
    PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[aliasTypeDef.paramCount];
    for (int k = 0; k < paramPropss.length; k++) {
      paramPropss[k] = PDefDict.TparamProps.create(Module.INVARIANT, false);
    }
    // ad.tconProps = PDefDict.TidProps.create(
      // ik, PDefDict.TID_CAT_TCON_ALIAS, paramPropss, aliasTypeDef.acc, g);
    ad.availability = aliasTypeDef.availability;
    ad.acc = aliasTypeDef.acc;
    ad.tcon = aliasTypeDef.tcon;
    ad.tparams = new PTypeVarSkel[aliasTypeDef.paramCount];
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    for (int i = 0; i < ad.tparams.length; i++) {
      ad.tparams[i] = PTypeVarSkel.create(PCompiledModule.this.theCompiler, null, null, PTypeVarSlot.createInternal(false), null);
      varList.add(ad.tparams[i]);
    }
    ad.body = (PTypeRefSkel)this.convertType(aliasTypeDef.body, mod, varList /* , unresolvedTypeRefList, unresolvedFeatureList */);
    return ad;
  }

  class AliasTypeDef implements PAliasTypeDef {
    // PDefDict.TidProps tconProps;
    Module.Availability availability;
    Module.Access acc;
    String tcon;
    PTypeVarSkel[] tparams;
    PTypeRefSkel body;

    public String getTcon() { return this.tcon; }

    public PTypeVarSlot[] getParamVarSlots() {
      PTypeVarSlot[] vs = new PTypeVarSlot[this.tparams.length];
      for (int i = 0; i < this.tparams.length; i++) {
        vs[i] = this.tparams[i].varSlot;
      }
      return vs;
    }

    public Module.Availability getAvailability() { return this.availability; }

    public Module.Access getAcc() { return this.acc; }

    // public void collectUnaliasTconProps(List<PDefDict.TidProps> list) { this.body.collectTconProps(list); }
    // public PDefDict.TidProps unaliasTconProps() { return this.body.tconProps; }

    public PTypeRefSkel getBody() { return this.body; }

    public PTypeRefSkel unalias(PTypeSkel[] params) throws CompileException {
      if (params.length != this.tparams.length) {
        throw new IllegalArgumentException("Length of unaliasing params mismatch.");
      }
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create();
      PTypeSkelBindings bindings = PTypeSkelBindings.create();
      for (int i = 0; i < params.length; i++) {
        bindings.bind(((PTypeVarSkel)this.tparams[i].instanciate(ic)).varSlot, params[i]);
      }
      PTypeRefSkel tr = (PTypeRefSkel)this.body.instanciate(ic).resolveBindings(bindings);
      return tr.normalize();
    }
  }

  class ConstrDef implements PDataDef.Constr {
    DataDef dataDef;
    String dcon;
    List<AttrDef> attrList;

    ConstrDef(String dcon) {
      this.dcon = dcon;
      this.attrList = new ArrayList<AttrDef>();
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public String getDcon() { return this.dcon; }

    public int getAttrCount() { return this.attrList.size(); }

    public PDataDef.Attr getAttrAt(int i) { return this.attrList.get(i); }

    public int getAttrIndex(String name) {
      int index = -1;
      for (int i = 0; index < 0 && i < this.attrList.size(); i++) {
        index = name.equals(this.attrList.get(i).name)? i: -1;
      }
      return index;
    }

    public PTypeSkel getType(PTypeSkelBindings bindings) {
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(bindings);
      return this.dataDef.getTypeSig().resolveBindings(bindings).instanciate(ic);
    }

    AttrDef addAttr(String name, PTypeSkel type) {
      AttrDef ad = new AttrDef();
      ad.name = name;
      ad.type = type;
      this.attrList.add(ad);
      return ad;
    }
  }

  class AttrDef implements PDataDef.Attr {
    String name;
    PTypeSkel type;

    public String getName() { return this.name; }

    public PTypeSkel getNormalizedType() { return this.type; }

    public PTypeSkel getFinalizedType() { return this.type; }
  }

  class FeatureImpl implements PDataDef.FeatureImpl {
    Cstr providerModName;
    String providerFunName;
    String getter;
    PFeatureSkel impl;

    public Cstr getProviderModName() { return this.providerModName; }

    public String getProviderFunName() { return this.providerFunName; }

    public String getGetter() { return this.getter; }

    public PFeatureSkel getImpl() { return this.impl; }
  }

  FeatureDef convertFeatureDef(Module mod, MFeatureDef featureDef) {
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    FeatureDef fd = new FeatureDef();
    fd.availability = featureDef.availability;
    fd.acc = featureDef.acc;
    fd.nameKey = PDefDict.IdKey.create(mod.actualName, featureDef.fname);
    fd.obj = convertTypeVar(featureDef.obj, mod, varList);
    fd.params = new PTypeVarSkel[featureDef.params.length];
    fd.paramVariances = new Module.Variance[featureDef.params.length];
    for (int i = 0; i < fd.params.length; i++) {
      fd.params[i] = convertTypeVar(featureDef.params[i].var, mod, varList);
      fd.paramVariances[i] = featureDef.params[i].variance;
    }
    fd.impl = this.convertTypeRef(featureDef.impl, mod, varList);
    return fd;
  }

  class FeatureDef implements PFeatureDef {
    Module.Availability availability;
    Module.Access acc;
    PDefDict.IdKey nameKey;
    PTypeVarSkel obj;
    PTypeVarSkel[] params;
    Module.Variance[] paramVariances;
    PTypeRefSkel impl;

    public Module.Availability getAvailability() { return this.availability; }

    public Module.Access getAcc() { return this.acc; }

    public PDefDict.IdKey getNameKey() { return this.nameKey; }

    public int getParamCount() { return this.params.length; }

    public PDefDict.TparamProps[] getParamPropss() {
      PDefDict.TparamProps[] pps = new PDefDict.TparamProps[this.paramVariances.length];
      for (int i = 0; i < pps.length; i++) {
        pps[i] = PDefDict.TparamProps.create(this.paramVariances[i], false);
      }
      return pps;
    }

    public PTypeVarSkel getObjType() { return this.obj; }

    // public PTypeVarSkel[] getParams() { return this.params; }

    public PFeatureSkel getFeatureSig() {
      return PFeatureSkel.create(PCompiledModule.this.theCompiler, null, this.nameKey, this.params);
    }

    // public Module.Variance getParamVarianceAt(int pos) { return this.paramVariances[pos]; }

    public PTypeRefSkel getImplType() { return this.impl; }
  }

  FunDef convertFunDef(Module mod, MFunDef funDef /* , List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList */) {
    FunDef fd = new FunDef();
    fd.availability = funDef.availability;
    fd.acc = funDef.acc;
    fd.modName = mod.actualName;
    fd.name = funDef.name;
    fd.aliases = funDef.aliases;
    fd.paramTypes = new PTypeSkel[funDef.paramTypes.length];
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    for (int i = 0; i < fd.paramTypes.length; i++) {
      fd.paramTypes[i] = this.convertType(funDef.paramTypes[i], mod, varList /* , unresolvedTypeRefList, unresolvedFeatureList */);
    }
    fd.retType = this.convertType(funDef.retType, mod, varList /* , unresolvedTypeRefList, unresolvedFeatureList */);
    return fd;
  }

  class FunDef implements PFunDef {
    Module.Availability availability;
    Module.Access acc;
    Cstr modName;
    String name;
    String[] aliases;
    PTypeSkel[] paramTypes;
    PTypeSkel retType;

    public Module.Availability getAvailability() { return this.availability; }

    public Module.Access getAcc() { return this.acc; }

    public Cstr getModName() { return this.modName; }

    public String getOfficialName() { return this.name; }

    public String[] getAliases() { return this.aliases; }

    public PTypeSkel[] getParamTypes() { return this.paramTypes; }

    public PTypeSkel[] getFinalizedParamTypes() { return this.paramTypes; }

    public PTypeSkel getRetType() { return this.retType; }

    public PTypeSkel getFinalizedRetType() { return this.retType; }
  }

  PTypeSkel convertType(MType type, Module mod, List<PTypeVarSkel> varList) {
    PTypeSkel t;
    if (type instanceof MTypeRef) {
      t = this.convertTypeRef((MTypeRef)type, mod, varList);
    } else if (type instanceof MTypeVar) {
      t = this.convertTypeVar((MTypeVar)type, mod, varList);
    } else {
      throw new IllegalArgumentException("Unknown type description. - " + type);
    }
    return t;
  }

  PTypeRefSkel convertTypeRef(MTypeRef tr, Module mod, List<PTypeVarSkel> varList) {
    PTypeSkel[] params = new PTypeSkel[tr.params.length];
    for (int i = 0; i < params.length; i++) {
      params[i] = this.convertType(tr.params[i], mod, varList);
    }
    PTypeRefSkel t;
    Cstr n = mod.getModTab().get(tr.modIndex);
    PDefDict.IdKey ik = PDefDict.IdKey.create(n, tr.tcon);
    t = PTypeRefSkel.create(
      PCompiledModule.this.theCompiler,
      null,
      ik,
      tr.ext,
      params);
    return t;
  }

  PTypeVarSkel convertTypeVar(MTypeVar tv, Module mod, List<PTypeVarSkel> varList) {
    PTypeVarSkel v;
    if (tv.slot < varList.size()) {
      v = varList.get(tv.slot);
// /* DEBUG */ System.out.print("DEFINED "); System.out.print(varList); System.out.print(" "); System.out.print(tv); System.out.print(" -> "); System.out.println(v);
    } else if (tv.slot == varList.size()) {
      v = PTypeVarSkel.create(PCompiledModule.this.theCompiler, null, null,
        PTypeVarSlot.createInternal(tv.requiresConcrete), /* null, */ null);
      varList.add(v);
      v.features = (tv.features != null)?
        this.convertFeatures(tv.features, mod, varList):
        null;
    } else {
      throw new RuntimeException("Slot number is not sequential. " + mod.actualName.toJavaString() + " " + tv.toString() + " " + varList.size());
    }
    return v;
  }

  PFeatureSkel.List convertFeatures(MFeature.List features, Module mod, List<PTypeVarSkel> varList) {
    PFeatureSkel[] fs = new PFeatureSkel[ (features != null)? features.features.length: 0 ];  // if list is null then empty array
    for (int i = 0; i < fs.length; i++) {
      fs[i] = this.convertFeature(features.features[i], mod, varList);
    }
    return PFeatureSkel.List.create(null, fs);
  }

  PFeatureSkel convertFeature(MFeature feature, Module mod, List<PTypeVarSkel> varList) {
    PTypeSkel[] ps = new PTypeSkel[feature.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.convertType(feature.params[i], mod, varList);
    }
    Cstr n = mod.getModTab().get(feature.modIndex);
    PDefDict.IdKey ik = PDefDict.IdKey.create(n, feature.name);
    PFeatureSkel f = PFeatureSkel.create(PCompiledModule.this.theCompiler, null, ik, ps) ;

    // PFeatureSkel f = PFeatureSkel.create(this.defDictGetter, null, PDefDict.FeatureProps.createUnresolved(ik), ps) ;
    // unresolvedFeatureList.add(f);
    return f;
  }
}
