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

public class RFrame {
  static final int ACCEPT_RET_VAL = 0;
  static final int ACCEPT_RET_RESULT = 1;
  static final int ACCEPT_ALL_RESULT = 2;

  RFrame parent;
  boolean transferred;  // useful for inspecting call stack
  RTaskControl theTaskControl;
  RClosureItem closure;
  RVMItem[] vars;
  int codeIndex;
  ROperandStack os;
  RNativeImplHelper helper;
  int resultHandling;
  RResult result;

  private RFrame() {}

  static RFrame create(RTaskControl taskControl, RClosureItem closure, RObjItem[] params) {
    RFrame f = new RFrame();
    f.theTaskControl = taskControl;
    init(f, closure, params);
    return f;
  }

  RFrame createChild(RClosureItem closure, RObjItem[] params) {
    RFrame c = new RFrame();
    c.parent = this;
    c.theTaskControl = this.theTaskControl;
    init(c, closure, params);
    return c;
  }

  RFrame toTransfer(RClosureItem closure, RObjItem[] params) {
    RFrame f;
    if (closure == this.closure) {
      f = this.reuse(params);
    } else {
      f = (this.parent != null)?
        this.parent.createChild(closure, params):
        create(this.theTaskControl, closure, params);
    }
    f.transferred = true;
    return f;
  }

  private static void init(RFrame frame, RClosureItem closure, RObjItem[] params) {
    frame.closure = closure;
    frame.os = new ROperandStack(frame);
    frame.vars = new RVMItem[closure.impl.varCount];
    frame.vars[0] = closure;
    int idx = 1;
    for (int j = 0; j < params.length; idx++, j++) {
      frame.vars[idx] = params[j];
    }
    for (; idx < frame.vars.length; idx++) {
      frame.vars[idx] = null;
    }
    frame.resultHandling = (closure.impl.tag == RClosureImpl.TAG_VMCODE)? ACCEPT_RET_VAL: ACCEPT_RET_RESULT;
  }

  RFrame reuse(RObjItem[] params) {
    this.codeIndex = 0;
    this.helper = null;
    this.os.reuse();
    this.vars[0] = this.closure;
    int idx = 1;
    for (int j = 0; j < params.length; idx++, j++) {
      this.vars[idx] = params[j];
    }
    for (; idx < this.vars.length; idx++) {
      this.vars[idx] = null;
    }
    return this;
  }

  void setReturnValue(RObjItem ret) {
    if (this.resultHandling == ACCEPT_RET_VAL) {
      this.os.push(ret);
    } else {
      this.result = this.theTaskControl.theMgr.theEngine.memMgr.createResult(ret);
    }
  }

  boolean setException(RObjItem exc) {
    boolean accepted;
    if (this.resultHandling == ACCEPT_ALL_RESULT) {
      this.result = this.theTaskControl.theMgr.theEngine.memMgr.createResult();
      this.result.setException(exc);
      accepted = true;
    } else {
      accepted = false;
    }
    return accepted;
  }

  boolean setResult(RResult res) {
    boolean accepted;
    if (this.resultHandling == ACCEPT_ALL_RESULT) {
      this.result = res;
      accepted = true;
    } else if (res.endCondition() == RResult.NORMAL_END) {
      if (this.resultHandling == ACCEPT_RET_VAL) {
        this.os.push(result.getReturnValue());
      } else {
        this.result = res;
      }
      accepted = true;
    } else {
      accepted = false;
    }
    return accepted;
  }

  public RVMItem popOstack() { return this.os.pop(); }

  public void pushOstack(RVMItem item) {
    this.os.push(item);
  }

  RInstruction fetchInstruction() {
    return this.closure.impl.codeBlock[this.codeIndex++];
  }

  RObjItem getConstItemAt(int index) {
    return this.closure.impl.mod.getConstItem(index);
  }

  RDataConstr getDataConstrAt(int index) {
    return this.closure.impl.mod.getDataConstr(index);
  }

  RClosureConstr getClosureConstrAt(int index) {
    return this.closure.impl.mod.getClosureConstrAt(index);
  }

  RClosureImpl getClosureImplAt(int index) {
    return this.closure.impl.mod.getClosureImplAt(index);
  }

  RVMItem getLocalAt(int index) {
    return this.vars[index];
  }

  RObjItem getEnvAt(int index) {
    return this.closure.env[index];
  }

  void setLocalAt(int index, RVMItem item) {
    this.vars[index] = item;
  }

  RExcInfoItem getExcInfo() { return RExcInfoItem.create(this.theTaskControl.theMgr.theEngine, this); }
}
