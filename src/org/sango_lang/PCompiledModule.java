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
  int availability;
  Cstr name;
  Cstr[] foreignMods;
  Map<PDefDict.TconKey, PDefDict.TconProps> foreignTconDict;
  Map<String, PDefDict.TconProps> tconDict;
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

    cm.foreignTconDict = new HashMap<PDefDict.TconKey, PDefDict.TconProps>();
    for (int i = 0; i < cm.foreignMods.length; i++) {
      MDataDef[] dds = mod.getForeignDataDefs(cm.foreignMods[i]);
      for (int j = 0; j < dds.length; j++) {
        DataDef dd = cm.convertDataDef(mod, dds[j], unresolvedTypeRefList);
        PDefDict.TconKey tk = PDefDict.TconKey.create(cm.foreignMods[i], dds[j].tcon);
        PDefDict.DataDefGetter g = createDataDefGetter(dd);
        PDefDict.TparamProps[] paramPropss;
        if (dds[j].params != null) {
          paramPropss = new PDefDict.TparamProps[dds[j].params.length];
          for (int k = 0; k < dds[j].params.length; k++) {
            paramPropss[k] = new PDefDict.TparamProps(dds[j].params[k].variance, dds[j].params[k].requiresConcrete);
          }
        } else {
          paramPropss = null;
        }
        PDefDict.TconProps tp = PDefDict.TconProps.create(
          (dds[j].baseModIndex == 0)? PTypeId.SUBCAT_DATA: PTypeId.SUBCAT_EXTEND,
          paramPropss, dds[j].acc, g);
        cm.foreignTconDict.put(tk, tp);
      }
      MAliasTypeDef[] ads = mod.getForeignAliasTypeDefs(cm.foreignMods[i]);
      for (int j = 0; j < ads.length; j++) {
        AliasTypeDef ad = cm.convertAliasTypeDef(mod, ads[j], unresolvedTypeRefList);
        PDefDict.TconKey tk = PDefDict.TconKey.create(cm.foreignMods[i], ads[j].tcon);
        PDefDict.DataDefGetter g = createDataDefGetter(ad);
        PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[ads[j].paramCount];
        for (int k = 0; k < paramPropss.length; k++) {
          paramPropss[k] = new PDefDict.TparamProps(Module.INVARIANT, false);
        }
        PDefDict.TconProps tp = PDefDict.TconProps.create(
          PTypeId.SUBCAT_ALIAS, paramPropss, ads[j].acc, g);
        cm.foreignTconDict.put(tk, tp);
      }
    }

    cm.tconDict = new HashMap<String, PDefDict.TconProps>();
    cm.eidDict = new HashMap<String, PDefDict.EidProps>();

    MDataDef[] mdds = mod.getDataDefs();
    for (int i = 0; i < mdds.length; i++) {
      MDataDef mdd = mdds[i];
      DataDef dd = cm.convertDataDef(mod, mdd, unresolvedTypeRefList);
      PDefDict.DataDefGetter g = createDataDefGetter(dd);
      PDefDict.TparamProps[] paramPropss;
      if (mdd.params != null) {
        paramPropss = new PDefDict.TparamProps[mdd.params.length];
        for (int k = 0; k < mdd.params.length; k++) {
          paramPropss[k] = new PDefDict.TparamProps(mdd.params[k].variance, mdd.params[k].requiresConcrete);
        }
      } else {
        paramPropss = null;
      }
      PDefDict.TconProps tp = PDefDict.TconProps.create(
        (mdd.baseModIndex == 0)? PTypeId.SUBCAT_DATA: PTypeId.SUBCAT_EXTEND,
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
        paramPropss[k] = new PDefDict.TparamProps(Module.INVARIANT, false);
      }
      cm.tconDict.put(matd.tcon, PDefDict.TconProps.create(
        PTypeId.SUBCAT_ALIAS,
        paramPropss, matd.acc, createDataDefGetter(cm.convertAliasTypeDef(mod, matd, unresolvedTypeRefList))));
    }
    cm.funOfficialDict = new HashMap<String, FunDef>();
    cm.funListDict = new HashMap<String, List<FunDef>>();
    MFunDef[] mfds = mod.getFunDefs();
    for (int i = 0; i < mfds.length; i++) {
      MFunDef mfd = mfds[i];
      FunDef fd = cm.convertFunDef(mod, mfd, unresolvedTypeRefList);
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
      if (cm.name.equals(tr.tconInfo.key.modName)) {
        tr.tconInfo.props = cm.tconDict.get(tr.tconInfo.key.tcon);
      } else {
        tr.tconInfo.props = cm.foreignTconDict.get(tr.tconInfo.key);
      }
      // HERE: props may not be resolved?
// /* DEBUG */ if (tr.tconInfo.props == null) { System.out.print("PN "); System.out.print(cm.name.toJavaString()); System.out.print(" "); System.out.println(tr); }
    }

    // /* DEBUG */ System.out.print("compiled "); System.out.print(mod.name.toJavaString()); System.out.print(" tcondict="); System.out.println(cm.tconDict);
    // /* DEBUG */ System.out.print("compiled "); System.out.print(mod.name.toJavaString()); System.out.print(" eiddict="); System.out.println(cm.eidDict);
    // /* DEBUG */ System.out.print("compiled "); System.out.print(mod.name.toJavaString()); System.out.print(" funofficialdict="); System.out.println(cm.funOfficialDict);
    // /* DEBUG */ System.out.print("compiled "); System.out.print(mod.name.toJavaString()); System.out.print(" funlistdict="); System.out.println(cm.funListDict);
    return cm;
  }

  void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    Iterator<String> i = this.tconDict.keySet().iterator();
    while (i.hasNext()) {
      String tcon = i.next();
      PDefDict.TconProps p = this.tconDict.get(tcon);
      if (p.subcat == PTypeId.SUBCAT_EXTEND) {
        PDataDef dd = p.defGetter.getDataDef();
        g.addExtension(dd.getBaseTconKey(), PDefDict.TconKey.create(this.name, tcon));
      }
    }
  }

  private void mergeFunToEidDict(String name, int cat, int acc) {
    PDefDict.EidProps p = this.eidDict.get(name);
    p.cat |= cat;
    p.acc |= acc;
  }

  public int getModAvailability() { return this.availability; }

  public Cstr[] getForeignMods() {
    return this.foreignMods;
  }

  public PDefDict.EidProps resolveEid(String id, int catOpts, int accOpts) {
    PDefDict.EidProps props = this.eidDict.get(id);
    return (props != null && (props.cat & catOpts) > 0 && (props.acc & accOpts) > 0)? props: null;
  }

  public PDefDict.TconInfo resolveTcon(String tcon, int subcatOpts, int accOpts) {
    PDefDict.TconProps tp;
    // /* DEBUG */ System.out.print("compiled_module resolve tcon ");
    // /* DEBUG */ System.out.print(tcon);
    // /* DEBUG */ System.out.print(" -> ");
    PDefDict.TconInfo ti = ((tp = this.tconDict.get(tcon)) != null && (tp.subcat & subcatOpts) > 0 && (tp.acc & accOpts) > 0)?
      PDefDict.TconInfo.create(PDefDict.TconKey.create(this.name, tcon), tp): null;
    // /* DEBUG */ System.out.println(ti);
    return ti;
    // return ((tp = this.tconDict.get(tcon)) != null && (tp.subcat & subcatOpts) > 0 && (tp.acc & accOpts) > 0)?
      // PDefDict.TconInfo.create(PDefDict.TconKey.create(this.name, tcon), tp): null;
  }

  DataDef convertDataDef(Module mod, MDataDef dataDef, List<PTypeRefSkel> unresolvedTypeRefList) {
    DataDef dd = new DataDef();
    dd.availability = dataDef.availability;
    dd.sigTcon = dataDef.tcon;
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    if (dataDef.params != null) {
      dd.sigParams = new PTypeVarSkel[dataDef.params.length];
      for (int i = 0; i < dataDef.params.length; i++) {
        PTypeVarSkel v = (PTypeVarSkel)convertType(dataDef.params[i], mod, varList, unresolvedTypeRefList);
        // PTypeVarSkel v = PTypeVarSkel.create(null, null,
          // PTypeVarSlot.createInternal(dataDef.params[i].variance, dataDef.params[i].requiresConcrete),
          // XXX);
        dd.sigParams[i] = v;
      }
    }
    dd.acc = dataDef.acc;
    for (int i = 0; i < dataDef.constrs.length; i++) {
      MConstrDef mcd = dataDef.constrs[i];
      ConstrDef cd = dd.addConstr(mcd.dcon);
      for (int j = 0; j < mcd.attrs.length; j++) {
        MAttrDef mad = mcd.attrs[j];
        AttrDef ad = cd.addAttr(mad.name, this.convertType(mad.type, mod, varList, unresolvedTypeRefList));
      }
    }
    if (dataDef.baseModIndex > 0) {
      dd.baseTconKey = PDefDict.TconKey.create(mod.getModAt(dataDef.baseModIndex), dataDef.baseTcon);
    }
    return dd;
  }

  class DataDef implements PDataDef {
    int availability;
    PTypeSkel sig;  // lazy setup
    String sigTcon;
    PTypeVarSkel[] sigParams;
    int acc;
    List<String> constrList;
    Map<String, ConstrDef> constrDict;
    PDefDict.TconKey baseTconKey;

    DataDef() {
      this.constrList = new ArrayList<String>();
      this.constrDict = new HashMap<String, ConstrDef>();
    }

    public String getFormalTcon() { return this.sigTcon; }

    public int getParamCount() { return (this.sigParams != null)? this.sigParams.length: -1 ; }

    public PTypeSkel getTypeSig() {
      if (this.sig == null) {
        // if (this.sigTcon.equals(Module.TCON_BOTTOM)) {
          // this.sig = PBottomSkel.create(null);
        // } else {
          PDefDict.TconKey tk = PDefDict.TconKey.create(PCompiledModule.this.name, this.sigTcon);
          PDefDict.TconProps tp = PCompiledModule.this.tconDict.get(this.sigTcon);
          this.sig = PTypeRefSkel.create(PCompiledModule.this.defDictGetter, null, PDefDict.TconInfo.create(tk, tp), false, this.sigParams);
        // }
      }
      return this.sig;
    }

    public int getAvailability() { return this.availability; }

    public int getAcc() { return this.acc; }

    public int getConstrCount() { return this.constrDict.size(); }

    public PDataDef.Constr getConstrAt(int index) { return this.constrDict.get(this.constrList.get(index)); }

    public PDataDef.Constr getConstr(String dcon) { return this.constrDict.get(dcon); }

    public PDefDict.TconKey getBaseTconKey() { return this.baseTconKey; }

    ConstrDef addConstr(String dcon) {
      ConstrDef cd = new ConstrDef(dcon);
      this.constrList.add(dcon);
      this.constrDict.put(dcon, cd);
      cd.dataDef = this;
      return cd;
    }
  }

  AliasTypeDef convertAliasTypeDef(Module mod, MAliasTypeDef aliasTypeDef, List<PTypeRefSkel> unresolvedTypeRefList) {
    AliasTypeDef ad = new AliasTypeDef();
    PDefDict.TconKey tk = PDefDict.TconKey.create(mod.name, aliasTypeDef.tcon);
    PDefDict.DataDefGetter g = createDataDefGetter(ad);
    PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[aliasTypeDef.paramCount];
    for (int k = 0; k < paramPropss.length; k++) {
      paramPropss[k] = new PDefDict.TparamProps(Module.INVARIANT, false);
    }
    PDefDict.TconProps tp = PDefDict.TconProps.create(
      PTypeId.SUBCAT_ALIAS, paramPropss, aliasTypeDef.acc, g);
    ad.tconInfo = PDefDict.TconInfo.create(tk, tp);
    ad.availability = aliasTypeDef.availability;
    ad.acc = aliasTypeDef.acc;
    ad.tparams = new PTypeVarSkel[aliasTypeDef.paramCount];
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    for (int i = 0; i < ad.tparams.length; i++) {
      ad.tparams[i] = PTypeVarSkel.create(null, null, PTypeVarSlot.createInternal(Module.INVARIANT, false), null);
      varList.add(ad.tparams[i]);
    }
    ad.body = (PTypeRefSkel)this.convertType(aliasTypeDef.body, mod, varList, unresolvedTypeRefList);
    return ad;
  }

  static class AliasTypeDef implements PAliasTypeDef {
    PDefDict.TconInfo tconInfo;
    int availability;
    int acc;
    PTypeVarSkel[] tparams;
    PTypeRefSkel body;

    public String getTcon() { return this.tconInfo.key.tcon; }

    public PTypeVarSlot[] getParamVarSlots() {
      PTypeVarSlot[] vs = new PTypeVarSlot[this.tparams.length];
      for (int i = 0; i < this.tparams.length; i++) {
        vs[i] = this.tparams[i].varSlot;
      }
      return vs;
    }

    public int getAvailability() { return this.availability; }

    public int getAcc() { return this.acc; }

    public void collectUnaliasTconInfo(List<PDefDict.TconInfo> list) { this.body.collectTconInfo(list); }
    // public PDefDict.TconInfo unaliasTconInfo() { return this.body.tconInfo; }

    public PTypeRefSkel getBody() { return this.body; }

    public PTypeRefSkel unalias(PTypeSkel[] params) {
      if (params.length != this.tparams.length) {
        throw new IllegalArgumentException("Length of unaliasing params mismatch.");
      }
      PTypeSkelBindings bindings = PTypeSkelBindings.create();
      for (int i = 0; i < params.length; i++) {
        bindings.bind(this.tparams[i].varSlot, params[i]);
      }
      PTypeRefSkel tr = (PTypeRefSkel)this.body.resolveBindings(bindings);
      // PTypeRefSkel tr = (PTypeRefSkel)this.body.instanciate(PTypeSkel.InstanciationBindings.create(bindings));
      // /* DEBUG */ System.out.print("unalias ");
      // /* DEBUG */ System.out.print(this.tconInfo.key);
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
      PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(bindings);
      return this.dataDef.getTypeSig().instanciate(ib);
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
  }

  FunDef convertFunDef(Module mod, MFunDef funDef, List<PTypeRefSkel> unresolvedTypeRefList) {
    FunDef fd = new FunDef();
    fd.modName = mod.name;
    fd.name = funDef.name;
    fd.availability = funDef.availability;
    fd.paramTypes = new PTypeSkel[funDef.paramTypes.length];
    List<PTypeVarSkel> varList = new ArrayList<PTypeVarSkel>();
    for (int i = 0; i < fd.paramTypes.length; i++) {
      fd.paramTypes[i] = this.convertType(funDef.paramTypes[i], mod, varList, unresolvedTypeRefList);
    }
    fd.retType = this.convertType(funDef.retType, mod, varList, unresolvedTypeRefList);
    return fd;
  }

  static class FunDef implements PFunDef {
    Cstr modName;
    String name;
    int availability;
    PTypeSkel[] paramTypes;
    PTypeSkel retType;

    public Cstr getModName() { return this.modName; }

    public String getOfficialName() { return this.name; }

    public int getAvailability() { return this.availability; }

    public PTypeSkel[] getParamTypes() { return this.paramTypes; }

    public PTypeSkel getRetType() { return this.retType; }
  }

  PDefDict.FunSelRes selectFun(String name, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenVarList) throws CompileException {
    List<FunDef> funList = this.funListDict.get(name);
    if (funList == null) { return null; }
    PDefDict.FunSelRes sel = null;
    for (int i = 0; sel == null && i < funList.size(); i++) {
      FunDef fd = funList.get(i);
      PTypeSkel[] pts = fd.getParamTypes();
      if (pts.length != paramTypes.length) { continue; }
      PTypeSkelBindings bindings = PTypeSkelBindings.create(givenVarList);
      boolean b = true;
      for (int j = 0; b && j < pts.length; j++) {
        b = pts[j].accept(PTypeSkel.NARROWER, true, paramTypes[j], bindings);
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

    public PDefDict.FunSelRes selectFunDef(PTypeSkel[] paramTypes, List<PTypeVarSlot> givenVarList) throws CompileException {
      PDefDict.FunSelRes r = null;
      if (this.funName == null) {
        ;
      } else if ((r = PCompiledModule.this.selectFun(this.funName, paramTypes, givenVarList)) != null) {
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

  PTypeSkel convertType(MType type, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList) {
    PTypeSkel t;
    if (type instanceof MTypeRef) {
      t = this.convertTypeRef((MTypeRef)type, mod, varList, unresolvedTypeRefList);
    } else if (type instanceof MTypeVar) {
      t = this.convertTypeVar((MTypeVar)type, mod, varList, unresolvedTypeRefList);
    } else {
      throw new IllegalArgumentException("Unknown type description. - " + type);
    }
    return t;
  }

  PTypeSkel convertTypeRef(MTypeRef tr, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList) {
    PTypeSkel[] params = new PTypeSkel[tr.params.length];
    for (int i = 0; i < params.length; i++) {
      params[i] = this.convertType(tr.params[i], mod, varList, unresolvedTypeRefList);
    }
    PTypeSkel t;
    Cstr n = (tr.modName != null)? tr.modName: mod.name;
    PDefDict.TconKey tk = PDefDict.TconKey.create(n, tr.tcon);
    t = PTypeRefSkel.create(this.defDictGetter, null, PDefDict.TconInfo.create(tk, null), tr.ext, params);
    unresolvedTypeRefList.add((PTypeRefSkel)t);
    return t;
  }

  PTypeSkel convertTypeVar(MTypeVar tv, Module mod, List<PTypeVarSkel> varList, List<PTypeRefSkel> unresolvedTypeRefList) {
    PTypeVarSkel v;
    if (tv.slot < varList.size()) {
      v = varList.get(tv.slot);
// /* DEBUG */ System.out.print("DEFINED "); System.out.print(varList); System.out.print(" "); System.out.print(tv); System.out.print(" -> "); System.out.println(v);
    } else if (tv.slot == varList.size()) {
      v = PTypeVarSkel.create(null, null, PTypeVarSlot.createInternal(tv.variance, tv.requiresConcrete), null);
      varList.add(v);
      if (tv.constraint != null) {
        v.constraint = (PTypeRefSkel)convertType(tv.constraint, mod, varList, unresolvedTypeRefList);
      }
    } else {
      throw new RuntimeException("Slot number is not sequential. " + mod.name.toJavaString() + " " + tv.toString() + " " + varList.size());
    }
// /* DEBUG */ if (v.constraint != null) { System.out.print("CONVERT "); System.out.print(tv); System.out.print(" -> "); System.out.println(v); }
    return v;
  }
}
