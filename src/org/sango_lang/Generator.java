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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

class Generator {
  Compiler theCompiler;
  PModule theMod;
  File modFile;
  Module.Builder modBuilder;
  boolean stop;

  Generator(Compiler theCompiler, Compiler.CompileEntry ce) {
    this.theCompiler = theCompiler;
    this.theMod = ce.parser.mod;
    this.modFile = ce.modFile;
    this.modBuilder = Module.newBuilder(ce.actualModName);
  }

  void generate() throws IOException, TransformerException {
    this.prepare();
    this.generateModInfo();
    this.generateForeignInfo();
    if (this.stop) { return; }
    try {
      this.generateDataDefs(false);
      this.generateAliasTypeDefs(false);
      this.generateFeatureDefs(false);
      this.generateFunDefs(false);
      this.generateFunImpls(false);
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpteced exception. " + ex.toString());
    }

    if (this.theCompiler.verboseModule) {
      this.theCompiler.msgOut.print("Generating ");
      this.theCompiler.msgOut.print(this.theMod.actualName.repr());
      this.theCompiler.msgOut.print(" = ");
      this.theCompiler.msgOut.print(this.modFile.getAbsolutePath());
      this.theCompiler.msgOut.println(" ... ");
    }
    FileSystem.getInstance().prepareForFileCreation(this.modFile);
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(this.modFile));
    zos.putNextEntry(new ZipEntry(FileSystem.MODULE_ZIP_ENTRY));
    this.modBuilder.create().writeTo(new StreamResult(zos));
    zos.closeEntry();
    zos.close();
  }

  void prepare() {
    Cstr[] foreignMods = this.theMod.getForeignMods();
    for (int i = 0; i < foreignMods.length; i++) {
      this.modBuilder.reserveForeignMod(foreignMods[i]); 
    }
  }

  void generateModInfo() {
    this.modBuilder.setDefinedName(this.theMod.definedName);
    this.modBuilder.setAvailability(this.theMod.availability);
    this.modBuilder.setSlotCount((this.theMod.isInitFunDefined())? 2: 1);
  }

  void generateForeignInfo() {
    if (this.theMod.isLang()) {
      ;
    } else {
      Cstr n;
      while ((n = this.modBuilder.startReservedForeignMod()) != null) {
        this.generateForeignRefsIn(n);
        this.modBuilder.endForeignMod(); 
      }

      // for (int i = 0; i < foreignMods.length; i++) {
        // this.modBuilder.startForeignMod(foreignMods[i]); 
        // this.generateForeignRefsIn(foreignMods[i]);
        // this.modBuilder.endForeignMod(); 
      // }
      // Cstr[] foreignMods2 = this.theMod.getForeignMods2();
      // for (int i = 0; i < foreignMods2.length; i++) {
        // this.modBuilder.addIndirectForeignMod(foreignMods2[i]); 
      // }
    }
  }

  void generateForeignRefsIn(Cstr modName) {
    List<PDataDef> dds = this.theCompiler.defDict.getForeignDataDefsIn(this.theMod.actualName, modName);
    for (int i = 0; i < dds.size(); i++) {
      PDataDef dd = dds.get(i);
      try {
        this.theCompiler.handleTypeAvailability(
          this.theMod.actualName, modName, dd.getFormalTcon(), dd.getAvailability());
        this.generateDataDefGeneric(true, dd);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
    List<PAliasTypeDef> ads = this.theCompiler.defDict.getForeignAliasTypeDefsIn(this.theMod.actualName, modName);
    for (int i = 0; i < ads.size(); i++) {
      PAliasTypeDef ad = ads.get(i);
      try {
/* TRAP */ if (ad.getTcon() == null) { throw new RuntimeException("Null tcon. " + ad + this.theMod.actualName.toJavaString()); }
        this.theCompiler.handleTypeAvailability(
          this.theMod.actualName, modName, ad.getTcon(), ad.getAvailability());
        this.generateAliasTypeDefGeneric(true, ad);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
    List<PFeatureDef> ftds = this.theCompiler.defDict.getForeignFeatureDefsIn(this.theMod.actualName, modName);
    for (int i = 0; i < ftds.size(); i++) {
      PFeatureDef ftd = ftds.get(i);
      try {
        this.theCompiler.handleTypeAvailability(
          this.theMod.actualName, modName, ftd.getNameKey().idName, ftd.getAvailability());
        this.generateFeatureDef(true, ftd);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
    List<PFunDef> fds = this.theCompiler.defDict.getForeignFunDefsIn(this.theMod.actualName, modName);
    for (int i = 0; i < fds.size(); i++) {
      PFunDef fd = fds.get(i);
      try {
        this.theCompiler.handleFunAvailability(
          this.theMod.actualName, modName, fd.getOfficialName(), fd.getAvailability());
        this.generateFunDef(true, fd);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
  }

  void generateDataDefs(boolean inReferredDef) throws CompileException {
    for (int i = 0; i < this.theMod.dataStmtList.size(); i++) {
      this.generateDataDefGeneric(inReferredDef, this.theMod.dataStmtList.get(i)) ;
      this.generateDataConstrImpls(inReferredDef, this.theMod.dataStmtList.get(i)) ;
    }
    for (int i = 0; i < this.theMod.extendStmtList.size(); i++) {
      this.generateDataDefGeneric(inReferredDef, this.theMod.extendStmtList.get(i)) ;
      this.generateDataConstrImpls(inReferredDef, this.theMod.extendStmtList.get(i)) ;
    }
  }

  void generateDataDefGeneric(boolean inReferredDef, PDataDef dd) throws CompileException {
    Module.Access acc = dd.getAcc();
    PTypeSkel sig = dd.getTypeSig();
    if (sig == null) { return; }  // fun, tuple?
    PTypeVarSkel[] pvs;
    if (sig instanceof PTypeRefSkel) {
      PTypeRefSkel str = (PTypeRefSkel)sig;
      if (str.params == null) { return; }  // fun, tuple?
      pvs = new PTypeVarSkel[str.params.length];
      for (int i = 0; i < pvs.length; i++) {
        pvs[i] = (PTypeVarSkel)str.params[i];
      }
    } else {
      throw new RuntimeException("Unknown type sig. " + sig.toString());
    }
    PDefDict.IdKey btk = dd.getBaseTconKey();
    if (btk != null) {
      this.modBuilder.startDataDef(dd.getFormalTcon(), dd.getAvailability(), acc,
        this.modBuilder.modNameToModIndex(btk.modName), btk.idName);
        // this.theMod.modNameToModRefIndex(inReferredDef, btk.modName), btk.idName);
    } else {
      this.modBuilder.startDataDef(dd.getFormalTcon(), dd.getAvailability(), acc);
    }
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    PDefDict.TparamProps[] tps = dd.getParamPropss();
    for (int i = 0; i < pvs.length; i++) {
      MTypeVar v = (MTypeVar)pvs[i].toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList);
      MType.ParamDef p = MType.ParamDef.create(tps[i].variance, v);
      this.modBuilder.putDataDefParam(p);
    }
    if (!inReferredDef || acc == Module.ACC_PUBLIC || acc == Module.ACC_PROTECTED) {
      for (int i = 0; i < dd.getConstrCount(); i++) {
        this.generateConstrDef(inReferredDef, dd.getConstrAt(i), new ArrayList<PTypeVarSlot>(varSlotList));
      }
    }
    for (int i = 0; i < dd.getFeatureImplCount(); i++) {
      this.generateFeatureImplDef(inReferredDef, dd.getFeatureImplAt(i), new ArrayList<PTypeVarSlot>(varSlotList));
    }
    this.modBuilder.endDataDef();
  }

  void generateConstrDef(boolean inReferredDef, PDataDef.Constr constrDef, List<PTypeVarSlot> varSlotList) {
    this.modBuilder.startConstrDef(constrDef.getDcon());
    for (int i = 0; i < constrDef.getAttrCount(); i++) {
      this.generateAttrDef(inReferredDef, constrDef.getAttrAt(i), varSlotList);
    }
    this.modBuilder.endConstrDef();
  }

  void generateAttrDef(boolean inReferredDef, PDataDef.Attr attrDef, List<PTypeVarSlot> varSlotList) {
    this.modBuilder.startAttrDef(attrDef.getName());
    this.modBuilder.setAttrType(attrDef.getFinalizedType().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    this.modBuilder.endAttrDef();
  }

  void generateFeatureImplDef(boolean inReferredDef, PDataDef.FeatureImpl featureImplDef, List<PTypeVarSlot> varSlotList) throws CompileException {
    this.modBuilder.addFeatureImplDef(
      this.modBuilder.modNameToModIndex(featureImplDef.getProviderModName()),
      // this.theMod.modNameToModRefIndex(inReferredDef, featureImplDef.getProviderModName()),
      featureImplDef.getProviderFunName(),
      featureImplDef.getGetter(),
      featureImplDef.getImpl().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
  }

  void generateDataConstrImpls(boolean inReferredDef, PDataDef dd) {
    for (int i = 0; i < dd.getConstrCount(); i++) {
      PDataDef.Constr dc = dd.getConstrAt(i);
      this.modBuilder.putUniqueDataConstrLocal(dc.getDcon(), dc.getAttrCount(), dd.getFormalTcon(), dd.getParamPropss().length);
    }
  }

  void generateAliasTypeDefs(boolean inReferredDef) {
    for (int i = 0; i < this.theMod.aliasTypeStmtList.size(); i++) {
      this.generateAliasTypeDefGeneric(inReferredDef, this.theMod.aliasTypeStmtList.get(i));
    }
  }

  void generateAliasTypeDefGeneric(boolean inReferredDef, PAliasTypeDef alias) {
    PTypeVarSlot[] pvs = alias.getParamVarSlots();
    MAliasTypeDef atd = MAliasTypeDef.create(
      alias.getTcon(), alias.getAvailability(), alias.getAcc(), pvs.length);
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    for (int i = 0; i < pvs.length; i++) {
      varSlotList.add(pvs[i]);
    }
    atd.setBody(alias.getBody().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    this.modBuilder.putAliasTypeDef(atd);
  }

  void generateFeatureDefs(boolean inReferredDef) {
    for (int i = 0; i < this.theMod.featureStmtList.size(); i++) {
      this.generateFeatureDef(inReferredDef, this.theMod.featureStmtList.get(i)) ;
    }
  }

  void generateFeatureDef(boolean inReferredDef, PFeatureStmt feature) {
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    MFeatureDef.Builder b = MFeatureDef.Builder.newInstance();
    b.setName(feature.fname);
/* DEBUG */ if (feature.availability == null) { throw new RuntimeException("Null availability. " + feature.fname); }
    b.setAvailability(feature.availability);
    b.setAcc(feature.acc);
    b.setObjType((MTypeVar)feature.obj.toSkel().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    for (int i = 0; i < feature.params.length; i++) {
      MTypeVar v = (MTypeVar)feature.params[i].getTypeSkel().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList);
      MType.ParamDef d = MType.ParamDef.create(feature.params[i].variance, v);
      b.addParam(d);
    }
    b.setImplType((MTypeRef)feature.impl.toSkel().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    this.modBuilder.putFeatureDef(b.create());
  }

  void generateFeatureDef(boolean inReferredDef, PFeatureDef fd) throws CompileException {  // foreign
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    MFeatureDef.Builder b = MFeatureDef.Builder.newInstance();
    b.setName(fd.getNameKey().idName);
    b.setAvailability(fd.getAvailability());
    b.setAcc(fd.getAcc());
    b.setObjType((MTypeVar)fd.getObjType().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    PFeatureSkel sig = fd.getFeatureSig();
    PDefDict.TparamProps[] pps = fd.getParamPropss();
    for (int i = 0; i < sig.params.length; i++) {
      MTypeVar v = (MTypeVar)sig.params[i].toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList);
      MType.ParamDef d = MType.ParamDef.create(pps[i].variance, v);
      b.addParam(d);
    }
    b.setImplType((MTypeRef)fd.getImplType().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    this.modBuilder.putFeatureDef(b.create());
  }

  void generateFunDefs(boolean inReferredDef) {
    for (int i = 0; i < this.theMod.evalStmtList.size(); i++) {
      this.generateFunDef(inReferredDef, this.theMod.evalStmtList.get(i)) ;
    }
  }

  void generateFunDef(boolean inReferredDef, PEvalStmt eval) {
    MFunDef.Builder b = MFunDef.Builder.newInstance();
    b.setName(eval.official);
/* DEBUG */ if (eval.availability == null) { throw new RuntimeException("Null availability. " + eval.official); }
    b.setAvailability(eval.availability);
    b.setAcc(eval.acc);
    for (int i = 0; i < eval.aliases.length; i++) {
      b.addAlias(eval.aliases[i]);
    }
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    for (int i = 0; i < eval.params.length; i++) {
      b.addParamType(eval.params[i].getNormalizedType().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    }
    b.setRetType(eval.retDef.getNormalizedType().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    this.modBuilder.putFunDef(b.create());
  }

  void generateFunDef(boolean inReferredDef, PFunDef fd) {  // foreign
// /* DEBUG */ System.out.print("Gen FD "); System.out.println(fd);
    MFunDef.Builder b = MFunDef.Builder.newInstance();
    b.setName(fd.getOfficialName());
    b.setAcc(Module.ACC_PUBLIC);
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    PTypeSkel[] pts = fd.getFinalizedParamTypes();
    for (int i = 0; i < pts.length; i++) {
// /* DEBUG */ System.out.print("param "); System.out.println(pts[i]);
      b.addParamType(pts[i].toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    }
    b.setRetType(fd.getFinalizedRetType().toMType(this.theMod, this.modBuilder, inReferredDef, varSlotList));
    this.modBuilder.putFunDef(b.create());
  }

  void generateFunImpls(boolean inReferredDef) {
    for (int i = 0; i < this.theMod.evalStmtList.size(); i++) {
      this.generateFunImpl(inReferredDef, this.theMod.evalStmtList.get(i)) ;
    }
  }

  void generateFunImpl(boolean inReferredDef, PEvalStmt eval) {
    PExprVarSlot[] paramVarSlots = new PExprVarSlot[eval.params.length];
    for (int i = 0; i < eval.params.length; i++) {
      paramVarSlots[i] = eval.params[i]._resolved_varSlot;
    }
    if (eval.implExprs != null) {
      GFlow flow = GFlow.create(this, eval.getSrcInfo(), eval.official, paramVarSlots, eval.getFinalizedParamTypes());
      GFlow.RootNode root = flow.getTopRoot();
      for (int i = 0; i < eval.implExprs.exprs.length -1; i++) {
        root.addChild(eval.implExprs.exprs[i].setupFlow(flow));
        root.addChild(flow.createSinkNode(eval.implExprs.exprs[i].srcInfo));
      }
      PExpr last = eval.implExprs.exprs[eval.implExprs.exprs.length - 1];
      root.addChild(last.setupFlow(flow));
      flow.prepareAll();
      List<GFlow.RootNode> rootList = flow.getRootList();
      for (int i = 0; i < rootList.size(); i++) {
        GFlow.RootNode n = rootList.get(i);
        n.generate(this.modBuilder);
        this.modBuilder.startClosureImpl(n.name, n.paramVarSlots.length /* , n.actualParamVarSlots.length */);
        this.modBuilder.startClosureImplVMCode(n.allocMapSize());
        CodeBlock cb = CodeBlock.create(n.codeChunk);
        for (int j = 0; j < cb.getCodeCount(); j++) {
          Code c = cb.getCodeAt(j);
          int[] params = new int[c.params.length];
          for (int k = 0; k < params.length; k++) {
            params[k] = c.params[k].intValue();
          }
          this.modBuilder.putInstruction(c.op, params);
        }
        for (int j = 0; j < cb.getSrcInfoCount(); j++) {
          SrcInfoEntry si = cb.getSrcInfoAt(j);
          this.modBuilder.putSrcInfo(si.codeIndex, si.srcInfo.loc);
        }
        this.modBuilder.endClosureImplVMCode();
        this.modBuilder.endClosureImpl();
      }
    } else if (eval.official.equals(Module.FUN_NAME)) {
      GFlow flow = GFlow.create(this, eval.getSrcInfo(), eval.official, paramVarSlots, eval.getFinalizedParamTypes());
      GFlow.RootNode root = flow.getTopRoot();
      root.addChild(flow.createNameImplNode(eval.getSrcInfo()));
      flow.prepareAll();
      root.generate(this.modBuilder);
      this.modBuilder.startClosureImpl(root.name, root.paramVarSlots.length /* , root.actualParamVarSlots.length */);
      this.modBuilder.startClosureImplVMCode(root.allocMapSize());
      CodeBlock cb = CodeBlock.create(root.codeChunk);
      for (int i = 0; i < cb.getCodeCount(); i++) {
        Code c = cb.getCodeAt(i);
        int[] params = new int[c.params.length];
        for (int j = 0; j < params.length; j++) {
          params[j] = c.params[j].intValue();
        }
        this.modBuilder.putInstruction(c.op, params);
      }
      for (int i = 0; i < cb.getSrcInfoCount(); i++) {
        SrcInfoEntry si = cb.getSrcInfoAt(i);
        this.modBuilder.putSrcInfo(si.codeIndex, si.srcInfo.loc);
      }
      this.modBuilder.endClosureImplVMCode();
      this.modBuilder.endClosureImpl();
    } else if (eval.official.equals(Module.FUN_INITD)) {
      GFlow flow = GFlow.create(this, eval.getSrcInfo(), eval.official, paramVarSlots, eval.getFinalizedParamTypes());
      GFlow.RootNode root = flow.getTopRoot();
      root.addChild(flow.createInitdImplNode(eval.getSrcInfo()));
      flow.prepareAll();
      root.generate(this.modBuilder);
      this.modBuilder.startClosureImpl(root.name, root.paramVarSlots.length /* , root.actualParamVarSlots.length */);
      this.modBuilder.startClosureImplVMCode(root.allocMapSize());
      CodeBlock cb = CodeBlock.create(root.codeChunk);
      for (int i = 0; i < cb.getCodeCount(); i++) {
        Code c = cb.getCodeAt(i);
        int[] params = new int[c.params.length];
        for (int j = 0; j < params.length; j++) {
          params[j] = c.params[j].intValue();
        }
        this.modBuilder.putInstruction(c.op, params);
      }
      for (int i = 0; i < cb.getSrcInfoCount(); i++) {
        SrcInfoEntry si = cb.getSrcInfoAt(i);
        this.modBuilder.putSrcInfo(si.codeIndex, si.srcInfo.loc);
      }
      this.modBuilder.endClosureImplVMCode();
      this.modBuilder.endClosureImpl();
    } else {
      this.modBuilder.startClosureImpl(eval.official, paramVarSlots.length /* , actualParamVarSlotList.size() */);
      this.modBuilder.endClosureImpl();
    }
  }

  abstract static class CodeChunk {
    Parser.SrcInfo srcInfo;
    CodeChunk parent;
    int entryAddr;
    int exitAddr;

    CodeChunk(Parser.SrcInfo srcInfo) {
      this.srcInfo = srcInfo;
    }

    Parser.SrcInfo getSrcInfo() {
      return (this.srcInfo != null)? this.srcInfo: parent.getSrcInfo();
    }

    abstract void addChild(CodeChunk cc);
    abstract void appendTo(CodeBlock cb);
  }

  static class CodeSeq extends CodeChunk {
    List<CodeChunk> children;

    CodeSeq(Parser.SrcInfo srcInfo) {  // srcInfo: must be non-null fot top-level
      super(srcInfo);
      this.children = new ArrayList<CodeChunk>();
    }

    static CodeSeq create(Parser.SrcInfo srcInfo) {
      return new CodeSeq(srcInfo);
    }

    static CodeSeq create() {
      return new CodeSeq(null);
    }

    void addChild(CodeChunk cc) {
      if (cc.parent != null) { throw new IllegalArgumentException("Already child of some CodeSeq."); }
      cc.parent = this;
      this.children.add(cc);
    }

    void appendTo(CodeBlock cb) {
      this.entryAddr = cb.getNextAddr();
      for (int i = 0; i < this.children.size(); i++) {
        this.children.get(i).appendTo(cb);
      }
      this.exitAddr = cb.getNextAddr();
    }
  }

  static class Code extends CodeChunk {
    GFlow.RootNode frameRoot;
    String op;
    OpParam[] params;
    int addr;

    Code() { super(null); }

    void addParam(OpParam p) {
      OpParam[] pp = new OpParam[this.params.length + 1];
      System.arraycopy(this.params, 0, pp, 0, this.params.length);
      pp[pp.length - 1] = p;
      this.params = pp;
    }

    void addChild(CodeChunk cc) {
      throw new RuntimeException("Cannot add child.");
    }

    void appendTo(CodeBlock cb) {
      this.entryAddr = cb.getNextAddr();
      cb.addCode(this);
      this.exitAddr = cb.getNextAddr();
    }

    RelAddrParam createRelAddrParamEntryOf(CodeChunk cc) {
      return this.createRelAddrParam(ImmediateCodeChunkGetter.create(cc), RelAddrParam.ENTRY_ADDR);
    }

    RelAddrParam createRelAddrParamExitOf(CodeChunk cc) {
      return this.createRelAddrParam(ImmediateCodeChunkGetter.create(cc), RelAddrParam.EXIT_ADDR);
    }

    RelAddrParam createRelAddrParamEntryOf(GFlow.Node node) {
      return this.createRelAddrParam(NodeCodeChunkGetter.create(node), RelAddrParam.ENTRY_ADDR);
    }

    RelAddrParam createRelAddrParamExitOf(GFlow.Node node) {
      return this.createRelAddrParam(NodeCodeChunkGetter.create(node), RelAddrParam.EXIT_ADDR);
    }

    private RelAddrParam createRelAddrParam(CodeChunkGetter ccg, int which) {
      RelAddrParam p = new RelAddrParam();
      p.op = this;
      p.ccg = ccg;
      p.which = which;
      // this.frameRoot.relAddrParamList.add(p);
      return p;
    }
  }

  abstract static class OpParam {
    abstract int intValue();
  }

  IntParam createIntParam(int value) {
    IntParam i = new IntParam();
    i.value = value;
    return i;
  }

  class IntParam extends OpParam {
    int value;

    IntParam() {}

    int intValue() { return this.value; }
  }

  static class RelAddrParam extends OpParam {
    static final int ENTRY_ADDR = 0;
    static final int EXIT_ADDR = 1;

    Code op;
    CodeChunkGetter ccg;
    int which;

    RelAddrParam() {}

    int intValue() {
      CodeChunk cc = this.ccg.get();
      int target = (this.which == ENTRY_ADDR)? cc.entryAddr: cc.exitAddr;
      return target - this.op.addr - 1;
    }
  }

  static class CodeBlock {
    List<Code> codeSeq;
    List<SrcInfoEntry> srcInfoTab;
    SrcInfoEntry lastSrcInfoEntry;

    CodeBlock() {
      this.codeSeq = new ArrayList<Code>();
      this.srcInfoTab = new ArrayList<SrcInfoEntry>();
      this.lastSrcInfoEntry = new SrcInfoEntry(0, null);
    }

    static CodeBlock create(CodeChunk cc) {
      CodeBlock cb = new CodeBlock();
      cc.appendTo(cb);
      return cb;
    }

    void addCode(Code c) {
      c.addr = this.codeSeq.size();
      this.codeSeq.add(c);
      Parser.SrcInfo si = c.getSrcInfo();
      if (si.equals(lastSrcInfoEntry.srcInfo)) {
        this.lastSrcInfoEntry.codeIndex = c.addr + 1;
      } else {
        this.lastSrcInfoEntry = new SrcInfoEntry(c.addr + 1, si);
        this.srcInfoTab.add(this.lastSrcInfoEntry);
      }
    }

    int getNextAddr() {
      return this.getCodeCount();
    }

    int getCodeCount() {
      return this.codeSeq.size();
    }

    Code getCodeAt(int i) {
      return this.codeSeq.get(i);
    }

    int getSrcInfoCount() {
      return this.srcInfoTab.size();
    }

    SrcInfoEntry getSrcInfoAt(int i) {
      return this.srcInfoTab.get(i);
    }
  }

  static abstract class CodeChunkGetter {
    abstract CodeChunk get();
  }

  static class ImmediateCodeChunkGetter extends CodeChunkGetter {
    CodeChunk codeChunk;

    static ImmediateCodeChunkGetter create(CodeChunk cc) {
      ImmediateCodeChunkGetter iccg = new ImmediateCodeChunkGetter();
      iccg.codeChunk = cc;
      return iccg;
    }

    ImmediateCodeChunkGetter() {}

    CodeChunk get() { return this.codeChunk; }
  }

  static class NodeCodeChunkGetter extends CodeChunkGetter {
    GFlow.Node node;

    static NodeCodeChunkGetter create(GFlow.Node node) {
      NodeCodeChunkGetter nccg = new NodeCodeChunkGetter();
      nccg.node = node;
      return nccg;
    }

    NodeCodeChunkGetter() {}

    CodeChunk get() { return this.node.codeChunk; }
  }

  static class SrcInfoEntry {
    int codeIndex;  // last code index + 1, because pointer is proceeded just after fetching code
    Parser.SrcInfo srcInfo;

    SrcInfoEntry(int codeIndex, Parser.SrcInfo srcInfo) {
      this.codeIndex = codeIndex;
      this.srcInfo = srcInfo;
    }
  }
}
