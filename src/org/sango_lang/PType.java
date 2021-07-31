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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class PType {
  static final int ACCEPTABLE_NONE = 0;
  static final int ACCEPTABLE_VARDEF = 1;

  static class Builder {
    Parser.SrcInfo srcInfo;
    List<PTypeDesc> itemList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.itemList = new ArrayList<PTypeDesc>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.srcInfo = si;
    }

    void addItem(PTypeDesc item) {
      this.itemList.add(item);
    }

    PTypeDesc create() throws CompileException {
      StringBuffer emsg;
      PTypeDesc t = null;
      if (this.itemList.isEmpty()) {
        emsg = new StringBuffer();
        emsg.append("Empty type description at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeDesc anchor = this.itemList.remove(this.itemList.size() - 1);
      if (anchor instanceof PTypeId) {
        PTypeId id = (PTypeId)anchor;
        if (!id.maybeVar() || this.itemList.size() > 0) {
          // id.cutOffCatOpt(PTypeId.CAT_VAR);
          t = PTypeRef.create(this.srcInfo, id, this.itemList.toArray(new PTypeDesc[this.itemList.size()]));
        } else {
          t = anchor;
        }
      } else if (anchor instanceof PTVarDef) {
        if (this.itemList.size() > 0) {
          emsg = new StringBuffer();
          emsg.append("No type constructor at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        t = anchor;
      } else if (anchor instanceof PTypeRef) {
        if (this.itemList.size() > 0) {
          emsg = new StringBuffer();
          emsg.append("No type constructor at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        t = anchor;
      } else {
        throw new IllegalArgumentException("Invalid item");
      }
      return t;
    }
  }

  static PTypeDesc accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return accept(reader, spc, ACCEPTABLE_VARDEF);
  }

  static PTypeDesc acceptRO(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return accept(reader, spc, ACCEPTABLE_NONE);
  }

  static PTypeDesc accept(ParserA.TokenReader reader, int spc, int acceptables) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LT, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    PTypeDesc item;
    int sp = ParserA.SPACE_DO_NOT_CARE;
    while ((item = acceptItem(reader, sp, acceptables)) != null) {
      builder.addItem(item);
      sp = ParserA.SPACE_NEEDED;
    }
    if (ParserA.acceptToken(reader, LToken.GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Syntax error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      ParserA.Token et = reader.getToken();
      if (et.value != null) {
        emsg.append(" - ");
        emsg.append(et.value.token);
      }
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PTypeDesc acceptXRO(ParserB.Elem elem) throws CompileException {
    return acceptX(elem, ACCEPTABLE_NONE);
  }

  static PTypeDesc acceptX(ParserB.Elem elem, int acceptables) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("type-spec")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Type description missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    Builder builder = Builder.newInstance();
    builder.setSrcInfo(e.getSrcInfo());
    builder.addItem(acceptXItem(e, acceptables));
    return builder.create();
  }

  static PTypeDesc acceptSig(ParserA.TokenReader reader, int qual) throws CompileException, IOException {
    StringBuffer emsg;
    PTypeDesc sig = accept(reader, ParserA.SPACE_DO_NOT_CARE);
    if (sig instanceof PTVarDef) {
      emsg = new StringBuffer();
      emsg.append("Type constructor missing at ");
      emsg.append(sig.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (sig instanceof PTypeRef) {
      PTypeRef tr = (PTypeRef)sig;
      for (int i = 0; i < tr.params.length; i++) {
        if (!(tr.params[i] instanceof PTVarDef)) {
          emsg = new StringBuffer();
          emsg.append("Type parameter missing at ");
          emsg.append(tr.params[i].getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
      if (qual == PExprId.ID_NO_QUAL && (/* tr.omod != null || */ tr.mod != null)) {
        emsg = new StringBuffer();
        emsg.append("Module id not allowed at ");
        emsg.append(tr.tconSrcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (tr.ext) {
        emsg = new StringBuffer();
        emsg.append("Extension not allowed at ");
        emsg.append(tr.tconSrcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    } else if (sig instanceof PTypeId) {
      PTypeId ti = (PTypeId)sig;
      if (qual == PExprId.ID_NO_QUAL && (/* ti.omod != null || */ ti.mod != null)) {
        emsg = new StringBuffer();
        emsg.append("Module id not allowed at ");
        emsg.append(ti.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (ti.ext) {
        emsg = new StringBuffer();
        emsg.append("Extension not allowed at ");
        emsg.append(ti.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    } else {
      throw new RuntimeException("Unexpected type.");
    }
    return sig;
  }

  static PTypeDesc acceptItem(ParserA.TokenReader reader, int spc, int acceptables) throws CompileException, IOException {
    StringBuffer emsg;
    PTypeId id;
    PTVarDef var;
    PType type;
    PTypeDesc item;
    if ((item = PTypeId.accept(reader, PExprId.ID_MAYBE_QUAL, spc)) != null) {
      ;
    } else if (((acceptables & ACCEPTABLE_VARDEF) > 0)
        && (item = PTVarDef.accept(reader)) != null) {
      ;
    } else if ((item = accept(reader, spc, acceptables)) != null) {
      ;
    } else {
      item = null;
    }
    return item;
  }

  static PTypeDesc acceptXItem(ParserB.Elem elem, int acceptables) throws CompileException {
    PTypeDesc item;
    if ((item = PTypeRef.acceptX(elem, acceptables)) != null) {
      ;
    } else if ((item = PTVarRef.acceptXTvar(elem)) != null) {
      ;
    } else if (((acceptables & ACCEPTABLE_VARDEF) > 0)
        && (item = PTVarDef.acceptX(elem)) != null) {
      ;
    } else {
      item = null;
    }
    return item;
  }
}
