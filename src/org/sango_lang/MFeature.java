/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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


class MFeature {
  int modIndex;
  String name;
  MType[] params;

  private MFeature() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    // buf.append("[ ");
    String sp = "";
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sp);
      buf.append(this.params[i]);
      sp = " ";
    }
    buf.append(sp);
    buf.append(this.modIndex);
    buf.append(".");
    buf.append(this.name);
    // buf.append(" ]");
    return buf.toString();
  }

  public Element externalize(Document doc) {
    Element node = doc.createElement(Module.TAG_FEATURE);
    node.setAttribute(Module.ATTR_NAME, this.name);
    if (this.modIndex != 0) {
      node.setAttribute(Module.ATTR_MOD_INDEX, Integer.toString(this.modIndex));
    }
    if (this.params != null) {
      for (int i = 0; i < this.params.length; i++) {
        node.appendChild(this.params[i].externalize(doc));
      }
    }
    return node;
  }

  public static MFeature internalize(Node node) throws FormatException {
    if (!node.getNodeName().equals(Module.TAG_FEATURE)) { return null; }

    Builder builder = Builder.newInstance();

    NamedNodeMap attrs = node.getAttributes();

    int modIndex = 0;
    Node aModIndex = attrs.getNamedItem(Module.ATTR_MOD_INDEX);
    if (aModIndex != null) {
      modIndex = Module.parseInt(aModIndex.getNodeValue());
    }
    builder.setModIndex(modIndex);

    Node aName = attrs.getNamedItem(Module.ATTR_NAME);
    if (aName == null) {
      throw new FormatException("'name' not found.");
    }
    builder.setName(aName.getNodeValue());

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
    MFeature feature;
    java.util.List<MType> paramList;

    private Builder() {
      this.feature = new MFeature();
      this.paramList = new ArrayList<MType>();
    }

    public static Builder newInstance() { return new Builder(); }

    public void setModIndex(int modIndex) {
      this.feature.modIndex = modIndex;
    }

    public void setName(String name) {
      this.feature.name = name;
    }

    public void addParam(MType t) {
      this.paramList.add(t);
    }

    public MFeature create() {
      this.feature.params = this.paramList.toArray(new MType[this.paramList.size()]);
      return this.feature;
    }
  }

  public boolean isCompatible(Module.ModTab modTab, MFeature feature, Module.ModTab defModTab) {
    boolean b;
    if (!defModTab.get(feature.modIndex).equals(modTab.get(this.modIndex))) {
      b = false;
    } else if (!feature.name.equals(this.name)) {
      b = false;
    } else {
      b = true;
      for (int i = 0; b && i < this.params.length; i++) {
        b = this.params[i].isCompatible(modTab, feature.params[i], defModTab);
      }
    }
    return b;
  }

  static class List {
    MFeature[] features;

    static List create() {
      List L = new List();
      L.features = new MFeature[0];
      return L;
    }

    static List create(java.util.List<MFeature> featureList) {
      List L = new List();
      L.features = featureList.toArray(new MFeature[featureList.size()]);
      return L;
    }

    private List() {}

    public String toString() {
      String s;
      if (this.features.length == 0) {
        s = "";
      } else {
        StringBuffer buf = new StringBuffer();
        buf.append("[ ");
        String sep = "";
        for (int i = 0; i < this.features.length; i++) {
          buf.append(sep);
          buf.append(this.features[i].toString());
          sep = ",";
        }
        buf.append(" ]");
        s = buf.toString();
      }
      return s;
    }

    public static List internalize(Node node) throws FormatException {
      if (!node.getNodeName().equals(Module.TAG_FEATURES)) { return null; }
      Node n = node.getFirstChild();
      java.util.List<MFeature> fs = new ArrayList<MFeature>();
      MFeature f = null;
      while (n != null) {
        if (Module.isIgnorable(n)) {
          ;
        } else {
          f = MFeature.internalize(n);
	  if (f == null) {
            throw new FormatException("Unknown element : " + n.getNodeName());
	  }
	  fs.add(f);
        }
        n = n.getNextSibling();
      }
      return create(fs);
      // return (fs.size() > 0)? create(fs): null;
    }

    public Element externalize(Document doc) {
      Element e = doc.createElement(Module.TAG_FEATURES);
      for (int i = 0; i < this.features.length; i++) {
        e.appendChild(this.features[i].externalize(doc));
      }
      return e;
    }
  }
}
