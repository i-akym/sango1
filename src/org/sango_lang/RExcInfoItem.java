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

public class RExcInfoItem extends RObjItem {
  List<FrameSnapshot> callStack;  // from current to root

  RExcInfoItem(RuntimeEngine e) { super(e); }

  static RExcInfoItem create(RuntimeEngine e, RFrame currentFrame) {
    RExcInfoItem eii = new RExcInfoItem(e);
    eii.callStack = new ArrayList<FrameSnapshot>();
    RFrame f = currentFrame;
    while (f != null) {
      FrameSnapshot fs = new FrameSnapshot();
      fs.transferred = f.transferred;
      fs.impl = f.closure.impl;
      fs.codeIndex = f.codeIndex;
      eii.callStack.add(fs);
      f = f.parent;
    }
    return eii;
  }

  public boolean objEquals(RFrame frame, RObjItem item) {
    throw new RuntimeException("RExcInfoItem#objEquals not implemented.");
  }

  public RType.Sig getTsig() {
    return RType.createTsig(Module.MOD_LANG, "exc_info", 0);
  }

  public void doHash(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getIntItem(0));  // TODO: improve!
  }

  public Cstr dumpInside() {
    return new Cstr(this.toString());
  }

  public List<FrameSnapshot> getCallStack() { return this.callStack; }

  public static class FrameSnapshot {
    public boolean transferred;
    public RClosureImpl impl;
    public int codeIndex;
  }
}
