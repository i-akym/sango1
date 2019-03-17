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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MClosureConstr implements Module.Elem {
  int modIndex;
  int envCount;
  String name;

  private MClosureConstr() {}

  static MClosureConstr create(int modIndex, String name, int envCount) {
    MClosureConstr cc = new MClosureConstr();
    cc.modIndex = modIndex;
    cc.name = name;
    cc.envCount = envCount;
    return cc;
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof MClosureConstr)) {
      b = false;
    } else {
      MClosureConstr cc = (MClosureConstr)o;
      b = (cc.modIndex == this.modIndex) && cc.name.equals(this.name);  // do not care envCount
    }
    return b;
  }

  public Element externalize(Document doc) {
    Element closureConstrNode = doc.createElement(Module.TAG_CLOSURE_CONSTR);
    if (this.modIndex > 0) {
      closureConstrNode.setAttribute(Module.ATTR_MOD_INDEX, Integer.toString(this.modIndex));
    }
    closureConstrNode.setAttribute(Module.ATTR_NAME, this.name);
    if (this.envCount > 0) {
      closureConstrNode.setAttribute(Module.ATTR_ENV_COUNT, Integer.toString(this.envCount));
    }
    return closureConstrNode;
  }
}
