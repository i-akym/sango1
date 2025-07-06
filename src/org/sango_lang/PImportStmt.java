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

class PImportStmt extends PDefaultProgObj {
  String id;
  Cstr modName;

  private PImportStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.startImport());
    // this.scope.startDef();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("import[src=");
    buf.append(this.srcInfo);
    buf.append(",id=");
    buf.append(this.id);
    buf.append(",mod_name=");
    buf.append(this.modName);
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PImportStmt imp;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.imp = new PImportStmt(srcInfo, outerScope);
    }

    PScope getScope() { return this.imp.scope; }

    void setModName(Cstr modName) {
      this.imp.modName = modName;
    }

    void setId(String id) {
      this.imp.id = id;
    }

    PImportStmt create() {
      // /* DEBUG */ System.out.print("import stmt: ");
      // /* DEBUG */ System.out.println(this.imp);
      return this.imp;
    }
  }

  static PImportStmt accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "import", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope scope = builder.getScope();
    if ((t = ParserA.acceptCstr(reader, ParserA.SPACE_NEEDED)) == null) {
      emsg = new StringBuffer();
      emsg.append("Imported module name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setModName(t.value.cstrValue);
    if ((t = ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\"missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PEid id;
    if ((id = PEid.accept(reader, scope, Parser.QUAL_INHIBITED, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("No identifier for imported module at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setId(id.name);
    if ((t = ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PImportStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("import-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    Cstr mod = elem.getAttrValueAsCstrData("mod");
    if (mod == null) {
      emsg = new StringBuffer();
      emsg.append("Imported module name missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setModName(mod);
    String id = elem.getAttrValueAsId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("No identifier for imported module at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setId(id);
    return builder.create();
  }

  public void collectModRefs() throws CompileException {}

  public PImportStmt resolve() throws CompileException {
    throw new RuntimeException("PImportStmt#resolveId() called. - " + this.toString());
  }
}
