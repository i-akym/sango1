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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract public class PFeature {

  static class Id extends PDefaultProgObj {
    String mod;
    String name;

    static Id accept(ParserA.TokenReader reader, PScope scope, int qual, int spc) throws CompileException, IOException {
      StringBuffer emsg;
      ParserA.Token word;
      if ((word = ParserA.acceptNormalWord(reader, spc)) == null) {
        return null;
      }
      Parser.SrcInfo si = word.getSrcInfo();
      String mod = null;
      String name = null;
      if (qual == PExprId.ID_NO_QUAL || ParserA.acceptToken(reader, LToken.DOT, ParserA.SPACE_DO_NOT_CARE) == null) {
        name = word.value.token;
      } else {
        ParserA.Token word2;
        if ((word2 = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE)) == null) {
          emsg = new StringBuffer();
          emsg.append("Name after \".\" missing at ");
          emsg.append(reader.getCurrentSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        mod = word.value.token;
        name = word2.value.token;
      }
      return Id.create(si, scope, mod, name);
    }

    private Id(Parser.SrcInfo srcInfo, PScope scope) {
      super(srcInfo, scope);
    }

    static Id create(Parser.SrcInfo srcInfo, PScope scope, String mod, String name) {
      Id id = new Id(srcInfo, scope);
      id.mod = mod;
      id.name = name;
      return id;
    }

    boolean isSimple() {
      return this.mod == null;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("featureid[");
      if (this.srcInfo != null) {
        buf.append("src=");
        buf.append(this.srcInfo);
      }
      buf.append(",id=");
      buf.append(this.toRepr());
      buf.append("]");
      return buf.toString();
    }

    String toRepr() {
      return repr(this.mod, this.name);
    }

    static String repr(String mod, String name) {
      StringBuffer buf = new StringBuffer();
      if (mod != null) {
        buf.append(mod);
        buf.append(".");
      }
      buf.append(name);
      return buf.toString();
    }

    public void collectModRefs() throws CompileException {
      this.scope.referredModId(this.srcInfo, this.mod);
    }

    public PProgObj resolve() throws CompileException { throw new RuntimeException("PFeature.Id#resolve is called."); }

    public void normalizeTypes() throws CompileException { throw new RuntimeException("PFeature.Id#normalizeTypes is called."); }
  }

  static class IdList extends PDefaultProgObj {
    Id[] ids;

    static IdList accept(ParserA.TokenReader reader, PScope scope, int spc) throws CompileException, IOException {
      // returns at least one id
      StringBuffer emsg;
      List<Id> ii = new ArrayList<Id>();
      Parser.SrcInfo si = reader.getCurrentSrcInfo();
      int state = 0;  // 0: requires id (first, after '+'), 1: id accepted
      int sp = spc;
      Id i;
      while (state >= 0) {
        if (state == 0 && (i = Id.accept(reader, scope, PExprId.ID_MAYBE_QUAL, sp)) != null) {
          ii.add(i);
          sp = ParserA.SPACE_DO_NOT_CARE;
          state = 1;
        } else if (state == 0) {
          emsg = new StringBuffer();
          emsg.append("Feature id missing at ");
          emsg.append(reader.getCurrentSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        } else if (state == 1 && ParserA.acceptToken(reader, LToken.PLUS, sp) != null) {
          state = 0;
        } else {
          state = -1;
        }
      }
      return IdList.create(si, scope, ii);
      // return (ii.size() > 0)? IdList.create(si, scope, ii): null;
    }

    static IdList create(Parser.SrcInfo srcInfo, PScope scope, List<Id> idList) {
      IdList ii = new IdList(srcInfo, scope);
      ii.ids = idList.toArray(new Id[idList.size()]);
      return ii;
    }

    private IdList(Parser.SrcInfo srcInfo, PScope scope) {
      super(srcInfo, scope);
    }

    public void collectModRefs() throws CompileException {
      for (int i = 0; i < this.ids.length; i++) {
        this.ids[i].collectModRefs();
      }
    }

    public PProgObj resolve() throws CompileException { throw new RuntimeException("PFeature.IdList#resolve is called."); }

    public void normalizeTypes() throws CompileException { throw new RuntimeException("PFeature.IdList#normalizeTypes is called."); }
  }

  static Id acceptDef(ParserA.TokenReader reader, PScope scope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    if (ParserA.acceptToken(reader, LToken.PLUS, spc) == null) { return null; }
    Id id;
    if ((id = Id.accept(reader, scope, PExprId.ID_NO_QUAL, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Feature id missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return id;
  }

  static IdList acceptSpec(ParserA.TokenReader reader, PScope scope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    if (ParserA.acceptToken(reader, LToken.TILD, spc) == null) { return null; }
    IdList idList;
    if ((idList = IdList.accept(reader, scope, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Feature specification missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return idList;
  }
}
