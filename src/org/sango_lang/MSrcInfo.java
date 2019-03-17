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
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public class MSrcInfo implements Module.Elem {
  int upperCodeIndex;
  String locInfo;

  private MSrcInfo() {}

  static MSrcInfo create(int upperCodeIndex, String locInfo) {
    MSrcInfo si = new MSrcInfo();
    si.upperCodeIndex = upperCodeIndex;
    si.locInfo = locInfo;
    return si;
  }

  static MSrcInfo internalize(Node node) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node au = attrs.getNamedItem(Module.ATTR_U);
    if (au == null) {
      throw new FormatException("'" + Module.ATTR_U + "' attribute not found.");
    }
    Node av = attrs.getNamedItem(Module.ATTR_V);
    if (av == null) {
      throw new FormatException("'" + Module.ATTR_V + "' attribute not found.");
    }
    int u = Module.parseInt(au.getNodeValue());
    return MSrcInfo.create(u, av.getNodeValue());
  }

  public Element externalize(Document doc) {
    Element infoNode = doc.createElement(Module.TAG_S);
    infoNode.setAttribute(Module.ATTR_U, Integer.toString(this.upperCodeIndex));
    infoNode.setAttribute(Module.ATTR_V, this.locInfo);
    return infoNode;
  }
}
