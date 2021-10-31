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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface PTypeSkel {

  Parser.SrcInfo getSrcInfo();

  boolean isLiteralNaked();

  boolean isConcrete();

  boolean isConcrete(PTypeSkelBindings bindings);

  PTypeSkel instanciate(InstanciationBindings iBindings);

  PTypeSkel resolveBindings(PTypeSkelBindings bindings);

  PTypeSkelBindings applyTo(int width, PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException;
  // where, width is
  static final int EQUAL = 0;
  static final int NARROWER = 1;
  static final int WIDER = - NARROWER;

  boolean includesVar(PTVarSlot varSlot, PTypeSkelBindings bindings);

  PTVarSlot getVarSlot();

  PTypeSkel join(PTypeSkel type, List<PTVarSlot> givenTVarList) throws CompileException;
    // foward to following method by combination of target and param
  PTypeSkel join2(PTypeSkel type, List<PTVarSlot> givenTVarList) throws CompileException;

  MType toMType(PModule mod, List<PTVarSlot> slotList);

  List<PTVarSlot> extractVars(List<PTVarSlot> alreadyExtracted);  // return value possibly null

  void collectTconInfo(List<PDefDict.TconInfo> list);

  PTypeSkel unalias(PTypeSkelBindings bindings);

  String repr();

  public static class InstanciationBindings {
    PTypeSkelBindings applBindings;
    Map<PTVarSlot, PTypeVarSkel> bindingDict;
    List<PTVarSlot> varSlotList;

    public static InstanciationBindings create(PTypeSkelBindings applBindings) {
      InstanciationBindings ib = new InstanciationBindings();
      ib.applBindings = applBindings;
      ib.bindingDict = new HashMap<PTVarSlot, PTypeVarSkel>();
      ib.varSlotList = new ArrayList<PTVarSlot>();
      return ib;
    }

    private InstanciationBindings() {}

    boolean isGivenTVar(PTVarSlot var) { return this.applBindings.givenTVarList.contains(var); }

    boolean isBound(PTVarSlot var) {
      return this.bindingDict.containsKey(var);
    }

    void bind(PTVarSlot var, PTypeVarSkel vs) {
      PTVarSlot s = vs.getVarSlot();
      if (this.varSlotList.contains(s)) {
        throw new IllegalArgumentException("Already added. " + s);
      }
      this.bindingDict.put(var, vs);
      this.varSlotList.add(s);
    }

    PTypeVarSkel lookup(PTVarSlot var) {
      return this.bindingDict.get(var);
    }

    boolean isBoundAppl(PTVarSlot var) {
      return this.applBindings.isBound(var);
    }

    PTypeSkel lookupAppl(PTVarSlot var) {
      return this.applBindings.lookup(var);
    }
  }

  public abstract static class Util {
    public static String repr(PTypeSkel t) {
      String r = t.repr();
      return r.endsWith(">")? r: "<" + r + ">";
    }
  }
}
