/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2024 AKIYAMA Isao                                         *
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

class PDefDict {
  static final int TID_CAT_NOT_FOUND = 0;
  static final int TID_CAT_VAR = 1;
  static final int TID_CAT_TCON_DATAEXT_DATA = 2;
  static final int TID_CAT_TCON_DATAEXT_EXTEND = 4;
  static final int TID_CAT_TCON_DATAEXT = TID_CAT_TCON_DATAEXT_DATA + TID_CAT_TCON_DATAEXT_EXTEND;
  static final int TID_CAT_TCON_ALIAS = 8;
  static final int TID_CAT_TCON = TID_CAT_TCON_DATAEXT + TID_CAT_TCON_ALIAS;
  static final int TID_CAT_FEATURE = 16;

  static final int EID_CAT_NOT_FOUND = 0;
  static final int EID_CAT_VAR = 1;
  static final int EID_CAT_DCON_EVAL = 2;
  static final int EID_CAT_DCON_PTN = 4;
  static final int EID_CAT_DCON = EID_CAT_DCON_EVAL + EID_CAT_DCON_PTN;
  static final int EID_CAT_FUN_OFFICIAL = 8;
  static final int EID_CAT_FUN_ALIAS = 16;
  static final int EID_CAT_FUN = EID_CAT_FUN_OFFICIAL + EID_CAT_FUN_ALIAS;
  static final int EID_CAT_ANCHOR = EID_CAT_DCON_EVAL + EID_CAT_FUN;

  Map<Cstr, PModDecl> modDeclDict;
  Set<Cstr> unavailableMods;
  Map<IdKey, IdKey> dconDict;  // dcon -> tcon
  Map<IdKey, TidProps> tidDict;
  Map<IdKey, EidProps> eidDict;
  Map<IdKey, PDataDef> dataDefDict;
  Map<IdKey, PAliasTypeDef> aliasTypeDefDict;
  Map<IdKey, PFeatureDef> featureDefDict;
  Map<Cstr, List<PFunDef>> funDefListDict;  // def list in each module
  Map<Cstr, Set<IdKey>> foreignDataDefs;
  Map<Cstr, Set<IdKey>> foreignAliasTypeDefs;
  Map<Cstr, Set<IdKey>> foreignFeatureDefs;
  Map<Cstr, Set<IdKey>> foreignFunDefs;
  Map<Cstr, Set<IdKey>> foreignDconsForEval;  // not used
  Map<Cstr, Set<IdKey>> foreignDconsForPtn;  // not used

  PDefDict(Set<Cstr> unavailableMods) {
    this.modDeclDict = new HashMap<Cstr, PModDecl>();
    this.unavailableMods = unavailableMods;
    this.tidDict = new HashMap<IdKey, TidProps>();
    this.eidDict = new HashMap<IdKey, EidProps>();
    this.dataDefDict = new HashMap<IdKey, PDataDef>();
    this.aliasTypeDefDict = new HashMap<IdKey, PAliasTypeDef>();
    this.featureDefDict = new HashMap<IdKey, PFeatureDef>();
    this.funDefListDict = new HashMap<Cstr, List<PFunDef>>();
    this.dconDict = new HashMap<IdKey, IdKey>();
    this.foreignDataDefs = new HashMap<Cstr, Set<IdKey>>();
    this.foreignAliasTypeDefs = new HashMap<Cstr, Set<IdKey>>();
    this.foreignFeatureDefs = new HashMap<Cstr, Set<IdKey>>();
    this.foreignFunDefs = new HashMap<Cstr, Set<IdKey>>();
    this.foreignDconsForEval = new HashMap<Cstr, Set<IdKey>>();
    this.foreignDconsForPtn = new HashMap<Cstr, Set<IdKey>>();
  }

  boolean predefineTconData(IdKey tid, Module.Access acc) {
    boolean succeeded;
    TidProps tp = this.tidDict.get(tid);
    if (tp == null) {
      tp = TidProps.create(tid, TID_CAT_TCON_DATAEXT_DATA, acc);
      this.tidDict.put(tid, tp);
      succeeded = true;
    } else {
      succeeded = false;
    }
    return succeeded;
  }

  boolean predefineTconExtend(IdKey tid, Module.Access acc) {
    boolean succeeded;
    TidProps tp = this.tidDict.get(tid);
    if (tp == null) {
      tp = TidProps.create(tid, TID_CAT_TCON_DATAEXT_EXTEND, acc);
      this.tidDict.put(tid, tp);
      succeeded = true;
    } else {
      succeeded = false;
    }
    return succeeded;
  }

  boolean predefineTconAliasType(IdKey tid, Module.Access acc) {
    boolean succeeded;
    TidProps tp = this.tidDict.get(tid);
    if (tp == null) {
      tp = TidProps.create(tid, TID_CAT_TCON_ALIAS, acc);
      this.tidDict.put(tid, tp);
      succeeded = true;
    } else {
      succeeded = false;
    }
    return succeeded;
  }

  boolean predefineFeature(IdKey tid, Module.Access acc) {
    boolean succeeded;
    TidProps tp = this.tidDict.get(tid);
    if (tp == null) {
      tp = TidProps.create(tid, TID_CAT_FEATURE, acc);
      this.tidDict.put(tid, tp);
      succeeded = true;
    } else {
      succeeded = false;
    }
    return succeeded;
  }

  boolean predefineDcon(IdKey eid, Module.Access acc) {
    boolean succeeded;
    EidProps ep = this.eidDict.get(eid);
    if (ep == null) {
      ep = EidProps.create(eid, EID_CAT_DCON, acc, null);
      this.eidDict.put(eid, ep);
      succeeded = true;
    } else {
      succeeded = false;
    }
    return succeeded;
  }

  boolean predefineFunOfficial(IdKey eid, Module.Access acc) {
// /* DEBUG */ System.out.print("predefine fun official "); System.out.println(eid);
    boolean succeeded;
    EidProps ep = this.eidDict.get(eid);
    if (ep == null) {
      ep = EidProps.create(eid, EID_CAT_FUN_OFFICIAL, acc, acc);
      this.eidDict.put(eid, ep);
      succeeded = true;
    } else if ((ep.cat & EID_CAT_DCON) == 0 && (ep.cat & EID_CAT_FUN_OFFICIAL) == 0) {
      ep.cat |= EID_CAT_FUN_OFFICIAL;
      ep.acc = Module.moreOpenAcc(acc, ep.acc)? acc: ep.acc;  // more open
      ep.acc2 = acc;
      succeeded = true;
    } else {
      succeeded = false;
    }
// /* DEBUG */ if (!succeeded) { throw new RuntimeException("TRAP"); }
    return succeeded;
  }

  boolean predefineFunAlias(IdKey eid, Module.Access acc) {
    boolean succeeded;
    EidProps ep = this.eidDict.get(eid);
    if (ep == null) {
      ep = EidProps.create(eid, EID_CAT_FUN_ALIAS, acc, null);
      this.eidDict.put(eid, ep);
      succeeded = true;
    } else if ((ep.cat & EID_CAT_DCON) == 0) {
      ep.cat |= EID_CAT_FUN_ALIAS;
      ep.acc = Module.moreOpenAcc(acc, ep.acc)? acc: ep.acc;  // more open
      succeeded = true;
    } else {
      succeeded = false;
    }
    return succeeded;
  }

  void putModDecl(Cstr modName, PModDecl decl) {
    this.modDeclDict.put(modName, decl);
  }

  void putDataDef(IdKey tid, PDataDef def) {
    // call predefineXXX and check in advance
    TidProps tp = this.tidDict.get(tid);
    if (tp == null || (tp.cat & TID_CAT_TCON_DATAEXT) == 0) { throw new IllegalArgumentException("Not predefined. " + tid); }
    if (def.getBaseTconKey() == null) {
      if ((tp.cat & TID_CAT_TCON_DATAEXT_DATA) == 0) { throw new IllegalArgumentException("Category mismatch. " + tid); }
    } else {
      if ((tp.cat & TID_CAT_TCON_DATAEXT_EXTEND) == 0) { throw new IllegalArgumentException("Category mismatch. " + tid); }
    }
    for (int i = 0; i < def.getConstrCount(); i++) {
      PDataDef.Constr c = def.getConstrAt(i);
      IdKey dcon = IdKey.create(tid.modName, c.getDcon());
      EidProps ep = this.eidDict.get(dcon);
      if (ep == null || (ep.cat & EID_CAT_DCON) == 0) { throw new IllegalArgumentException("Not predefined. " + dcon); }
      this.dconDict.put(dcon, tid);
    }
    this.dataDefDict.put(tid, def);
  }

  void putAliasTypeDef(IdKey tid, PAliasTypeDef def) {
    // call predefineXXX and check in advance
/* TRAP */ if (def.getTcon() == null) { throw new IllegalArgumentException("Null tcon."); }
    TidProps tp = this.tidDict.get(tid);
    if (tp == null || (tp.cat & TID_CAT_TCON_ALIAS) == 0) { throw new IllegalArgumentException("Not predefined. " + tid); }
    this.aliasTypeDefDict.put(tid, def);
  }

  void putFeatureDef(IdKey tid, PFeatureDef def) {
    // call predefineXXX and check in advance
    TidProps tp = this.tidDict.get(tid);
    if (tp == null || (tp.cat & TID_CAT_FEATURE) == 0) { throw new IllegalArgumentException("Not predefined. " + tid); }
    this.featureDefDict.put(tid, def);
  }

  void putFunDef(IdKey eid, PFunDef def) {
    // call predefineXXX and check in advance
    EidProps ep = this.eidDict.get(eid);
    if (ep == null || (ep.cat & EID_CAT_FUN_OFFICIAL) == 0) { throw new IllegalArgumentException("Not predefined. " + eid); }
    String[] aliases = def.getAliases();
    for (int i = 0; i < aliases.length; i++) {
      IdKey alias = IdKey.create(eid.modName, aliases[i]);
      EidProps epa = this.eidDict.get(alias);
      if (epa == null || (epa.cat & EID_CAT_FUN_ALIAS) == 0) { throw new IllegalArgumentException("Not predefined. " + alias); }
    }
    List<PFunDef> fds = this.funDefListDict.get(eid.modName);
    if (fds == null) {
      fds = new ArrayList<PFunDef>();
      this.funDefListDict.put(eid.modName, fds);
    }
    fds.add(def);
  }

  PDataDef getDataDef(Cstr referrer, IdKey tid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(tid.modName);  // may throw exception
    PDataDef def = this.dataDefDict.get(tid);
    if (def != null) {
      this.recordForeignDataDef(referrer, tid);
    }
    return def;
  }

  void checkModIsAvailable(Cstr modName) throws CompileException {
    if (this.unavailableMods.contains(modName)) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Module ");
      emsg.append(modName.repr());
      emsg.append(" is unavailable.");
      throw new CompileException(emsg.toString());
    }
  }

  void addUnavailableMod(Cstr modName) {
    this.unavailableMods.add(modName);
  }

  void checkCyclicAlias() throws CompileException {
    List<IdKey> tconsToCheck = new ArrayList<IdKey>();
    Set<IdKey> tconsChecked = new HashSet<IdKey>();
    Iterator<IdKey> iter = this.tidDict.keySet().iterator();
    while (iter.hasNext()) {
      IdKey k = iter.next();
      TidProps tp = this.tidDict.get(k);
      if ((tp.cat & TID_CAT_TCON_DATAEXT) > 0) {
        tconsChecked.add(k);
      } else if ((tp.cat & TID_CAT_TCON_ALIAS) > 0) {
        tconsToCheck.add(k);
      }
    }
    for (int i = 0; i < tconsToCheck.size(); i++) {
      this.checkCyclicAlias1(tconsToCheck.get(i), tconsChecked);
    }
  }

  private void checkCyclicAlias1(IdKey tcon, Set<IdKey> tconsChecked) throws CompileException {
    Set<IdKey> tconsChecking = new HashSet<IdKey>();
    tconsChecking.add(tcon);
    this.checkCyclicAlias2(
      this.aliasTypeDefDict.get(tcon).getBody(),
      tconsChecking,
      tconsChecked);
  }

  private void checkCyclicAlias2(PTypeSkel t, Set<IdKey> tconsChecking, Set<IdKey> tconsChecked) throws CompileException {
    if (!(t instanceof PTypeRefSkel)) { return; }
    PTypeRefSkel tr = (PTypeRefSkel)t;
    if (tconsChecked.contains(tr.tconKey)) {
      ;
    } else if (tconsChecking.contains(tr.tconKey)) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cyclic definition for type alias on \"");
      emsg.append(tr.tconKey.repr());
      emsg.append("\".");
      throw new CompileException(emsg.toString());
    } else {
      tconsChecking.add(tr.tconKey);
      for (int i = 0; i < tr.params.length; i++) {
        this.checkCyclicAlias2(tr.params[i], new HashSet<IdKey>(tconsChecking), tconsChecked);
      }
      tconsChecked.add(tr.tconKey);
    }
  }

  void checkCyclicExtension() throws CompileException {
    List<IdKey> tconsToCheck = new ArrayList<IdKey>();
    Set<IdKey> tconsChecked = new HashSet<IdKey>();
    Iterator<IdKey> iter = this.tidDict.keySet().iterator();
    while (iter.hasNext()) {
      IdKey k = iter.next();
      TidProps tp = this.tidDict.get(k);
      if ((tp.cat & TID_CAT_TCON_DATAEXT_DATA) > 0) {
        tconsChecked.add(k);
      } else if ((tp.cat & TID_CAT_TCON_DATAEXT_EXTEND) > 0) {
        tconsToCheck.add(k);
      }
    }
    for (int i = 0; i < tconsToCheck.size(); i++) {
      this.checkCyclicExtension1(tconsToCheck.get(i), tconsChecked);
    }
  }

  private void checkCyclicExtension1(IdKey tcon, Set<IdKey> tconsChecked) throws CompileException {
    Set<IdKey> tconsChecking = new HashSet<IdKey>();
    tconsChecking.add(tcon);
    this.checkCyclicExtension2(
      this.dataDefDict.get(tcon).getBaseTconKey(),
      tconsChecking,
      tconsChecked);
  }

  private void checkCyclicExtension2(IdKey baseTconKey, Set<IdKey> tconsChecking, Set<IdKey> tconsChecked) throws CompileException {
    if (tconsChecked.contains(baseTconKey)) {
      ;
    } else if (tconsChecking.contains(baseTconKey)) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cyclic definition for data extention on \"");
      emsg.append(baseTconKey.repr());
      emsg.append("\".");
      throw new CompileException(emsg.toString());
    } else {
      tconsChecking.add(baseTconKey);
      this.checkCyclicExtension2(
        this.dataDefDict.get(baseTconKey).getBaseTconKey(),
        tconsChecking,
        tconsChecked);
      tconsChecked.add(baseTconKey);
    }
  }

  TidProps resolveTcon(Cstr referrer, IdKey tid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(tid.modName);  // may throw exception
    TidProps tp = this.tidDict.get(tid);
    if (tp == null) {
      ;  // go through
    } else if ((tp.cat & TID_CAT_TCON) == 0) {
      tp = null;  // not hit
    } else if (referrer == null || referrer.equals(tp.key.modName) || tp.acc != Module.ACC_PRIVATE) {
      ;  // access mode ok
    } else {
      tp = null;  // access not allowed
    }
    return tp;
  }

  TidProps resolveFeature(Cstr referrer, IdKey tid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(tid.modName);  // may throw exception
    TidProps tp = this.tidDict.get(tid);
    if (tp == null) {
      ;  // go through
    } else if ((tp.cat & TID_CAT_FEATURE) == 0) {
      tp = null;  // not hit
    } else if (referrer == null || referrer.equals(tp.key.modName) || tp.acc != Module.ACC_PRIVATE) {
      ;  // access mode ok
    } else {
      tp = null;  // access not allowed
    }
    return tp;
  }

  EidProps resolveAnchor(Cstr referrer, IdKey eid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(eid.modName);  // may throw exception
    EidProps ep = this.eidDict.get(eid);
    if (ep == null) {
      ;  // go through
    } else if ((ep.cat & EID_CAT_ANCHOR) == 0) {
      ep = null;  // not hit
    } else if (referrer == null || referrer.equals(ep.key.modName) || ep.acc == Module.ACC_PUBLIC || ep.acc == Module.ACC_PROTECTED) {
      ;  // access mode ok
    } else {
      ep = null;  // access not allowed
    }
    return ep;
  }

  EidProps resolveDconPtn(Cstr referrer, IdKey eid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(eid.modName);  // may throw exception
    EidProps ep = this.eidDict.get(eid);
    if (ep == null) {
      ;  // go through
    } else if ((ep.cat & EID_CAT_DCON_PTN) == 0) {
      ep = null;  // not hit
    } else if (referrer == null || referrer.equals(ep.key.modName) || ep.acc == Module.ACC_PUBLIC || ep.acc == Module.ACC_PROTECTED) {
      ;  // access mode ok
    } else {
      ep = null;  // access not allowed
    }
    return ep;
  }

  EidProps resolveFunOfficial(Cstr referrer, IdKey eid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(eid.modName);  // may throw exception
    EidProps ep = this.eidDict.get(eid);
    if (ep == null) {
      ;  // go through
    } else if ((ep.cat & EID_CAT_FUN_OFFICIAL) == 0) {
      ep = null;  // not hit
    } else if (referrer == null || referrer.equals(ep.key.modName) || ep.acc2 != Module.ACC_PRIVATE) {
      ;  // access mode ok
    } else {
      ep = null;  // access not allowed
    }
    return ep;
  }

  boolean isBaseOf(IdKey base, IdKey ext) throws CompileException {
    PDataDef ed = this.dataDefDict.get(ext);
    if (ed == null) { throw new IllegalArgumentException("Not a data/ext. " + ext); }
    if (ext.equals(base)) { return true; }
    IdKey x = ed.getBaseTconKey();
    boolean b = false;
    while (!b && x != null) {
      if (x.equals(base)) {
        b = true;  // found
      } else {
        PDataDef xd = this.dataDefDict.get(x);
        if (xd == null) { throw new IllegalArgumentException("Not a data/ext. " + x); }
        x = xd.getBaseTconKey();  // repeat
      }
    }
    return b;
  }

  PModDecl getModDecl(Cstr modName) {
    return this.modDeclDict.get(modName);
  }

  IdKey getTconFromDconForEval(Cstr referrer, IdKey eid) throws CompileException {
    // returns null if undefined dcon
    EidProps ep = this.resolveAnchor(referrer, eid);  // may throw exception
    if (ep == null || (ep.cat & EID_CAT_DCON_EVAL) == 0) { return null; }
    // this.checkModIsAvailable(tid.modName);  // may throw exception  // checked in resolveAnchor
    IdKey tcon = this.dconDict.get(eid);
    if (tcon == null) { throw new RuntimeException("Tcon not defined. " + eid); }
    return tcon;
  }

  IdKey getTconFromDconForPtn(Cstr referrer, IdKey eid) throws CompileException {
    // returns null if undefined dcon
    EidProps ep = this.resolveAnchor(referrer, eid);  // may throw exception
    if (ep == null || (ep.cat & EID_CAT_DCON_PTN) == 0) { return null; }
    // this.checkModIsAvailable(tid.modName);  // may throw exception  // checked in resolveAnchor
    IdKey tcon = this.dconDict.get(eid);
    if (tcon == null) { throw new RuntimeException("Tcon not defined. " + eid); }
    return tcon;
  }

  PAliasTypeDef getAliasTypeDef(Cstr referrer, IdKey tid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(tid.modName);  // may throw exception
    PAliasTypeDef def = this.aliasTypeDefDict.get(tid);
    if (def != null) {
      this.recordForeignAliasTypeDef(referrer, tid);
    }
    return def;
  }

  PFeatureDef getFeatureDef(Cstr referrer, IdKey tid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(tid.modName);  // may throw exception
    PFeatureDef def = this.featureDefDict.get(tid);
    if (def != null) {
      this.recordForeignFeatureDef(referrer, tid);
    }
    return def;
  }

  PFunDef getFunDefByOfficial(Cstr referrer, IdKey eid) throws CompileException {
    // returns null if not found
    this.checkModIsAvailable(eid.modName);  // may throw exception
    List<PFunDef> fds = this.funDefListDict.get(eid.modName);
    PFunDef def = null;
    if (fds != null) {
      for (int i = 0; def == null && i < fds.size(); i++) {
        PFunDef f = fds.get(i);
        if (f.getOfficialName().equals(eid.idName)) {
          def = f;
        }
      }
    }
    if (def != null) {
      this.recordForeignFunDef(referrer, eid);
    }
    return def;
  }

  FunSelRes selectFunDef(Cstr referrer, IdKey eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
// /* DEBUG */ System.out.println(eid);
    // returns null if not found
    this.checkModIsAvailable(eid.modName);  // may throw exception
    List<PFunDef> fds = this.funDefListDict.get(eid.modName);
    FunSelRes sel = null;
    if (fds != null) {
      for (int i = 0; sel == null && i < fds.size(); i++) {
        PFunDef def = fds.get(i);
        String[] aliases = def.getAliases();
        PTypeSkelBindings bindings = null;
        if (def.getOfficialName().equals(eid.idName)) {
          bindings = tryApply(def, paramTypes, givenTVarList);
        }
        for (int j = 0; bindings == null && j < aliases.length; j++)  {
          if (aliases[j].equals(eid.idName)) {
            bindings = tryApply(def, paramTypes, givenTVarList);
          }
        }
        sel = (bindings != null)? FunSelRes.create(def, bindings): null;
      }
    }
    if (sel != null) {
      this.recordForeignFunDef(referrer, IdKey.create(eid.modName, sel.funDef.getOfficialName()));
    }
    return sel;
  }

  private static PTypeSkelBindings tryApply(PFunDef def, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    // PFunDef#getParamTypes maybe throw CompileException current implementation...
    PTypeSkel[] pts = def.getParamTypes();
    if (pts.length != paramTypes.length) { return null; }
    PTypeSkelBindings bindings = PTypeSkelBindings.create(givenTVarList);
    boolean b = true;
    for (int i = 0; b && i < pts.length; i++) {
// /* DEBUG */ System.out.print(pts[i]); System.out.println(paramTypes[i]);
      b = pts[i].accept(PTypeSkel.NARROWER, paramTypes[i], bindings);
    }
    if (b) {
      for (int i = 0; b && i < pts.length; i++) {
        PTypeSkel p = paramTypes[i].resolveBindings(bindings);
        b = pts[i].extractAnyInconcreteVar(p) == null;
      }
    }
   return b? bindings: null;
  }

  void addReferredForeignTcon(Cstr referrer, IdKey tid) throws CompileException {
/* TRAP */ if (tid.idName == null) { throw new RuntimeException("Null id name."); }
    if (referrer == null) { return; }  // skip if internal ref
    if (referrer.equals(tid.modName)) { return; }  // skip if local ref
    TidProps tp = this.resolveTcon(referrer, tid);
    if (tp == null) { throw new IllegalArgumentException("Unknown tid. " + tid.toString()); }
    if ((tp.cat & TID_CAT_TCON_DATAEXT) > 0) {
      this.getDataDef(referrer, tid);  // record reference internally
    } else if ((tp.cat & TID_CAT_TCON_ALIAS) > 0) {
      this.getAliasTypeDef(referrer, tid);  // record reference internally
    } else {
      throw new IllegalArgumentException("Unexpected cat. " + tid.toString());
    }
  }

  void recordForeignDataDef(Cstr referrer, IdKey tid) {
/* TRAP */ if (tid.idName == null) { throw new RuntimeException("Null id name."); }
    if (referrer == null) { return; }  // skip if internal ref
    if (referrer.equals(tid.modName)) { return; }  // skip if local ref
    Set<IdKey> referred = this.foreignDataDefs.get(referrer);
    if (referred == null) {
      referred = new HashSet<IdKey>();
      this.foreignDataDefs.put(referrer, referred);
    }
    referred.add(tid);
  }

  void recordForeignAliasTypeDef(Cstr referrer, IdKey tid) {
/* TRAP */ if (tid.idName == null) { throw new RuntimeException("Null id name."); }
    if (referrer == null) { return; }  // skip if internal ref
    if (referrer.equals(tid.modName)) { return; }  // skip if local ref
    Set<IdKey> referred = this.foreignAliasTypeDefs.get(referrer);
    if (referred == null) {
      referred = new HashSet<IdKey>();
      this.foreignAliasTypeDefs.put(referrer, referred);
    }
    referred.add(tid);
  }

  void recordForeignFeatureDef(Cstr referrer, IdKey tid) {
/* TRAP */ if (tid.idName == null) { throw new RuntimeException("Null id name."); }
    if (referrer == null) { return; }  // skip if internal ref
    if (referrer.equals(tid.modName)) { return; }  // skip if local ref
    Set<IdKey> referred = this.foreignFeatureDefs.get(referrer);
    if (referred == null) {
      referred = new HashSet<IdKey>();
      this.foreignFeatureDefs.put(referrer, referred);
    }
    referred.add(tid);
  }

  void recordForeignFunDef(Cstr referrer, IdKey eid) {
    if (referrer == null) { return; }  // skip if internal ref
    if (referrer.equals(eid.modName)) { return; }  // skip if local ref
    Set<IdKey> referred = this.foreignFunDefs.get(referrer);
    if (referred == null) {
      referred = new HashSet<IdKey>();
      this.foreignFunDefs.put(referrer, referred);
    }
    referred.add(eid);
  }

  List<PDataDef> getAllDataDefsIn(Cstr modName) {
    List<PDataDef> defs = new ArrayList<PDataDef>();
    Iterator<IdKey> ki = this.dataDefDict.keySet().iterator();
    while (ki.hasNext()) {
      IdKey k = ki.next();
      if (modName.equals(k.modName)) {
        defs.add(this.dataDefDict.get(k));
      }
    }
    return defs;
  }

  List<PAliasTypeDef> getAllAliasTypeDefsIn(Cstr modName) {
    List<PAliasTypeDef> defs = new ArrayList<PAliasTypeDef>();
    Iterator<IdKey> ki = this.aliasTypeDefDict.keySet().iterator();
    while (ki.hasNext()) {
      IdKey k = ki.next();
      if (modName.equals(k.modName)) {
        defs.add(this.aliasTypeDefDict.get(k));
      }
    }
    return defs;
  }

  List<PFeatureDef> getAllFeatureDefsIn(Cstr modName) {
    List<PFeatureDef> defs = new ArrayList<PFeatureDef>();
    Iterator<IdKey> ki = this.featureDefDict.keySet().iterator();
    while (ki.hasNext()) {
      IdKey k = ki.next();
      if (modName.equals(k.modName)) {
        defs.add(this.featureDefDict.get(k));
      }
    }
    return defs;
  }

  List<PFunDef> getAllFunDefsIn(Cstr modName) {
    List<PFunDef> defs = this.funDefListDict.get(modName);
    return (defs != null)? defs: new ArrayList<PFunDef>();
  }

  List<PDataDef> getForeignDataDefsIn(Cstr referrer, Cstr modName) {
    List<PDataDef> defs = new ArrayList<PDataDef>();
    Set<IdKey> ids = this.foreignDataDefs.get(referrer);
    if (ids != null) {
      Iterator<IdKey> i = ids.iterator();
      while (i.hasNext()) {
        IdKey id = i.next();
        if (id.modName.equals(modName)) {
          defs.add(this.dataDefDict.get(id));
        }
      }
    }
    return defs;
  }

  List<PAliasTypeDef> getForeignAliasTypeDefsIn(Cstr referrer, Cstr modName) {
    List<PAliasTypeDef> defs = new ArrayList<PAliasTypeDef>();
    Set<IdKey> ids = this.foreignAliasTypeDefs.get(referrer);
    if (ids != null) {
      Iterator<IdKey> i = ids.iterator();
      while (i.hasNext()) {
        IdKey id = i.next();
        if (id.modName.equals(modName)) {
          defs.add(this.aliasTypeDefDict.get(id));
        }
      }
    }
    return defs;
  }

  List<PFeatureDef> getForeignFeatureDefsIn(Cstr referrer, Cstr modName) {
    List<PFeatureDef> defs = new ArrayList<PFeatureDef>();
    Set<IdKey> ids = this.foreignFeatureDefs.get(referrer);
    if (ids != null) {
      Iterator<IdKey> i = ids.iterator();
      while (i.hasNext()) {
        IdKey id = i.next();
        if (id.modName.equals(modName)) {
          defs.add(this.featureDefDict.get(id));
        }
      }
    }
    return defs;
  }

  List<PFunDef> getForeignFunDefsIn(Cstr referrer, Cstr modName) {
    List<PFunDef> defs = new ArrayList<PFunDef>();
    Set<IdKey> ids = this.foreignFunDefs.get(referrer);
    if (ids != null) {
      Iterator<IdKey> i = ids.iterator();
      while (i.hasNext()) {
        IdKey id = i.next();
        if (id.modName.equals(modName)) {
          try {
            defs.add(this.getFunDefByOfficial(null, id));
          } catch (CompileException ex) {
            throw new RuntimeException(ex.toString());
          }
        }
      }
    }
    return defs;
  }

  // not used
  List<String> getForeignDconsForEvalIn(Cstr referrer, Cstr modName) {
    List<String> dcons = new ArrayList<String>();
    Set<IdKey> ids = this.foreignDconsForEval.get(referrer);
    if (ids != null) {
      Iterator<IdKey> i = ids.iterator();
      while (i.hasNext()) {
        IdKey id = i.next();
        if (id.modName.equals(modName)) {
          dcons.add(id.idName);
        }
      }
    }
    return dcons;
  }

  // not used
  List<String> getForeignDconsForPtnIn(Cstr referrer, Cstr modName) {
    List<String> dcons = new ArrayList<String>();
    Set<IdKey> ids = this.foreignDconsForPtn.get(referrer);
    if (ids != null) {
      Iterator<IdKey> i = ids.iterator();
      while (i.hasNext()) {
        IdKey id = i.next();
        if (id.modName.equals(modName)) {
          dcons.add(id.idName);
        }
      }
    }
    return dcons;
  }

  static class IdKey {
    Cstr modName;  // "" if variables...
    String idName;

    public static IdKey create(Cstr modName, String idName) {
      return new IdKey(modName, idName);
    }

    IdKey(Cstr modName, String idName) {
      /* DEBUG */ if (modName == null) { throw new IllegalArgumentException("Mod name is null. " + idName); }
      /* DEBUG */ if (idName == null) { throw new IllegalArgumentException("Name is null. " + modName.repr()); }
      /* DEBUG */ if (idName.length() == 0) { throw new IllegalArgumentException("Name is empty. " + modName.repr()); }
      this.modName = modName;
      this.idName = idName;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("idkey[mod=");
      buf.append(this.modName.repr());
      buf.append(",id=");
      buf.append(this.idName);
      buf.append("]");
      return buf.toString();
    }

    String repr() {
      StringBuffer buf = new StringBuffer();
      buf.append(this.modName.repr());
      buf.append(".");
      buf.append(this.idName);
      return buf.toString();
    }

    public int hashCode() {
      return this.modName.hashCode() ^ this.idName.hashCode();
    }

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (!(o instanceof IdKey)) {
        b = false;
      } else {
        IdKey ik = (IdKey)o;
        b = ik.modName.equals(this.modName) && ik.idName.equals(this.idName); 
      }
      return b;
    }
  }

  static class TidProps {
    IdKey key;
    int cat;
    Module.Access acc;

    static TidProps create(IdKey tid, int cat, Module.Access acc) {
      return new TidProps(tid, cat, acc);
    }

    private TidProps(IdKey tid, int cat, Module.Access acc) {
      this.key = tid;
      this.cat = cat;
      this.acc = acc;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tidprops[key=");
      buf.append(this.key);
      buf.append(",cat=");
      buf.append(this.cat);
      // buf.append(",avial=");
      // buf.append(this.avail);
      buf.append(",acc=");
      buf.append(this.acc);
      buf.append("]");
      return buf.toString();
    }
  }

  static class EidProps {
    IdKey key;
    int cat;
    Module.Access acc;
    Module.Access acc2;  // used only for function official name

    static EidProps create(IdKey eid, int cat, Module.Access acc, Module.Access acc2) {
      return new EidProps(eid, cat, acc, acc2);
    }

    private EidProps(IdKey eid, int cat, Module.Access acc, Module.Access acc2) {
      this.key = eid;
      this.cat = cat;
      this.acc = acc;
      this.acc2 = acc2;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("eidprops[key=");
      buf.append(this.key);
      buf.append(",cat=");
      buf.append(this.cat);
      // buf.append(",avial=");
      // buf.append(this.avail);
      buf.append(",acc=");
      buf.append(this.acc);
      buf.append(",acc=2");
      buf.append(this.acc2);
      buf.append("]");
      return buf.toString();
    }
  }

  static class TparamProps {
    public Module.Variance variance;  // public for impl of sango.lang.module
    boolean concrete;

    static TparamProps create(Module.Variance variance, boolean concrete) {
      return new TparamProps(variance, concrete);
    }

    private TparamProps(Module.Variance variance, boolean concrete) {
      this.variance = variance;
      this.concrete = concrete;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tparamprops[variance=");
      buf.append(this.variance);
      buf.append(",concrete=");
      buf.append(this.concrete);
      buf.append("]");
      return buf.toString();
    }
  }

  static class FunSelRes {
    PFunDef funDef;
    PTypeSkelBindings bindings;

    static FunSelRes create(PFunDef funDef, PTypeSkelBindings bindings) {
      return new FunSelRes(funDef, bindings);
    }

    private FunSelRes(PFunDef funDef, PTypeSkelBindings bindings) {
      this.funDef = funDef;
      this.bindings = bindings;
    }
  }
}
