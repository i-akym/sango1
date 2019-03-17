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

  PTypeSkel instanciate(InstanciationBindings iBindings);

  PTypeSkel resolveBindings(PTypeSkelBindings bindings);

  PTypeSkelBindings applyTo(PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException;

  PTypeSkelBindings applyTo2(PTypeSkel type, PTypeSkelBindings trialBindings) throws CompileException;

  boolean includesVar(PVarSlot varSlot, PTypeSkelBindings bindings);

  PVarSlot getVarSlot();

  PTypeSkel join(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException;

  PTypeSkel join2(PTypeSkel type, PTypeSkelBindings bindings) throws CompileException;

  MType toMType(PModule mod, List<PVarSlot> slotList);

  List<PVarSlot> extractVars(List<PVarSlot> alreadyExtracted);  // return value possibly null

  void collectTconInfo(List<PDefDict.TconInfo> list);

  PTypeSkel unalias(PTypeSkelBindings bindings);

  GFlow.Node setupFlow(GFlow flow, PScope scope, PTypeSkelBindings bindings);

  String repr();

  public static class InstanciationBindings {
    PTypeSkelBindings applBindings;
    Map<PVarSlot, PTypeVarSkel> bindingDict;
    List<PVarSlot> varSlotList;

    public static InstanciationBindings create(PTypeSkelBindings applBindings) {
      InstanciationBindings ib = new InstanciationBindings();
      ib.applBindings = applBindings;
      ib.bindingDict = new HashMap<PVarSlot, PTypeVarSkel>();
      ib.varSlotList = new ArrayList<PVarSlot>();
      return ib;
    }

    private InstanciationBindings() {}

    boolean isGivenTvar(PVarSlot var) { return this.applBindings.givenTvarList.contains(var); }

    boolean isBound(PVarSlot var) {
      return this.bindingDict.containsKey(var);
    }

    void bind(PVarSlot var, PTypeVarSkel vs) {
      PVarSlot s = vs.getVarSlot();
      if (this.varSlotList.contains(s)) {
        throw new IllegalArgumentException("Already added. " + s);
      }
      this.bindingDict.put(var, vs);
      this.varSlotList.add(s);
    }

    PTypeVarSkel lookup(PVarSlot var) {
      return this.bindingDict.get(var);
    }

    boolean isBoundAppl(PVarSlot var) {
      return this.applBindings.isBound(var);
    }

    PTypeSkel lookupAppl(PVarSlot var) {
      return this.applBindings.lookup(var);
    }

    List<PVarSlot> getVarSlotList() {
      List<PVarSlot> vl = new ArrayList<PVarSlot>(this.applBindings.virtualTvarList);
      vl.addAll(this.varSlotList);
      return vl;
    }
  }

  public abstract static class Util {
    public static String repr(PTypeSkel t) {
      String r = t.repr();
      return r.endsWith(">")? r: "<" + r + ">";
    }
  }
}
