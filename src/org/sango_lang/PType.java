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

interface PType extends PProgObj {
  PType resolve() throws CompileException;

  PType unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt);
  static final int COPY_EXT_KEEP = -1;
  static final int COPY_EXT_OFF = 0;
  static final int COPY_EXT_ON = 1;
  // static final int COPY_VARIANCE_KEEP = -1;
  // static final int COPY_VARIANCE_CUT = 0;
  // static final int COPY_VARIANCE_INVARIANT = 1;
  // static final int COPY_VARIANCE_COVARIANT = 2;
  // static final int COPY_VARIANCE_CONTRAVARIANT = 3;
  static final int COPY_CONCRETE_KEEP = -1;
  static final int COPY_CONCRETE_OFF = 0;
  static final int COPY_CONCRETE_ON = 1;

  PDefDict.TconProps getTconProps();

  void excludePrivateAcc() throws CompileException;

  PTypeSkel toSkel();

  PTypeSkel getNormalizedSkel() throws CompileException;

  static final int INHIBIT_REQUIRE_CONCRETE = 0;
  static final int ALLOW_REQUIRE_CONCRETE = 1;

  // internal
  static final int ACCEPTABLE_NONE = 0;
  static final int ACCEPTABLE_ID = 1;
  static final int ACCEPTABLE_VARDEF = 2;
  static final int ACCEPTABLE_TYPE = 4;
  static final int ACCEPTABLE_BOUND = 8;

  static class Builder {

    private static final int[] acceptable_tab = new int[] {
      /* 0 */ ACCEPTABLE_NONE,  // no more
      /* 1 */ ACCEPTABLE_ID + ACCEPTABLE_VARDEF + ACCEPTABLE_TYPE,  // -> 3
      /* 2 */ ACCEPTABLE_ID + ACCEPTABLE_TYPE,  // -> 2
      /* 3 */ ACCEPTABLE_ID + ACCEPTABLE_VARDEF + ACCEPTABLE_TYPE /* + ACCEPTABLE_BOUND, */  // -> 3; 4 if constrained var
      // /* 4 */ ACCEPTABLE_VARDEF  // -> 0
    };

    Parser.SrcInfo srcInfo;
    PScope scope;
    List<PProgObj> itemList;
    // PTypeVarDef constrainedVar;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope scope) {
      return new Builder(srcInfo, scope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope scope) {
      this.srcInfo = srcInfo;
      this.scope = scope;
      this.itemList = new ArrayList<PProgObj>();
    }

    void addItem(PProgObj item) {
      this.itemList.add(item);
    }

    // void setConstrainedVar(PTypeVarDef var) {
      // this.constrainedVar = var;
    // }

    PType create() throws CompileException {
      StringBuffer emsg;
      PType t = null;
      if (this.itemList.isEmpty()) {
        emsg = new StringBuffer();
        emsg.append("Empty type description at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PProgObj anchor = this.itemList.remove(this.itemList.size() - 1);
      if (anchor instanceof PType.Undet) {
        anchor = ((PType.Undet)anchor).id;
      }
      if (anchor instanceof PTypeId) {
        PTypeId id = (PTypeId)anchor;
        if (!id.maybeVar() || this.itemList.size() > 0) {
          PType[] ps = new PType[this.itemList.size()];
          for (int i = 0; i < this.itemList.size(); i++) {
            ps[i] = progObjToType(this.itemList.get(i));
          }
          t = PTypeRef.create(this.srcInfo, this.scope, id, ps);
        } else {
          t = Undet.create(id);
        }
      } else if (anchor instanceof PTypeVarDef) {
        if (this.itemList.size() > 0) {
          emsg = new StringBuffer();
          emsg.append("No type constructor at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        t = (PTypeVarDef)anchor;
      } else if (anchor instanceof PTypeRef) {
        if (this.itemList.size() > 0) {
          emsg = new StringBuffer();
          emsg.append("No type constructor at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        t = (PTypeRef)anchor;
      } else {
        throw new IllegalArgumentException("Invalid item " + anchor.toString());
      }
      // if (this.constrainedVar != null) {
        // this.constrainedVar.constraint = t;
        // t = this.constrainedVar;
      // }
      return t;
    }
  }

  static PType accept(ParserA.TokenReader reader, PScope scope, int spc) throws CompileException, IOException {
    return accept(reader, scope, spc, true);
  }

  static PType acceptRO(ParserA.TokenReader reader, PScope scope, int spc) throws CompileException, IOException {
    return accept(reader, scope, spc, false);
  }

  static PType accept(ParserA.TokenReader reader, PScope scope, int spc, boolean acceptsVarDef) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LT, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), scope);
    PProgObj item;
    int state = acceptsVarDef? 1: 2;
    int sp = ParserA.SPACE_DO_NOT_CARE;
    while (state > 0) {
      if ((item = acceptItem(reader, scope, sp, acceptsVarDef, Builder.acceptable_tab[state])) != null) {
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
          // if (item instanceof Bound) {
            // sp = ParserA.SPACE_DO_NOT_CARE;;
            // state = 4;
          // } else {
            builder.addItem(item);
            sp = ParserA.SPACE_NEEDED;
          // }
          break;
        // case 4:
          // builder.setConstrainedVar((PTypeVarDef)item);
          // state = 0;
          // break;
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

  static PType acceptX(ParserB.Elem elem, PScope scope) throws CompileException {
    return acceptX(elem, scope, ACCEPTABLE_VARDEF);
  }

  static PType acceptXRO(ParserB.Elem elem, PScope scope) throws CompileException {
    return acceptX(elem, scope, ACCEPTABLE_NONE);
  }

  static PType acceptX(ParserB.Elem elem, PScope scope, int acceptables) throws CompileException {
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

    Builder builder = Builder.newInstance(e.getSrcInfo(), scope);
    builder.addItem(acceptXItem(e, scope, acceptables));
    return builder.create();
  }

  static PTypeRef acceptSig(ParserA.TokenReader reader, PScope scope,
    Option.Set<Parser.QualState> qual) throws CompileException, IOException {
    StringBuffer emsg;
    PType t = accept(reader, scope, ParserA.SPACE_DO_NOT_CARE);
    if (t instanceof Undet) {
      Undet u = (Undet)t;
      t = PTypeRef.create(u.srcInfo, scope, u.id, new PType[0]);
    }
    if (!(t instanceof PTypeRef)) {
      emsg = new StringBuffer();
      emsg.append("Invalid signature definition at ");
      emsg.append(t.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeRef sig = (PTypeRef)t;
    for (int i = 0; i < sig.params.length; i++) {
      if (!(sig.params[i] instanceof PTypeVarDef)) {
        emsg = new StringBuffer();
        emsg.append("Type parameter missing at ");
        emsg.append(sig.params[i].getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeVarDef v = (PTypeVarDef)sig.params[i];
      // if (v.constraint != null) {
        // emsg = new StringBuffer();
        // emsg.append("Constrained type parameter not allowed at ");
        // emsg.append(sig.params[i].getSrcInfo());
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
    }
    if (!qual.contains(Parser.WITH_QUAL) && sig.tcon.modId != null) {
      emsg = new StringBuffer();
      emsg.append("Module id not allowed at ");
      emsg.append(sig.tcon.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (sig.tcon.ext) {
      emsg = new StringBuffer();
      emsg.append("Extension not allowed at ");
      emsg.append(sig.tcon.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return sig;
  }

  static PProgObj acceptItem(ParserA.TokenReader reader, PScope scope, int spc, boolean acceptsVarDef, int acceptables) throws CompileException, IOException {
    PProgObj item;
    if ((acceptables & ACCEPTABLE_ID) > 0
        && (item = PTypeId.accept(reader, scope, Parser.QUAL_MAYBE, spc)) != null) {
      ;
    } else if ((acceptables & ACCEPTABLE_VARDEF) > 0
        && (item = PTypeVarDef.accept(reader, scope)) != null) {
      ;
    } else if ((acceptables & ACCEPTABLE_TYPE) > 0
        && (item = accept(reader, scope, spc, acceptsVarDef)) != null) {
      ;
    // } else if ((acceptables & ACCEPTABLE_BOUND) > 0
        // && (item = Bound.accept(reader, scope)) != null) {
      // ;
    } else {
      item = null;
    }
    return item;
  }

  static PProgObj acceptXItem(ParserB.Elem elem, PScope scope, int acceptables) throws CompileException {
    PProgObj item;
    if ((item = PTypeRef.acceptX(elem, scope, acceptables)) != null) {
      ;
    } else if ((item = PTypeVarRef.acceptXTvar(elem, scope)) != null) {
      ;
    } else if (((acceptables & ACCEPTABLE_VARDEF) > 0)
        && (item = PTypeVarDef.acceptX(elem, scope)) != null) {
      ;
    } else {
      item = null;
    }
    return item;
  }

  static PType voidType(Parser.SrcInfo srcInfo, PScope scope) {
    Builder builder = Builder.newInstance(srcInfo, scope);
    builder.addItem(PTypeId.create(srcInfo, scope, PModule.MOD_ID_LANG, "void", false));
    PType v = null;
    try {
      v = builder.create();
    } catch (CompileException ex) {}  // not reached
    return v;
  }

  static PType progObjToType(PProgObj o) {
    PType t;
    if (o instanceof PTypeVarDef) {
      t = (PTypeVarDef)o;
    } else if (o instanceof PTypeVarRef) {
      t = (PTypeVarRef)o;
    } else if (o instanceof PTypeId) {
      t = Undet.create((PTypeId)o);
    } else if (o instanceof PTypeRef) {
      t = (PTypeRef)o;
    } else if (o instanceof Undet) {
      t = (Undet)o;
    } else {
      throw new IllegalArgumentException("Invalid type. " + o.toString());
    }
    return t;
  }

  static class Undet extends PDefaultProgObj implements PType {
    PTypeId id;

    private Undet(Parser.SrcInfo srcInfo, PScope scope) {
      super(srcInfo, scope);
    }

    static Undet create(PTypeId id) {
      Undet u = new Undet(id.srcInfo, id.scope);
      u.id = id;
      return u;
    }

    public void collectModRefs() throws CompileException {
      this.id.collectModRefs();
    }

    public PType resolve() throws CompileException {
      StringBuffer emsg;
      /* DEBUG */ if (this.scope == null || this.scope.pos == 0) { System.out.print("Scope is null or inactive. "); System.out.println(this); }
      PType t;
      if (this.id.modId == null) {
        PTypeVarDef v;
        if ((v = this.scope.lookupTVar(this.id.name)) != null) {
          t = PTypeVarRef.create(this.id.srcInfo, this.id.scope, v);
          t = t.resolve();
        } else if (this.scope.resolveTcon(this.id.modId, this.id.name) != null) {
          t = PTypeRef.create(this.id.srcInfo, this.id.scope, this.id, new PType[0]);
          t = t.resolve();
        } else {
          emsg = new StringBuffer();
          emsg.append("Type constructor \"");
          emsg.append(this.id.name);
          emsg.append("\" not defined at ");
          emsg.append(this.id.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      } else if (this.scope.resolveTcon(this.id.modId, this.id.name) != null) {
        t = PTypeRef.create(this.id.srcInfo, this.id.scope, this.id, new PType[0]);
        t = t.resolve();
      } else {
        emsg = new StringBuffer();
        emsg.append("Type constructor \"");
        emsg.append(PTypeId.repr(this.id.modId, this.id.name, false));
        emsg.append("\" not defined at ");
        emsg.append(this.id.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return t;
    }

    public PDefDict.TconProps getTconProps() {
      throw new RuntimeException("Undet#getTconProps is called.");
    }

    public void excludePrivateAcc() throws CompileException {
      throw new RuntimeException("Undet#excludePrivateAcc is called.");
    }

    public PTypeSkel toSkel() {
      throw new RuntimeException("Undet#toSkel is called.");
    }

    public PTypeSkel getNormalizedSkel() {
      throw new RuntimeException("Undet#getNormalizedSkel is called.");
    }

    public PType unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
      boolean ext;
      if (extOpt == COPY_EXT_KEEP) {
        ext = this.id.ext;
      } else if (extOpt == COPY_EXT_OFF) {
        ext = false;
      } else if (extOpt == COPY_EXT_ON) {
        ext = true;
      } else {
        throw new IllegalArgumentException("Unknown extOpt.");
      }
      return Undet.create(PTypeId.create(srcInfo, scope, this.id.modId, this.id.name, ext));
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      if (this.srcInfo != null) {
        buf.append("undet[src=");
        buf.append(this.srcInfo);
        buf.append(",");
      }
      String sep = "";
      buf.append("<");
      buf.append(this.id);
      buf.append(">");
      return buf.toString();
    }
  }

  // // pseudo object
  // static class Bound extends PDefaultProgObj {
    // private Bound(Parser.SrcInfo srcInfo, PScope scope) {
      // super(srcInfo, scope);
    // }

    // static Bound accept(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
      // ParserA.Token t;
      // if ((t = ParserA.acceptToken(reader, LToken.EQ, ParserA.SPACE_DO_NOT_CARE)) == null) {
        // return null;
      // }
      // return new Bound(t.getSrcInfo(), scope);
    // }

    // public String toString() {
      // StringBuffer buf = new StringBuffer();
      // buf.append("bound");
      // return buf.toString();
    // }

    // public Bound unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
      // throw new RuntimeException("Bound#unresolvedCopy is called.");
    // }

    // public void collectModRefs() throws CompileException {
      // throw new RuntimeException("Bound#collectModRefs is called.");
    // }

    // public PTypeId resolve() throws CompileException {
      // throw new RuntimeException("Bound#resolveId is called.");
    // }

    // public PDefDict.TconProps getTconProps() {
      // throw new RuntimeException("Bound#getTconProps is called.");
    // }

    // public void excludePrivateAcc() throws CompileException {
      // throw new RuntimeException("Bound#excludePrivateAcc is called.");
    // }

    // public PTypeSkel getSkel() {
      // throw new RuntimeException("Bound#getSkel is called.");
    // }
  // }
}
