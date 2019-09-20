/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2019 Isao Akiyama                                         *
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RClientHelper {
  RuntimeEngine theEngine;

  RClientHelper(RuntimeEngine eng) {
    this.theEngine = eng;
  }

  public RuntimeEngine getRuntimeEngine() { return this.theEngine; }

  // environment

  public Locale getLocale() { return this.theEngine.getLocale(); }

  public Cstr getProgName() { return this.theEngine.getProgName(); }

  public List<Cstr> getArgs() { return this.theEngine.getArgs(); }

  public List<File> getSysLibPaths() { return this.theEngine.getSysLibPaths(); }

  // handling object

  public RStructItem getBoolItem(boolean b) {
    return this.theEngine.memMgr.getBoolItem(b);
  }

  public boolean boolItemToBoolean(RStructItem b) {
    RDataConstr dc = b.dataConstr;
    if (dc.modName.equals(Module.MOD_LANG) && dc.tcon.equals("bool")) {
      ;
    } else {
      throw new IllegalArgumentException("Not bool item.");
    }
    return dc.name.equals("true$")? true: false;
  }

  public RIntItem getByteItem(int b) {
    return this.theEngine.memMgr.getByteItem(b);
  }

  public RIntItem getCharItem(int c) {
    return this.theEngine.memMgr.getCharItem(c);
  }

  public RIntItem getIntItem(int i) {
    return this.theEngine.memMgr.getIntItem(i);
  }

  public RRealItem getRealItem(double d) {
    return this.theEngine.memMgr.getRealItem(d);
  }

  public RRealItem getNaNItem() {
    return this.theEngine.memMgr.getNaNItem();
  }

  public RRealItem getPosInfItem() {
    return this.theEngine.memMgr.getPosInfItem();
  }

  public RRealItem getNegInfItem() {
    return this.theEngine.memMgr.getNegInfItem();
  }

  public RStructItem getVoidItem() {
    return this.theEngine.memMgr.getVoidItem();
  }

  public RListItem.Nil getListNilItem() {
    return this.theEngine.memMgr.getListNilItem();
  }

  public RListItem.Cell createListCellItem() {
    return this.theEngine.memMgr.createListCellItem();
  }

  public RDataConstr getDataConstr(Cstr modName, String name) {
    return this.theEngine.memMgr.getDataConstr(modName, name);
  }

  public RStructItem getStructItem(RDataConstr dataConstr, RObjItem[] attrs) {
    return this.theEngine.memMgr.getStructItem(dataConstr, attrs);
  }

  public RStructItem getTupleItem(RObjItem[] elems) {
    return this.theEngine.memMgr.getTupleItem(elems);
  }

  public RArrayItem createArrayItem(int size) {
    return this.theEngine.memMgr.createArrayItem(size);
  }

  public RArrayItem cstrToArrayItem(Cstr cstr) {
    return this.theEngine.memMgr.cstrToArrayItem(cstr);
  }

  public Cstr arrayItemToCstr(RArrayItem array) {
    return this.theEngine.memMgr.arrayItemToCstr(array);
  }

  public RListItem listToListItem(List<? extends RObjItem> os) {
    // programmer must guarantee type consistency
    RListItem L = this.getListNilItem();
    for (int i = os.size() - 1; i >= 0; i--) {
      RListItem.Cell c = this.createListCellItem();
      c.tail = L;
      c.head = os.get(i);
      L = c;
    }
    return L;
  }

  public RClosureItem createClosureOfNativeImpl(Cstr modName, String name, int paramCount, Object nativeImplTargetObject, Method nativeImpl) {
    RModule mod = this.theEngine.modMgr.getRMod(modName);
    if (mod == null) {
      throw new IllegalArgumentException("Invalid module name.");
    }
    return this.theEngine.memMgr.createClosureOfNativeImpl(mod, name, paramCount, nativeImplTargetObject, nativeImpl);
  }

  // invocation

  public RResult apply(RObjItem[] params, RClosureItem closure) {
    RTaskControl execTask = this.theEngine.taskMgr.createTask(
      RTaskMgr.PRIO_DEFAULT,
      RTaskMgr.TASK_TYPE_APPL,
      closure,
      params);
    ResultHolder rh = new ResultHolder();
    ResultSetter setter = new ResultSetter(execTask, rh);
    Method setterImpl = null;
    try {
      setterImpl = setter.getClass().getMethod(
        "transfer", new Class[] { RNativeImplHelper.class, RClosureItem.class });
    } catch (Exception ex) {
      throw new RuntimeException("Unexpected exception. " + ex.toString());
    }
    RClosureItem resClosure = this.createClosureOfNativeImpl(
      Module.MOD_LANG,
      "transfer_result",
      0,
      setter,
      setterImpl);
    RTaskControl resTask = this.theEngine.taskMgr.createTask(
      8,  // good?
      RTaskMgr.TASK_TYPE_APPL,
      resClosure);
    resTask.terminateOnAbnormalEnd = true;  // ***
    execTask.start();
    resTask.startWaitingFor(execTask);
    return rh.getResult();
  }

  // misc

  public Version getVersion() { return RuntimeEngine.getVersion(); }

  // 

  private class ResultHolder {
    RResult result;

    RResult getResult() {
      RResult r = null;
      while (r == null) {
        synchronized (this) {
          r = this.result;
          if (r == null) {
            try {
              this.wait();
            } catch (InterruptedException ex) {}
          }
        }
      }
      return r;
    }

    void putResult(RResult r) {
      synchronized (this) {
        this.result = r;
        this.notify();
      }
    }
  }

  private class ResultSetter {
    RTaskControl execTask;
    ResultHolder resultHolder;

    ResultSetter(RTaskControl exec, ResultHolder res) {
      this.execTask = exec;
      this.resultHolder = res;
    }

    public void transfer(RNativeImplHelper helper, RClosureItem self) {
      this.resultHolder.putResult(this.execTask.getResult());
    }
  }
}
