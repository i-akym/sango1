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

import java.lang.reflect.Method;

public class RClosureImpl {
  static final int TAG_VMCODE = 1;
  static final int TAG_NATIVE = 2;

  int tag;
  RModule mod;
  String name;
  int paramCount;
  RInstruction[] codeBlock;
  MSrcInfo[] srcInfoTab;
  int varCount;
  // String implFor;
  Object nativeImplTargetObject;
  Method nativeImplMethod;

  RClosureImpl() {}

  static RClosureImpl createVMCode(RModule mod, String name, int paramCount, RInstruction[] codeBlock, MSrcInfo[] srcInfoTab, int varCount) {
    RClosureImpl i = new RClosureImpl();
    i.tag = TAG_VMCODE;
    i.mod = mod;
    i.name = name;
    i.paramCount = paramCount;
    i.codeBlock = codeBlock;
    i.srcInfoTab = srcInfoTab;
    i.varCount = varCount;
    return i;
  }

  static RClosureImpl createNative(RModule mod, String name, int paramCount, /* String implFor, */ Object target, Method nativeImpl) {
    RClosureImpl i = new RClosureImpl();
    i.tag = TAG_NATIVE;
    i.mod = mod;
    i.name = name;
    i.paramCount = paramCount;
    // i.implFor = implFor;
    i.nativeImplTargetObject = target;
    i.nativeImplMethod = nativeImpl;
    i.varCount = paramCount + 1;
    return i;
  }

  boolean implEquals(RClosureImpl impl) {
    // not by name, but implementation
    boolean b;
    if (impl == this) {
      b = true;
    } else if (impl.paramCount != this.paramCount) {
      b = false;
    } else if (impl.tag != this.tag) {
      b = false;
    } else if (tag == TAG_VMCODE) {
      if (impl.varCount != this.varCount) {
        b = false;
      } else if (impl.codeBlock.length != this.codeBlock.length) {
        b = false;
      } else {
        b = true;
        for (int i = 0; b && i < impl.codeBlock.length; i++) {
          b = impl.codeBlock[i].equals(this.codeBlock[i]);
        }
      }
    } else {
      if (impl.nativeImplTargetObject != this.nativeImplTargetObject) {
        b = false;
      } else if (impl.nativeImplMethod != this.nativeImplMethod) {
        b = false;
      } else if (impl.varCount != this.varCount) {
        b = false;
      } else {
        b = true;
      }
    }
    return b;
  }

  public RModule getModule() { return this.mod; }

  public String getName() { return this.name; }

  public String getSrcLoc(int codeIndex) {  // null if native impl or location info not found
    String loc = null;
    if (this.srcInfoTab != null) {
      for (int i = 0; loc == null && i < this.srcInfoTab.length; i++) {
        if (this.srcInfoTab[i].upperCodeIndex >= codeIndex) {
          loc = this.srcInfoTab[i].locInfo;
	}
      }
    }
    return loc;
  }
}
