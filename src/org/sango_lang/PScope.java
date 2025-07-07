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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PScope {
  static final int POS_MODULE = 0;
  static final int POS_FUN_BASE = 1;
  static final int POS_IMPORT_HEAD = -1;
  static final int POS_DATA_HEAD = -5;
  static final int POS_DATA_CONSTR = -6;
  static final int POS_DATA_FEATURE_IMPL = -7;
  static final int POS_ALIAS_TYPE_HEAD = -11;
  static final int POS_ALIAS_TYPE_BODY = -12;
  static final int POS_FEATURE_HEAD = -21;
  static final int POS_FEATURE_IMPL = -22;

  PModule theMod;
  PScope parent;
  PScope copyFrom;
  int pos;
  PEvalStmt evalStmt;  // set if pos == 1
  PClosure closure;  // set if pos > 1
  boolean canDefineTVar;
  boolean canDefineEVar;
  List<PScope> parallelScopes;
  boolean inParallel;
  Map<String, PTypeVarDef> tvarDict;
  Map<String, PExprVarDef> evarDict;
  Map<String, PTypeVarDef> outerTVarDict;
  Map<String, PExprVarDef> outerEVarDict;
  List<PTypeVarSlot> envTVarList;  // null when created
  List<PExprVarSlot> envEVarList;  // null when created
  List<PTypeVarSlot> givenTVarList;  // null when created

  private PScope(PModule theMod) {
    this.theMod = theMod;
    this.pos = POS_MODULE;
    this.tvarDict = new HashMap<String, PTypeVarDef>();
    this.evarDict = new HashMap<String, PExprVarDef>();
    this.outerTVarDict = new HashMap<String, PTypeVarDef>();
    this.outerEVarDict = new HashMap<String, PExprVarDef>();
  }

  static PScope create(PModule theMod) {
    return new PScope(theMod);
  }

  private PScope createForDelayedCopy() { 
    PScope s = new PScope(this.theMod);
    s.parent = this.parent;
    s.pos = this.pos;
    s.evalStmt = this.evalStmt;
    s.closure = this.closure;
    s.canDefineTVar = this.canDefineTVar;
    s.canDefineEVar = this.canDefineEVar;
    s.inParallel = this.inParallel;
    s.copyFrom = this;
    return s;
  }

  void doCopy() {
    if (this.copyFrom == null) {
      throw new IllegalStateException("Cannot copy scope data.");
    }
    this.tvarDict.putAll(this.copyFrom.tvarDict);
    this.evarDict.putAll(this.copyFrom.evarDict);
    this.outerTVarDict.putAll(this.copyFrom.outerTVarDict);
    this.outerEVarDict.putAll(this.copyFrom.outerEVarDict);
    if (this.copyFrom.envTVarList != null) {
      this.envTVarList = new ArrayList<PTypeVarSlot>(this.copyFrom.envTVarList);
    }
    if (this.copyFrom.envEVarList != null) {
      this.envEVarList = new ArrayList<PExprVarSlot>(this.copyFrom.envEVarList);
    }
    if (this.copyFrom.givenTVarList != null) {
      this.givenTVarList = new ArrayList<PTypeVarSlot>(this.copyFrom.givenTVarList);
    }
    this.copyFrom = null;
  }

  private PScope createInnerScope() {
    PScope s = new PScope(this.theMod);
    s.parent = this;
    s.pos = this.pos;
    s.evalStmt = this.evalStmt;
    s.closure = this.closure;
    return s;
  }

  PScope startImport() {
    if (this.pos != POS_MODULE) {
      throw new IllegalStateException("Cannot start definition.");
    }
    return this;
  }

  PScope startData() {
    if (this.pos != POS_MODULE) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_DATA_HEAD;
    s.canDefineTVar = true;
    s.canDefineEVar = false;
    return s;
  }

  PScope startDataConstr() {
    if (this.pos != POS_DATA_HEAD) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_DATA_CONSTR;
    s.canDefineTVar = false;
    s.canDefineEVar = false;
    return s;
  }

  PScope startDataFeatureImpl() {  // delayed copy
    if (this.pos != POS_DATA_HEAD) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createForDelayedCopy();
    s.pos = POS_DATA_FEATURE_IMPL;
    s.canDefineTVar = true;
    s.canDefineEVar = false;
    return s;
  }

  PScope startAliasType() {
    if (this.pos != POS_MODULE) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_ALIAS_TYPE_HEAD;
    s.canDefineTVar = true;
    s.canDefineEVar = false;
    return s;
  }

  PScope startAliasTypeBody() {
    if (this.pos != POS_ALIAS_TYPE_HEAD) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_ALIAS_TYPE_BODY;
    s.canDefineTVar = false;
    s.canDefineEVar = false;
    return s;
  }

  PScope startFeature() {
    if (this.pos != POS_MODULE) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_FEATURE_HEAD;
    s.canDefineTVar = true;
    s.canDefineEVar = false;
    return s;
  }

  PScope startFeatureImpl() {
    if (this.pos != POS_FEATURE_HEAD) {
      throw new IllegalStateException("Cannot start definition.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_FEATURE_IMPL;
    s.canDefineTVar = false;
    s.canDefineEVar = false;
    return s;
  }

  PScope prepareFun() {  // must call defineFun after me
    if (this.pos != POS_MODULE) {
      throw new IllegalStateException("Cannot define function.");
    }
    PScope s = this.createInnerScope();
    s.pos = POS_FUN_BASE;
    s.canDefineTVar = true;
    s.canDefineEVar = true;
    return s;
  }

  void defineFun(PEvalStmt eval) {
    if (this.pos != POS_FUN_BASE) {
      throw new IllegalStateException("Cannot define function.");
    }
    this.evalStmt = eval;
  }

  PScope prepareClosure() {  // must call defineClosure after me
    if (this.pos < 1) {
      throw new IllegalStateException("Cannot define closure.");
    }
    PScope s = this.createInnerScope();
    s.pos++;
    s.canDefineTVar = true;
    s.canDefineEVar = true;
    return s;
  }

  void defineClosure(PClosure closure) {
    if (this.pos <= POS_FUN_BASE) {
      throw new IllegalStateException("Cannot define function.");
    }
    this.closure = closure;
  }

  PScope startRetType() {  // delayed copy
    if (this.pos < POS_FUN_BASE) {
      throw new IllegalStateException("Cannot start ret type.");
    }
    if (this.pos == this.parent.pos) {
      throw new IllegalStateException("Cannot start ret type.");
    }
    PScope s = this.createForDelayedCopy();
    s.canDefineTVar = true;
    s.canDefineEVar = false;
    return s;
  }

  PScope startInnerScope() {
    if (this.pos < POS_FUN_BASE) {
      throw new IllegalStateException("Cannot start inner scope.");
    }
    PScope s = this.createInnerScope();
    s.canDefineTVar = true;
    s.canDefineEVar = true;
    return s;
  }

  void enableToStartParallelSubScopes() {
    if (this.pos < POS_FUN_BASE) {
      throw new IllegalStateException("Cannot start inner parallel scopes.");
    }
    this.parallelScopes = new ArrayList<PScope>();
  }

  PScope startPossiblyParallelSubScope() {  // for PCasePtnMatch
    if (this.parallelScopes == null) {
      throw new IllegalStateException("Cannot start parallel scopes.");
    }
    PScope s = this.createInnerScope();
    this.parallelScopes.add(s);
    s.inParallel = true;
    s.canDefineTVar = true;
    s.canDefineEVar = true;
    return s;
  }

  private boolean canDefineTVarHere() {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    boolean b;
    if (this.inParallel) {
      b = this.canDefineTVar & (this.parent.parallelScopes.size() <= 1);
    } else {
      b = this.canDefineTVar;
    }
    return b;
  }

  private boolean canDefineEVarHere() {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    boolean b;
    if (this.inParallel) {
      b = this.canDefineEVar & (this.parent.parallelScopes.size() <= 1);
    } else {
      b = this.canDefineEVar;
    }
    return b;
  }

  // PScope enterInnerWithParallelScopes() {  // for PCaseClause
    // PScope s = this.createInnerScope();
    // return s;
  // }

  // boolean isForParallel() {
    // return this.parallelScopes != null;
  // }

  // boolean isActuallyInParallelScope() {
    // return this.inParallel && this.parent.parallelScopes.size() > 1;
  // }

  PTypeVarDef lookupTVar(String var) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    if (this.pos == 0) {
      return null;
    }
    PTypeVarDef v = this.tvarDict.get(var);
    if (v == null) {
      v = this.outerTVarDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupTVar(var);
  }

  PExprVarDef lookupEVar(String var) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    if (this.pos < 1) {
      return null;
    }
    PExprVarDef v = this.evarDict.get(var);
    if (v == null) {
      v = this.outerEVarDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupEVar(var);
  }

  boolean canDefineTVar(PTypeVarDef varDef) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    return this.canDefineTVarHere()
      && !this.tvarDict.containsKey(varDef.name)
      && !this.evarDict.containsKey(varDef.name)
      && !this.outerTVarDict.containsKey(varDef.name)
      && !this.outerEVarDict.containsKey(varDef.name);
  }

  boolean canDefineEVar(PExprVarDef varDef) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    return this.canDefineEVarHere()
      && !this.tvarDict.containsKey(varDef.name)
      && !this.evarDict.containsKey(varDef.name)
      && !this.outerTVarDict.containsKey(varDef.name)
      && !this.outerEVarDict.containsKey(varDef.name);
  }

  PTypeVarSlot defineTVar(PTypeVarDef varDef) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    // check if defining var is available
    PTypeVarSlot slot;
    if (this.inParallel) {
      slot = this.parent.defineTVar(varDef);  // temporal impl - forward simply
    } else {
      slot = PTypeVarSlot.create(varDef);
      varDef._resolved_varSlot = slot;
      this.tvarDict.put(varDef.name, varDef);
    }
    return slot;
  }

  PExprVarSlot defineEVar(PExprVarDef varDef) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    // check if defining var is available
    PExprVarSlot slot;
    if (this.inParallel) {
      slot = this.parent.defineEVar(varDef);  // temporal impl - forward simply
    } else {
      slot = PExprVarSlot.create(varDef);
      varDef._resolved_varSlot = slot;
      this.evarDict.put(varDef.name, varDef);
    }
    return slot;
  }

  PTypeVarDef referSimpleTid(String id) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    if (this.pos == 0) {
      return null;
      // throw new IllegalStateException("Not active.");
    }
    PTypeVarDef v;
    if (this.inParallel) {
      v = this.parent.referSimpleTid(id);  // temporal impl - forward simply
    } else {
      v = this.tvarDict.get(id);
      if (v == null) {
        v = this.outerTVarDict.get(id);
        if (v == null && this.parent != null) {
          v = this.parent.referSimpleTid(id);
          if (v != null) {
            this.outerTVarDict.put(id, v);
            if (this.parent.pos != this.pos) {  // in top scope of closure
              this.envTVarList.add(v._resolved_varSlot);
            }
          }
        }
      }
    }
    return v;
  }

  PExprVarDef referSimpleEid(String id) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    if (this.pos < 1) {
      return null;
      // throw new IllegalStateException("Not active.");
    }
    PExprVarDef v;
    if (this.inParallel) {
      v = this.parent.referSimpleEid(id);  // temporal impl - forward simply
    } else {
      v = this.evarDict.get(id);
      if (v == null) {
        v = this.outerEVarDict.get(id);
        if (v == null && this.parent != null) {
          v = this.parent.referSimpleEid(id);
          if (v != null) {
            this.outerEVarDict.put(id, v);
            if (this.parent.pos != this.pos) {  // in top scope of closure
              this.envEVarList.add(v._resolved_varSlot);
            }
          }
        }
      }
    }
    return v;
  }

  List<PTypeVarSlot> getEnvTVarList() {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    return this.inParallel?
      this.parent.getEnvTVarList():  // temporal impl - forward simply
      this.envTVarList;
  }

  List<PExprVarSlot> getEnvEVarList() {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    return this.inParallel?
      this.parent.getEnvEVarList():  // temporal impl - forward simply
      this.envEVarList;
  }

  Compiler getCompiler() { return this.theMod.theCompiler; }

  Cstr myModName() {
    return this.theMod.actualName;
  }

  void referredModId(Parser.SrcInfo si, String id) throws CompileException {
    if (id != null) {
      if (this.theMod.resolveModId(id) == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Unknown module id \"");
        emsg.append(id);
        emsg.append("\" at ");
        emsg.append(si);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (id.equals(PModule.MOD_ID_LANG) || id.equals(PModule.MOD_ID_HERE) ) {
        ;  // pass
      } else if (this.theMod.myId != null && id.equals(this.theMod.myId)) {
        ;  // pass
      } else if (this.theMod.referredModIds.contains(id)) {
        ;  // already added
      } else {
        this.theMod.referredModIds.add(id);
      }
    }
  }

  Cstr resolveModId(String id) {
    return this.theMod.resolveModId(id);
  }

  PDefDict.TidProps resolveTcon(PTid tcon) throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    PDefDict.TidProps tp;
    if (tcon.maybeVar() && this.lookupTVar(tcon.name) != null) {
      tp = PDefDict.TidProps.create(
        PDefDict.IdKey.create(new Cstr(""), tcon.name),
        PDefDict.TID_CAT_VAR, Module.ACC_PRIVATE);
    } else {
      tcon.cutOffVar();
      tp = this.theMod.resolveTcon(tcon);
    }
    return tp;
  }

  PDefDict.TidProps resolveFeature(PTid fname) throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    return this.theMod.resolveFeature(fname);
  }

  PDefDict.EidProps resolveEid(PEid id) throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    PDefDict.EidProps ep;
    if (id.maybeVar() && this.lookupEVar(id.name) != null) {
      ep = PDefDict.EidProps.create(
        PDefDict.IdKey.create(new Cstr(""), id.name),
        // PDefDict.IdKey.create(this.theMod.resolveModId(id.modId), id.name),
        PDefDict.EID_CAT_VAR, Module.ACC_PRIVATE, null);
    } else {
      id.cutOffVar();
      ep = this.theMod.resolveAnchor(id);
    }
    return ep;
  }

  // void addReferredTcons(List<PDefDict.TidProps> tis) {
    // for (int i = 0; i < tis.size(); i++) {
      // this.addReferredTcon(tis.get(i));
    // }
  // }

  // void addReferredTcon(PDefDict.TidProps ti) {
    // this.theMod.addReferredTcon(ti);
  // }

  PTypeRef getLangDefinedType(Parser.SrcInfo srcInfo, String tcon, PType[] paramTypeDescs) {
    PTypeRef t = null;
    try {
      t = PTypeRef.getLangDefinedType(srcInfo, this, tcon, paramTypeDescs);
      t.scope = this;  // needed for resolveId only
      t = t.resolve();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return t;
  }

  PTypeRef getLangPrimitiveType(Parser.SrcInfo srcInfo, String tcon) {
    return this.getLangDefinedType(srcInfo, tcon, new PType[0]);
  }

  PTypeVarDef getNewTVar(Parser.SrcInfo srcInfo) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    PTypeVarDef v = PTypeVarDef.create(srcInfo, this, this.generateId(), false, /* null, */ null);
    return v;
  }

  PTypeSkel getEmptyListType(Parser.SrcInfo srcInfo) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    PTypeVarDef nv = this.getNewTVar(srcInfo);
    return this.getLangDefinedType(srcInfo, "list", new PType[] { nv }).toSkel();
  }

  PTypeSkel getEmptyStringType(Parser.SrcInfo srcInfo) {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    PTypeVarDef nv = this.getNewTVar(srcInfo);
    return this.getLangDefinedType(srcInfo, "string", new PType[] { nv }).toSkel();
  }

  PTypeRef getCharStringType(Parser.SrcInfo srcInfo) {
    PType.Builder b = PType.Builder.newInstance(srcInfo, this);
    PType ct;
    try {
      b.addItem(PTid.create(srcInfo, this, PModule.MOD_ID_LANG, "char", false));
      ct = b.create();
      ct = ct.resolve();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return this.getLangDefinedType(srcInfo, "string", new PType[] { ct });
  }

  PTypeRefSkel getLangDefinedTypeSkel(Parser.SrcInfo srcInfo, String tcon, PTypeSkel[] paramTypeSkels) throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    PDefDict.IdKey k = PDefDict.IdKey.create(Module.MOD_LANG, tcon);
    PDefDict.TidProps tp = this.theMod.theCompiler.defDict.resolveTcon(this.theMod.getName(), k);
    if (tp == null) {
      throw new RuntimeException("Internal error.");
    }
    return PTypeRefSkel.create(this.theMod.theCompiler, srcInfo, k, false, paramTypeSkels);
  }

  List<PTypeVarSlot> getGivenTVarList() throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    if (this.givenTVarList == null) {
      if (this.parent == null || this.parent.pos < 1) {  // function top pos
        this.setupGivenTVarListForFun();
      } else if (this.parent.pos != this.pos) {
        this.setupGivenTVarListForClosure();
      } else {
        this.givenTVarList = this.parent.getGivenTVarList();
      }
    }
    return this.givenTVarList;
  }

  private void setupGivenTVarListForFun() throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    this.givenTVarList = new ArrayList<PTypeVarSlot>();
    PTypeSkel[] pts = this.evalStmt.getParamTypes();
    for (int i = 0; i < pts.length; i++) {
      pts[i].extractVars(this.givenTVarList);
    }
  }

  private void setupGivenTVarListForClosure() throws CompileException {
    if (this.copyFrom != null) {
      throw new IllegalStateException("Not copied yet.");
    }
    this.givenTVarList = new ArrayList<PTypeVarSlot>();
    this.givenTVarList.addAll(this.parent.getGivenTVarList());
    PTypeSkel[] pts = this.closure.getParamDefinedTypes();
    for (int i = 0; i < pts.length; i++) {
      pts[i].extractVars(this.givenTVarList);
    }
  }

  String getFunOfficial() {
    if (this.pos < 1) {
      throw new IllegalStateException("Out of function.");
    }
    return (this.pos == 1)? this.evalStmt.official: this.parent.getFunOfficial();
  }

  String generateId() {
    return this.theMod.generateId();
  }
}
