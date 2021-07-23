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
package sni_sango;

import java.util.ArrayList;
import java.util.List;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RActorHItem;
import org.sango_lang.RAsyncResultHItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RErefItem;
import org.sango_lang.RIntItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RResult;
import org.sango_lang.RuntimeEngine;

public class SNIactor {
  public static SNIactor getInstance(RuntimeEngine e) {
    return new SNIactor();
  }

  public void sni_my_actor_h(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getCore().myActorH());
  }

  public void sni_actor_state(RNativeImplHelper helper, RClosureItem self, RObjItem actor) {
    helper.setReturnValue(helper.getCore().getActorState((RActorHItem)actor));
  }

  public void sni_yield(RNativeImplHelper helper, RClosureItem self) {
    RNativeImplHelper.Core core = helper.getCore();
    if (helper.getAndClearResumeInfo() == null) {
      core.yield();
      core.setResumeInfo(self);  // anything ok
      core.releaseTask();
    } else {
      ;
    }
  }

  public void sni_spawn_actor(RNativeImplHelper helper, RClosureItem self, RObjItem f) {
    helper.setReturnValue(helper.getCore().spawnActor((RClosureItem)f));
  }

  public void sni_set_priority(RNativeImplHelper helper, RClosureItem self, RObjItem actor, RObjItem priority) {
    int p = ((RIntItem)priority).getValue();
    if (p >= 0 && p <= 9) {
      helper.getCore().setPriority((RActorHItem)actor, p);
    } else {
      helper.setException(
        sni_sango.SNIlang.createBadArgException(helper, new Cstr("Priority is out of range."), null));
    }
  }

  public void sni_actor_run_background(RNativeImplHelper helper, RClosureItem self, RObjItem actor, RObjItem bg) {
    helper.getCore().setBackground((RActorHItem)actor, helper.boolItemToBoolean((RStructItem)bg));
  }

  public void sni_start_actor(RNativeImplHelper helper, RClosureItem self, RObjItem actor) {
    helper.getCore().startActor((RActorHItem)actor);
  }

  public void sni_wait_some_actors_ended(RNativeImplHelper helper, RClosureItem self, RObjItem actors, RObjItem wait) {
    RNativeImplHelper.Core core = helper.getCore();
    ActorListHolder alh;
    if ((alh = (ActorListHolder)helper.getAndClearResumeInfo()) == null) {
      List<RActorHItem> al = new ArrayList<RActorHItem>();
      RObjItem a = actors;
      while (a instanceof RListItem.Cell) {
        RListItem.Cell c = (RListItem.Cell)a;
        al.add((RActorHItem)c.head);
        a = c.tail;
      }
      RStructItem w = (RStructItem)wait;
      RDataConstr ms = helper.getDataConstr(new Cstr("sango.actor"), "wait_ms$");
      Integer expiration = (w.getDataConstr().equals(ms))? ((RIntItem)w.getFieldAt(0)).getValue(): null;
      List<RActorHItem> ended = core.waitFor(al, expiration);
      if (!ended.isEmpty()) {
        helper.setReturnValue(helper.listToListItem(ended));
      } else if (expiration == null || expiration > 0) {
        core.setResumeInfo(new ActorListHolder(al));
        core.releaseTask();
      } else {
        helper.setReturnValue(helper.getListNilItem());
      }
    } else {
      List<RActorHItem> ended = core.waitFor(alh.actorHList, 0);
      helper.setReturnValue(helper.listToListItem(ended));
    }
  }

  static private class ActorListHolder {
    List<RActorHItem> actorHList;

    ActorListHolder(List<RActorHItem> as) {
      this.actorHList = as;
    }
  }

  public void sni_async_result_peek(RNativeImplHelper helper, RClosureItem self, RObjItem result) {
    RResult res = helper.getCore().peekAsyncResult((RAsyncResultHItem)result);
    RObjItem r;
    if (res != null) {
      RObjItem rr;
      if (res.endCondition() == RResult.NORMAL_END) {
        RDataConstr dcFin = helper.getDataConstr(Module.MOD_LANG, "fin$");
        rr = helper.getStructItem(dcFin, new RObjItem[] { res.getReturnValue() });
      } else {
        RDataConstr dcExc = helper.getDataConstr(Module.MOD_LANG, "exc$");
        rr = helper.getStructItem(dcExc, new RObjItem[] { res.getException() });
      }
      r = sni_sango.SNIlang.getMaybeItem(helper, rr);
    } else {
      r = sni_sango.SNIlang.getMaybeItem(helper, null);
    }
    helper.setReturnValue(r);
  }

  public void sni_create_mbox(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getCore().createMbox());
  }

  public void sni_notify_sysmsg(RNativeImplHelper helper, RClosureItem self, RObjItem be) {
    helper.getCore().notifySysMsg((RErefItem)be);
  }

  public void sni_mbe_put_msg(RNativeImplHelper helper, RClosureItem self, RObjItem be, RObjItem m) {
    helper.getCore().putMsg((RErefItem)be, m);
  }

  public void sni_mbe_owner(RNativeImplHelper helper, RClosureItem self, RObjItem be) {
    helper.setReturnValue(helper.getCore().getOwnerOfMbox((RErefItem)be));
  }

  public void sni_mbe_listen_multiple(RNativeImplHelper helper, RClosureItem self, RObjItem bes, RObjItem wait) {
    RNativeImplHelper.Core core = helper.getCore();
    BEListHolder belh;
    if ((belh = (BEListHolder)helper.getAndClearResumeInfo()) == null) {
      RActorHItem myActorH = core.myActorH();
      List<RErefItem> bel = new ArrayList<RErefItem>();
      RListItem L = (RListItem)bes;
      RObjItem e = null;
      while (e == null && (L instanceof RListItem.Cell)) {
        RListItem.Cell c = (RListItem.Cell)L;
        RErefItem be = (RErefItem)c.head;
        if (!helper.objEquals(helper.getCore().getOwnerOfMbox(be), myActorH)) {
          e = sni_sango.SNIlang.createBadArgException(helper, new Cstr("Caller is not the owner of mbox."), null);
        }
        bel.add(be);
        L = c.tail;
      }
      List<RErefItem> receivables = null;
      Integer expiration = null;
      if (e == null) {
        RStructItem w = (RStructItem)wait;
        RDataConstr ms = helper.getDataConstr(new Cstr("sango.actor"), "wait_ms$");
        expiration = (w.getDataConstr().equals(ms))? ((RIntItem)w.getFieldAt(0)).getValue(): null;
        receivables = core.listenMboxes(bel, expiration);
      }
      if (e != null) {
        helper.setException(e);
      } else if (!receivables.isEmpty()) {
        helper.setReturnValue(helper.listToListItem(receivables));
      } else if (expiration == null || expiration > 0) {
        core.setResumeInfo(new BEListHolder(bel));
        core.releaseTask();
      } else {
        helper.setReturnValue(helper.getListNilItem());
      }
    } else {
      List<RErefItem> receivables = core.listenMboxes(belh.bel, 0);
      helper.setReturnValue(helper.listToListItem(receivables));
    }
  }

  static private class BEListHolder {  // avoid compilation warning
    List<RErefItem> bel;
 
    BEListHolder(List<RErefItem> bel) {
      this.bel = bel;
    }
  }

  public void sni_mbe_receive_msg(RNativeImplHelper helper, RClosureItem self, RObjItem be) {
    RErefItem bee = (RErefItem)be;
    RActorHItem myActorH = helper.getCore().myActorH();
    if (helper.objEquals(helper.getCore().getOwnerOfMbox(bee), myActorH)) {
      RObjItem m = helper.getCore().receiveMsg(bee);
      RObjItem ret = sni_sango.SNIlang.getMaybeItem(helper, m);
      helper.setReturnValue(ret);
    } else {
      RObjItem e = sni_sango.SNIlang.createBadArgException(helper, new Cstr("Caller is not the owner of mbox."), null);
      helper.setException(e);
    }
  }

  public void sni_start_monitoring(RNativeImplHelper helper, RClosureItem self, RObjItem a, RObjItem p) {
    helper.getCore().addActorMonitor(a, (RErefItem)((RStructItem)p).getFieldAt(0));
  }
  public void sni_stop_monitoring(RNativeImplHelper helper, RClosureItem self, RObjItem a, RObjItem p) {
    helper.getCore().removeActorMonitor(a, (RErefItem)((RStructItem)p).getFieldAt(0));
  }
}
