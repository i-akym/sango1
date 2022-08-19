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

public class MAttrDef {
  MConstrDef theConstrDef;
  String name;
  MType type;

  private MAttrDef() {}

  static MAttrDef create(String name) {
    MAttrDef a = new MAttrDef();
    a.name = name;
    return a;
  }

  void setConstrDef(MConstrDef constrDef) {
    this.theConstrDef = constrDef;
  }

  void setType(MType type) {
    this.type = type;
  }

  public Element externalize(Document doc) {
    Element attrDefNode = doc.createElement(Module.TAG_ATTR);
    if (this.name != null) {
      attrDefNode.setAttribute(Module.ATTR_NAME, this.name);
    }
    attrDefNode.appendChild(Module.externalizeType(doc, this.type));
    return attrDefNode;
  }

  void checkCompat(Module.ModTab modTab, MAttrDef ad, Module.ModTab defModTab) throws FormatException {
    StringBuffer emsg;
    if (this.type.isCompatible(modTab, ad.type, defModTab)) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible attribute - data constructor: ");
      emsg.append(this.theConstrDef.dcon);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
  }
}
