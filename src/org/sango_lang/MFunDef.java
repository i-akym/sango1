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

public class MFunDef implements Module.Elem {
  String name;
  int availability;
  int acc;
  String[] aliases;
  MType[] paramTypes;
  MType retType;

  private MFunDef() {}

  static class Builder {
    MFunDef funDef;
    List<String> aliasList;
    List<MType> paramTypeList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.funDef = new MFunDef();
      this.aliasList = new ArrayList<String>();
      this.paramTypeList = new ArrayList<MType>();
    }

    void setName(String name) {
      this.funDef.name = name;
    }

    void setAvailability(int availability) {
      this.funDef.availability = availability;
    }

    void setAcc(int acc) {
      this.funDef.acc = acc;
    }

    void addAlias(String name) {
      this.aliasList.add(name);
    }

    void addParamType(MType type) {
      this.paramTypeList.add(type);
    }

    void setRetType(MType type) {
      this.funDef.retType = type;
    }

    MFunDef create() {
      this.funDef.aliases = this.aliasList.toArray(new String[this.aliasList.size()]);
      this.funDef.paramTypes = this.paramTypeList.toArray(new MType[this.paramTypeList.size()]);
      return this.funDef;
    }
  }

  public Element externalize(Document doc) {
    Element funDefNode = doc.createElement(Module.TAG_FUN_DEF);
    funDefNode.setAttribute(Module.ATTR_NAME, this.name);
    if (this.availability != Module.AVAILABILITY_GENERAL) {
      funDefNode.setAttribute(Module.ATTR_AVAILABILITY, Module.reprOfAvailability(this.availability));
    }
    if (this.acc != Module.ACC_PRIVATE) {
      funDefNode.setAttribute(Module.ATTR_ACC, Module.reprOfAcc(this.acc));
    }
    if (this.aliases.length > 0) {
      Element aliasesNode = doc.createElement(Module.TAG_ALIASES);
      funDefNode.appendChild(aliasesNode);
      for (int j = 0; j < this.aliases.length; j++) {
        Element aliasNode = doc.createElement(Module.TAG_ALIAS);
        aliasesNode.appendChild(aliasNode);
        aliasNode.setAttribute(Module.ATTR_NAME, this.aliases[j]);
      }
    }
    if (this.paramTypes.length > 0) {
      Element paramsNode = doc.createElement(Module.TAG_PARAMS);
      funDefNode.appendChild(paramsNode);
      for (int j = 0; j < this.paramTypes.length; j++) {
        Element paramNode = doc.createElement(Module.TAG_PARAM);
        paramsNode.appendChild(paramNode);
        paramNode.appendChild(Module.externalizeType(doc, this.paramTypes[j]));
      }
    }
    Element retNode = doc.createElement(Module.TAG_RET);
    funDefNode.appendChild(retNode);
    retNode.appendChild(Module.externalizeType(doc, this.retType));
    return funDefNode;
  }

  void checkCompat(Module.ModTab modTab, MFunDef fd, Module.ModTab defModTab) throws FormatException {
    StringBuffer emsg;
    if (Module.equalOrMoreOpenAcc(fd.acc, this.acc)) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible access mode - function: ");
      emsg.append(fd.name);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    if (fd.paramTypes.length == this.paramTypes.length) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible parameter count - function: ");
      emsg.append(this.name);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
    for (int i = 0; i < this.paramTypes.length; i++) {
      if (this.paramTypes[i].isCompatible(modTab, fd.paramTypes[i], defModTab)) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Incompatible parameter - function: ");
        emsg.append(this.name);
        emsg.append(", referred in: ");
        emsg.append(modTab.getMyModName().repr());
        emsg.append(" defined in: ");
        emsg.append(defModTab.getMyModName().repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
    }
    if (this.retType.isCompatible(modTab, fd.retType, defModTab)) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Incompatible return value - function: ");
      emsg.append(this.name);
      emsg.append(", referred in: ");
      emsg.append(modTab.getMyModName().repr());
      emsg.append(" defined in: ");
      emsg.append(defModTab.getMyModName().repr());
      emsg.append(".");
      throw new FormatException(emsg.toString());
    }
  }
}
