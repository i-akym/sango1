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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


class MTypeRef implements MType {
  Cstr modName;
  String tcon;
  boolean ext;
  MType[] params;
  // MTypeVar bound;

  private MTypeRef() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<");
    String sp = "";
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sp);
      buf.append(this.params[i]);
      sp = " ";
    }
    buf.append(sp);
    if (this.modName != null) {
      buf.append(this.modName.toJavaString());
      buf.append(".");
    }
    buf.append(tcon);
    if (this.ext) {
      buf.append("+");
    }
    // if (this.bound != null) {
      // buf.append("=");
      // buf.append(bound.toString());
    // }
    buf.append(">");
    return buf.toString();
  }

  public Element externalize(Document doc) {
    Element node = doc.createElement(Module.TAG_TYPE_REF);
    node.setAttribute(Module.ATTR_TCON, this.tcon);
    if (this.modName != null) {
      node.setAttribute(Module.ATTR_MOD, this.modName.toJavaString());
    }
    if (this.ext) {
      node.setAttribute(Module.ATTR_EXT, "yes");
    }
    if (this.params != null) {
      for (int i = 0; i < this.params.length; i++) {
        node.appendChild(this.params[i].externalize(doc));
      }
    }
    // if (this.bound != null) {
      // Element nb = doc.createElement(Module.TAG_BOUND);
      // nb.appendChild(this.bound.externalize(doc));
      // node.appendChild(nb);
    // }
    return node;
  }

  public static MTypeRef internalize(Node node) throws FormatException {
    if (!node.getNodeName().equals(Module.TAG_TYPE_REF)) { return null; }

    Builder builder = Builder.newInstance();

    NamedNodeMap attrs = node.getAttributes();

    Cstr modName = null;
    Node aModName = attrs.getNamedItem(Module.ATTR_MOD);
    if (aModName != null) {
      modName = new Cstr(aModName.getNodeValue());
    }
    builder.setModName(modName);

    Node aTcon = attrs.getNamedItem(Module.ATTR_TCON);
    if (aTcon == null) {
      throw new FormatException("'tcon' not found.");
    }
    builder.setTcon(aTcon.getNodeValue());

    boolean ext = false;  // default
    Node aExt = attrs.getNamedItem(Module.ATTR_EXT);
    if (aExt != null) {
      String sExt = aExt.getNodeValue();
      if (sExt.equals(Module.REPR_YES)) {
        ext = true;
      } else if (sExt.equals(Module.REPR_NO)) {
        ;
      } else {
        throw new FormatException("Invalid type extention: " + sExt);
      }
    }
    builder.setExt(ext);

    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (Module.isIgnorable(n)) {
        ;
      } else {
        type = MType.Envelope.internalizeDesc(n);
	if (type == null) {
          throw new FormatException("Unknown element : " + n.getNodeName());
	}
	builder.addParam(type);
      }
      n = n.getNextSibling();
    }

    return builder.create();
  }

  public static class Builder {
    MTypeRef typeRef;
    List<MType> paramList;

    private Builder() {
      this.typeRef = new MTypeRef();
      this.paramList = new ArrayList<MType>();
    }

    public static Builder newInstance() { return new Builder(); }

    public void setModName(Cstr modName) {
      this.typeRef.modName = modName;
    }

    public void setTcon(String tcon) {
      this.typeRef.tcon = tcon;
    }

    public void setExt(boolean b) {
      this.typeRef.ext = b;
    }

    public void addParam(MType t) {
      this.paramList.add(t);
    }

    public MTypeRef create() {
      this.typeRef.params = this.paramList.toArray(new MType[this.paramList.size()]);
      return this.typeRef;
    }
  }

  public boolean isCompatible(Cstr defModName, MType type) {
    boolean b;
    if (!(type instanceof MTypeRef)) {
      b = false;
    } else {
      MTypeRef r = (MTypeRef)type;
      Cstr m = (r.modName != null)? r.modName: defModName;
      if (!m.equals(this.modName)) {
        b = false;
      } else if (!r.tcon.equals(this.tcon)) {
        b = false;
      } else if (!r.ext && this.ext) {
        b = false;
      } else {
        b = true;
        for (int i = 0; b && i < this.params.length; i++) {
          b = this.params[i].isCompatible(defModName, r.params[i]);
        }
      }
    }
    return b;
  }
}
