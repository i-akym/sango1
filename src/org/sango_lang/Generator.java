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
  Parser parser;
  Cstr modName;
  File modFile;
  Module.Builder modBuilder;
  boolean stop;

  Generator(Compiler theCompiler, Compiler.CompileEntry ce) {
    this.theCompiler = theCompiler;
    this.parser = theCompiler.parserDict.get(ce.modName);
    this.modName = ce.modName;
    this.modFile = ce.modFile;
    this.modBuilder = Module.newBuilder();
  }

  void generate() throws IOException, TransformerException {
    this.prepare();
    this.generateModInfo();
    this.generateForeignInfo();
    if (this.stop) { return; }
    this.generateDataDefs();
    this.generateAliasTypeDefs();
    this.generateFeatureDefs();
    this.generateFunDefs();
    this.generateFunImpls();

    if (this.theCompiler.verboseModule) {
      this.theCompiler.msgOut.print("Generating ");
      this.theCompiler.msgOut.print(this.parser.modName.repr());
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
    PModule m = this.parser.mod;
    for (int i = 0; i < m.aliasTypeStmtList.size(); i++) {
      PAliasTypeStmt a = m.aliasTypeStmtList.get(i);
      if (a.acc != Module.ACC_PRIVATE) {
        a.getBody();  // finish all bodies' setup
      }
    }
  }

  void generateModInfo() {
    this.modBuilder.setName(this.parser.mod.definedName);
    this.modBuilder.setAvailability(this.parser.mod.availability);
    this.modBuilder.setSlotCount((this.parser.mod.isInitFunDefined())? 2: 1);
  }

  void generateForeignInfo() {
    if (this.parser.mod.isLang()) {
      ;
    } else {
      Cstr[] foreignMods = this.parser.mod.getForeignMods();
      for (int i = 0; i < foreignMods.length; i++) {
        this.modBuilder.startForeignMod(foreignMods[i]); 
        this.generateForeignRefsIn(foreignMods[i]);
        this.modBuilder.endForeignMod(); 
      }
    }
  }

  void generateForeignRefsIn(Cstr modName) {
    PDataDef[] dds = this.parser.mod.foreignIdResolver.getReferredDataDefsIn(modName);
    for (int i = 0; i < dds.length; i++) {
      PDataDef dd = dds[i];
      try {
        this.theCompiler.handleTypeAvailability(
          this.modName, modName, dd.getFormalTcon(), dd.getAvailability());
        this.generateDataDefGeneric(dd);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
    PAliasTypeDef[] ads = this.parser.mod.foreignIdResolver.getReferredAliasTypeDefsIn(modName);
    for (int i = 0; i < ads.length; i++) {
      PAliasTypeDef ad = ads[i];
      try {
        this.theCompiler.handleTypeAvailability(
          this.modName, modName, ad.getTcon(), ad.getAvailability());
        this.generateAliasTypeDefGeneric(ad);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
    PFeatureDef[] ftds = this.parser.mod.foreignIdResolver.getReferredFeatureDefsIn(modName);
    for (int i = 0; i < ftds.length; i++) {
      PFeatureDef ftd = ftds[i];
      try {
        this.theCompiler.handleTypeAvailability(
          this.modName, modName, ftd.getNameKey().idName, ftd.getAvailability());
        this.generateFeatureDef(ftd);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
    PFunDef[] fds = this.parser.mod.foreignIdResolver.getReferredFunDefsIn(modName);
    for (int i = 0; i < fds.length; i++) {
      PFunDef fd = fds[i];
      try {
        this.theCompiler.handleFunAvailability(
          this.modName, modName, fd.getOfficialName(), fd.getAvailability());
        this.generateFunDef(fd);
      } catch (CompileException ex) {
        this.stop = true;
      }
    }
  }

  void generateDataDefs() {
    for (int i = 0; i < this.parser.mod.dataStmtList.size(); i++) {
      this.generateDataDefGeneric(this.parser.mod.dataStmtList.get(i)) ;
      this.generateDataConstrImpls(this.parser.mod.dataStmtList.get(i)) ;
    }
    for (int i = 0; i < this.parser.mod.extendStmtList.size(); i++) {
      this.generateDataDefGeneric(this.parser.mod.extendStmtList.get(i)) ;
      this.generateDataConstrImpls(this.parser.mod.extendStmtList.get(i)) ;
    }
  }

  void generateDataDefGeneric(PDataDef dd) {
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
      this.modBuilder.startDataDef(dd.getFormalTcon(), dd.getAvailability(), dd.getAcc(),
        this.parser.mod.modNameToModRefIndex(btk.modName), btk.idName);
    } else {
      this.modBuilder.startDataDef(dd.getFormalTcon(), dd.getAvailability(), dd.getAcc());
    }
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    for (int i = 0; i < pvs.length; i++) {
      MTypeVar v = (MTypeVar)pvs[i].toMType(this.parser.mod, varSlotList);
      MTypeVar.DefWithVariance p = MTypeVar.DefWithVariance.create(dd.getParamVarianceAt(i), v);
      this.modBuilder.putDataDefParam(p);
    }
    for (int i = 0; i < dd.getConstrCount(); i++) {
      this.generateConstrDef(dd.getConstrAt(i), new ArrayList<PTypeVarSlot>(varSlotList));
    }
    for (int i = 0; i < dd.getFeatureImplCount(); i++) {
      this.generateFeatureImplDef(dd.getFeatureImplAt(i), new ArrayList<PTypeVarSlot>(varSlotList));
    }
    this.modBuilder.endDataDef();

  }

  void generateConstrDef(PDataDef.Constr constrDef, List<PTypeVarSlot> varSlotList) {
    this.modBuilder.startConstrDef(constrDef.getDcon());
    for (int i = 0; i < constrDef.getAttrCount(); i++) {
      this.generateAttrDef(constrDef.getAttrAt(i), varSlotList);
    }
    this.modBuilder.endConstrDef();
  }

  void generateAttrDef(PDataDef.Attr attrDef, List<PTypeVarSlot> varSlotList) {
    this.modBuilder.startAttrDef(attrDef.getName());
    this.modBuilder.setAttrType(attrDef.getFixedType().toMType(this.parser.mod, varSlotList));
    this.modBuilder.endAttrDef();
  }

  void generateFeatureImplDef(PDataDef.FeatureImpl featureImplDef, List<PTypeVarSlot> varSlotList) {
    this.modBuilder.addFeatureImplDef(
      this.parser.mod.modNameToModRefIndex(featureImplDef.getProviderModName()),
      featureImplDef.getProviderFunName(),
      featureImplDef.getGetter(),
      featureImplDef.getImpl().toMType(this.parser.mod, varSlotList));
  }

  void generateDataConstrImpls(PDataDef dd) {
    for (int i = 0; i < dd.getConstrCount(); i++) {
      PDataDef.Constr dc = dd.getConstrAt(i);
      this.modBuilder.putUniqueDataConstrLocal(dc.getDcon(), dc.getAttrCount(), dd.getFormalTcon(), dd.getParamCount());
    }
  }

  void generateAliasTypeDefs() {
    for (int i = 0; i < this.parser.mod.aliasTypeStmtList.size(); i++) {
      this.generateAliasTypeDefGeneric(this.parser.mod.aliasTypeStmtList.get(i));
    }
  }

  void generateAliasTypeDefGeneric(PAliasTypeDef alias) {
    PTypeVarSlot[] pvs = alias.getParamVarSlots();
    MAliasTypeDef atd = MAliasTypeDef.create(
      alias.getTcon(), alias.getAvailability(), alias.getAcc(), pvs.length);
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    for (int i = 0; i < pvs.length; i++) {
      varSlotList.add(pvs[i]);
    }
    atd.setBody(alias.getBody().toMType(this.parser.mod, varSlotList));
    this.modBuilder.putAliasTypeDef(atd);
  }

  void generateFeatureDefs() {
    for (int i = 0; i < this.parser.mod.featureStmtList.size(); i++) {
      this.generateFeatureDef(this.parser.mod.featureStmtList.get(i)) ;
    }
  }

  void generateFeatureDef(PFeatureStmt feature) {
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    MFeatureDef.Builder b = MFeatureDef.Builder.newInstance();
    b.setName(feature.sig.fname.name);
/* DEBUG */ if (feature.availability == null) { throw new RuntimeException("Null availability. " + feature.sig); }
    b.setAvailability(feature.availability);
    b.setAcc(feature.acc);
    b.setObjType((MTypeVar)feature.obj.toSkel().toMType(this.parser.mod, varSlotList));
    for (int i = 0; i < feature.sig.params.length; i++) {
      b.addParam((MTypeVar)feature.sig.params[i].toSkel().toMType(this.parser.mod, varSlotList));
    }
    b.setImplType((MTypeRef)feature.impl.toSkel().toMType(this.parser.mod, varSlotList));
    this.modBuilder.putFeatureDef(b.create());
  }

  void generateFeatureDef(PFeatureDef fd) {  // foreign
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    MFeatureDef.Builder b = MFeatureDef.Builder.newInstance();
    b.setName(fd.getNameKey().idName);
    b.setAvailability(fd.getAvailability());
    b.setAcc(fd.getAcc());
    b.setObjType((MTypeVar)fd.getObjType().toMType(this.parser.mod, varSlotList));
    PTypeVarSkel[] ps = fd.getParams();
    for (int i = 0; i < ps.length; i++) {
      b.addParam((MTypeVar)ps[i].toMType(this.parser.mod, varSlotList));
    }
    b.setImplType((MTypeRef)fd.getImplType().toMType(this.parser.mod, varSlotList));
    this.modBuilder.putFeatureDef(b.create());
  }

  void generateFunDefs() {
    for (int i = 0; i < this.parser.mod.evalStmtList.size(); i++) {
      this.generateFunDef(this.parser.mod.evalStmtList.get(i)) ;
    }
  }

  void generateFunDef(PEvalStmt eval) {
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
      b.addParamType(eval.params[i].nTypeSkel.toMType(this.parser.mod, varSlotList));
    }
    b.setRetType(eval.retDef.nTypeSkel.toMType(this.parser.mod, varSlotList));
    this.modBuilder.putFunDef(b.create());
  }

  void generateFunDef(PFunDef fd) {  // foreign
// /* DEBUG */ System.out.print("Gen FD "); System.out.println(fd);
    MFunDef.Builder b = MFunDef.Builder.newInstance();
    b.setName(fd.getOfficialName());
    b.setAcc(Module.ACC_PUBLIC);
    List<PTypeVarSlot> varSlotList = new ArrayList<PTypeVarSlot>();
    PTypeSkel[] pts = fd.getFixedParamTypes();
    for (int i = 0; i < pts.length; i++) {
// /* DEBUG */ System.out.print("param "); System.out.println(pts[i]);
      b.addParamType(pts[i].toMType(this.parser.mod, varSlotList));
    }
    b.setRetType(fd.getFixedRetType().toMType(this.parser.mod, varSlotList));
    this.modBuilder.putFunDef(b.create());
  }

  void generateFunImpls() {
    for (int i = 0; i < this.parser.mod.evalStmtList.size(); i++) {
      this.generateFunImpl(this.parser.mod.evalStmtList.get(i)) ;
    }
  }

  void generateFunImpl(PEvalStmt eval) {
    PExprVarSlot[] paramVarSlots = new PExprVarSlot[eval.params.length];
    for (int i = 0; i < eval.params.length; i++) {
      paramVarSlots[i] = eval.params[i].varSlot;
    }
    if (eval.implExprs != null) {
      GFlow flow = GFlow.create(this, eval.getSrcInfo(), eval.official, paramVarSlots, eval.getFixedParamTypes());
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
      GFlow flow = GFlow.create(this, eval.getSrcInfo(), eval.official, paramVarSlots, eval.getFixedParamTypes());
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
      GFlow flow = GFlow.create(this, eval.getSrcInfo(), eval.official, paramVarSlots, eval.getFixedParamTypes());
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
