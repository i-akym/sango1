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
package sni_sango.sni_system;
 
import java.lang.reflect.Method;
import java.io.File;
import java.util.ArrayList;
import org.sango_lang.Cstr;
import org.sango_lang.RActorHItem;
import org.sango_lang.RArrayItem;
import org.sango_lang.RListItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;
import org.sango_lang.RStructItem;

public class SNIos {
  public static SNIos getInstance(RuntimeEngine e) {
    return new SNIos();
  }

  public void sni_exec_impl(RNativeImplHelper helper, RClosureItem self, RObjItem command, RObjItem dir, RObjItem opts) {
    try {
      ArrayList<String> cmdTokens = new ArrayList<String>();
      RObjItem L = command;
      while (L instanceof RListItem.Cell) {
        RListItem.Cell lc = (RListItem.Cell)L;
        cmdTokens.add(helper.arrayItemToCstr((RArrayItem)lc.head).toJavaString());
        L = lc.tail;
      }
      RArrayItem aDir = (RArrayItem)dir;
      RStructItem sOpts = (RStructItem)opts;
      boolean redirectErr = helper.boolItemToBoolean((RStructItem)sOpts.getFieldAt(0));
      ProcessBuilder pb = new ProcessBuilder();
      pb.command(cmdTokens);
      if (aDir.getElemCount() > 0) {
        pb.directory(new File(helper.arrayItemToCstr(aDir).toJavaString()));
      }
      if (redirectErr) {
        pb.redirectErrorStream();
      }
      Process p = pb.start();
      ProcessPeer peer = new ProcessPeer(p);
      Method exec = peer.getClass().getMethod("_waitFor", new Class[] { RNativeImplHelper.class, RClosureItem.class });
      Method term = peer.getClass().getMethod("term", new Class[] { RNativeImplHelper.class, RClosureItem.class });
      RClosureItem cExec = helper.createClosureOfNativeImplHere("_waitFor", 0, /* "_waitFor", */ peer, exec);
      RClosureItem cTerm = helper.createClosureOfNativeImplHere("term_sig", 0, /* "term_sig", */ peer, term);
      RObjItem to_in = sni_sango.SNIio.openByteOutstreamToJavaOutputStream(helper, p.getOutputStream());
      RObjItem from_out = sni_sango.SNIio.openByteInstreamFromJavaInputStream(helper, p.getInputStream());
      RObjItem from_err_ = sni_sango.SNIlang.getMaybeItem(helper,
        redirectErr? null: sni_sango.SNIio.openByteInstreamFromJavaInputStream(helper, p.getErrorStream()));
      RObjItem r = helper.getTupleItem(new RObjItem[] { cExec, to_in, from_out, from_err_, cTerm });
      helper.setReturnValue(r);
    } catch (Exception ex) {
      RObjItem e = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
      helper.setException(e);
    }
  }

  public static class ProcessPeer {
    Process process;

    ProcessPeer(Process p) {
      this.process = p;
    }

    public void _waitFor(RNativeImplHelper helper, RClosureItem self) {
// /* DEBUG */ System.out.println("_waitFor started.");
      try {
        this.process.waitFor();
        helper.setReturnValue(helper.getIntItem(this.process.exitValue()));
      } catch (InterruptedException ex) {
        RObjItem e = sni_sango.SNIio.createInterruptedIOFailureException(helper, new Cstr("Interrupted."), null);
        helper.setException(e);
      }
// /* DEBUG */ System.out.println("_waitFor ended.");
    }

    public void term(RNativeImplHelper helper, RClosureItem self) {
      this.process.destroy();
    }
  }
}
