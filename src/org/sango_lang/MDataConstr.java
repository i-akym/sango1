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

public class MDataConstr implements Module.Elem {
  int modIndex;
  String name;
  int attrCount;
  String tcon;
  int tparamCount;

  private MDataConstr() {}

  static MDataConstr create(int modIndex, String name, int attrCount, String tcon, int tparamCount) {
    MDataConstr dc = new MDataConstr();
    dc.modIndex = modIndex;
    dc.name = name;
    dc.attrCount = attrCount;
    dc.tcon = tcon;
    dc.tparamCount = tparamCount;
    return dc;
  }

  public boolean equals(Object o) {
    boolean b;
    if (o == this) {
      b = true;
    } else if (!(o instanceof MDataConstr)) {
      b = false;
    } else {
      MDataConstr dc = (MDataConstr)o;
      b = (dc.modIndex == this.modIndex) && dc.name.equals(this.name);  // do not care attrCount
    }
    return b;
  }

  public Element externalize(Document doc) {
    Element dataConstrNode = doc.createElement(Module.TAG_DATA_CONSTR);
    if (this.modIndex > 0) {
      dataConstrNode.setAttribute(Module.ATTR_MOD_INDEX, Integer.toString(this.modIndex));
    }
    dataConstrNode.setAttribute(Module.ATTR_DCON, this.name);
    dataConstrNode.setAttribute(Module.ATTR_ATTR_COUNT, Integer.toString(this.attrCount));
    if (this.tcon != null) {
      dataConstrNode.setAttribute(Module.ATTR_TCON, this.tcon);
      dataConstrNode.setAttribute(Module.ATTR_TPARAM_COUNT, Integer.toString(this.tparamCount));
    }
    return dataConstrNode;
  }
}
