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

import java.lang.reflect.InvocationTargetException;

abstract class RInstruction {
  static final boolean DEBUG_TRACE = false;

  String mnem;
  int[] params;

  RInstruction(String mnem, int[] params) {
    this.mnem = mnem;
    this.params = params;
  }

  public boolean equals(Object o) {
    boolean b;
    if (!(o instanceof RInstruction)) {
      b = false;
    } else {
      RInstruction inst = (RInstruction)o;
      if (!inst.mnem.equals(this.mnem)) {
        b = false;
      } else if (inst.params == null) {
        if (this.params == null) {
          b = true;
        } else {
          b = false;
        }
      } else {
        if (this.params == null) {
          b = false;
        } else if (inst.params.length != this.params.length) {
          b = false;
        } else {
          b = true;
          for (int i = 0; b && i < inst.params.length; i++) {
            b = inst.params[i] == this.params[i];
          }
        }
      }
    }
    return b;
  }

  static RInstruction internalize(MInstruction mi) {
    RInstruction inst = null;
    if (mi.operation.equals(MInstruction.OP_NOP)) {
      inst = new NopInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_RETURN)) {
      inst = new ReturnInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_INT)) {
      inst = new LoadIntInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_NIL)) {
      inst = new LoadNilInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_CONST)) {
      inst = new LoadConstInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_LOCAL)) {
      inst = new LoadLocalInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_AND_CLEAR_LOCAL)) {
      inst = new LoadAndClearLocalInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_ENV)) {
      inst = new LoadEnvInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_FIELD)) {
      inst = new LoadFieldInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_ARRAY_ELEM)) {
      inst = new LoadArrayElemInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_ARRAY_LEN)) {
      inst = new LoadArrayLenInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_HEAD)) {
      inst = new LoadHeadInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_TAIL)) {
      inst = new LoadTailInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_MSLOT)) {
      inst = new LoadMslotInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_LOAD_SP)) {
      inst = new LoadSpInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_STORE_LOCAL)) {
      inst =  new StoreLocalInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_CLEAR_LOCAL)) {
      inst = new ClearLocalInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_STORE_ARRAY_ELEM)) {
      inst = new StoreArrayElemInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_NEW_CLOSURE)) {
      inst =  new NewClosureInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_NEW_DATA)) {
      inst =  new NewDataInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_NEW_TUPLE)) {
      inst =  new NewTupleInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_NEW_LIST)) {
      inst =  new NewListInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_NEW_ARRAY)) {
      inst =  new NewArrayInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_JUMP)) {
      inst =  new JumpInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_INVOKE)) {
      inst =  new InvokeInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_INVOKE_GOTO)) {
      inst =  new InvokeGotoInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_POP)) {
      inst =  new PopInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_DUP)) {
      inst =  new DupInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_REWIND)) {
      inst =  new RewindInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_TRUE)) {
      inst =  new BranchTrueInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_FALSE)) {
      inst =  new BranchFalseInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_OBJ_EQ)) {
      inst =  new BranchObjEqInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_OBJ_NE)) {
      inst =  new BranchObjNeInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_DCON_EQ)) {
      inst =  new BranchDconEqInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_DCON_NE)) {
      inst =  new BranchDconNeInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_LIST_CELL)) {
      inst =  new BranchListCellInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_LIST_NIL)) {
      inst =  new BranchListNilInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_TSIG_COMPAT)) {
      inst =  new BranchTsigCompatInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_BRANCH_TSIG_INCOMPAT)) {
      inst =  new BranchTsigIncompatInst(mi.params);
    } else if (mi.operation.equals(MInstruction.OP_EXCEPTION)) {
      inst =  new ExceptionInst(mi.params);
    } else {
      throw new IllegalArgumentException("Invalid op: " + mi.operation);
    }
    return inst;
  }

  static RInstruction[] internalize(MInstruction[] mis) {
    RInstruction[] insts = new RInstruction[mis.length];
    for (int i = 0; i < mis.length; i++) {
      insts[i] = internalize(mis[i]);
    }
    return insts;
  }

  static RFrame exec(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
    if (frame.closure.impl.tag == RClosureImpl.TAG_VMCODE) {
      return execVMCode(eng, work, frame);
    } else {
      return execNative(eng, work, frame);
    }
  }

  static RFrame execVMCode(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
    return frame.fetchInstruction().execInst(eng, work, frame);
  }

  RFrame execInst(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
    if (DEBUG_TRACE) { this.debugTrace(eng, frame); }
    RFrame f = null;
    RResult excRes = null;
    try {
      f = this.execOp(eng, work, frame);
    } catch (Exception ex) {
      RDataConstr dc = eng.memMgr.getDataConstr(Module.MOD_LANG, "sys_error$");
      RObjItem exc = eng.memMgr.getStructItem(
        eng.memMgr.getDataConstr(Module.MOD_LANG, "exception$"),
        new RObjItem[] {
          eng.memMgr.getStructItem(dc, new RObjItem[0]),
          eng.memMgr.cstrToArrayItem(new Cstr(ex.toString())),
          frame.getExcInfo(),
          eng.memMgr.getStructItem(eng.memMgr.getDataConstr(Module.MOD_LANG, "none$"), new RObjItem[0])
        });
      excRes = eng.memMgr.createResult();
      excRes.setException(exc);
    }
    return (excRes == null)? f: throwExceptionResult(excRes, frame);
  }

  abstract RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame);

  void debugTrace(RuntimeEngine eng, RFrame frame) {  // for debug
    StringBuffer b = new StringBuffer();
    b.append(frame.theTaskControl.toString());
    b.append(" ");
    b.append(frame.closure.impl.name);
    b.append(" ");
    b.append(this.mnem);
    if (this.params != null) {
      char sep = ' ';
      for (int i = 0; i < this.params.length; i++) {
        b.append(sep);
        b.append(this.params[i]);
        sep = ',';
      }
    }
    System.out.println(b.toString());
  }

  static void debugTraceNative(RuntimeEngine eng, RFrame frame) {  // for debug
    StringBuffer b = new StringBuffer();
    b.append(frame.theTaskControl.toString());
    b.append(" ");
    b.append(frame.closure.impl.name);
    b.append(" *NATIVE*");
    System.out.println(b.toString());
  }

  static RFrame execNative(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
    RNativeImplHelper helper;
    if (frame.helper != null) {
      helper = frame.helper;
      helper.core.restart();
    } else {
      helper = new RNativeImplHelper(eng, frame);
    }
    frame.helper = null;
    Object[] params = new Object[frame.vars.length + 1];
    params[0] = helper;
    for (int i = 1, j = 0; j < frame.vars.length; i++, j++) {
      params[i] = frame.vars[j];
    }
    if (DEBUG_TRACE) { debugTraceNative(eng, frame); }
    try {
      frame.closure.impl.nativeImplMethod.invoke(frame.closure.impl.nativeImplTargetObject, params);
    } catch (Exception ex) {
      Throwable cause = ex.getCause();
      String m = ex.toString() + ((cause != null)? "\n Cause:" + cause.toString(): "");
      RDataConstr dc = eng.memMgr.getDataConstr(Module.MOD_LANG, "sys_error$");
      RObjItem exc = eng.memMgr.getStructItem(
        eng.memMgr.getDataConstr(Module.MOD_LANG, "exception$"),
        new RObjItem[] {
          eng.memMgr.getStructItem(dc, new RObjItem[0]),
          eng.memMgr.cstrToArrayItem(new Cstr(m)),
          frame.getExcInfo(),
          eng.memMgr.getStructItem(eng.memMgr.getDataConstr(Module.MOD_LANG, "none$"), new RObjItem[0])
        });
      helper.setException(exc);
    }
    Object resume = helper.getResumeInfo();
    RResult res = helper.getResult();
    RFrame f = frame;
    if (res.endCondition() != RResult.NORMAL_END) {  // exit$ or abnormal end
      f = throwExceptionResult(res, f);  // throw exception to catcher; if not caught, abort task
      if (f == null) {
        frame.theTaskControl.end(res);  // abort
      }
    } else if (helper.core.mustReleaseTask()) {  // blocked or turned to ready
      if (resume != null) {  // suspend and resume
        f.helper = helper;
      }
      work.quit();
    } else if (resume != null) {  // suspend and resume
      f.helper = helper;
      f = helper.getFrameToExecNext();
    } else if (f.parent != null) {
      f = f.parent;
      f.setReturnValue(res.getReturnValue());
    } else  {
      f.theTaskControl.end(res);  // exit
      f = null;
    }
    return f;
  }

  private static RFrame throwExceptionResult(RResult res, RFrame currentFrame) {
    RFrame f = currentFrame.parent;
    while (f != null && !f.setResult(res)) {  // set exception to frame which catches exception
      f = f.parent;
    }
    return f;
  }

  static class NopInst extends RInstruction {
    NopInst(int[] params) { super(MInstruction.OP_NOP, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      return frame;
    }
  }

  static class ReturnInst extends RInstruction {
    ReturnInst(int[] params) { super(MInstruction.OP_RETURN, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RFrame f;
      RObjItem ret = (RObjItem)frame.os.pop();
      if (frame.parent != null) {
        f = frame.parent;
        f.setReturnValue(ret);
      } else {
        frame.theTaskControl.finish(ret);
        f = null;
      }
      return f;
    }
  }

  static class LoadIntInst extends RInstruction {
    LoadIntInst(int[] params) { super(MInstruction.OP_LOAD_INT, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(eng.memMgr.getIntItem(this.params[0], this.params[1]));
      return frame;
    }
  }

  static class LoadNilInst extends RInstruction {
    LoadNilInst(int[] params) { super(MInstruction.OP_LOAD_NIL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(eng.memMgr.getListNilItem());
      return frame;
    }
  }

  static class LoadConstInst extends RInstruction {
    LoadConstInst(int[] params) { super(MInstruction.OP_LOAD_CONST, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(frame.getConstItemAt(this.params[0]));
      return frame;
    }
  }

  static class LoadLocalInst extends RInstruction {
    LoadLocalInst(int[] params) { super(MInstruction.OP_LOAD_LOCAL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(frame.getLocalAt(this.params[0]));
      return frame;
    }
  }

  static class LoadAndClearLocalInst extends RInstruction {
    LoadAndClearLocalInst(int[] params) { super(MInstruction.OP_LOAD_AND_CLEAR_LOCAL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(frame.getLocalAt(this.params[0]));
      frame.setLocalAt(this.params[0], null);
      return frame;
    }
  }

  static class LoadEnvInst extends RInstruction {
    LoadEnvInst(int[] params) { super(MInstruction.OP_LOAD_ENV, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(frame.getEnvAt(this.params[0]));
      return frame;
    }
  }

  static class LoadFieldInst extends RInstruction {
    LoadFieldInst(int[] params) { super(MInstruction.OP_LOAD_FIELD, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RStructItem s = (RStructItem)frame.os.pop();
/* DEBUG */ if (this.params[0] >= s.fields.length) { System.out.print(s.dataConstr.name); System.out.println(s.fields.length); }
      frame.os.push(s.getFieldAt(this.params[0]));
      return frame;
    }
  }

  static class LoadArrayElemInst extends RInstruction {
    LoadArrayElemInst(int[] params) { super(MInstruction.OP_LOAD_ARRAY_ELEM, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      int i = ((RIntItem)frame.os.pop()).getValue();
      RArrayItem a = (RArrayItem)frame.os.pop();
      frame.os.push(a.getElemAt(i));
      return frame;
    }
  }

  static class LoadArrayLenInst extends RInstruction {
    LoadArrayLenInst(int[] params) { super(MInstruction.OP_LOAD_ARRAY_LEN, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RArrayItem a = (RArrayItem)frame.os.pop();
      frame.os.push(eng.memMgr.getIntItem(a.getElemCount()));
      return frame;
    }
  }

  static class LoadHeadInst extends RInstruction {
    LoadHeadInst(int[] params) { super(MInstruction.OP_LOAD_HEAD, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RListItem.Cell ci = (RListItem.Cell)frame.os.pop();
      frame.os.push(ci.head);
      return frame;
    }
  }

  static class LoadTailInst extends RInstruction {
    LoadTailInst(int[] params) { super(MInstruction.OP_LOAD_TAIL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RListItem.Cell ci = (RListItem.Cell)frame.os.pop();
      frame.os.push(ci.tail);
      return frame;
    }
  }

  static class LoadMslotInst extends RInstruction {
    LoadMslotInst(int[] params) { super(MInstruction.OP_LOAD_MSLOT, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RObjItem item = frame.closure.impl.mod.slots[params[0]];
      if (item != null) {
        frame.os.push(item);
      } else {
        RDataConstr dc = eng.memMgr.getDataConstr(Module.MOD_LANG, "no_elem$");
        RStructItem i = eng.memMgr.getStructItem(dc, new RObjItem[0]);
        RDataConstr dc2 = eng.memMgr.getDataConstr(Module.MOD_LANG, "runtime_failure$");
        RStructItem desc = eng.memMgr.getStructItem(dc2, new RObjItem[] { i });
        frame.theTaskControl.abort(eng.memMgr.getStructItem(
          eng.memMgr.getDataConstr(Module.MOD_LANG, "exception$"),
          new RObjItem[] {
            desc,
            eng.memMgr.cstrToArrayItem(new Cstr("Not initialized.")),
            frame.getExcInfo(),
            eng.memMgr.getStructItem(eng.memMgr.getDataConstr(Module.MOD_LANG, "none$"), new RObjItem[0])
          }
        ));
      }
      return frame;
    }
  }

  static class LoadSpInst extends RInstruction {
    LoadSpInst(int[] params) { super(MInstruction.OP_LOAD_SP, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(frame.os.getPointer());
      return frame;
    }
  }

  static class StoreLocalInst extends RInstruction {
    StoreLocalInst(int[] params) { super(MInstruction.OP_STORE_LOCAL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.setLocalAt(this.params[0], frame.os.pop());
      return frame;
    }
  }

  static class ClearLocalInst extends RInstruction {
    ClearLocalInst(int[] params) { super(MInstruction.OP_CLEAR_LOCAL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.setLocalAt(this.params[0], null);
      return frame;
    }
  }

  static class StoreArrayElemInst extends RInstruction {
    StoreArrayElemInst(int[] params) { super(MInstruction.OP_STORE_ARRAY_ELEM, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RObjItem v = (RObjItem)frame.os.pop();
      int i = ((RIntItem)frame.os.pop()).getValue();
      RArrayItem a = (RArrayItem)frame.os.pop();
      a.setElemAt(i, v);
      return frame;
    }
  }

  static class NewClosureInst extends RInstruction {
    NewClosureInst(int[] params) { super(MInstruction.OP_NEW_CLOSURE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RClosureConstr cc = frame.getClosureConstrAt(this.params[0]);
      RTaskControl init = null;
      boolean goAhead = false;
      if (cc.modIndex == Module.MOD_INDEX_SELF) {
        goAhead = true;
      } else if ((init = cc.impl.mod.getInitTask()) == null) {
        goAhead = true;
      } else if (frame.theTaskControl.join(init, null)) {
        goAhead = true;
      }  // else wait for initialization of other module
      if (goAhead) {
        RObjItem[] env = frame.os.popMultipleObjItemsPushOrder(cc.envCount);
        frame.os.push(RClosureItem.create(eng, cc.impl, env));
      } else {
        frame.codeIndex--;  // suspend and reexec
        work.quit();
      }
      return frame;
    }
  }

  static class NewDataInst extends RInstruction {
    NewDataInst(int[] params) { super(MInstruction.OP_NEW_DATA, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RDataConstr dc = frame.getDataConstrAt(this.params[0]);
      RObjItem[] attrs = frame.os.popMultipleObjItemsPushOrder(dc.attrCount);
      frame.os.push(eng.memMgr.getStructItem(dc, attrs));
      return frame;
    }
  }

  static class NewTupleInst extends RInstruction {
    NewTupleInst(int[] params) { super(MInstruction.OP_NEW_TUPLE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RObjItem[] elems = frame.os.popMultipleObjItemsPushOrder(this.params[0]);
      frame.os.push(eng.memMgr.getStructItem(RDataConstr.pseudoOfTuple, elems));
      return frame;
    }
  }

  static class NewListInst extends RInstruction {
    NewListInst(int[] params) { super(MInstruction.OP_NEW_LIST, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RListItem.Cell cell = eng.memMgr.createListCellItem();
      cell.tail = (RListItem)frame.os.pop();
      cell.head = (RObjItem)frame.os.pop();
      frame.os.push(cell);
      return frame;
    }
  }

  static class NewArrayInst extends RInstruction {
    NewArrayInst(int[] params) { super(MInstruction.OP_NEW_ARRAY, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      int count = ((RIntItem)frame.os.pop()).getValue();
      frame.os.push(eng.memMgr.createArrayItem(count));
      return frame;
    }
  }

  static class JumpInst extends RInstruction {
    JumpInst(int[] params) { super(MInstruction.OP_JUMP, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.codeIndex += this.params[0];
      return frame;
    }
  }

  static class InvokeInst extends RInstruction {
    InvokeInst(int[] params) { super(MInstruction.OP_INVOKE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RClosureItem closure = (RClosureItem)frame.os.pop();
      int paramCount = closure.getParamCount();
      RObjItem[] params = frame.os.popMultipleObjItemsPushOrder(paramCount);
      return frame.createChild(closure, params);
    }
  }

  static class InvokeGotoInst extends RInstruction {
    InvokeGotoInst(int[] params) { super(MInstruction.OP_INVOKE_GOTO, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RClosureItem closure = (RClosureItem)frame.os.pop();
      int paramCount = closure.getParamCount();
      RObjItem[] params = frame.os.popMultipleObjItemsPushOrder(paramCount);
      return frame.toTransfer(closure, params);
    }
  }

  static class PopInst extends RInstruction {
    PopInst(int[] params) { super(MInstruction.OP_POP, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.pop();
      return frame;
    }
  }

  static class DupInst extends RInstruction {
    DupInst(int[] params) { super(MInstruction.OP_DUP, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.push(frame.os.getTop());
      return frame;
    }
  }

  static class RewindInst extends RInstruction {
    RewindInst(int[] params) { super(MInstruction.OP_REWIND, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      frame.os.rewindTo((ROperandStack.Pointer)frame.os.pop());
      return frame;
    }
  }

  static class BranchTrueInst extends RInstruction {
    BranchTrueInst(int[] params) { super(MInstruction.OP_BRANCH_TRUE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      if (isTrue((RObjItem)frame.os.pop())) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static class BranchFalseInst extends RInstruction {
    BranchFalseInst(int[] params) { super(MInstruction.OP_BRANCH_FALSE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      if (!isTrue((RObjItem)frame.os.pop())) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static boolean isTrue(RObjItem item) {
    boolean b = false;
    if (item instanceof RStructItem) {
      RStructItem s = (RStructItem)item;
      if (s.dataConstr != null && s.dataConstr.modName.equals(Module.MOD_LANG) && s.dataConstr.name.equals("true$")) {
        b = true;
      }
    }
    return b;
  }

  static class BranchObjEqInst extends RInstruction {
    BranchObjEqInst(int[] params) { super(MInstruction.OP_BRANCH_OBJ_EQ, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RObjItem i1 = (RObjItem)frame.os.pop();
      RObjItem i0 = (RObjItem)frame.os.pop();
      if (objCompare(frame, i0, i1)) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static class BranchObjNeInst extends RInstruction {
    BranchObjNeInst(int[] params) { super(MInstruction.OP_BRANCH_OBJ_NE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RObjItem i1 = (RObjItem)frame.os.pop();
      RObjItem i0 = (RObjItem)frame.os.pop();
      if (!objCompare(frame, i0, i1)) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static boolean objCompare(RFrame frame, RObjItem item0, RObjItem item1) {
    ROperandStack.Pointer currentStackPointer = frame.os.getPointer();
    frame.os.push(item0);
    frame.os.push(item1);
    boolean b = true;
    while (b && !frame.os.isPointerEqualTo(currentStackPointer)) {
      RObjItem i1 = (RObjItem)frame.os.pop();
      RObjItem i0 = (RObjItem)frame.os.pop();
      b = i0.objEquals(frame, i1);
    }
    frame.os.rewindTo(currentStackPointer);
    return b;
  }

  static class BranchDconEqInst extends RInstruction {
    BranchDconEqInst(int[] params) { super(MInstruction.OP_BRANCH_DCON_EQ, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      if (isDconEqual((RObjItem)frame.os.pop(), frame.getDataConstrAt(this.params[0]))) {
        frame.codeIndex += this.params[1];
      }
      return frame;
    }
  }

  static class BranchDconNeInst extends RInstruction {
    BranchDconNeInst(int[] params) { super(MInstruction.OP_BRANCH_DCON_NE, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      if (!isDconEqual((RObjItem)frame.os.pop(), frame.getDataConstrAt(this.params[0]))) {
        frame.codeIndex += this.params[1];
      }
      return frame;
    }
  }

  static boolean isDconEqual(RObjItem item, RDataConstr dataConstr) {
    return dataConstr.equals(((RStructItem)item).dataConstr);
  }

  static class BranchListCellInst extends RInstruction {
    BranchListCellInst(int[] params) { super(MInstruction.OP_BRANCH_LIST_CELL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      if (frame.os.pop() instanceof RListItem.Cell) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static class BranchListNilInst extends RInstruction {
    BranchListNilInst(int[] params) { super(MInstruction.OP_BRANCH_LIST_NIL, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      if (frame.os.pop() instanceof RListItem.Nil) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static class BranchTsigCompatInst extends RInstruction {
    BranchTsigCompatInst(int[] params) { super(MInstruction.OP_BRANCH_TSIG_COMPAT, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RIntItem paramCount = (RIntItem)frame.os.pop();
      RArrayItem name = (RArrayItem)frame.os.pop();
      RArrayItem mod = (RArrayItem)frame.os.pop();
      RObjItem obj = (RObjItem)frame.os.pop();
      if (obj.getTsig().equals(frame, mod, name, paramCount)) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static class BranchTsigIncompatInst extends RInstruction {
    BranchTsigIncompatInst(int[] params) { super(MInstruction.OP_BRANCH_TSIG_INCOMPAT, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
      RIntItem paramCount = (RIntItem)frame.os.pop();
      RArrayItem name = (RArrayItem)frame.os.pop();
      RArrayItem mod = (RArrayItem)frame.os.pop();
      RObjItem obj = (RObjItem)frame.os.pop();
      if (!obj.getTsig().equals(frame, mod, name, paramCount)) {
        frame.codeIndex += this.params[0];
      }
      return frame;
    }
  }

  static class ExceptionInst extends RInstruction {
    ExceptionInst(int[] params) { super(MInstruction.OP_EXCEPTION, params); }

    RFrame execOp(RuntimeEngine eng, RTaskMgr.Work work, RFrame frame) {
// /* DEBUG */ System.out.println(frame.theTaskControl.toString()+" call EX");
      RDataConstr dc;
      RDataConstr dc2;
      RObjItem i;
      RObjItem desc;
      String m = "";
      switch (this.params[0]) {
      case MInstruction.EXCEPTION_INCOMPAT:
        dc = eng.memMgr.getDataConstr(Module.MOD_LANG, "incompat$");
        i = eng.memMgr.getStructItem(dc, new RObjItem[0]);
        dc2 = eng.memMgr.getDataConstr(Module.MOD_LANG, "runtime_failure$");
        desc = eng.memMgr.getStructItem(dc2, new RObjItem[] { i });
        break;
      case MInstruction.EXCEPTION_NO_CASE:
        dc = eng.memMgr.getDataConstr(Module.MOD_LANG, "no_case$");
        i = eng.memMgr.getStructItem(dc, new RObjItem[0]);
        dc2 = eng.memMgr.getDataConstr(Module.MOD_LANG, "runtime_failure$");
        desc = eng.memMgr.getStructItem(dc2, new RObjItem[] { i });
        break;
      case MInstruction.EXCEPTION_NO_ELEM:
        dc = eng.memMgr.getDataConstr(Module.MOD_LANG, "no_elem$");
        i = eng.memMgr.getStructItem(dc, new RObjItem[0]);
        dc2 = eng.memMgr.getDataConstr(Module.MOD_LANG, "runtime_failure$");
        desc = eng.memMgr.getStructItem(dc2, new RObjItem[] { i });
        break;
      default:
        dc2 = eng.memMgr.getDataConstr(Module.MOD_LANG, "sys_error$");
        desc = eng.memMgr.getStructItem(dc2, new RObjItem[0]);
        m = Integer.toString(this.params[0]);
      }
      RObjItem exc = eng.memMgr.getStructItem(
        eng.memMgr.getDataConstr(Module.MOD_LANG, "exception$"),
        new RObjItem[] {
          desc,
          eng.memMgr.cstrToArrayItem(new Cstr(m)),
          frame.getExcInfo(),
          eng.memMgr.getStructItem(eng.memMgr.getDataConstr(Module.MOD_LANG, "none$"), new RObjItem[0])
        });
      RResult res = eng.memMgr.createResult();
      res.setException(exc);
      RFrame f= throwExceptionResult(res, frame);
      if (f == null) {
        frame.theTaskControl.end(res);
      }
      return f;
    }
  }
}
