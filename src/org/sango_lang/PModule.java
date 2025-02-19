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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class PModule implements PModDecl {
  static final String IMPL_WORD_NATIVE = "@native";

  static final String MOD_ID_LANG = "@LANG";
  static final String MOD_ID_HERE = "@HERE";

  static final String ACC_WORD_PUBLIC = "@public";
  static final String ACC_WORD_PROTECTED = "@protected";
  static final String ACC_WORD_OPAQUE = "@opaque";
  static final String ACC_WORD_PRIVATE = "@private";
  static final String ACC_WORDX_PUBLIC = "public";
  static final String ACC_WORDX_PROTECTED = "protected";
  static final String ACC_WORDX_OPAQUE = "opaque";
  static final String ACC_WORDX_PRIVATE = "private";
  static final Option.Set<Module.Access> ACC_OPTS_FOR_EVAL
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_DATA
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PROTECTED)
    .add(Module.ACC_OPAQUE).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_EXTEND
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PROTECTED)
    .add(Module.ACC_OPAQUE).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_ALIAS
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_FEATURE
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PRIVATE);
  static final Module.Access ACC_DEFAULT_FOR_EVAL = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_DATA = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_EXTEND = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_ALIAS = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_FEATURE = Module.ACC_PRIVATE;

  static final String AVAILABILITY_WORD_GENERAL = "@general";
  static final String AVAILABILITY_WORD_ALPHA = "@alpha";
  static final String AVAILABILITY_WORD_BETA = "@beta";
  static final String AVAILABILITY_WORD_LIMITED = "@limited";
  static final String AVAILABILITY_WORD_DEPRECATED = "@deprecated";
  static final String AVAILABILITY_WORDX_GENERAL = "general";
  static final String AVAILABILITY_WORDX_ALPHA = "alpha";
  static final String AVAILABILITY_WORDX_BETA = "beta";
  static final String AVAILABILITY_WORDX_LIMITED = "limited";
  static final String AVAILABILITY_WORDX_DEPRECATED = "deprecated";

  Compiler theCompiler;
  Parser.SrcInfo srcInfo;
  PScope scope;
  Parser.SrcInfo modDefSrcInfo;
  Module.Availability availability;
  Cstr definedName;  // maybe null
  Cstr name;
  String myId;
  Map<String, Cstr> modTab;  // mod id -> mod name
  List<String> referredModIds;  // except @LANG, @HERE, my mod id
  List<Cstr> referredFarMods;  // except sango.lang
  List<PImportStmt> importStmtList;
  List<PDataStmt> dataStmtList;
  List<PExtendStmt> extendStmtList;
  List<PAliasTypeStmt> aliasTypeStmtList;
  List<PFeatureStmt> featureStmtList;
  List<PEvalStmt> evalStmtList;
  int idSuffix;

  private PModule(Compiler theCompiler, Parser.SrcInfo srcInfo) {
    this.theCompiler = theCompiler;
    this.srcInfo = srcInfo;
    this.scope = PScope.create(this);  // hmmm, not elegant
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.modTab = new HashMap<String, Cstr>();
    this.referredModIds = new ArrayList<String>();
    this.referredFarMods = new ArrayList<Cstr>();
    this.importStmtList = new ArrayList<PImportStmt>();
    this.dataStmtList = new ArrayList<PDataStmt>();
    this.extendStmtList = new ArrayList<PExtendStmt>();
    this.aliasTypeStmtList = new ArrayList<PAliasTypeStmt>();
    this.featureStmtList = new ArrayList<PFeatureStmt>();
    this.evalStmtList = new ArrayList<PEvalStmt>();
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("module[src=");
    b.append(this.modDefSrcInfo);
    b.append(",name=");
    b.append(this.name);
    if (this.myId != null) {
      b.append(",id=");
      b.append(this.myId);
    }
    b.append("]");
    return b.toString();
  }

  static PModule accept(Compiler theCompiler, ParserA.TokenReader reader, Cstr modName) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance(theCompiler, reader.getCurrentSrcInfo(), modName);
    acceptModuleStmt(reader, builder);
    if (modName.equals(Module.MOD_LANG)) {
      Parser.SrcInfo si = new Parser.SrcInfo(Module.MOD_LANG, "builtin");
      builder.addDataStmt(
        PDataStmt.createForVariableParams(si, builder.getScope(), Module.TCON_TUPLE, Module.ACC_OPAQUE));
      builder.addDataStmt(
        PDataStmt.createForVariableParams(si, builder.getScope(), Module.TCON_FUN, Module.ACC_OPAQUE));
    }
    acceptDefStmts(reader, builder);
    PModule mod = builder.create();
    mod.generateNameFun();
    mod.generateInitdFun();
    mod.generateFeatureAliases();
    mod.generateFeatureFuns();
    mod.generateDataFuns();
    return mod;
  }

  static PModule acceptX(Compiler theCompiler, ParserB.Elem elem, Cstr modName) throws CompileException {
    StringBuffer emsg;
    if (elem == null) {
      throw new CompileException("No module definition.");
    }
    if (!elem.getName().equals("module")) {
      throw new CompileException("No module definition.");
    }

    Builder builder = Builder.newInstance(theCompiler, elem.getSrcInfo(), modName);
    Cstr name = elem.getAttrValueAsCstrData("name");
    if (name != null) {
      builder.setDefinedName(name);
    }
    builder.setAvailability(acceptXAvailabilityAttr(elem));
    String id = elem.getAttrValueAsId("id");
    if (id != null) {
      builder.setMyId(id);
    }

    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      acceptXDef(e, builder);
      e = e.getNextSibling();
    }
    PModule mod = builder.create();
    mod.generateNameFun();
    mod.generateInitdFun();
    mod.generateFeatureAliases();
    mod.generateFeatureFuns();
    mod.generateDataFuns();
    return mod;
  }

  static void acceptXDef(ParserB.Elem elem, Builder builder) throws CompileException {
    PImportStmt imp;
    PDataStmt dat;
    PExtendStmt ext;
    PAliasTypeStmt alias;
    PEvalStmt eval;
    if ((imp = PImportStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addImportStmt(imp);
    } else if ((dat = PDataStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addDataStmt(dat);
    } else if ((ext = PExtendStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addExtendStmt(ext);
    } else if ((alias = PAliasTypeStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addAliasTypeStmt(alias);
    } else if ((eval = PEvalStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addEvalStmt(eval);
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(elem.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
  }

  private static void acceptModuleStmt(ParserA.TokenReader reader, Builder builder) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "module", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return;
    }
    PScope scope = builder.getScope();
    builder.setAvailability(acceptAvailability(reader));
    if ((t = ParserA.acceptCstr(reader, ParserA.SPACE_NEEDED)) == null) {
      emsg = new StringBuffer();
      emsg.append("No module name at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setDefinedName(t.value.cstrValue);
    if ((t = ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE)) != null) {
      PEid id;
      if ((id = PEid.accept(reader, scope, Parser.QUAL_MAYBE, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("My id missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setMyId(id.name);
    }
    if ((t = ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
  }

  private static void acceptDefStmts(ParserA.TokenReader reader, Builder builder) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t = reader.getToken();
    PImportStmt imp;
    PDataStmt dat;
    PExtendStmt ext;
    PAliasTypeStmt alias;
    PFeatureStmt feat;
    PEvalStmt eval;
    while (!t.isEOF()) {
      if ((imp = PImportStmt.accept(reader, builder.getScope())) != null) {
        builder.addImportStmt(imp);
      } else if ((dat = PDataStmt.accept(reader, builder.getScope())) != null) {
        builder.addDataStmt(dat);
      } else if ((ext = PExtendStmt.accept(reader, builder.getScope())) != null) {
        builder.addExtendStmt(ext);
      } else if ((alias = PAliasTypeStmt.accept(reader, builder.getScope())) != null) {
        builder.addAliasTypeStmt(alias);
      } else if ((feat = PFeatureStmt.accept(reader, builder.getScope())) != null) {
        builder.addFeatureStmt(feat);
      } else if ((eval = PEvalStmt.accept(reader, builder.getScope())) != null) {
        builder.addEvalStmt(eval);
      } else {
        emsg = new StringBuffer();
        emsg.append("Syntax error at ");
        emsg.append(t.getSrcInfo());
        emsg.append(". - ");
        emsg.append(t.value.token);
        throw new CompileException(emsg.toString());
      }
      t = reader.getToken();
    }
  }

  static Module.Access acceptAcc(ParserA.TokenReader reader, Option.Set<Module.Access> options, Module.Access defaultValue) throws CompileException, IOException {
    ParserA.Token a = ParserA.acceptSpecialWord(reader, ParserA.SPACE_NEEDED);
    if (a == null) {
      return defaultValue;
    } else if (options.contains(Module.ACC_PUBLIC) && a.value.token.equals(ACC_WORD_PUBLIC)) {
      reader.tokenConsumed();
      return Module.ACC_PUBLIC;
    } else if (options.contains(Module.ACC_PROTECTED) && a.value.token.equals(ACC_WORD_PROTECTED)) {
      reader.tokenConsumed();
      return Module.ACC_PROTECTED;
    } else if (options.contains(Module.ACC_OPAQUE) && a.value.token.equals(ACC_WORD_OPAQUE)) {
      reader.tokenConsumed();
      return Module.ACC_OPAQUE;
    } else if (options.contains(Module.ACC_PRIVATE) && a.value.token.equals(ACC_WORD_PRIVATE)) {
      reader.tokenConsumed();
      return Module.ACC_PRIVATE;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid access descriptor at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(". - ");
      emsg.append(a.value.token);
      throw new CompileException(emsg.toString());
    }
  }

  static Module.Access acceptXAccAttr(ParserB.Elem elem, Option.Set<Module.Access> options, Module.Access defaultValue) throws CompileException {
    Module.Access a;
    String acc = elem.getAttrValue("acc");
    if (acc == null) {
      a = defaultValue;
    } else if (options.contains(Module.ACC_PUBLIC) && acc.equals(ACC_WORDX_PUBLIC)) {
      a = Module.ACC_PUBLIC;
    } else if (options.contains(Module.ACC_PROTECTED) && acc.equals(ACC_WORDX_PROTECTED)) {
      a = Module.ACC_PROTECTED;
    } else if (options.contains(Module.ACC_OPAQUE) && acc.equals(ACC_WORDX_OPAQUE)) {
      a = Module.ACC_OPAQUE;
    } else if (options.contains(Module.ACC_PRIVATE) && acc.equals(ACC_WORDX_PRIVATE)) {
      a = Module.ACC_PRIVATE;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid access descriptor at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(". - ");
      emsg.append(acc);
      throw new CompileException(emsg.toString());
    }
    return a;
  }

  static Module.Availability acceptAvailability(ParserA.TokenReader reader) throws CompileException, IOException {
    ParserA.Token a = ParserA.acceptSpecialWord(reader, ParserA.SPACE_NEEDED);
    Module.Availability av;
    if (a == null) {
      av = Module.AVAILABILITY_GENERAL;
    } else if (a.value.token.equals(AVAILABILITY_WORD_GENERAL)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_GENERAL;
    } else if (a.value.token.equals(AVAILABILITY_WORD_ALPHA)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_ALPHA;
    } else if (a.value.token.equals(AVAILABILITY_WORD_BETA)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_BETA;
    } else if (a.value.token.equals(AVAILABILITY_WORD_LIMITED)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_LIMITED;
    } else if (a.value.token.equals(AVAILABILITY_WORD_DEPRECATED)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_DEPRECATED;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid availability descriptor at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(". - ");
      emsg.append(a.value.token);
      throw new CompileException(emsg.toString());
    }
    return av;
  }

  static Module.Availability acceptXAvailabilityAttr(ParserB.Elem elem) throws CompileException {
    Module.Availability av;
    String avail = elem.getAttrValue("availability");
    if (avail == null) {
      av = Module.AVAILABILITY_GENERAL;
    } else if (avail.equals(AVAILABILITY_WORDX_GENERAL)) {
      av = Module.AVAILABILITY_GENERAL;
    } else if (avail.equals(AVAILABILITY_WORDX_ALPHA)) {
      av = Module.AVAILABILITY_ALPHA;
    } else if (avail.equals(AVAILABILITY_WORDX_BETA)) {
      av = Module.AVAILABILITY_BETA;
    } else if (avail.equals(AVAILABILITY_WORDX_LIMITED)) {
      av = Module.AVAILABILITY_LIMITED;
    } else if (avail.equals(AVAILABILITY_WORDX_DEPRECATED)) {
      av = Module.AVAILABILITY_DEPRECATED;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid availability descriptor at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(". - ");
      emsg.append(avail);
      throw new CompileException(emsg.toString());
    }
    return av;
  }

  static ParserA.Token acceptWildCard(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return ParserA.acceptToken(reader, LToken.AST_AST, spc);
  }

  static ParserA.Token acceptWildCards(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return ParserA.acceptToken(reader, LToken.AST_AST_AST, spc);
  }

  void addImportStmt(PImportStmt imp) throws CompileException {
    StringBuffer emsg;
    if (imp.id.equals(this.myId)) {
      emsg = new StringBuffer();
      emsg.append("Imported module id \"");
      emsg.append(imp.id);
      emsg.append("\" conflicts with my module id at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (this.modTab.containsKey(imp.id)) {
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(imp.id);
      emsg.append("\" already defined at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    this.importStmtList.add(imp);  // multiple imports allowed
    this.modTab.put(imp.id, imp.modName);
    this.addReferredFarMod(imp.modName);
  }

  void addDataStmt(PDataStmt dat) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey tconKey = PDefDict.IdKey.create(this.name, dat.tcon);
    boolean b = this.theCompiler.defDict.predefineTconData(tconKey, dat.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define type constructor \"");
      emsg.append(dat.tcon);
      emsg.append("\" at ");
      emsg.append(dat.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (dat.constrs != null) {
      for (int i = 0; i < dat.constrs.length; i++) {
        PDataConstrDef constr = dat.constrs[i];
        b = this.theCompiler.defDict.predefineDcon(
          PDefDict.IdKey.create(this.name, constr.dcon), dat.acc);
        if (!b) {
          emsg = new StringBuffer();
          emsg.append("Cannot define data constructor \"");
          emsg.append(constr.dcon);
          emsg.append("\" at ");
          emsg.append(constr.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }

    dat.collectModRefs();
    this.dataStmtList.add(dat);
    this.theCompiler.defDict.putDataDef(tconKey, dat);
  }

  void addExtendStmt(PExtendStmt ext) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey tconKey = PDefDict.IdKey.create(this.name, ext.tcon);
    Cstr baseModName;
    if (ext.baseModId == null) {
      baseModName = Module.MOD_LANG;
    } else {
      baseModName = this.resolveModId(ext.baseModId);
      if (baseModName == null) {
        emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(ext.baseModId);
        emsg.append("\" not defined at ");
        emsg.append(ext.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    ext.baseTconKey = PDefDict.IdKey.create(baseModName, ext.baseTcon);
    boolean b = this.theCompiler.defDict.predefineTconExtend(tconKey, ext.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define type constructor \"");
      emsg.append(ext.tcon);
      emsg.append("\" at ");
      emsg.append(ext.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < ext.constrs.length; i++) {
      PDataConstrDef constr = ext.constrs[i];
      b = this.theCompiler.defDict.predefineDcon(
        PDefDict.IdKey.create(this.name, constr.dcon), ext.acc);
      if (!b) {
        emsg = new StringBuffer();
        emsg.append("Cannot define data constructor \"");
        emsg.append(constr.dcon);
        emsg.append("\" at ");
        emsg.append(constr.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }

    ext.collectModRefs();
    this.extendStmtList.add(ext);
    this.theCompiler.defDict.putDataDef(tconKey, ext);
  }

  void addAliasTypeStmt(PAliasTypeStmt alias) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey tconKey = PDefDict.IdKey.create(this.name, alias.tcon);
    boolean b = this.theCompiler.defDict.predefineTconAliasType(tconKey, alias.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define type alias \"");
      emsg.append(alias.tcon);
      emsg.append("\" at ");
      emsg.append(alias.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    alias.collectModRefs();
    this.aliasTypeStmtList.add(alias);
    this.theCompiler.defDict.putAliasTypeDef(tconKey, alias);
  }

  void addFeatureStmt(PFeatureStmt feat) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey fnameKey = PDefDict.IdKey.create(this.name, feat.fname);
    boolean b = this.theCompiler.defDict.predefineFeature(fnameKey, feat.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define feature \"");
      emsg.append(feat.fname);
      emsg.append("\" at ");
      emsg.append(feat.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    feat.collectModRefs();
    this.featureStmtList.add(feat);
    this.theCompiler.defDict.putFeatureDef(fnameKey, feat);
  }

  void addEvalStmt(PEvalStmt eval) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey officialKey = PDefDict.IdKey.create(this.name, eval.official);
    // boolean b = this.theCompiler.defDict.predefineFunOfficial(officialKey, eval.acc);
    if (!this.predefineFunOfficial(officialKey, eval.acc)) {
      emsg = new StringBuffer();
      emsg.append("Cannot define \"");
      emsg.append(eval.official);
      emsg.append("\" for function's official name at ");
      emsg.append(eval.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < eval.aliases.length; i++) {
      if (!this.theCompiler.defDict.predefineFunAlias(
            PDefDict.IdKey.create(this.name, eval.aliases[i]), eval.acc)) {
        emsg = new StringBuffer();
        emsg.append("Cannot define \"");
        emsg.append(eval.aliases[i]);
        emsg.append("\" for function's alias at ");
        emsg.append(eval.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }

    eval.collectModRefs();
    this.evalStmtList.add(eval);
    this.theCompiler.defDict.putFunDef(officialKey, eval);
  }

  boolean isLang() {
    return this.name != null && this.name.equals(Module.MOD_LANG);
  }

  boolean predefineFunOfficial(String official, Module.Access acc) {
    PDefDict.IdKey k = PDefDict.IdKey.create(this.name, official);
    return this.predefineFunOfficial(k, acc);
  }

  boolean predefineFunOfficial(PDefDict.IdKey officialKey, Module.Access acc) {
    return this.theCompiler.defDict.predefineFunOfficial(officialKey, acc);
  }

  Cstr resolveModId(String id) {
    Cstr n = null;
    if (id == null) {
      ;
    } else if (id.equals(this.myId)) {
      n = this.name;
    } else if (id.equals(MOD_ID_HERE)) {
      n = this.name;
    } else if (id.equals(MOD_ID_LANG)) {
      n = Module.MOD_LANG;
    } else {
      n = this.modTab.get(id);
    }
    return n;
  }

  void addReferredFarMod(Cstr modName) {  // add implicitly referred mod
    if (modName.equals(Module.MOD_LANG) || modName.equals(this.name)) { return; }  // skip
    if (this.referredFarMods.contains(modName)) { return; }  // already included
// /* DEBUG */ System.out.println(modName);
    this.referredFarMods.add(modName);
  }

  public Cstr[] getForeignMods() {
    Cstr[] ms;
    if (this.isLang()) {
      ms = new Cstr[0];
    } else {
      // this.referredFarMods.clear();
      // for (int i = 0; i < this.referredModIds.size(); i++) {
        // Cstr n = this.modTab.get(this.referredModIds.get(i));
        // if (n.equals(Module.MOD_LANG) || n.equals(this.name)) {
          // ;  // pass
        // } else {
          // this.referredFarMods.add(n);
        // }
      // }
      ms = new Cstr[1 + this.referredFarMods.size()];  // sango.lang and others
      ms[0] = Module.MOD_LANG;
      for (int i = 1, j = 0; i < ms.length; i++, j++) {
        ms[i] = this.referredFarMods.get(j);
      }
    }
    return ms;
  }

  int modNameToModRefIndex(Cstr modName) {
    int index;
    if (modName.equals(this.name)) {
      index = Module.MOD_INDEX_SELF;
    } else if (modName.equals(Module.MOD_LANG)) {
      index = Module.MOD_INDEX_LANG;
    } else {
      int i = this.referredFarMods.indexOf(modName);
/* DEBUG */ if (i < 0) { throw new RuntimeException("Unknown mod name. " + modName.repr() + " " + this.referredFarMods); }
      index = 2 + i;
    }
    return index;
  }

  PEvalStmt getInitFunDef() {
    PEvalStmt eval = null;
    for (int i = 0; eval == null && i < this.evalStmtList.size(); i++) {
      PEvalStmt e = this.evalStmtList.get(i);
      if (e.official.equals(Module.FUN_INIT) && e.params.length == 0) {
        eval = e;
      }
    }
    // if (this.funOfficialDict.containsKey(Module.FUN_INIT)) {
      // PEvalStmt e = this.evalStmtList.get(this.funOfficialDict.get(Module.FUN_INIT));
      // eval = (e.params.length == 0)? e: null;
    // }
    return eval;
  }

  boolean isInitFunDefined() {
    return this.getInitFunDef() != null;
  }

  void setupAliasBody() throws CompileException {
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.get(i).setupBodySkel();
    }
  }

  PModDecl getModDecl() { return this; }

  PModule resolve() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.set(i, this.dataStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.set(i, this.extendStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.set(i, this.aliasTypeStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      this.featureStmtList.set(i, this.featureStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.set(i, this.evalStmtList.get(i).resolve());
    }
    return this;
  }

  PDefDict.TidProps resolveTcon(PTid tcon) throws CompileException {
    // variable is already processed in PScope
    if ((tcon.catOpt & PDefDict.TID_CAT_VAR) > 0) {
      throw new IllegalArgumentException("invalid cat of id - " + tcon.toString());
    }
    return this.isLang()?  this.resolveTconInLang(tcon): this.resolveTconInOther(tcon);
  }

  private PDefDict.TidProps resolveTconInLang(PTid tcon) throws CompileException {
    if (tcon.modId == null || tcon.modId.equals(this.myId) || tcon.modId.equals(MOD_ID_HERE) || tcon.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(tcon.modId);
      emsg.append("\" at ");
      emsg.append(tcon.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveTcon(this.name, tcon.name, tcon.catOpt);
  }

  private PDefDict.TidProps resolveTconInOther(PTid tcon) throws CompileException {
    PDefDict.TidProps tp;
    if (tcon.modId == null) {
      tp = this.doResolveTcon(this.name, tcon.name, tcon.catOpt);
      if (tp == null) {
        tp = this.doResolveTcon(Module.MOD_LANG, tcon.name, tcon.catOpt);
      }
    } else if (tcon.modId.equals(this.myId) || tcon.modId.equals(MOD_ID_HERE)) {
      tp = this.doResolveTcon(this.name, tcon.name, tcon.catOpt);
    } else {
      Cstr targetModName = this.modTab.get(tcon.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(tcon.modId);
        emsg.append("is not defined at ");
        emsg.append(tcon.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      tp = this.doResolveTcon(targetModName, tcon.name, tcon.catOpt);
    }
    return tp;
  }

  private PDefDict.TidProps doResolveTcon(Cstr modName, String idName, int catOpt) throws CompileException {
    PDefDict.TidProps tp = this.theCompiler.defDict.resolveTcon(
      this.name,
      PDefDict.IdKey.create(modName, idName));
    if (tp == null) {
      ;  // pass
    } else if ((tp.cat & catOpt) == 0) {
      tp = null;  // not hit
    }
    return tp;
  }

  PDefDict.TidProps resolveFeature(PTid fname) throws CompileException {
    return this.isLang()?  this.resolveFeatureInLang(fname): this.resolveFeatureInOther(fname);
  }

  private PDefDict.TidProps resolveFeatureInLang(PTid fname) throws CompileException {
    if (fname.modId == null || fname.modId.equals(this.myId) || fname.modId.equals(MOD_ID_HERE) || fname.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(fname.modId);
      emsg.append("\" at ");
      emsg.append(fname.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveFeature(this.name, fname.name);
  }

  private PDefDict.TidProps resolveFeatureInOther(PTid fname) throws CompileException {
    PDefDict.TidProps tp;
    if (fname.modId == null) {
      tp = this.doResolveFeature(this.name, fname.name);
      if (tp == null) {
        tp = this.doResolveFeature(Module.MOD_LANG, fname.name);
      }
    } else if (fname.modId.equals(this.myId) || fname.modId.equals(MOD_ID_HERE)) {
      tp = this.doResolveFeature(this.name, fname.name);
    } else if (fname.modId.equals(MOD_ID_LANG)) {
      tp = this.doResolveFeature(Module.MOD_LANG, fname.name);
    } else {
      Cstr targetModName = this.modTab.get(fname.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(fname.modId);
        emsg.append("is not defined at ");
        emsg.append(fname.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      tp = this.doResolveFeature(targetModName, fname.name);
    }
    return tp;
  }

  private PDefDict.TidProps doResolveFeature(Cstr modName, String idName) throws CompileException {
    return this.theCompiler.defDict.resolveFeature(
      this.name,
      PDefDict.IdKey.create(modName, idName));
  }

  PDefDict.EidProps resolveAnchor(PEid eid) throws CompileException {
    // variable is already processed in PScope
    if ((eid.catOpt & PDefDict.EID_CAT_VAR) > 0) {
      throw new IllegalArgumentException("invalid cat of id - " + eid.toString());
    }
    return this.isLang()? this.resolveAnchorInLang(eid): this.resolveAnchorInOther(eid);
  }

  PDefDict.EidProps resolveAnchorInLang(PEid eid) throws CompileException {
    if (eid.modId == null || eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE) || eid.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(eid.modId);
      emsg.append("\" at ");
      emsg.append(eid.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveAnchor(this.name, eid.name, eid.catOpt);
  }

  PDefDict.EidProps resolveAnchorInOther(PEid eid) throws CompileException {
    PDefDict.EidProps ep;
    if (eid.modId == null) {
      ep = this.doResolveAnchor(this.name, eid.name, eid.catOpt);
      if (ep == null) {
        ep = this.doResolveAnchor(Module.MOD_LANG, eid.name, eid.catOpt);
      }
    } else if (eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE)) {
      ep = this.doResolveAnchor(this.name, eid.name, eid.catOpt);
    } else if (eid.modId.equals(MOD_ID_LANG)) {
      ep = this.doResolveAnchor(Module.MOD_LANG, eid.name, eid.catOpt);
    }  else {
      Cstr targetModName = this.modTab.get(eid.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(eid.modId);
        emsg.append("is not defined at ");
        emsg.append(eid.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      ep = this.doResolveAnchor(targetModName, eid.name, eid.catOpt);
    }
    return ep;
  }

  PDefDict.EidProps doResolveAnchor(Cstr modName, String idName, int catOpt) throws CompileException {
    PDefDict.EidProps ep = this.theCompiler.defDict.resolveAnchor(
      this.name,
      PDefDict.IdKey.create(modName, idName));
    if (ep == null) {
      ;  // pass
    } else if ((ep.cat & catOpt) == 0) {
      ep = null;  // not hit
    }
    return ep;
  }

  // not used
  boolean isFunDefinedHere(String official) {
    PDefDict.EidProps ep = null;
    try {
      ep = this.doResolveFunOfficial(this.name, official);
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpected exception. " + ex.toString());
    }
    return ep != null;
  }

  PDefDict.EidProps resolveFunOfficial(PEid eid) throws CompileException {
    if ((eid.catOpt & PDefDict.EID_CAT_FUN_OFFICIAL) == 0) {
      throw new IllegalArgumentException("invalid cat of id - " + eid.toString());
    }
    return this.isLang()? this.resolveFunOfficialInLang(eid): this.resolveFunOfficialInOther(eid);
  }

  PDefDict.EidProps resolveFunOfficialInLang(PEid eid) throws CompileException {
    if (eid.modId == null || eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE) || eid.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(eid.modId);
      emsg.append("\" at ");
      emsg.append(eid.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveFunOfficial(this.name, eid.name);
  }

  PDefDict.EidProps resolveFunOfficialInOther(PEid eid) throws CompileException {
    PDefDict.EidProps ep;
    if (eid.modId == null) {
      ep = this.doResolveFunOfficial(this.name, eid.name);
      if (ep == null) {
        ep = this.doResolveFunOfficial(Module.MOD_LANG, eid.name);
      }
    } else if (eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE)) {
      ep = this.doResolveFunOfficial(this.name, eid.name);
    } else if (eid.modId.equals(MOD_ID_LANG)) {
      ep = this.doResolveFunOfficial(Module.MOD_LANG, eid.name);
    }  else {
      Cstr targetModName = this.modTab.get(eid.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(eid.modId);
        emsg.append("is not defined at ");
        emsg.append(eid.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      ep = this.doResolveFunOfficial(targetModName, eid.name);
    }
    return ep;
  }

  PDefDict.EidProps doResolveFunOfficial(Cstr modName, String idName) throws CompileException {
    PDefDict.EidProps ep = this.theCompiler.defDict.resolveAnchor(
      this.name,
      PDefDict.IdKey.create(modName, idName));
    if (ep == null) {
      ;  // pass
    } else if ((ep.cat & PDefDict.EID_CAT_FUN_OFFICIAL) == 0) {
      ep = null;  // not hit
    }
    return ep;
  }

  PDefDict.FunSelRes selectFunDef(PEid eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    if ((eid.catOpt & PDefDict.EID_CAT_FUN) == 0) {
      throw new IllegalArgumentException("invalid cat of id - " + eid.toString());
    }
    return this.isLang()? this.selectFunDefInLang(eid, paramTypes, givenTVarList): this.selectFunDefInOther(eid, paramTypes, givenTVarList);
  }

  PDefDict.FunSelRes selectFunDefInLang(PEid eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    if (eid.modId == null || eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE) || eid.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(eid.modId);
      emsg.append("\" at ");
      emsg.append(eid.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doSelectFunDef(this.name, eid.name, paramTypes, givenTVarList);
  }

  PDefDict.FunSelRes selectFunDefInOther(PEid eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    PDefDict.FunSelRes res = null;
    if (eid.modId == null) {
      res = this.doSelectFunDef(this.name, eid.name, paramTypes, givenTVarList);
      if (res == null) {
        res = this.doSelectFunDef(Module.MOD_LANG, eid.name, paramTypes, givenTVarList);
      }
    } else if (eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE)) {
      res = this.doSelectFunDef(this.name, eid.name, paramTypes, givenTVarList);
    } else if (eid.modId.equals(MOD_ID_LANG)) {
      res = this.doSelectFunDef(Module.MOD_LANG, eid.name, paramTypes, givenTVarList);
    }  else {
      Cstr targetModName = this.modTab.get(eid.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(eid.modId);
        emsg.append("is not defined at ");
        emsg.append(eid.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      res = this.doSelectFunDef(targetModName, eid.name, paramTypes, givenTVarList);
    }
    return res;
  }

  PDefDict.FunSelRes doSelectFunDef(Cstr modName, String idName, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    PDefDict.FunSelRes res = this.theCompiler.defDict.selectFunDef(
      this.name,
      PDefDict.IdKey.create(modName, idName),
      paramTypes, givenTVarList);
    return res;
  }

  public Module.Availability getAvailability() { return this.availability; }

  public Cstr getName() { return this.name; }

  // PDefDict.TconProps resolveTcon(String modId, String tcon) {
    // PDefDict.TconProps tp;
    // return
      // ((tp = this.tconDict.get(tcon)) != null && (tp.cat & catOpts) > 0 && accOpts.contains(tp.acc))?
      // tp: null;
  // }

  // public PDefDict.EidProps resolveEid(String id, int catOpts, Option.Set<Module.Access> accOpts) {
    // PDefDict.EidProps props = this.eidDict.get(id);
    // return
      // (props != null && (props.cat & catOpts) > 0 && accOpts.contains(props.acc))?
      // props: null;
  // }

  // public PDefDict.FeatureProps resolveFeature(String fname, Option.Set<Module.Access> accOpts) {
    // PDefDict.FeatureProps fp;
    // return
      // ((fp = this.fnameDict.get(fname)) != null && accOpts.contains(fp.acc))?
      // fp: null;
  // }

  // ExprDefGetter createExprDefGetter(PDataDef dataDef) {
    // return new ExprDefGetter(dataDef, null);
  // }

  // ExprDefGetter createExprDefGetter(String funName) {
    // return new ExprDefGetter(null, funName);
  // }

  private void generateFeatureAliases() throws CompileException {
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      List<PAliasTypeStmt> as = this.featureStmtList.get(i).generateAliases(this);
      for (int j = 0; j < as.size(); j++) {
        this.addAliasTypeStmt(as.get(j));
      }
    }
  }

  String generateId() {
    return "@@" + this.idSuffix++;
  }

  static String[] generateInFunNames(String tcon) {
    return new String[] { "_in_" + tcon + "?", tcon + "?" };
  }

  static String[] generateNarrowFunNames(String tcon) {
    return new String[] { "_narrow_" + tcon, "narrow" };
  }

  static String[] generateAttrFunNames(String tcon, String attrName) {
    return new String[] { "_attr_" + tcon + "_" + attrName, attrName };
  }

  static String[] generateMaybeAttrFunNames(String tcon, String attrName) {
    return new String[] { "_maybe_attr_" + tcon + "_" + attrName, "maybe_" + attrName };
  }

  static String[] generateIds(String prefix, int count) {
    String[] ids = new String[count];
    for (int i = 0; i < count; i++) {
      ids[i] = prefix + i;
    }
    return ids;
  }

  private void generateFeatureFuns() throws CompileException {
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      List<PEvalStmt> es = this.featureStmtList.get(i).generateFuns(this);
      for (int j = 0; j < es.size(); j++) {
        this.addEvalStmt(es.get(j));
      }
    }
  }

  private void generateDataFuns() throws CompileException {
    List<PEvalStmt> es;
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      es = this.dataStmtList.get(i).generateFuns(this);
      for (int j = 0; j < es.size(); j++) {
        this.addEvalStmt(es.get(j));
      }
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      es = this.extendStmtList.get(i).generateFuns(this);
      for (int j = 0; j < es.size(); j++) {
        this.addEvalStmt(es.get(j));
      }
    }
  }

  static class Builder {
    Cstr requiredName;
    PModule mod;
    List<PImportStmt> importStmtList;
    List<PDataStmt> dataStmtList;
    List<PExtendStmt> extendStmtList;
    List<PAliasTypeStmt> aliasTypeStmtList;
    List<PFeatureStmt> featureStmtList;
    List<PEvalStmt> evalStmtList;

    static Builder newInstance(Compiler theCompiler, Parser.SrcInfo srcInfo, Cstr requiredName) {
      return new Builder(theCompiler, srcInfo, requiredName);
    }

    Builder(Compiler theCompiler, Parser.SrcInfo srcInfo, Cstr requiredName) {
      this.requiredName = requiredName;
      this.mod = new PModule(theCompiler, srcInfo);
      this.importStmtList = new ArrayList<PImportStmt>();
      this.dataStmtList = new ArrayList<PDataStmt>();
      this.extendStmtList = new ArrayList<PExtendStmt>();
      this.aliasTypeStmtList = new ArrayList<PAliasTypeStmt>();
      this.featureStmtList = new ArrayList<PFeatureStmt>();
      this.evalStmtList = new ArrayList<PEvalStmt>();
    }

    PScope getScope() { return this.mod.scope; }

    void setAvailability(Module.Availability availability) {
      this.mod.availability = availability;
    }

    void setDefinedName(Cstr name) throws CompileException {
      StringBuffer emsg;
      if (!Module.isValidModName(name)) {
        emsg = new StringBuffer();
        emsg.append("Invalid module name. ");
        emsg.append(name.repr());
        emsg.append(" (Remark: Non-printable characters may be included.)");
        throw new CompileException(emsg.toString());
      }
      if (this.requiredName != null && !name.equals(this.requiredName)) {
        emsg = new StringBuffer();
        emsg.append("Module name mismatch.");
        emsg.append("\n  required: ");
        emsg.append(this.requiredName.repr());
        emsg.append("\n  defined: ");
        emsg.append(name.repr());
        throw new CompileException(emsg.toString());
      }
      this.mod.definedName = name;
    }

    void setMyId(String id) {
      this.mod.myId = id;
    }

    void addImportStmt(PImportStmt imp) throws CompileException {
      this.importStmtList.add(imp);
    }

    void addDataStmt(PDataStmt dat) throws CompileException {
      this.dataStmtList.add(dat);
    }

    void addExtendStmt(PExtendStmt ext) throws CompileException {
      this.extendStmtList.add(ext);
    }

    void addAliasTypeStmt(PAliasTypeStmt alias) throws CompileException {
      this.aliasTypeStmtList.add(alias);
    }

    void addFeatureStmt(PFeatureStmt feat) throws CompileException {
      this.featureStmtList.add(feat);
    }

    void addEvalStmt(PEvalStmt eval) throws CompileException {
      this.evalStmtList.add(eval);
    }

    PModule create() throws CompileException {
      if (this.mod.definedName != null) {
        this.mod.name = this.mod.definedName;
      } else {
        this.mod.name = this.requiredName;
      }
      if (!this.mod.name.equals(Module.MOD_LANG)) {
        PImportStmt.Builder ib = PImportStmt.Builder.newInstance(
          new Parser.SrcInfo(Module.MOD_LANG,"auto"), this.mod.scope);
        ib.setModName(Module.MOD_LANG);
        ib.setId(MOD_ID_LANG);
        this.mod.addImportStmt(ib.create());
      }
      for (int i = 0; i < this.importStmtList.size(); i++) {
        this.mod.addImportStmt(this.importStmtList.get(i));
      }
      for (int i = 0; i < this.dataStmtList.size(); i++) {
        this.mod.addDataStmt(this.dataStmtList.get(i));
      }
      for (int i = 0; i < this.extendStmtList.size(); i++) {
        this.mod.addExtendStmt(this.extendStmtList.get(i));
      }
      for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
        this.mod.addAliasTypeStmt(this.aliasTypeStmtList.get(i));
      }
      for (int i = 0; i < this.featureStmtList.size(); i++) {
        this.mod.addFeatureStmt(this.featureStmtList.get(i));
      }
      for (int i = 0; i < this.evalStmtList.size(); i++) {
        this.mod.addEvalStmt(this.evalStmtList.get(i));
      }
      return this.mod;
    }
  }


  // PDefDict.FunSelRes selectFun(String name, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    // List<Integer> indices = this.funDict.get(name);
    // PDefDict.FunSelRes sel = null;
    // if (indices == null) { return null; }
    // for (int i = 0; sel == null && i < indices.size(); i++) {
      // PFunDef fd = this.evalStmtList.get(indices.get(i));
      // PTypeSkel[] pts = fd.getParamTypes();
      // if (pts.length != paramTypes.length) { continue; }
      // PTypeSkelBindings bindings = PTypeSkelBindings.create(givenTVarList);
      // boolean b = true;
      // for (int j = 0; b && j < pts.length; j++) {
        // b = pts[j].accept(PTypeSkel.NARROWER, paramTypes[j], bindings);
      // }
      // if (b) {
        // for (int j = 0; b && j < pts.length; j++) {
          // PTypeSkel p = paramTypes[j].resolveBindings(bindings);
          // b = pts[j].extractAnyInconcreteVar(p /* , givenTVarList */) == null;
        // }
      // }
      // if (b) {
        // sel = PDefDict.FunSelRes.create(fd, bindings);
      // }
    // }
    // return sel;
  // }

  // PFunDef getFun(String official) {
    // Integer index = this.funOfficialDict.get(official);
    // if (index == null) { return null; }
    // return this.evalStmtList.get(index);
  // }

  void checkAccInDefs() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).checkAcc();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkAcc();
    }
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.get(i).checkAcc();
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.get(i).checkAcc();
    }
  }

  // void  checkCyclicAlias() throws CompileException {
    // for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      // this.aliasTypeStmtList.get(i).checkCyclicAlias();
    // }
  // }

  // void collectTconProps() throws CompileException {
    // for (int i = 0; i < this.dataStmtList.size(); i++) {
      // this.dataStmtList.get(i).collectTconProps();
    // }
    // for (int i = 0; i < this.extendStmtList.size(); i++) {
      // this.extendStmtList.get(i).collectTconProps();
    // }
    // for (int i = 0; i < this.evalStmtList.size(); i++) {
      // this.evalStmtList.get(i).collectTconProps();
    // }
  // }

  // void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    // for (int i = 0; i < this.extendStmtList.size(); i++) {
      // this.extendStmtList.get(i).setupExtensionGraph(g);
    // }
  // }

  void checkVariance() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).checkVariance();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkVariance();
    }
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      this.featureStmtList.get(i).checkVariance();
    }
  }

  void checkConcreteness() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).checkConcreteness();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkConcreteness();
    }
  }

  void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      this.featureStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.get(i).normalizeTypes();
    }
  }

  void checkFeatureImplOnExtension() throws CompileException {
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkFeatureImpl();
    }
  }

  void makeSureTypeConsistency() throws CompileException {
    PTypeGraph g = PTypeGraph.create(this.theCompiler, this);
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      PEvalStmt e = this.evalStmtList.get(i);
      e.setupTypeGraph(g);
    }
    g.inferAll();
  }

  private void generateNameFun() throws CompileException {
    // eval _name_ @public -> <cstr> @native
    Parser.SrcInfo si = new Parser.SrcInfo(this.name, ":name");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope);
    evalStmtBuilder.setOfficial(Module.FUN_NAME);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTid.create(si, retScope, MOD_ID_LANG, "cstr", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    this.addEvalStmt(evalStmtBuilder.create());
  }

  private void generateInitdFun() throws CompileException {
    // eval _initd_ -> <_init_'s ret type> @native
    PEvalStmt eval;
    if ((eval = this.getInitFunDef()) == null) { return; }
    Parser.SrcInfo si = eval.srcInfo.appendPostfix("_initd");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope);
    evalStmtBuilder.setOfficial(Module.FUN_INITD);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(eval.retDef.type.unresolvedCopy(
      si, retScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_OFF));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    this.addEvalStmt(evalStmtBuilder.create());
  }

  // PDefDict.TconProps resolveTcon(String modId, String tcon) throws CompileException {
    // Cstr modName = PModule.this.resolveModId(modId);
    // Option.Set<Module.Access> as = new Option.Set<Module.Access>();
    // as = as.add(Module.ACC_PUBLIC).add(Module.ACC_PROTECTED).add(Module.ACC_OPAQUE);
    // PDefDict.TconProps tp = PModule.this.theCompiler.getReferredDefDict(modName).resolveTcon(
      // tcon, PDefDict.TID_CAT_TCON, as);
    // if (tp != null) {
      // this.referredTcon(modName, tcon, tp);
    // }
    // return tp;
  // }

    // PDefDict.FeatureProps resolveFeature(String modId, String fname) throws CompileException {
      // Cstr modName = PModule.this.resolveModId(modId);
      // Option.Set<Module.Access> as = new Option.Set<Module.Access>();
      // as = as.add(Module.ACC_PUBLIC);
      // PDefDict.FeatureProps fp = PModule.this.theCompiler.getReferredDefDict(modName).resolveFeature(
        // fname, as);
      // if (fp != null) {
        // this.referredFeature(modName, fname, fp.defGetter.getFeatureDef());
      // }
      // return fp;
    // }

    // void referredEid(Cstr modName, String id, int catOpts, PDefDict.EidProps ep) throws CompileException {
      // PDataDef dd;
      // PFunDef fd;
      // switch (catOpts & ep.cat) {
      // case PDefDict.EID_CAT_DCON_EVAL:
      // // /* DEBUG */ System.out.println(" >> DCON_EVAL");
        // dd = ep.defGetter.getDataDef();
        // if (this.dataDefDictDict.containsKey(modName)) {
          // Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
          // if (m.containsKey(dd.getFormalTcon())) {
      // // /* DEBUG */ System.out.println(" >> DCON_EVAL >> ++ " + dd.getFormalTcon());
            // ForeignDataDef fdd = m.get(dd.getFormalTcon());
            // fdd.referredDcon(id);
            // fdd.requireAcc(Module.ACC_PUBLIC);
          // } else {
      // // /* DEBUG */ System.out.println(" >> DCON_EVAL >> new data_def " + dd.getFormalTcon());
            // ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PUBLIC);
            // fdd.referredDcon(id);
            // m.put(dd.getFormalTcon(), fdd);
          // }
        // } else {
      // // /* DEBUG */ System.out.println(" >> DCON_EVAL >> new module " + dd.getFormalTcon());
          // Map<String, ForeignDataDef> m = new HashMap<String, ForeignDataDef>();
          // ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PUBLIC);
          // fdd.referredDcon(id);
          // m.put(dd.getFormalTcon(), fdd);
          // this.dataDefDictDict.put(modName, m);
          // PModule.this.maintainFarModRef(modName);
        // }
        // break;
      // case PDefDict.EID_CAT_DCON_PTN:
      // // /* DEBUG */ System.out.println(" >> DCON_PTN");
        // dd = ep.defGetter.getDataDef();
        // if (this.dataDefDictDict.containsKey(modName)) {
          // Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
          // if (m.containsKey(dd.getFormalTcon())) {
      // // /* DEBUG */ System.out.println(" >> DCON_PTN >> ++ " + dd.getFormalTcon());
            // ForeignDataDef fdd = m.get(dd.getFormalTcon());
            // fdd.referredDcon(id);
            // fdd.requireAcc(Module.ACC_PROTECTED);
          // } else {
      // // /* DEBUG */ System.out.println(" >> DCON_PTN >> new data_def " + dd.getFormalTcon());
            // ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PROTECTED);
            // fdd.referredDcon(id);
            // m.put(dd.getFormalTcon(), fdd);
          // }
        // } else {
      // // /* DEBUG */ System.out.println(" >> DCON_PTN >> new module " + dd.getFormalTcon());
          // Map<String, ForeignDataDef> m = new HashMap<String, ForeignDataDef>();
          // ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PROTECTED);
          // fdd.referredDcon(id);
          // m.put(dd.getFormalTcon(), fdd);
          // this.dataDefDictDict.put(modName, m);
          // PModule.this.maintainFarModRef(modName);
        // }
        // break;
      // default:
      // // /* DEBUG */ System.out.println(" >> EID_OTHER " + id + Integer.toString(catOpts) + " " + Integer.toString(ep.cat));
        // break;
      // }
    // }

    // void referredFunOfficial(PFunDef fd) {
      // Cstr modName = fd.getModName();
      // // PModule.this.addImplicitFarModRef(modName);  // maybe not registered...
      // String official = fd.getOfficialName();
// // /* DEBUG */ System.out.println("official " + official);
      // Map<String, PFunDef> m;
      // if ((m = this.funDefDictDict.get(modName)) == null) {
        // m = new HashMap<String, PFunDef>();
        // m.put(official, fd);
        // this.funDefDictDict.put(modName, m);
      // } else if (!m.containsKey(official)) {
        // m.put(official, fd);
      // }
    // }

    // void referredTcon(Cstr modName, String tcon, PDefDict.TconProps tp) {
      // switch (tp.cat) {
      // case PDefDict.TID_CAT_TCON_ALIAS:
      // // /* DEBUG */ System.out.println(" >> ALIAS");
        // PAliasTypeDef ad = tp.defGetter.getAliasTypeDef();
        // if (this.aliasDefDictDict.containsKey(modName)) {
          // Map<String, PAliasTypeDef> m = this.aliasDefDictDict.get(modName);
          // if (m.containsKey(ad.getTcon())) {
      // // /* DEBUG */ System.out.println(" >> ALIAS >> already registered " + ad.getTcon());
          // } else {
      // // /* DEBUG */ System.out.println(" >> ALIAS >> new alias_def " + ad.getTcon());
            // m.put(ad.getTcon(), ad);
          // }
        // } else {
      // // /* DEBUG */ System.out.println(" >> ALIAS >> new module " + ad.getTcon());
          // Map<String, PAliasTypeDef> m = new HashMap<String, PAliasTypeDef>();
          // m.put(ad.getTcon(), ad);
          // this.aliasDefDictDict.put(modName, m);
          // PModule.this.maintainFarModRef(modName);
        // }
        // break;
      // default:
      // // /* DEBUG */ System.out.println(" >> DATA");
        // PDataDef dd = tp.defGetter.getDataDef();
        // if (this.dataDefDictDict.containsKey(modName)) {
          // Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
          // if (m.containsKey(dd.getFormalTcon())) {
      // // /* DEBUG */ System.out.println(" >> DATA >> ++ " + dd.getFormalTcon());
            // ForeignDataDef fdd = m.get(dd.getFormalTcon());
            // fdd.requireAcc(Module.ACC_OPAQUE);
          // } else {
      // // /* DEBUG */ System.out.println(" >> DATA >> new data_def " + dd.getFormalTcon());
            // ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_OPAQUE);
            // m.put(dd.getFormalTcon(), fdd);
          // }
        // } else {
      // // /* DEBUG */ System.out.println(" >> DATA >> new module " + dd.getFormalTcon());
          // Map<String, ForeignDataDef> m = new HashMap<String, ForeignDataDef>();
          // ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_OPAQUE);
          // m.put(dd.getFormalTcon(), fdd);
          // this.dataDefDictDict.put(modName, m);
          // PModule.this.maintainFarModRef(modName);
        // }
        // break;
      // }
    // }

    //void referredFeature(Cstr modName, String fname, PFeatureDef fd) {
      ///* DEBUG */ if (modName == null) { throw new IllegalArgumentException("Null module name. " + fname); }
      //Map<String, PFeatureDef> m;
      //if ((m = this.featureDefDictDict.get(modName)) == null) {
        //m = new HashMap<String, PFeatureDef>();
        //m.put(fname, fd);
        //this.featureDefDictDict.put(modName, m);
      //} else if (!m.containsKey(fname)) {
        //m.put(fname, fd);
      //}
    //}

    List<PDataDef> getForeignDataDefsIn(Cstr modName) {
      return this.theCompiler.defDict.getForeignDataDefsIn(this.name, modName);
    }

    List<PAliasTypeDef> getForeignAliasTypeDefsIn(Cstr modName) {
      return this.theCompiler.defDict.getForeignAliasTypeDefsIn(this.name, modName);
    }

    List<PFeatureDef> getForeignFeatureDefsIn(Cstr modName) {
      return this.theCompiler.defDict.getForeignFeatureDefsIn(this.name, modName);
    }

    List<PFunDef> getForeignFunDefsIn(Cstr modName) {
      return this.theCompiler.defDict.getForeignFunDefsIn(this.name, modName);
    }

  class ForeignDataDef implements PDataDef {
    PDataDef referredDataDef;
    Module.Access requiredAcc;
    List<String> referredDconList;
    List<String> referredFeatureList;

    ForeignDataDef(PDataDef dd, Module.Access acc) {
      this.referredDataDef = dd;
      this.requiredAcc = acc;
      this.referredDconList = new ArrayList<String>();
      this.referredFeatureList = new ArrayList<String>();
    }

    void requireAcc(Module.Access acc) {
      this.requiredAcc = Module.moreOpenAcc(requiredAcc, this.requiredAcc)? requiredAcc: this.requiredAcc;
    }

    void referredDcon(String dcon) {
      if (!this.referredDconList.contains(dcon)) {
        this.referredDconList.add(dcon);
      }
    }

    void referredFeature(String fname) {
      if (!this.referredFeatureList.contains(fname)) {
        this.referredFeatureList.add(fname);
      }
    }

    public String getFormalTcon() { return this.referredDataDef.getFormalTcon(); }

    public PDefDict.IdKey getBaseTconKey() { return this.referredDataDef.getBaseTconKey(); }

    public PDefDict.TparamProps[] getParamPropss() { return this.referredDataDef.getParamPropss(); }

    // public int getParamCount() { return this.referredDataDef.getParamCount(); }

    public PTypeRefSkel getTypeSig() { return this.referredDataDef.getTypeSig(); }

    // public Module.Variance getParamVarianceAt(int pos) { return this.referredDataDef.getParamVarianceAt(pos); }

    public Module.Availability getAvailability() { return this.referredDataDef.getAvailability(); }

    public Module.Access getAcc() {
      return this.requiredAcc;
    }

    public int getConstrCount() { return this.referredDconList.size(); }

    public PDataDef.Constr getConstr(String dcon) {
      return this.referredDconList.contains(dcon)? this.referredDataDef.getConstr(dcon): null;
    }

    public PDataDef.Constr getConstrAt(int index) {
      return this.referredDataDef.getConstr(this.referredDconList.get(index));
    }

    public int getFeatureImplCount() {
      return this.referredFeatureList.size();
    }

    public PDataDef.FeatureImpl getFeatureImplAt(int index) {
      throw new RuntimeException("PModule.ForeignDataDef#getFeatureImplAt() not implemented.");
    }
  }
}
