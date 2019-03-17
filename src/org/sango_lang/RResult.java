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

public class RResult {
  private static final int CAT_RET = 0;
  private static final int CAT_EXC = 1;

  public static final int NORMAL_END = 0;
  public static final int EXIT_END = 1;
  public static final int ABNORMAL_END = 2;

  RuntimeEngine theEngine;
  int cat;
  RObjItem resultItem;

  RResult(RuntimeEngine eng, RObjItem ret) {
    this.theEngine = eng;
    this.cat = CAT_RET;
    this.resultItem = ret;
  }

  public RObjItem toResultItem() {
    RDataConstr d = this.theEngine.memMgr.getDataConstr(Module.MOD_LANG, (this.cat == CAT_RET)? "fin$": "exc$");
    return this.theEngine.memMgr.getStructItem(d, new RObjItem[] { this.resultItem });
  }

  public int endCondition() {
    int c;
    if (this.cat == CAT_RET) {
      c = NORMAL_END;
    } else {
      RStructItem e = (RStructItem)this.resultItem;
      RStructItem desc = (RStructItem)e.getFieldAt(0);
      RDataConstr dc = desc.getDataConstr();
      if (dc.modName.equals(Module.MOD_ACTOR) && dc.name.equals("exit$")) {
        c = EXIT_END;
      } else if (dc.modName.equals(Module.MOD_LANG) && dc.name.equals("thru$")) {
        RStructItem desc2 = (RStructItem)desc.getFieldAt(0);
        RDataConstr dc2 = desc2.getDataConstr();
        if (dc2.modName.equals(Module.MOD_ACTOR) && dc2.name.equals("exit$")) {
          c = EXIT_END;
        } else {
          c = ABNORMAL_END;
        }
      } else {
        c = ABNORMAL_END;
      }
    }
    return c;
  }

  public RObjItem getReturnValue() {
    return (this.cat == CAT_RET)? this.resultItem: null;
  }

  public RStructItem getException() {
    return (this.cat == CAT_EXC)? (RStructItem)this.resultItem: null;
  }

  public void setReturnValue(RObjItem ret) {
    this.cat = CAT_RET;
    this.resultItem = ret;
  }

  public void setException(RObjItem exc) {
    this.cat = CAT_EXC;
    this.resultItem = exc;
  }

  // public RObjItem getResultItem() { return this.resultItem; }
}
