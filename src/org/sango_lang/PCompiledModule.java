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

class PCompiledModule implements PDefDict {
  PDefDict.DefDictGetter defDictGetter;
  Module.Availability availability;
  Cstr name;
  Cstr[] foreignMods;
  Map<PDefDict.IdKey, PDefDict.TconProps> foreignTconDict;
  Map<PDefDict.IdKey, PDefDict.FeatureProps> foreignFnameDict;
  Map<String, PDefDict.TconProps> tconDict;
  Map<String, PDefDict.FeatureProps> fnameDict;
  Map<String, PDefDict.EidProps> eidDict;
  HashMap<String, FunDef> funOfficialDict;
  HashMap<String, List<FunDef>> funListDict;

  private PCompiledModule() {}

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("module[COMPILED,name=");
    b.append(this.name);
    b.append("]");
    return b.toString();
  }

  static PCompiledModule create(PDefDict.DefDictGetter defDictGetter, Module mod) /* throws FormatException */ {
    PCompiledModule cm = new PCompiledModule();
    cm.defDictGetter = defDictGetter;
    cm.name = mod.name;
    cm.availability = mod.availability;

    cm.foreignMods = mod.getModTab().getForeignMods();

    List<PTypeRefSkel> unresolvedTypeRefList = new ArrayList<PTypeRefSkel>();
    List<PFeatureSkel> unresolvedFeatureList = new ArrayList<PFeatureSkel>();

    cm.foreignTconDict = new HashMap<PDefDict.IdKey, PDefDict.TconProps>();
    cm.foreignFnameDict = new HashMap<PDefDict.IdKey, PDefDict.FeatureProps>();

    for (int i = 0; i < cm.foreignMods.length; i++) {
      MDataDef[] dds = mod.getForeignDataDefs(cm.foreignMods[i]);
      for (int j = 0; j < dds.length; j++) {
        DataDef dd = cm.convertDataDef(mod, dds[j], unresolvedTypeRefList, unresolvedFeatureList);
        PDefDict.IdKey ik = PDefDict.IdKey.create(cm.foreignMods[i], dds[j].tcon);
        PDefDict.DataDefGetter g = createDataDefGetter(dd);
        PDefDict.TparamProps[] paramPropss;
        if (dds[j].params != null) {
          paramPropss = new PDefDict.TparamProps[dds[j].params.length];
          for (int k = 0; k < dds[j].params.length; k++) {
            paramPropss[k] = PDefDict.TparamProps.create(dds[j].params[k].variance, dds[j].params[k].var.requiresConcrete);
          }
        } else {
          paramPropss = null;
        }
        PDefDict.TconProps tp = PDefDict.TconProps.create(ik,
          (dds[j].baseModIndex == 0)? PDefDict.TID_SUBCAT_DATA: PDefDict.TID_SUBCAT_EXTEND,
          paramPropss, dds[j].acc, g);
        cm.foreignTconDict.put(ik, tp);
      }
      MAliasTypeDef[] ads = mod.getForeignAliasTypeDefs(cm.foreignMods[i]);
      for (int j = 0; j < ads.length; j++) {
        AliasTypeDef ad = cm.convertAliasTypeDef(mod, ads[j], unresolvedTypeRefList, unresolvedFeatureList);
        PDefDict.IdKey ik = PDefDict.IdKey.create(cm.foreignMods[i], ads[j].tcon);
        PDefDict.DataDefGetter g = createDataDefGetter(ad);
        PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[ads[j].paramCount];
        for (int k = 0; k < paramPropss.length; k++) {
          paramPropss[k] = PDefDict.TparamProps.create(Module.INVARIANT, false);
        }
        PDefDict.TconProps tp = PDefDict.TconProps.create(
          ik, PDefDict.TID_SUBCAT_ALIAS, paramPropss, ads[j].acc, g);
        cm.foreignTconDict.put(ik, tp);
      }
      MFeatureDef[] fds = mod.getForeignFeatureDefs(cm.foreignMods[i]);
      for (int j = 0; j < fds.length; j++) {
        FeatureDef fd = cm.convertFeatureDef(mod, fds[j], unresolvedTypeRefList, unresolvedFeatureList);
        PDefDict.IdKey ik = PDefDict.IdKey.create(cm.foreignMods[i], fds[j].fname);
        PDefDict.FeatureDefGetter g = createFeatureDefGetter(fd);
        PDefDict.FeatureProps fp = PDefDict.FeatureProps.create(
          ik, fds[j].params.length, fds[j].acc, g);
        cm.foreignFnameDict.put(ik, fp);
      }
    }

    cm.tconDict = new HashMap<String, PDefDict.TconProps>();
    cm.fnameDict = new HashMap<String, PDefDict.FeatureProps>();
    cm.eidDict = new HashMap<String, PDefDict.EidProps>();

    MDataDef[] mdds = mod.getDataDefs();
    for (int i = 0; i < mdds.length; i++) {
      MDataDef mdd = mdds[i];
      DataDef dd = cm.convertDataDef(mod, mdd, unresolvedTypeRefList, unresolvedFeatureList);
      PDefDict.DataDefGetter g = createDataDefGetter(dd);
      PDefDict.TparamProps[] paramPropss;
      if (mdd.params != null) {
        paramPropss = new PDefDict.TparamProps[mdd.params.length];
        for (int k = 0; k < mdd.params.length; k++) {
          paramPropss[k] = PDefDict.TparamProps.create(mdd.params[k].variance, mdd.params[k].var.requiresConcrete);
        }
      } else {
        paramPropss = null;
      }
      PDefDict.TconProps tp = PDefDict.TconProps.create(
        PDefDict.IdKey.create(mod.name, mdd.tcon),
        (mdd.baseModIndex == 0)? PDefDict.TID_SUBCAT_DATA: PDefDict.TID_SUBCAT_EXTEND,
        paramPropss, mdd.acc, g);
      cm.tconDict.put(mdd.tcon, tp);
      for (int j = 0; j < mdd.constrs.length; j++) {
        MConstrDef mcd = mdd.constrs[j];
        cm.eidDict.put(mcd.dcon, PDefDict.EidProps.create(
          mod.name, PExprId.CAT_DCON, mdd.acc, cm.createExprDefGetter(dd)));
      }
    }
    MAliasTypeDef[] matds = mod.getAliasTypeDefs();
    for (int i = 0; i < matds.length; i++) {
      MAliasTypeDef matd = matds[i];
      PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[matd.paramCount];
      for (int k = 0; k < matd.paramCount; k++) {
        paramPropss[k] = PDefDict.TparamProps.create(Module.INVARIANT, false);
      }
      cm.tconDict.put(matd.tcon, PDefDict.TconProps.create(
        PDefDict.IdKey.create(mod.name, matd.tcon),
        PDefDict.TID_SUBCAT_ALIAS,
        paramPropss, matd.acc, createDataDefGetter(cm.convertAliasTypeDef(mod, matd, unresolvedTypeRefList, unresolvedFeatureList))));
    }
    MFeatureDef[] mftds = mod.getFeatureDefs();
    for (int i = 0; i < mftds.length; i++) {
      MFeatureDef mftd = mftds[i];
      cm.fnameDict.put(mftd.fname, PDefDict.FeatureProps.create(
        PDefDict.IdKey.create(mod.name, mftd.fname),
        mftd.params.length,
        mftd.acc, createFeatureDefGetter(cm.convertFeatureDef(mod, mftd, unresolvedTypeRefList, unresolvedFeatureList))));
    }

    cm.funOfficialDict = new HashMap<String, FunDef>();
    cm.funListDict = new HashMap<String, List<FunDef>>();
    MFunDef[] mfds = mod.getFunDefs();
    for (int i = 0; i < mfds.length; i++) {
      MFunDef mfd = mfds[i];
      FunDef fd = cm.convertFunDef(mod, mfd, unresolvedTypeRefList, unresolvedFeatureList);
      cm.funOfficialDict.put(mfd.name, fd);
      if (cm.eidDict.containsKey(mfd.name)) {
        cm.mergeFunToEidDict(mfd.name, PExprId.CAT_FUN_OFFICIAL, mfd.acc);
      } else {
        cm.eidDict.put(mfd.name, PDefDict.EidProps.create(mod.name, PExprId.CAT_FUN_OFFICIAL, mfd.acc, cm.createExprDefGetter(mfd.name)));
      }
      List<FunDef> funList;
      if (cm.funListDict.containsKey(mfd.name)) {
        funList = cm.funListDict.get(mfd.name);
      } else {
        funList = new ArrayList<FunDef>();
        cm.funListDict.put(mfd.name, funList);
      }
      funList.add(fd);
      for (int j = 0; j < mfd.aliases.length; j++) {
        String a = mfd.aliases[j];
        if (cm.eidDict.containsKey(a)) {
          cm.mergeFunToEidDict(a, PExprId.CAT_FUN_ALIAS, mfd.acc);
        } else {
          cm.eidDict.put(a, PDefDict.EidProps.create(mod.name, PExprId.CAT_FUN_ALIAS, mfd.acc, cm.createExprDefGetter(a)));
        }
        if (cm.funListDict.containsKey(a)) {
          funList = cm.funListDict.get(a);
        } else {
          funList = new ArrayList<FunDef>();
          cm.funListDict.put(a, funList);
        }
        funList.add(fd);
      }
    }

// /* DEBUG */ System.out.println("foreign tcon dict " + cm.foreignTconDict);

    for (int i = 0; i < unresolvedTypeRefList.size(); i++) {
      PTypeRefSkel tr = unresolvedTypeRefList.get(i);
      if (cm.name.equals(tr.tconProps.key.modName)) {
        tr.tconProps= cm.tconDict.get(tr.tconProps.key.idName);
      } else {
        tr.tconProps = cm.foreignTconDict.get(tr.tconProps.key);
      }
    }
    for (int i = 0; i < unresolvedFeatureList.size(); i++) {
      PFeatureSkel f = unresolvedFeatureList.get(i);
      if (cm.name.equals(f.featureProps.key.modName)) {
        f.featureProps= cm.fnameDict.get(f.featureProps.key.idName);
      } else {
        f.featureProps = cm.foreignFnameDict.get(f.featureProps.key);
      }
    }

    return cm;
  }

  void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    Iterator<String> i = this.tconDict.keySet().iterator();
    while (i.hasNext()) {
      String tcon = i.next();
      PDefDict.TconProps p = this.tconDict.get(tcon);
      if (p.subcat == PDefDict.TID_SUBCAT_EXTEND) {
        PDataDef dd = p.defGetter.getDataDef();
        g.addExtension(dd.getBaseTconKey(), PDefDict.IdKey.create(this.name, tcon));
      }
    }
  }

  private void mergeFunToEidDict(String name, int cat, Module.Access acc) {
    PDefDict.EidProps p = this.eidDict.get(name);
    p.cat |= cat;
    p.acc = Module.moreOpenAcc(acc, p.acc)? acc: p.acc;
  }

  public Module.Availability getModAvailability() { return this.availability; }

  public Cstr[] getForeignMods() {
    return this.foreignMods;
  }

  public PDefDict.EidProps resolveEid(String id, int catOpts, Option.Set<Module.Access> accOpts) {
    PDefDict.EidProps props = this.eidDict.get(id);
    return (props != null && (props.cat & catOpts) > 0 && accOpts.contains(props.acc))?
     props: null;
  }

  public PDefDict.TconProps resolveTcon(String tcon, int subcatOpts, Option.Set<Module.Access> accOpts) {
    // /* DEBUG */ System.out.print("compiled_module resolve tcon ");
    // /* DEBUG */ System.out.print(tcon);
    // /* DEBUG */ System.out.print(" -> ");
    PDefDict.TconProps tp =
      ((tp = this.tconDict.get(tcon)) != null
        && (tp.subcat & subcatOpts) > 0
        && accOpts.contains(tp.acc))?
      tp: null;
      // PDefDict.TconProps.create(PDefDict.IdKey.create(this.name, tcon), tp): null;
    // /* DEBUG */ System.out.println(ti);
    return tp;
    // return ((tp = this.tconDict.get(tcon)) != null && (tp.subcat & subcatOpts) > 0 && (tp.acc & accOpts) > 0)?
      // PDefDict.TconProps.create(PDefDict.IdKey.create(this.name, tcon), tp): null;
  }

  public PDefDict.FeatureProps resolveFeature(String fname, Option.Set<Module.Access> accOpts) {
    PDefDict.FeatureProps fp =
      ((fp = this.fnameDict.get(fname)) != null && accOpts.contains(fp.acc))?
      fp: null;
    return fp;
  }

  DataDef convertDataDef(Module mod, MDataDef dataDef, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    DataDef dd = new DataDef();
    dd.availability = dataDef.availability;
    dd.sigTcon = dataDef.tcon;
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    if (dataDef.params != null) {
      dd.paramVariances = new Module.Variance[dataDef.params.length];
      dd.sigParams = new PTypeVarSkel[dataDef.params.length];
      for (int i = 0; i < dataDef.params.length; i++) {
        dd.paramVariances[i] = dataDef.params[i].variance;
        PTypeVarSkel v = (PTypeVarSkel)this.convertType(dataDef.params[i].var, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
        dd.sigParams[i] = v;
      }
    }
    dd.acc = dataDef.acc;
    for (int i = 0; i < dataDef.constrs.length; i++) {
      MConstrDef mcd = dataDef.constrs[i];
      ConstrDef cd = dd.addConstr(mcd.dcon);
      for (int j = 0; j < mcd.attrs.length; j++) {
        MAttrDef mad = mcd.attrs[j];
        AttrDef ad = cd.addAttr(mad.name, this.convertType(mad.type, mod, varList, unresolvedTypeRefList, unresolvedFeatureList));
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
      fi.impl = convertFeature(mfi.provided, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
      dd.featureImpls[i] = fi;
    }
    return dd;
  }

  class DataDef implements PDataDef {
    Module.Availability availability;
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

    public int getParamCount() { return (this.sigParams != null)? this.sigParams.length: -1 ; }

    public PTypeRefSkel getTypeSig() {
      if (this.sig == null) {
        PDefDict.TconProps tp = PCompiledModule.this.tconDict.get(this.sigTcon);
        this.sig = PTypeRefSkel.create(PCompiledModule.this.defDictGetter, null, tp, false, this.sigParams);
      }
      return this.sig;
    }

    public Module.Variance getParamVarianceAt(int pos) { return this.paramVariances[pos]; }

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

  AliasTypeDef convertAliasTypeDef(Module mod, MAliasTypeDef aliasTypeDef, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    AliasTypeDef ad = new AliasTypeDef();
    PDefDict.IdKey ik = PDefDict.IdKey.create(mod.name, aliasTypeDef.tcon);
    PDefDict.DataDefGetter g = createDataDefGetter(ad);
    PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[aliasTypeDef.paramCount];
    for (int k = 0; k < paramPropss.length; k++) {
      paramPropss[k] = PDefDict.TparamProps.create(Module.INVARIANT, false);
    }
    ad.tconProps = PDefDict.TconProps.create(
      ik, PDefDict.TID_SUBCAT_ALIAS, paramPropss, aliasTypeDef.acc, g);
    ad.availability = aliasTypeDef.availability;
    ad.acc = aliasTypeDef.acc;
    ad.tparams = new PTypeVarSkel[aliasTypeDef.paramCount];
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    for (int i = 0; i < ad.tparams.length; i++) {
      ad.tparams[i] = PTypeVarSkel.create(null, null, PTypeVarSlot.createInternal(false), null);
      varList.add(ad.tparams[i]);
    }
    ad.body = (PTypeRefSkel)this.convertType(aliasTypeDef.body, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    return ad;
  }

  static class AliasTypeDef implements PAliasTypeDef {
    PDefDict.TconProps tconProps;
    Module.Availability availability;
    Module.Access acc;
    PTypeVarSkel[] tparams;
    PTypeRefSkel body;

    public String getTcon() { return this.tconProps.key.idName; }

    public PTypeVarSlot[] getParamVarSlots() {
      PTypeVarSlot[] vs = new PTypeVarSlot[this.tparams.length];
      for (int i = 0; i < this.tparams.length; i++) {
        vs[i] = this.tparams[i].varSlot;
      }
      return vs;
    }

    public Module.Availability getAvailability() { return this.availability; }

    public Module.Access getAcc() { return this.acc; }

    public void collectUnaliasTconProps(List<PDefDict.TconProps> list) { this.body.collectTconProps(list); }
    // public PDefDict.TconProps unaliasTconProps() { return this.body.tconProps; }

    public PTypeRefSkel getBody() { return this.body; }

    public PTypeRefSkel unalias(PTypeSkel[] params) {
      if (params.length != this.tparams.length) {
        throw new IllegalArgumentException("Length of unaliasing params mismatch.");
      }
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create();
      PTypeSkelBindings bindings = PTypeSkelBindings.create();
      for (int i = 0; i < params.length; i++) {
        bindings.bind(((PTypeVarSkel)this.tparams[i].instanciate(ic)).varSlot, params[i]);
      }
      PTypeRefSkel tr = (PTypeRefSkel)this.body.instanciate(ic).resolveBindings(bindings);
      // PTypeRefSkel tr = (PTypeRefSkel)this.body.instanciate(PTypeSkel.InstanciationBindings.create(bindings));
      // /* DEBUG */ System.out.print("unalias ");
      // /* DEBUG */ System.out.print(this.tconProps.key);
      // /* DEBUG */ System.out.print(" -> ");
      // /* DEBUG */ System.out.println(tr);
      // HERE: chain unaliasing
      return tr;
    }
  }

  static class ConstrDef implements PDataDef.Constr {
    DataDef dataDef;
    String dcon;
    List<AttrDef> attrList;

    ConstrDef(String dcon) {
      this.dcon = dcon;
      this.attrList = new ArrayList<AttrDef>();
    }

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

  static class AttrDef implements PDataDef.Attr {
    String name;
    PTypeSkel type;

    public String getName() { return this.name; }

    public PTypeSkel getNormalizedType() { return this.type; }

    public PTypeSkel getFixedType() { return this.type; }
  }

  static class FeatureImpl implements PDataDef.FeatureImpl {
    Cstr providerModName;
    String providerFunName;
    String getter;
    PFeatureSkel impl;

    public Cstr getProviderModName() { return this.providerModName; }

    public String getProviderFunName() { return this.providerFunName; }

    public String getGetter() { return this.getter; }

    public PFeatureSkel getImpl() { return this.impl; }
  }

  FeatureDef convertFeatureDef(Module mod, MFeatureDef featureDef, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    FeatureDef fd = new FeatureDef();
    fd.availability = featureDef.availability;
    fd.acc = featureDef.acc;
    fd.nameKey = PDefDict.IdKey.create(mod.name, featureDef.fname);
    fd.obj = convertTypeVar(featureDef.obj, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    fd.params = new PTypeVarSkel[featureDef.params.length];
    fd.paramVariances = new Module.Variance[featureDef.params.length];
    for (int i = 0; i < fd.params.length; i++) {
      fd.params[i] = convertTypeVar(featureDef.params[i].var, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
      fd.paramVariances[i] = featureDef.params[i].variance;
    }
    fd.impl = this.convertTypeRef(featureDef.impl, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    return fd;
  }

  static class FeatureDef implements PFeatureDef {
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

    public PTypeVarSkel getObjType() { return this.obj; }

    public PTypeVarSkel[] getParams() { return this.params; }

    // public PFeatureSkel getFeatureSig() { return HERE; }

    public Module.Variance getParamVarianceAt(int pos) { return this.paramVariances[pos]; }

    public PTypeRefSkel getImplType() { return this.impl; }
  }

  FunDef convertFunDef(Module mod, MFunDef funDef, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    FunDef fd = new FunDef();
    fd.modName = mod.name;
    fd.name = funDef.name;
    fd.availability = funDef.availability;
    fd.paramTypes = new PTypeSkel[funDef.paramTypes.length];
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    for (int i = 0; i < fd.paramTypes.length; i++) {
      fd.paramTypes[i] = this.convertType(funDef.paramTypes[i], mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    }
    fd.retType = this.convertType(funDef.retType, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    return fd;
  }

  static class FunDef implements PFunDef {
    Cstr modName;
    String name;
    Module.Availability availability;
    PTypeSkel[] paramTypes;
    PTypeSkel retType;

    public Cstr getModName() { return this.modName; }

    public String getOfficialName() { return this.name; }

    public Module.Availability getAvailability() { return this.availability; }

    public PTypeSkel[] getParamTypes() { return this.paramTypes; }

    public PTypeSkel[] getFixedParamTypes() { return this.paramTypes; }

    public PTypeSkel getRetType() { return this.retType; }

    public PTypeSkel getFixedRetType() { return this.retType; }
  }

  PDefDict.FunSelRes selectFun(String name, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    List<FunDef> funList = this.funListDict.get(name);
    if (funList == null) { return null; }
    PDefDict.FunSelRes sel = null;
    for (int i = 0; sel == null && i < funList.size(); i++) {
      FunDef fd = funList.get(i);
      PTypeSkel[] pts = fd.getParamTypes();
      if (pts.length != paramTypes.length) { continue; }
      PTypeSkelBindings bindings = PTypeSkelBindings.create(givenTVarList);
      boolean b = true;
      for (int j = 0; b && j < pts.length; j++) {
        b = pts[j].accept(PTypeSkel.NARROWER, paramTypes[j], bindings);
      }
      if (b) {
        for (int j = 0; b && j < pts.length; j++) {
          PTypeSkel p = paramTypes[j].resolveBindings(bindings);
          b = pts[j].extractAnyInconcreteVar(p /* , givenTVarList */) == null;
        }
      }
      if (b) {
        sel = PDefDict.FunSelRes.create(fd, bindings);
      }
    }
    return sel;
  }

  PFunDef getFun(String official) {
    return this.funOfficialDict.get(official);
  }

  ExprDefGetter createExprDefGetter(PDataDef dataDef) {
    return new ExprDefGetter(dataDef, null);
  }

  ExprDefGetter createExprDefGetter(String funName) {
    return new ExprDefGetter(null, funName);
  }

  class ExprDefGetter implements PDefDict.ExprDefGetter {
    PDataDef dataDef;
    String funName;
    boolean searchInLang;

    private ExprDefGetter(PDataDef dataDef, String funName) {
      this.dataDef = dataDef;
      this.funName = funName;
    }

    public void setSearchInLang() {
      this.searchInLang = true;
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public PDefDict.FunSelRes selectFunDef(PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
      PDefDict.FunSelRes r = null;
      if (this.funName == null) {
        ;
      } else if ((r = PCompiledModule.this.selectFun(this.funName, paramTypes, givenTVarList)) != null) {
        ;
      } else if (this.searchInLang) {
        throw new RuntimeException("PCompiledModule.ExprDefGetter#selectFunDef when 'search in lang' is on.");
      }
      return r;
    }

    public PFunDef getFunDef() { // get by official name
      PFunDef d = null;
      if (this.funName == null) {
        ;
      } else if ((d = PCompiledModule.this.getFun(this.funName)) != null) {
        ;
      } else if (this.searchInLang) {
        throw new RuntimeException("PCompiledModule.ExprDefGetter#getFunDef when 'search in lang' is on.");
      }
      return d;
    }
  }

  static DataDefGetter createDataDefGetter(DataDef def) {
    return new DataDefGetter(def, null);
  }

  static DataDefGetter createDataDefGetter(PAliasTypeDef def) {
    return new DataDefGetter(null, def);
  }

  static class DataDefGetter implements PDefDict.DataDefGetter {
    PDataDef dataDef;
    PAliasTypeDef aliasTypeDef;

    DataDefGetter(PDataDef dataDef, PAliasTypeDef aliasTypeDef) {
      this.dataDef = dataDef;
      this.aliasTypeDef = aliasTypeDef;
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public PAliasTypeDef getAliasTypeDef() { return this.aliasTypeDef; }
  }

  static FeatureDefGetter createFeatureDefGetter(PFeatureDef def) {
/* DEBUG */ if (def == null) { throw new IllegalArgumentException("Null feature def passed."); }
    return new FeatureDefGetter(def);
  }

  static class FeatureDefGetter implements PDefDict.FeatureDefGetter {
    PFeatureDef featureDef;

    FeatureDefGetter(PFeatureDef fd) {
/* DEBUG */ if (fd == null) { throw new IllegalArgumentException("Null feature def."); }
      this.featureDef = fd;
    }

    public PFeatureDef getFeatureDef() { return this.featureDef; }
  }

  PTypeSkel convertType(MType type, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    PTypeSkel t;
    if (type instanceof MTypeRef) {
      t = this.convertTypeRef((MTypeRef)type, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    } else if (type instanceof MTypeVar) {
      t = this.convertTypeVar((MTypeVar)type, mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    } else {
      throw new IllegalArgumentException("Unknown type description. - " + type);
    }
    return t;
  }

  PTypeRefSkel convertTypeRef(MTypeRef tr, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    PTypeSkel[] params = new PTypeSkel[tr.params.length];
    for (int i = 0; i < params.length; i++) {
      params[i] = this.convertType(tr.params[i], mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    }
    PTypeRefSkel t;
    Cstr n = mod.getModTab().get(tr.modIndex);
    // Cstr n = (tr.modName != null)? tr.modName: mod.name;
    PDefDict.IdKey ik = PDefDict.IdKey.create(n, tr.tcon);
    t = PTypeRefSkel.create(this.defDictGetter, null, PDefDict.TconProps.createUnresolved(ik), tr.ext, params);
    unresolvedTypeRefList.add((PTypeRefSkel)t);
    return t;
  }

  PTypeVarSkel convertTypeVar(MTypeVar tv, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    PTypeVarSkel v;
    if (tv.slot < varList.size()) {
      v = varList.get(tv.slot);
// /* DEBUG */ System.out.print("DEFINED "); System.out.print(varList); System.out.print(" "); System.out.print(tv); System.out.print(" -> "); System.out.println(v);
    } else if (tv.slot == varList.size()) {
      v = PTypeVarSkel.create(null, null,
        PTypeVarSlot.createInternal(tv.requiresConcrete), /* null, */ null);
      varList.add(v);
      v.features = (tv.features != null)?
        this.convertFeatures(tv.features, mod, varList, unresolvedTypeRefList, unresolvedFeatureList):
        null;
    } else {
      throw new RuntimeException("Slot number is not sequential. " + mod.name.toJavaString() + " " + tv.toString() + " " + varList.size());
    }
    return v;
  }

  PFeatureSkel.List convertFeatures(MFeature.List features, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    PFeatureSkel[] fs = new PFeatureSkel[ (features != null)? features.features.length: 0 ];  // if list is null then empty array
    for (int i = 0; i < fs.length; i++) {
      fs[i] = this.convertFeature(features.features[i], mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    }
    return PFeatureSkel.List.create(null, fs);
  }

  PFeatureSkel convertFeature(MFeature feature, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList, List<PFeatureSkel> unresolvedFeatureList) {
    PTypeSkel[] ps = new PTypeSkel[feature.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.convertType(feature.params[i], mod, varList, unresolvedTypeRefList, unresolvedFeatureList);
    }
    Cstr n = mod.getModTab().get(feature.modIndex);
    PDefDict.IdKey ik = PDefDict.IdKey.create(n, feature.name);
    PFeatureSkel f = PFeatureSkel.create(this.defDictGetter, null, PDefDict.FeatureProps.createUnresolved(ik), ps) ;
    unresolvedFeatureList.add(f);
    return f;
  }
}
