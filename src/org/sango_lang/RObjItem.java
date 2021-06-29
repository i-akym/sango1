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

import java.lang.reflect.Method;

abstract public class RObjItem extends RVMItem {
  RIntItem hashValue;

  public RObjItem(RuntimeEngine e) { super(e); }

  abstract public boolean objEquals(RFrame frame, RObjItem item);

  abstract public RType.Sig getTsig();

  final public void objHash(RNativeImplHelper helper, RClosureItem self) {
    Object ri = helper.getAndClearResumeInfo();
    if (ri == null) {
      RIntItem h = null;
      synchronized (this) {
        h = this.hashValue;
      }
      if (h != null) {
        helper.setReturnValue(h);
      } else {
        this.scheduleDoHash(helper, this);
      }
    } else {
      RIntItem h = (RIntItem)helper.getInvocationResult().getReturnValue();
      synchronized (this) {
        if (this.hashValue == null) {
          this.hashValue = h;  // make cache
        } else if (this.hashValue.getValue() == h.getValue()) {
          ;  // OK, do nothing
        } else {
          throw new RuntimeException("Hash value mismatch.");
        }
      }
      helper.setReturnValue(h);
    }
  }

  private void scheduleDoHash(RNativeImplHelper helper, Object resumeInfo) {
    RType.Sig tsig = this.getTsig();
    RClosureItem c = helper.core.getClosureItem(tsig.mod, "_call_hash_" + tsig.name.toJavaString());
    if (c != null) {
      helper.scheduleInvocation(c, new RObjItem[] { this }, resumeInfo);
    } else {
      Method impl = null;
      try {
        impl = this.getClass().getMethod(
          "doHash", new Class[] { RNativeImplHelper.class, RClosureItem.class });
      } catch (Exception ex) {
        throw new RuntimeException("Unexpected exception. " + ex.toString());
      }
      c = helper.createClosureOfNativeImpl(
        new Cstr("sango.lang"),
        "do_hash_f",
        0,
        this,
        impl);
      helper.scheduleInvocation(c, new RObjItem[0], resumeInfo);
    }
  }

  abstract public void doHash(RNativeImplHelper helper, RClosureItem self);

  public Cstr dump() {
    Cstr s = this.createDumpHeader();
    s.append(this.dumpInside());
    s.append(this.createDumpTrailer());
    return s;
  }

  public Cstr createDumpHeader() {
    Cstr r = new Cstr();
    RType.Sig tsig = this.getTsig();
    r.append('{');
    r.append(tsig.mod.repr());
    r.append('.');
    r.append(tsig.name.toJavaString());
    r.append('|');
    return r;
  }

  abstract public Cstr dumpInside();

  public Cstr createDumpTrailer() {
    Cstr r = new Cstr();
    r.append('}');
    return r;
  }

  final public void objDebugRepr(RNativeImplHelper helper, RClosureItem self) {
    if (helper.getAndClearResumeInfo() == null) {
      this.scheduleDoDebugRepr(helper, this);
    } else {
      helper.setReturnValue(helper.getInvocationResult().getReturnValue());
    }
  }

  private void scheduleDoDebugRepr(RNativeImplHelper helper, Object resumeInfo) {
    RType.Sig tsig = this.getTsig();
    RClosureItem c = helper.core.getClosureItem(tsig.mod, "_call_debug_repr_" + tsig.name.toJavaString());
    if (c != null) {
      helper.scheduleInvocation(c, new RObjItem[] { this }, resumeInfo);
    } else {
      Method impl = null;
      try {
        impl = this.getClass().getMethod(
          "doDebugRepr", new Class[] { RNativeImplHelper.class, RClosureItem.class });
      } catch (Exception ex) {
        throw new RuntimeException("Unexpected exception. " + ex.toString());
      }
      c = helper.createClosureOfNativeImpl(
        new Cstr("sango.debug"),
        "do_debug_repr_f",
        0,
        this,
        impl);
      helper.scheduleInvocation(c, new RObjItem[0], resumeInfo);
    }
  }

  public void doDebugRepr(RNativeImplHelper helper, RClosureItem self) {
    // default implementation; override this for contained RObjItems
    helper.setReturnValue(helper.cstrToArrayItem(this.dump()));
  }
}
