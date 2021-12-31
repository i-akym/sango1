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
  private static final int ACCEPTABLE_NONE = 0;
  private static final int ACCEPTABLE_ID = 1;
  private static final int ACCEPTABLE_VARDEF = 2;
  private static final int ACCEPTABLE_TYPE = 4;
  private static final int ACCEPTABLE_BOUND = 8;

  private static final int[] acceptable_tab = new int[] {
    /* 0 */ ACCEPTABLE_NONE,  // no more
    /* 1 */ ACCEPTABLE_ID + ACCEPTABLE_VARDEF + ACCEPTABLE_TYPE,  // -> 3
    /* 2 */ ACCEPTABLE_ID + ACCEPTABLE_TYPE,  // -> 2
    /* 3 */ ACCEPTABLE_ID + ACCEPTABLE_VARDEF + ACCEPTABLE_TYPE + ACCEPTABLE_BOUND,  // -> 3; 4 if bound
    /* 4 */ ACCEPTABLE_VARDEF  // -> 0
  };

  static final int INHIBIT_REQUIRE_CONCRETE = 0;
  static final int ALLOW_REQUIRE_CONCRETE = 1;

  static class Builder {
    Parser.SrcInfo srcInfo;
    List<PTypeDesc> itemList;
    PTVarDef bound;

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

    void setBound(PTVarDef varDef) {
      this.bound = varDef;
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
        if (!id.maybeVar() || this.itemList.size() > 0 || this.bound != null) {
          // id.cutOffCatOpt(PTypeId.CAT_VAR);
          t = PTypeRef.create(this.srcInfo, id, this.itemList.toArray(new PTypeDesc[this.itemList.size()]), this.bound);
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
    return accept(reader, spc, true);
  }

  static PTypeDesc acceptRO(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return accept(reader, spc, false);
  }

  private static PTypeDesc accept(ParserA.TokenReader reader, int spc, boolean acceptsVarDef) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LT, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    PTypeDesc item;
    int state = acceptsVarDef? 1: 2;
    int sp = ParserA.SPACE_DO_NOT_CARE;
    while (state > 0) {
      if ((item = acceptItem(reader, sp, acceptsVarDef, acceptable_tab[state])) != null) {
        switch (state) {
        case 1:
          builder.addItem(item);
          state = 3;
          sp = ParserA.SPACE_NEEDED;
          break;
        case 2:
          builder.addItem(item);
          sp = ParserA.SPACE_NEEDED;
          break;
        case 3:
          if (item instanceof Bound) {
            sp = ParserA.SPACE_DO_NOT_CARE;;
            state = 4;
          } else {
            builder.addItem(item);
            sp = ParserA.SPACE_NEEDED;
          }
          break;
        case 4:
          builder.setBound((PTVarDef)item);
          state = 0;
          break;
        default:
          throw new RuntimeException("Should not reach here.");
        }
      } else {
        state = 0;
      }
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

  static PTypeDesc acceptX(ParserB.Elem elem) throws CompileException {
    return acceptX(elem, ACCEPTABLE_VARDEF);
  }

  static PTypeDesc acceptXRO(ParserB.Elem elem) throws CompileException {
    return acceptX(elem, ACCEPTABLE_NONE);
  }

  private static PTypeDesc acceptX(ParserB.Elem elem, int acceptables) throws CompileException {
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

  static PTypeDesc acceptSig1(ParserA.TokenReader reader, int qual) throws CompileException, IOException {
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
        if ((tr.params[i] instanceof PTVarDef)) {
          ;
        } else if ((tr.params[i] instanceof PTypeRef)) {
          PTypeRef ptr = (PTypeRef)tr.params[i];
          if (ptr.bound == null) {
            emsg = new StringBuffer();
            emsg.append("Bound variable missing at ");
            emsg.append(tr.tconSrcInfo);
            emsg.append(".");
            throw new CompileException(emsg.toString());
          }
        } else {
          emsg = new StringBuffer();
          emsg.append("Type parameter missing at ");
          emsg.append(tr.params[i].getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
      if (qual == PExprId.ID_NO_QUAL && tr.mod != null) {
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
      if (qual == PExprId.ID_NO_QUAL && ti.mod != null) {
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

  static PTypeDesc acceptSig2(ParserA.TokenReader reader) throws CompileException, IOException {
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
        PTVarDef v = (PTVarDef)tr.params[i];
        if (v.requiresConcrete) {
          emsg = new StringBuffer();
          emsg.append("Requiring concrete type is not allowed at ");
          emsg.append(tr.params[i].getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
      if (tr.mod != null) {
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
      if (ti.mod != null) {
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

  static PTypeDesc acceptItem(ParserA.TokenReader reader, int spc, boolean acceptsVarDef, int acceptables) throws CompileException, IOException {
    StringBuffer emsg;
    PTypeId id;
    PTVarDef var;
    PType type;
    PTypeDesc item;
    if ((acceptables & ACCEPTABLE_ID) > 0
        && (item = PTypeId.accept(reader, PExprId.ID_MAYBE_QUAL, spc)) != null) {
      ;
    } else if ((acceptables & ACCEPTABLE_VARDEF) > 0
        && (item = PTVarDef.accept(reader)) != null) {
      ;
    } else if ((acceptables & ACCEPTABLE_TYPE) > 0
        && (item = accept(reader, spc, acceptsVarDef)) != null) {
      ;
    } else if ((acceptables & ACCEPTABLE_BOUND) > 0
        && (item = Bound.accept(reader)) != null) {
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

  static class Bound extends PDefaultProgElem implements PTypeDesc {
    private Bound() {}

    static Bound accept(ParserA.TokenReader reader) throws CompileException, IOException {
      ParserA.Token t;
      if ((t = ParserA.acceptToken(reader, LToken.EQ, ParserA.SPACE_DO_NOT_CARE)) == null) {
        return null;
      }
      return new Bound();
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("bound");
      return buf.toString();
    }

    public PTypeDesc setupScope(PScope scope) throws CompileException {
      throw new RuntimeException("Bound#setupScope is called.");
    }

    public PTVarDef deepCopy(Parser.SrcInfo srcInfo, int extOpt, int varianceOpt, int concreteOpt) {
      throw new RuntimeException("Bound#deepCopy is called.");
    }

    public PTypeId resolveId() throws CompileException {
      throw new RuntimeException("Bound#resolveId is called.");
    }

    public PDefDict.TconInfo getTconInfo() {
      throw new RuntimeException("Bound#getTconInfo is called.");
    }

    public void excludePrivateAcc() throws CompileException {
      throw new RuntimeException("Bound#excludePrivateAcc is called.");
    }

    public void checkRequiringConcreteIn() throws CompileException {
      throw new RuntimeException("Bound#checkRequiringConcreteIn is called.");
    }

    public void checkRequiringConcreteOut() throws CompileException {
      throw new RuntimeException("Bound#checkRequiringConcreteOut is called.");
    }

    public void normalizeTypes() {
      throw new RuntimeException("Bound#normalizeTypes is called.");
    }

    public PTypeSkel normalize() {
      throw new RuntimeException("Bound#normalize is called.");
    }

    public PTypeSkel getSkel() {
      throw new RuntimeException("Bound#getSkel is called.");
    }
  }
}
