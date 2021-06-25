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

import java.util.IdentityHashMap;

public class RStructItem extends RObjItem {
  RDataConstr dataConstr;  // RDataConstr.pseudoOfTuple if tuple
  RObjItem[] fields;

  RStructItem(RuntimeEngine e, RDataConstr dataConstr, RObjItem[] fields) {
    super(e);
    this.dataConstr = dataConstr;
    this.fields = fields;
  }

  static RStructItem create(RuntimeEngine e, RDataConstr dataConstr, RObjItem[] fields) {
    if (dataConstr == null) { throw new IllegalArgumentException("Null data constructor."); }
    if (dataConstr.attrCount < 0 || fields.length == dataConstr.attrCount) {
      ;
    } else {
      throw new IllegalArgumentException("Attribute count mismatch.");
    }
    return new RStructItem(e, dataConstr, fields);
  }

  public boolean objEquals(RFrame frame, RObjItem item) {
    boolean b;
    if (item == this) {
      b = true;
    } else if (!(item instanceof RStructItem)) {
      b = false;
    } else {
      RStructItem s = (RStructItem)item;
      if (!this.dataConstr.equals(s.dataConstr)) {
        b = false;
      } else if (s.fields.length != this.fields.length) {
        b = false;
      } else {
        b = true;
        for (int i = this.fields.length - 1; i >= 0; i--) {
          frame.os.push(this.fields[i]);
          frame.os.push(s.fields[i]);
        }
      }
    }
    return b;
  }

  public RType.Sig getTsig() {
    RType.Sig tsig;
    if (this.dataConstr == RDataConstr.pseudoOfTuple) {
      tsig = RType.createTsig(Module.MOD_LANG, "tuple", this.fields.length);
    } else {
      tsig = this.dataConstr.getTsig(this.theEngine);
    }
    return tsig;
  }

  public Cstr dumpInside() {
    Cstr s = new Cstr();
    String sep;
    if (this.dataConstr != RDataConstr.pseudoOfTuple) {
      s.append(this.dataConstr.name);
      sep = ";";
    } else {
      sep = "";
    }
    for (int i = 0; i < this.fields.length; i++) {
      s.append(sep);
      s.append(this.fields[i].dump());
      sep = ",";
    }
    return s;
  }

  public void debugRepr(RNativeImplHelper helper, RClosureItem self) {
    Object[] ris = (Object[])helper.getAndClearResumeInfo();
    if (ris == null) {
      if (this.fields.length == 0) {
        Cstr r = this.createDumpHeader();
        r.append(this.dataConstr.name);
        r.append(this.createDumpTrailer());
        helper.setReturnValue(helper.cstrToArrayItem(r));
      } else {
        Cstr r = this.createDumpHeader();
        if (this.dataConstr != RDataConstr.pseudoOfTuple) {
          r.append(this.dataConstr.name);
          r.append(';');
        }
        helper.scheduleDebugRepr(this.fields[0], new Object[] { 0, r });
      }
    } else {
      RArrayItem rx = (RArrayItem)helper.getInvocationResult().getReturnValue();
      int current = (Integer)ris[0];
      Cstr r = (Cstr)ris[1];
      r.append(helper.arrayItemToCstr(rx));
      int next = current + 1;
      if (next < this.fields.length) {
        r.append(',');
        helper.scheduleDebugRepr(this.fields[next], new Object[] { next, r });
      } else {
        r.append(this.createDumpTrailer());
        helper.setReturnValue(helper.cstrToArrayItem(r));
      }
    }
  }

  public boolean equals(Object o) {
    boolean eq;
    if (o == this) {
      eq = true;
    } else if (!(o instanceof RStructItem)) {
      eq = false;
    } else {
      RStructItem s = (RStructItem)o;
      if ((s.dataConstr == this.dataConstr)
          || (s.dataConstr != null && s.dataConstr.equals(this.dataConstr))) {
        eq = true;
        for (int i = 0; i < this.fields.length; i++) {
          if (!s.fields[i].equals(this.fields[i])) {
            eq = false;
            break;
          }
        }
      } else {
        eq = false;
      }
    }
    return eq;
  }

  public RDataConstr getDataConstr() { return this.dataConstr; }

  public int getFieldCount() { return this.fields.length; }

  public RObjItem getFieldAt(int index) { return this.fields[index]; }

}
