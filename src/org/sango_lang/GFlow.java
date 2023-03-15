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
import java.util.List;

class GFlow {
  // variable reference
  private static final int ENV_REF = 1;
  private static final int LOCAL_REF = 2;
  private static final int LOCAL_LAST_REF = 3;

  // context
  private static final int EVAL_CONTEXT = 1;
  private static final int MATCHING_CONTEXT = 2;

  Generator theGenerator;
  List<RootNode> rootList;

  static GFlow create(Generator g, Parser.SrcInfo srcInfo, String name, PExprVarSlot[] paramVarSlots, PTypeSkel[] paramTypes) {
    return new GFlow(g, srcInfo, name, paramVarSlots, paramTypes);
  }

  private GFlow(Generator g, Parser.SrcInfo srcInfo, String name, PExprVarSlot[] paramVarSlots, PTypeSkel[] paramTypes) {
    this.theGenerator = g;
    this.rootList = new ArrayList<RootNode>();
    this.rootList.add(new RootNode(srcInfo, name, paramVarSlots, paramTypes));
  }

  IntNode createNodeForByte(Parser.SrcInfo srcInfo, int value) {
    return new IntNode(srcInfo, MInstruction.INT_OBJ_BYTE, value);
  }

  IntNode createNodeForInt(Parser.SrcInfo srcInfo, int value) {
    return new IntNode(srcInfo, MInstruction.INT_OBJ_INT, value);
  }

  IntNode createNodeForChar(Parser.SrcInfo srcInfo, int value) {
    return new IntNode(srcInfo, MInstruction.INT_OBJ_CHAR, value);
  }

  RealNode createNodeForReal(Parser.SrcInfo srcInfo, double value) {
    return new RealNode(srcInfo, value);
  }

  TupleNode createNodeForTuple(Parser.SrcInfo srcInfo) {
    return new TupleNode(srcInfo);
  }

  TuplePtnNode createNodeForTuplePtn(Parser.SrcInfo srcInfo) {
    return new TuplePtnNode(srcInfo);
  }

  // SeqNode createNodeForEmptyList(Parser.SrcInfo srcInfo) {
    // return new SeqNode(srcInfo);
  // }

  NilNode createNodeForEmptyListBody(Parser.SrcInfo srcInfo) {
    return new NilNode(srcInfo);
  }

  ListNode createNodeForList(Parser.SrcInfo srcInfo) {
    return new ListNode(srcInfo);
  }

  ListPtnNode createNodeForListPtn(Parser.SrcInfo srcInfo) {
    return new ListPtnNode(srcInfo);
  }

  StringNode createNodeForString(Parser.SrcInfo srcInfo /* , Node elemType */) {
    return new StringNode(srcInfo /* , elemType */);
  }

  // SeqNode createNodeForEmptyString(Parser.SrcInfo srcInfo) {
    // return new SeqNode(srcInfo);
  // }

  // StringNode createNodeForEmptyStringBody(Parser.SrcInfo srcInfo, Node elemType) {
    // return new StringNode(srcInfo, elemType);
  // }

  StringPtnNode createNodeForStringPtn(Parser.SrcInfo srcInfo) {
    return new StringPtnNode(srcInfo);
  }

  CstrNode createNodeForCstr(Parser.SrcInfo srcInfo, Cstr cstr) {
    return createNodeForCstr(srcInfo, cstr.toJavaString());
  }

  CstrNode createNodeForCstr(Parser.SrcInfo srcInfo, String s) {
    return new CstrNode(srcInfo, s);
  }

  SeqNode createNodeForDataConstr(Parser.SrcInfo srcInfo) {
    return new SeqNode(srcInfo);
  }

  DataConstrNode createNodeForDataConstrBody(Parser.SrcInfo srcInfo, int modRefIndex, String dcon, String tcon, int tparamCount) {
    return new DataConstrNode(srcInfo, modRefIndex, dcon, tcon, tparamCount);
  }

  DataConstrPtnNode createNodeForDataConstrPtn(Parser.SrcInfo srcInfo, int modRefIndex, String dcon, String tcon, int tparamCount) {
    return new DataConstrPtnNode(srcInfo, modRefIndex, dcon, tcon, tparamCount);
  }

  SelfRefNode createNodeForSelfRef(Parser.SrcInfo srcInfo) {
    return new SelfRefNode(srcInfo);
  }

  SeqNode createNodeForFunRef(Parser.SrcInfo srcInfo) {
    return new SeqNode(srcInfo);
  }

  FunRefNode createNodeForFunRefBody(Parser.SrcInfo srcInfo, int modRefIndex, String official) {
    return new FunRefNode(srcInfo, modRefIndex, official);
  }

  VarRefNode createNodeForVarRef(Parser.SrcInfo srcInfo, PExprVarSlot varSlot) {
    return new VarRefNode(srcInfo, varSlot);
  }

  RootNode createNodeForClosureImpl(Parser.SrcInfo srcInfo, String name, PExprVarSlot[] paramVarSlots, PTypeSkel[] paramTypes) {
    RootNode c = new RootNode(srcInfo, name, paramVarSlots, paramTypes);
    this.rootList.add(c);
    return c;
  }

  ClosureConstrNode createNodeForClosureConstr(Parser.SrcInfo srcInfo, RootNode implNode /* , int envCount */) {
    return new ClosureConstrNode(srcInfo, implNode /* , envCount */);
  }

  InvNode createNodeForInv(Parser.SrcInfo srcInfo) {
    return new InvNode(srcInfo);
  }

  ForceMatchingNode createNodeForMatchingExpr(Parser.SrcInfo srcInfo) {
    return new ForceMatchingNode(srcInfo);
  }

  SeqNode createNodeForCaseGuard(Parser.SrcInfo srcInfo) {
    return new SeqNode(srcInfo);
  }

  EndValueSupplierSeqNode createNodeForCaseEval(Parser.SrcInfo srcInfo) {
    return new EndValueSupplierSeqNode(srcInfo);
  }

  ForkNode createNodeForCaseBlock(Parser.SrcInfo srcInfo) {
    return new ForkNode(srcInfo);
  }

  ForkNode createNodeForIfBlock(Parser.SrcInfo srcInfo) {
    return new ForkNode(srcInfo);
  }

  SeqNode createNodeForExprWithMatching(Parser.SrcInfo srcInfo) {
    return new SeqNode(srcInfo);
  }

  TrialNode createTrialNodeInExpr(Parser.SrcInfo srcInfo, Node trial, TrialFrame trialFrame) {
    return new TrialNode(srcInfo, trial, trialFrame);
  }

  TrialNode createTrialNodeInBranch(Parser.SrcInfo srcInfo, Node trial, TrialFrame trialFrame) {
    return new TrialNode(srcInfo, trial, trialFrame);
  }

  NameImplNode createNameImplNode(Parser.SrcInfo srcInfo) {
    return new NameImplNode(srcInfo);
  }

  InitdImplNode createInitdImplNode(Parser.SrcInfo srcInfo) {
    return new InitdImplNode(srcInfo);
  }

  BranchNode createNodeForCaseClause(Parser.SrcInfo srcInfo) {
    return new BranchNode(srcInfo, null);
  }

  BranchNode createNodeForIfClause(Parser.SrcInfo srcInfo) {
    return new BranchNode(srcInfo, null);
  }

  MatchingRootNode createMatchingRootNode(Parser.SrcInfo srcInfo, PtnMatchNode ptn) {
    return new MatchingRootNode(srcInfo, ptn);
  }

  ForkNode createNodeForCasePtnMatches(Parser.SrcInfo srcInfo) {
    return new ForkNode(srcInfo);
  }

  BranchNode createNodeForCasePtnMatchOne(Parser.SrcInfo srcInfo, Node ptn, BranchNode outerBranch) {
    BranchNode b = new BranchNode(srcInfo, outerBranch);
    b.addChild(this.createTrialNodeInBranch(srcInfo, ptn, b));
    return b;
  }

  SeqNode createNodeForCasePtnMatch(Parser.SrcInfo srcInfo) {
    return new SeqNode(srcInfo);
  }

  PtnMatchNode createNodeForPtnMatch(Parser.SrcInfo srcInfo) {
    return new PtnMatchNode(srcInfo);
  }

  Node createNodeForImpose(Parser.SrcInfo srcInfo, PTypeSkel imposingType) {
    return new ImposeNode(srcInfo, imposingType);
  }

  SeqNode createNodeForPtnMatchDetail(Parser.SrcInfo srcInfo) {
    return new SeqNode(srcInfo);
  }

  VarDefNode createNodeForVarDef(Parser.SrcInfo srcInfo, PExprVarSlot varSlot) {
    return new VarDefNode(srcInfo, varSlot);
  }

  WildCardNode createNodeForWildCard(Parser.SrcInfo srcInfo) {
    return new WildCardNode(srcInfo);
  }

  CondNode createNodeForCond(Parser.SrcInfo srcInfo) {
    return new CondNode(srcInfo);
  }

  SeqNode createNodeForAction(Parser.SrcInfo srcInfo) {
    return new EndValueSupplierSeqNode(srcInfo);
  }

  CopyNode createCopyNode(Parser.SrcInfo srcInfo) {
    return new CopyNode(srcInfo);
  }

  SinkNode createSinkNode(Parser.SrcInfo srcInfo) {
    return new SinkNode(srcInfo);
  }

  List<RootNode> getRootList() { return this.rootList; }

  RootNode getTopRoot() { return this.rootList.get(0); }

  void prepareAll() {
    for (int i = this.rootList.size() - 1; i >= 0; i--) {  // reverse order because inner first
      this.rootList.get(i).prepareForGeneration();
    }
  }

  abstract class Node {
    Parser.SrcInfo srcInfo;
    Node parent;
    Node prior;
    Node next;
    VarSet varSetReferredLater;
    Generator.CodeChunk codeChunk;
    
    Node(Parser.SrcInfo srcInfo) {
      this.srcInfo = srcInfo;
      this.codeChunk = Generator.CodeSeq.create(srcInfo);
    }

    void markTailInv() {}

    void collectVars(RootNode frameRoot) {}  // default impl

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      this.varSetReferredLater = varSet;
      return varSet;
    }

    boolean doesDispose() { return false; }

    void generate(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.generateClearExtraVars(frameRoot, allocMap);
      this.generateBody(builder, context, frameRoot, trialNode, allocMap);
    }

    abstract void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap);

    void generateClearExtraVars(RootNode frameRoot, AllocMap allocMap) {
      for (int i = 0; i < allocMap.size(); i++) {
        PExprVarSlot slot = allocMap.allocatedAt(i);
        /* DEBUG */ if (this.varSetReferredLater == null) { System.out.println("varset null: " + this.toString()); }
        if (slot != null && !this.varSetReferredLater.contains(slot)) {
          this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_CLEAR_LOCAL, GFlow.this.theGenerator.createIntParam(i)));
          allocMap.deallocVar(slot);
        }
      }
    }
  }

  class SeqNode extends Node {
    Node first;
    Node last;

    SeqNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    int getChildCount() {
      int i = 0;  
      for (Node n = this.first; n != null; i++, n = n.next);
      return i;
    }

    void addChild(Node n) {
      if (n.parent != null) {
        throw new IllegalArgumentException("Already in flow.");
      }
      n.parent = this;
      n.next = null;
      if (this.last == null) {  // first child
        n.prior = null;
        this.first = n;
        this.last = n;
      } else {
        n.prior = this.last;
        this.last.next = n;
        this.last = n;
      }
    }

    void collectVars(RootNode frameRoot) {
      Node n = this.first;
      while (n != null) {
        n.collectVars(frameRoot);
        n = n.next;
      }
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      VarSet vs = varSet;
      Node n = this.last;
      while (n != null) {
        vs = n.scanVars(frameRoot, vs);
        n = n.prior;
      }
      this.varSetReferredLater = vs;
      return vs;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      Node n = this.first;
      while (n != null) {
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        n = n.next;
      }
    }
  }

  class EndValueSupplierSeqNode extends SeqNode {
    EndValueSupplierSeqNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void markTailInv() {
      if (this.last != null) {
        this.last.markTailInv();
      }
    }
  }

  class BranchNode extends EndValueSupplierSeqNode implements TrialFrame {
    BranchNode outerBranch;
    BranchFailureHandler failureHandler;
    TrialNode superTrialNode;

    BranchNode(Parser.SrcInfo srcInfo, BranchNode outerBranch) {
      super(srcInfo);
      this.outerBranch = outerBranch;
      this.failureHandler = GFlow.this.createBranchFailureHandler(this);
    }

    void addChild(Node n) {
      if (this.first != this.last) { throw new RuntimeException("Too many nodes."); }
      super.addChild(n);
    }

    public TrialFailureHandler getTrialFailureHandler() { return this.failureHandler; }

    BranchNode getNextBranch() {
      BranchNode b = null;
      ForkNode f = (ForkNode)this.parent;
      if (f.branchList.get(f.branchList.size() - 1) != this) {
        b = f.branchList.get(f.branchList.indexOf(this) + 1);
      }
      return b;
    }

    BranchNode getOuterBranch() { return this.outerBranch; }

    PExprVarSlot getVarToRewind() {
      return ((ForkNode)this.parent).rewindTo;
    }

    void rollbackVar(int index) {
      this.failureHandler.rollbackVar(index);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      super.generateBody(builder, context,frameRoot, trialNode, allocMap);
      Generator.Code jump = frameRoot.createCode(MInstruction.OP_JUMP);
      jump.addParam(jump.createRelAddrParamEntryOf(((ForkNode)this.parent).onExit));
      this.codeChunk.addChild(jump);
      this.failureHandler.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.failureHandler.onFailure);
    }
  }

  class ForkNode extends Node {
    List<BranchNode> branchList;
    Generator.CodeSeq onExit;
    PExprVarSlot rewindTo;

    ForkNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
      this.branchList = new ArrayList<BranchNode>();
      this.onExit = Generator.CodeSeq.create();
    }

    void addBranch(BranchNode n) {
      if (n.parent != null) {
        throw new IllegalArgumentException("Already in flow.");
      }
      n.parent = this;
      n.next = null;
      n.prior = null;
      this.branchList.add(n);
    }

    void markTailInv() {
      for (int i = 0; i < this.branchList.size(); i++) {
        this.branchList.get(i).markTailInv();
      }
    }

    void collectVars(RootNode frameRoot) {
      for (int i = 0; i < this.branchList.size(); i++) {
        this.branchList.get(i).collectVars(frameRoot);
      }
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      this.rewindTo = PExprVarSlot.createInternal();
      VarSet vs = varSet.add(this.rewindTo);
      VarSet cvs = GFlow.this.createVarSet();
      for (int i = this.branchList.size() - 1; i >= 0 ; i--) {
        BranchNode on = this.branchList.get(i);
        if (on.first == on.last) {
          VarSet lvs = on.first.scanVars(frameRoot, vs);
          cvs = lvs.union(cvs);
        } else {
          VarSet avs = on.last.scanVars(frameRoot, vs);
          cvs = on.first.scanVars(frameRoot, avs.union(cvs));
        }
        on.varSetReferredLater = cvs;
      }
      this.varSetReferredLater = cvs;
      return cvs;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int ri = allocMap.allocVar(this.rewindTo);
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_SP));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_STORE_LOCAL, GFlow.this.theGenerator.createIntParam(ri)));
      Node n;
      for (int i = 0; i < this.branchList.size(); i++) {
        n = this.branchList.get(i);
        AllocMap m = allocMap.copy();
        n.generate(builder, context, frameRoot, trialNode, m);
        allocMap.expandTo(m.size());
        this.codeChunk.addChild(n.codeChunk);
      }
      this.onExit.addChild(frameRoot.createCode(MInstruction.OP_CLEAR_LOCAL, GFlow.this.theGenerator.createIntParam(ri)));
      allocMap.deallocVar(this.rewindTo);
      this.codeChunk.addChild(this.onExit);
    }
  }

  class RootNode extends EndValueSupplierSeqNode {
    String name;
    PExprVarSlot[] paramVarSlots;
    PTypeSkel[] paramTypes;
    PExprVarSlot selfRef;
    List<PExprVarSlot> definedVarList;
    List<PExprVarSlot> envVarList;
    AllocMap allocMap;

    RootNode(Parser.SrcInfo srcInfo, String name, PExprVarSlot[] paramVarSlots, PTypeSkel[] paramTypes) {
      super(srcInfo);
      this.name = name;
      this.paramVarSlots = paramVarSlots;
      this.paramTypes = paramTypes;
      this.selfRef = PExprVarSlot.createInternal();
      this.definedVarList = new ArrayList<PExprVarSlot>();
      this.envVarList = new ArrayList<PExprVarSlot>();
      this.allocMap = GFlow.this.createAllocMap();
    }

    private void prepareForGeneration() {
      this.collectVars();
      this.scanVars();
      this.markTailInv();
    }

    void collectVars() {
      this.collectVars(null);
    }

    void scanVars() {
      this.scanVars(null, null);
    }

    void collectVars(RootNode frameRoot) {
      for (int i = 0; i < this.paramVarSlots.length; i++) {
        PExprVarSlot slot = this.paramVarSlots[i];
        this.addDefinedVar(slot);
      }
      super.collectVars(this);
      // /* DEBUG */ System.out.println("local vars of " + this.name + " " + this.definedVarList);
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      VarSet newVarSet = super.scanVars(this, GFlow.this.createVarSet());
      this.allocMap.allocVar(this.selfRef);
      for (int i = 0; i < this.paramVarSlots.length; i++) {
        PExprVarSlot slot = this.paramVarSlots[i];
        this.allocMap.allocVar(slot);
      }
      this.varSetReferredLater = newVarSet;
      return newVarSet;
    }

    void generate(Module.Builder builder) {
      this.generate(builder, EVAL_CONTEXT, this, null, this.allocMap);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      super.generateBody(builder, context, this, null, this.allocMap);
      this.codeChunk.addChild(this.createCode(MInstruction.OP_RETURN));
    }

    void addDefinedVar(PExprVarSlot varSlot) {
      if (this.definedVarList.contains(varSlot)) { throw new IllegalArgumentException("Defined var already registered. " + varSlot); }
      this.definedVarList.add(varSlot);
    }

    boolean isDefinedHere(PExprVarSlot varSlot) {
      return this.definedVarList.contains(varSlot);
    }

    void addEnvVar(PExprVarSlot varSlot) {
      if (!this.envVarList.contains(varSlot)) {
        this.envVarList.add(varSlot);
      }
    }

    int envIndexOf(PExprVarSlot varSlot) {  // -1 : not found
      return this.envVarList.indexOf(varSlot);
    }

    Generator.Code createCode(String op) {
      return this.createCode(op, new Generator.OpParam[0]);
    }

    Generator.Code createCode(String op, Generator.OpParam param) {
      return this.createCode(op, new Generator.OpParam[] { param });
    }

    Generator.Code createCode(String op, Generator.OpParam[] params) {
      Generator.Code c = new Generator.Code();
      c.frameRoot = this;
      c.op = op;
      c.params = params;
      return c;
    }

    int allocMapSize() { return this.allocMap.size(); }
  }

  class MatchingRootNode extends Node {
    PtnMatchNode ptn;

    MatchingRootNode(Parser.SrcInfo srcInfo, PtnMatchNode ptn) {
      super(srcInfo);
      this.ptn = ptn;
    }

    void collectVars(RootNode frameRoot) {
      this.ptn.collectVars(frameRoot);
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      this.varSetReferredLater = this.ptn.scanVars(frameRoot, varSet);
      return this.varSetReferredLater;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      if (context == MATCHING_CONTEXT) {
        throw new RuntimeException("Matching nested.");
      }
      this.ptn.generate(builder, MATCHING_CONTEXT, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.ptn.codeChunk);
    }
  }

  class SinkNode extends Node{
    SinkNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    boolean doesDispose() { return true; }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_POP));
    }
  }

  class CopyNode extends Node{
    CopyNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
    }
  }

  class IntNode extends Node{
    int cat;
    int value;

    IntNode(Parser.SrcInfo srcInfo, int cat, int value) {
      super(srcInfo);
      this.cat = cat;
      this.value = value;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode( MInstruction.OP_LOAD_INT,
        new Generator.OpParam[] { GFlow.this.theGenerator.createIntParam(cat), GFlow.this.theGenerator.createIntParam(value) }));
      if (context == MATCHING_CONTEXT) {
        Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_OBJ_NE);
        test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
        this.codeChunk.addChild(test);
      }
    }
  }

  class RealNode extends Node{
    double value;

    RealNode(Parser.SrcInfo srcInfo, double value) {
      super(srcInfo);
      this.value = value;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int constIndex = builder.putUniqueConstItem(Module.createConstForReal(this.value));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_CONST, GFlow.this.theGenerator.createIntParam(constIndex)));
      if (context == MATCHING_CONTEXT) {
        Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_OBJ_NE);
        test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
        this.codeChunk.addChild(test);
      }
    }
  }

  class TupleNode extends SeqNode {
    TupleNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int elemCount = 0;
      Node n = this.first;
      while (n != null) {
        elemCount++;
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        n = n.next;
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_NEW_TUPLE, GFlow.this.theGenerator.createIntParam(elemCount)));
    }
  }

  class TuplePtnNode extends SeqNode {
    TuplePtnNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      Node n;
      int i;
      for (n = this.first, i = 0; n != null; n = n.next, i++) {
        if (!n.doesDispose()) {
          this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
          this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_FIELD, GFlow.this.theGenerator.createIntParam(i)));
          n.generate(builder, context, frameRoot, trialNode, allocMap);
          this.codeChunk.addChild(n.codeChunk);
        }
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_POP));
    }
  }

  class NilNode extends Node {
    NilNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_NIL));
      if (context == MATCHING_CONTEXT) {
        Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_OBJ_NE);
        test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
        this.codeChunk.addChild(test);
      }
    }
  }

  class ListNode extends SeqNode {
    ListNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void addChild(Node n) {
      if (this.first != this.last) { throw new RuntimeException("Too many nodes."); }
      super.addChild(n);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.first.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.first.codeChunk);
      last.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.last.codeChunk);
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_NEW_LIST));
    }
  }

  class ListPtnNode extends SeqNode {
    ListPtnNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void addChild(Node n) {
      if (this.first != this.last) { throw new RuntimeException("Too many nodes."); }
      super.addChild(n);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
      Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_LIST_NIL);
      test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
      this.codeChunk.addChild(test);
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_HEAD));
      this.first.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.first.codeChunk);
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_TAIL));
      this.last.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.last.codeChunk);
    }
  }

  class StringNode extends SeqNode {
    StringNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int elemCount = this.getChildCount();
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_INT,
        new Generator.OpParam[] { GFlow.this.theGenerator.createIntParam(MInstruction.INT_OBJ_INT), GFlow.this.theGenerator.createIntParam(elemCount) }));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_NEW_ARRAY));
      Node n = this.first;
      int i = 0;
      while (n != null) {
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_INT,
          new Generator.OpParam[] { GFlow.this.theGenerator.createIntParam(MInstruction.INT_OBJ_INT), GFlow.this.theGenerator.createIntParam(i) }));
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_STORE_ARRAY_ELEM));
        n = n.next;
        i++;
      }
    }
  }

  class StringPtnNode extends SeqNode {
    StringPtnNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int elemCount = this.getChildCount();
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_ARRAY_LEN));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_INT,
        new Generator.OpParam[] { GFlow.this.theGenerator.createIntParam(MInstruction.INT_OBJ_INT), GFlow.this.theGenerator.createIntParam(elemCount) }));
      Generator.Code elemCountTest = frameRoot.createCode(MInstruction.OP_BRANCH_OBJ_NE);
      elemCountTest.addParam(elemCountTest.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
      this.codeChunk.addChild(elemCountTest);
      Node n = this.first;
      int i = 0;
      while (n != null) {
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_INT,
          new Generator.OpParam[] { GFlow.this.theGenerator.createIntParam(MInstruction.INT_OBJ_INT), GFlow.this.theGenerator.createIntParam(i) }));
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_ARRAY_ELEM));
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        n = n.next;
        i++;
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_POP));
    }
  }

  class CstrNode extends Node {
    String value;

    CstrNode(Parser.SrcInfo srcInfo, String s) {
      super(srcInfo);
      this.value = s;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int constIndex = builder.putUniqueConstItem(Module.createConstForCstr(this.value));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_CONST, GFlow.this.theGenerator.createIntParam(constIndex)));
      if (context == MATCHING_CONTEXT) {
        Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_OBJ_NE);
        test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
        this.codeChunk.addChild(test);
      }
    }
  }

  class DataConstrNode extends SeqNode{
    int modRefIndex;
    String dcon;
    String tcon;
    int tparamCount;

    DataConstrNode(Parser.SrcInfo srcInfo, int modRefIndex, String dcon, String tcon, int tparamCount) {
      super(srcInfo);
      this.modRefIndex = modRefIndex;
      this.dcon = dcon;
      this.tcon = tcon;
      this.tparamCount = tparamCount;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int attrCount = 0;
      Node n = this.first;
      while (n != null) {
        attrCount++;
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        n = n.next;
      }
      int dcIndex;
      if (this.modRefIndex == Module.MOD_INDEX_SELF) {
        dcIndex = builder.putUniqueDataConstrLocal(this.dcon, attrCount, this.tcon, this.tparamCount);
      } else{
        dcIndex = builder.startUniqueDataConstrForeign(this.modRefIndex, this.dcon, attrCount, this.tcon, this.tparamCount);
        if (dcIndex < 0) {
          // HERE: set attr types for verification
          dcIndex = builder.endUniqueDataConstrForeign();
        }
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_NEW_DATA, GFlow.this.theGenerator.createIntParam(dcIndex)));
    }
  }

  class DataConstrPtnNode extends SeqNode{
    int modRefIndex;
    String dcon;
    String tcon;
    int tparamCount;

    DataConstrPtnNode(Parser.SrcInfo srcInfo, int modRefIndex, String dcon, String tcon, int tparamCount) {
      super(srcInfo);
      this.modRefIndex = modRefIndex;
      this.dcon = dcon;
      this.tcon = tcon;
      this.tparamCount = tparamCount;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int attrCount = 0;
      Node n;
      for (n = this.first; n != null; n = n.next) {
        attrCount++;
      }
      int dcIndex;
      if (this.modRefIndex == Module.MOD_INDEX_SELF) {
        dcIndex = builder.putUniqueDataConstrLocal(this.dcon, attrCount, this.tcon, this.tparamCount);
      } else{
        dcIndex = builder.startUniqueDataConstrForeign(this.modRefIndex, this.dcon, attrCount, this.tcon, this.tparamCount);
        if (dcIndex < 0) {
          // HERE: set attr types for verification
          dcIndex = builder.endUniqueDataConstrForeign();
        }
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
      Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_DCON_NE, GFlow.this.theGenerator.createIntParam(dcIndex));
      test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
      this.codeChunk.addChild(test);
      int i;
      for (n = this.first, i = 0; n != null; n = n.next, i++) {
        if (!n.doesDispose()) {
          this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
          this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_FIELD, GFlow.this.theGenerator.createIntParam(i)));
          n.generate(builder, context, frameRoot, trialNode, allocMap);
          this.codeChunk.addChild(n.codeChunk);
        }
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_POP));
    }
  }

  class PtnMatchNode extends SeqNode {
    PtnMatchNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    boolean doesDispose() {
      return this.first != null && this.first == this.last && this.first.doesDispose();
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      if (this.first != this.last) {
        this.first.generate(builder, EVAL_CONTEXT, frameRoot, trialNode, allocMap);  // EVAL_CONTEXT <- imposing type
        this.codeChunk.addChild(this.first.codeChunk);
      }
      this.last.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.last.codeChunk);
    }
  }

  class VarRefNode extends Node {
    PExprVarSlot varSlot;
    int ref;
    int index;

    VarRefNode(Parser.SrcInfo srcInfo, PExprVarSlot varSlot) {
      super(srcInfo);
      this.varSlot = varSlot;
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      VarSet vs = varSet;
      if (!frameRoot.isDefinedHere(this.varSlot)) {
        this.ref = ENV_REF;
        frameRoot.addEnvVar(this.varSlot);
      } else if (varSet.contains(this.varSlot)) {
        this.ref = LOCAL_REF;
      } else {
        this.ref = LOCAL_LAST_REF;
        vs = varSet.add(this.varSlot);
      }
      this.varSetReferredLater = vs;
      return vs;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      String op;
      if (this.ref == ENV_REF) {
        op = MInstruction.OP_LOAD_ENV;
        this.index = frameRoot.envIndexOf(this.varSlot);
      } else if (this.ref == LOCAL_LAST_REF) {
        op = MInstruction.OP_LOAD_AND_CLEAR_LOCAL;
        this.index = allocMap.varIndexOf(this.varSlot);
        if (this.index < 0) { throw new RuntimeException("local var not allocated. " + this.varSlot); }
        allocMap.deallocVar(this.varSlot);
      } else {
        op = MInstruction.OP_LOAD_LOCAL;
        this.index = allocMap.varIndexOf(this.varSlot);
        if (this.index < 0) { throw new RuntimeException("local var not allocated. " + this.varSlot); }
      }
      this.codeChunk.addChild(frameRoot.createCode(op, GFlow.this.theGenerator.createIntParam(this.index)));
      if (context == MATCHING_CONTEXT) {
        Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_OBJ_NE);
        test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
        this.codeChunk.addChild(test);
      }
    }
  }

  class VarDefNode extends Node {
    PExprVarSlot varSlot;
    boolean referredLater;

    VarDefNode(Parser.SrcInfo srcInfo, PExprVarSlot varSlot) {
      super(srcInfo);
      this.varSlot = varSlot;
    }

    void collectVars(RootNode frameRoot) {
      frameRoot.addDefinedVar(this.varSlot);
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      this.referredLater = varSet.contains(this.varSlot);
      this.varSetReferredLater = varSet;
      return varSet;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      if (this.referredLater) {
        int index = allocMap.allocVar(this.varSlot);
        this.codeChunk = frameRoot.createCode(MInstruction.OP_STORE_LOCAL, GFlow.this.theGenerator.createIntParam(index));
        if (trialNode != null) {
          trialNode.rollbackVar(index);
        }
      } else {
        this.codeChunk = frameRoot.createCode(MInstruction.OP_POP);
      }
    }
  }

  class WildCardNode extends Node {
    WildCardNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    boolean doesDispose() { return true; }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk = frameRoot.createCode(MInstruction.OP_POP);  // dispose
    }
  }

  class ClosureConstrNode extends SeqNode {
    RootNode implNode;
    String name;

    ClosureConstrNode(Parser.SrcInfo srcInfo, RootNode implNode) {
      super(srcInfo);
      this.implNode = implNode;
      this.name = implNode.name;
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      for (int i = 0; i < this.implNode.envVarList.size(); i++) {
// /* DEBUG */  System.out.println("env ref node added for " + this.implNode.name + " " + this.implNode.envVarList.get(i));
        this.addChild(new VarRefNode(this.srcInfo, this.implNode.envVarList.get(i)));
      }
      this.varSetReferredLater = super.scanVars(frameRoot, varSet);
      return this.varSetReferredLater;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int ccIndex = builder.putUniqueClosureConstrLocal(this.name, this.implNode.envVarList.size());
      Node n = this.first;
      while (n != null) {
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        n = n.next;
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_NEW_CLOSURE, GFlow.this.theGenerator.createIntParam(ccIndex)));
    }
  }

  class InvNode extends SeqNode {
    boolean atTail;

    InvNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void markTailInv() {
      this.atTail = true;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      Node n = this.first;
      while (n != null) {
        n.generate(builder, context, frameRoot, trialNode, allocMap);
        this.codeChunk.addChild(n.codeChunk);
        n = n.next;
      }
      this.codeChunk.addChild(frameRoot.createCode(
        this.atTail? MInstruction.OP_INVOKE_GOTO: MInstruction.OP_INVOKE));
    }
  }

  class CondNode extends SeqNode {
    CondNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      super.generateBody(builder, context, frameRoot, trialNode, allocMap);
      Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_FALSE);
      test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
      this.codeChunk.addChild(test);
    }
  }

  class ImposeNode extends SeqNode {
    PTypeRefSkel imposingType;

    ImposeNode(Parser.SrcInfo srcInfo, PTypeSkel type) {
      super(srcInfo);
      this.imposingType = (PTypeRefSkel)type;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int modIndex = builder.putUniqueConstItem(Module.createConstForCstr(this.imposingType.tconProps.key.modName));
      int tconIndex = builder.putUniqueConstItem(Module.createConstForCstr(this.imposingType.tconProps.key.idName));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_DUP));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_CONST, GFlow.this.theGenerator.createIntParam(modIndex)));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_CONST, GFlow.this.theGenerator.createIntParam(tconIndex)));
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_INT,
        new Generator.OpParam[] { GFlow.this.theGenerator.createIntParam(MInstruction.INT_OBJ_INT), GFlow.this.theGenerator.createIntParam(this.imposingType.params.length) }));
      Generator.Code test = frameRoot.createCode(MInstruction.OP_BRANCH_TSIG_INCOMPAT);
      test.addParam(test.createRelAddrParamEntryOf(trialNode.getCodeChunkOnFailure()));
      this.codeChunk.addChild(test);
    }
  }

  class FunRefNode extends Node {
    int modRefIndex;
    String official;

    FunRefNode(Parser.SrcInfo srcInfo, int modRefIndex, String official) {
      super(srcInfo);
      this.modRefIndex = modRefIndex;
      this.official = official;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      int ccIndex;
      if (this.modRefIndex == Module.MOD_INDEX_SELF) {
        ccIndex = builder.putUniqueClosureConstrLocal(this.official, 0);
      } else{
        ccIndex = builder.startUniqueClosureConstrForeign(this.modRefIndex, this.official);
        if (ccIndex < 0) {
          // HERE: set param and ret types for verification
          ccIndex = builder.endUniqueClosureConstrForeign();
        }
      }
      this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_NEW_CLOSURE, GFlow.this.theGenerator.createIntParam(ccIndex)));
    }
  }

  class SelfRefNode extends Node {
    boolean referredLater;

    SelfRefNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      this.referredLater = varSet.contains(frameRoot.selfRef);
      VarSet vs = varSet;
      if (!this.referredLater) {
        vs = vs.add(frameRoot.selfRef);
      }
      this.varSetReferredLater = vs;
      return vs;
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      if (this.referredLater) {
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_LOCAL, GFlow.this.theGenerator.createIntParam(0)));
      } else {
        this.codeChunk.addChild(frameRoot.createCode(MInstruction.OP_LOAD_AND_CLEAR_LOCAL, GFlow.this.theGenerator.createIntParam(0)));
        allocMap.deallocVar(frameRoot.selfRef);
      }
    }
  }

  class ForceMatchingNode extends SeqNode implements TrialFrame {
    TrialFailureHandler failureHandler;

    ForceMatchingNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
      this.failureHandler = GFlow.this.createForceMatchingFailureHandler();
    }

    public TrialFailureHandler getTrialFailureHandler() { return this.failureHandler; }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      super.generateBody(builder, context, frameRoot, trialNode, allocMap);
      Generator.Code exit = frameRoot.createCode(MInstruction.OP_JUMP);
      exit.addParam(exit.createRelAddrParamExitOf(this.codeChunk));
      this.codeChunk.addChild(exit);
      this.failureHandler.generate(builder, context, frameRoot, trialNode, allocMap);
      this.codeChunk.addChild(this.failureHandler.onFailure);
    }
  }

  class TrialNode extends Node {
    Node trial;
    TrialFrame trialFrame;

    TrialNode(Parser.SrcInfo srcInfo, Node trial, TrialFrame trialFrame) {
      super(srcInfo);
      this.trial = trial;
      this.trialFrame = trialFrame;
    }

    void collectVars(RootNode frameRoot) {
      this.trial.collectVars(frameRoot);
    }

    VarSet scanVars(RootNode frameRoot, VarSet varSet) {
      this.varSetReferredLater = this.trial.scanVars(frameRoot, varSet);
      return this.varSetReferredLater;
    }

    TrialFailureHandler getTrialFailureHandler() { return this.trialFrame.getTrialFailureHandler(); }

    Generator.CodeChunk getCodeChunkOnFailure() { return this.getTrialFailureHandler().getCodeChunkOnFailure(); }

    void rollbackVar(int index) {
      this.getTrialFailureHandler().rollbackVar(index);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.trial.generate(builder, context,frameRoot, this, allocMap);
      this.codeChunk.addChild(this.trial.codeChunk);
    }
  }

  class NameImplNode extends Node {
    NameImplNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode(
        MInstruction.OP_LOAD_MSLOT,
        GFlow.this.theGenerator.createIntParam(Module.MSLOT_INDEX_NAME)));
    }
  }

  class InitdImplNode extends Node {
    InitdImplNode(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    void generateBody(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.codeChunk.addChild(frameRoot.createCode(
        MInstruction.OP_LOAD_MSLOT,
        GFlow.this.theGenerator.createIntParam(Module.MSLOT_INDEX_INITD)));
    }
  }

  interface TrialFrame {
    TrialFailureHandler getTrialFailureHandler();
  }

  private abstract class TrialFailureHandler {
    Generator.CodeSeq codeChunk;
    Generator.CodeSeq goAround;
    Generator.CodeSeq onFailure;
    List<Integer> rollbackList;

    TrialFailureHandler(boolean needsGoAround, boolean needsRollbackVars) {
      this.codeChunk = Generator.CodeSeq.create();
      this.onFailure = Generator.CodeSeq.create();
      if (needsGoAround) {
        this.goAround = Generator.CodeSeq.create();
      }
      if (needsRollbackVars) {
        this.rollbackList = new ArrayList<Integer>();
      }
    }

    Generator.CodeChunk getCodeChunkOnFailure() { return this.onFailure; }

    void rollbackVar(int index) {
      if (this.rollbackList != null && !this.rollbackList.contains(index)) {
        this.rollbackList.add(index);
      }
    }

    void cancelRollbackVars() {
      this.rollbackList = null;
    }

    void generate(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      if (this.goAround != null) {
        Generator.Code jump = frameRoot.createCode(MInstruction.OP_JUMP);
        jump.addParam(jump.createRelAddrParamExitOf(this.codeChunk));
        this.goAround.addChild(jump);
        this.codeChunk.addChild(this.goAround); 
      }
      if (this.rollbackList != null) {
        for (int i = 0; i < allocMap.size(); i++) {
          if (this.rollbackList.contains(i)) {
            this.onFailure.addChild(frameRoot.createCode(
              MInstruction.OP_CLEAR_LOCAL,
              GFlow.this.theGenerator.createIntParam(i)));
          }
        }
      }
      this.generateQuit(builder, context, frameRoot, trialNode, allocMap);
    }

    abstract void generateQuit(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap);
  }

  ForceMatchingFailureHandler createForceMatchingFailureHandler() {
    return new ForceMatchingFailureHandler();
  }

  private class ForceMatchingFailureHandler extends TrialFailureHandler {
    ForceMatchingFailureHandler() { super(true, false); }

    void generateQuit(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.onFailure.addChild(frameRoot.createCode(
        MInstruction.OP_EXCEPTION,
        GFlow.this.theGenerator.createIntParam(MInstruction.EXCEPTION_INCOMPAT)));
    }
  }

  BranchFailureHandler createBranchFailureHandler(BranchNode branch) {
    return new BranchFailureHandler(branch);
  }

  private class BranchFailureHandler extends TrialFailureHandler {
    BranchNode branch;
    BranchNode nextOption;
    BranchNode outerOption;

    BranchFailureHandler(BranchNode branch) {
      super(false, true);
      this.branch = branch;
    }

    void generate(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      this.nextOption = this.branch.getNextBranch();
      this.outerOption = this.branch.getOuterBranch();
      if (this.nextOption == null && this.outerOption == null) {  // exception will be raised, so no rollback needed
        this.cancelRollbackVars();
      }
      super.generate(builder, context, frameRoot, trialNode, allocMap);
    }

    void generateQuit(Module.Builder builder, int context, RootNode frameRoot, TrialNode trialNode, AllocMap allocMap) {
      if (this.nextOption != null) {
        this.onFailure.addChild(frameRoot.createCode(
          MInstruction.OP_LOAD_LOCAL,
          GFlow.this.theGenerator.createIntParam(allocMap.varIndexOf(this.branch.getVarToRewind()))));
        this.onFailure.addChild(frameRoot.createCode(MInstruction.OP_REWIND));
      } else if (this.outerOption != null) {
        Generator.Code quit = frameRoot.createCode(MInstruction.OP_JUMP);
        quit.addParam(quit.createRelAddrParamEntryOf(this.outerOption.getTrialFailureHandler().getCodeChunkOnFailure()));
        this.onFailure.addChild(quit);
      } else {
        this.onFailure.addChild(frameRoot.createCode(
          MInstruction.OP_EXCEPTION,
          GFlow.this.theGenerator.createIntParam(MInstruction.EXCEPTION_NO_CASE)));
      }
    }
  }

  VarSet createVarSet() {
    return  new VarSet();
  }

  private class VarSet {
    PExprVarSlot varSlot;  // null at the last elem in chain
    VarSet next;

    private VarSet() {}

    boolean contains(PExprVarSlot varSlot) {
      VarSet vs = this;
      boolean b = false;
      while (!b && vs.varSlot != null) {
        b = vs.varSlot == varSlot;
        vs = vs.next;
      }
      return b;
    }

    VarSet add(PExprVarSlot varSlot) {
      if (varSlot == null) { throw new IllegalArgumentException("Null VarSlot."); }
      VarSet vs = this;
      if (!this.contains(varSlot)) {
        vs = new VarSet();
        vs.varSlot = varSlot;
        vs.next = this;
      }
      return vs;
    }

    VarSet union(VarSet vs) {
      VarSet u = this;
      VarSet s = vs;
      while (s.varSlot != null) {
        if (!u.contains(s.varSlot)) {
          u = u.add(s.varSlot);
        }
        s = s.next;
      }
      return u;
    }
  }

  AllocMap createAllocMap() {
    return new AllocMap();
  }

  private class AllocMap {
    List<PExprVarSlot> map;

    AllocMap() {
      this.map = new ArrayList<PExprVarSlot>();
    }

    int size() { return this.map.size(); }

    AllocMap copy() {
      AllocMap am = new AllocMap();
      for (int i = 0; i < this.map.size(); i++) {
        am.map.add(this.map.get(i));
      }
      return am;
    }

    int allocVar(PExprVarSlot varSlot) {
      if (this.map.contains(varSlot)) { throw new RuntimeException("VarSlot already allocated. " + varSlot); }
      int i = this.map.indexOf(null);
      if (i >= 0) {
         this.map.set(i, varSlot);
      } else {
         i = this.map.size();
         this.map.add(varSlot);  // i = index of added elem
      }
      return i;
    }

    void deallocVar(PExprVarSlot varSlot) {
      int i = this.map.indexOf(varSlot);
      if (i < 0) { throw new RuntimeException("VarSlot not allocated. " + GFlow.this.getTopRoot().name + " " + varSlot.toString()); }
      this.map.set(i, null);
    }

    int varIndexOf(PExprVarSlot varSlot) {
      return this.map.indexOf(varSlot);
    }

    boolean isAllocatedAt(int i) {
      return this.map.get(i) != null;
    }

    PExprVarSlot allocatedAt(int i) { return this.map.get(i); }

    void expandTo(int size) {
      for (int i = this.map.size(); i < size; i++) {
        this.map.add(null);
      }
    }
  }
}
