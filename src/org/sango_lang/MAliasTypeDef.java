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

public class MAliasTypeDef implements Module.Elem {
  String tcon;
  Module.Availability availability;
  int acc;
  int paramCount;
  MType body;

  private MAliasTypeDef() {}

  static MAliasTypeDef create(String tcon, Module.Availability availability, int acc, int paramCount) {
/* DEBUG */ if (availability == null) { throw new IllegalArgumentException("Null availability. " + tcon); }
    MAliasTypeDef atd = new MAliasTypeDef();
    atd.tcon = tcon;
    atd.availability = availability;
    atd.acc = acc;
    atd.paramCount = paramCount;
    return atd;
  }

  void setBody(MType body) {
    this.body = body;
  }

  public Element externalize(Document doc) {
    Element aliasTypeDefNode = doc.createElement(Module.TAG_ALIAS_TYPE_DEF);
    aliasTypeDefNode.setAttribute(Module.ATTR_TCON, this.tcon);
    if (this.availability != Module.AVAILABILITY_GENERAL) {
      aliasTypeDefNode.setAttribute(Module.ATTR_AVAILABILITY, Module.reprOfAvailability(this.availability));
    }
    if (this.acc != Module.ACC_PRIVATE) {
      aliasTypeDefNode.setAttribute(Module.ATTR_ACC, Module.reprOfAcc(this.acc));
    }
    aliasTypeDefNode.setAttribute(Module.ATTR_PARAM_COUNT, Integer.toString(this.paramCount));
    aliasTypeDefNode.appendChild(Module.externalizeType(doc, this.body));
    return aliasTypeDefNode;
  }
}
