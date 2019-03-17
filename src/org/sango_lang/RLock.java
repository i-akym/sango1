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

import java.util.ArrayList;
import java.util.List;

class RLock {
  static final int SHARED = 1;
  static final int EXCLUSIVE = 2;

  List<Client> sharedClientList;
  Client exclusiveClient;

  static RLock create() {
    return new RLock();
  }

  private RLock() {
    this.sharedClientList = new ArrayList<Client>();
  }

  Client createClient() {
    return new Client();
  }

  private void lock(Client c) {
    synchronized (this) {
      boolean finished = false;
      while (!finished) {
        switch (c.mode) {
        case SHARED:
          if (this.exclusiveClient != null) {
            try { this.wait(); } catch (InterruptedException ex) {}
          } else {
            this.sharedClientList.add(c);
            finished = true;
          }
          break;
        case EXCLUSIVE:
          if (this.exclusiveClient != null || this.sharedClientList.size() > 0) {
            try { this.wait(); } catch (InterruptedException ex) {}
          } else {
            this.exclusiveClient = c;
            finished = true;
          }
          break;
        default:
          throw new IllegalArgumentException("Invalid mode.");  // already guarded by caller
        }
      }
    }
  }

  private void unlock(Client c) {
    synchronized (this) {
      this.checkLocked(c);
      switch (c.mode) {
      case SHARED:
        this.sharedClientList.remove(c);
        this.notifyAll();
        break;
      case EXCLUSIVE:
        this.exclusiveClient = null;
        this.notifyAll();
        break;
      default:
        throw new IllegalArgumentException("Invalid mode.");  // already guarded by caller
      }
    }
  }

  private void waitSignal(Client c, SignalReceiver r) {
    Object s;
    synchronized (this) {
      this.checkLocked(c);
      s = r.lastSignal;
    }
    this.unlock(c);
    synchronized (r) {
      if (r.lastSignal == s) {
        try {
          r.wait();
        } catch (InterruptedException ex) {}
      }
    }
    this.lock(c);
  }

  private void sendSignal(Client c, SignalReceiver r) {
    synchronized (this) {
      this.checkLocked(c);
      r.lastSignal = new Object();
      synchronized (r) {
        r.notifyAll();
      }
    }
  }

  private void checkLocked(Client c) {  // in critical section
    switch (c.mode) {
    case SHARED:
      if (!this.sharedClientList.contains(c)) {
        throw new IllegalStateException("Not hold lock.");
      }
      break;
    case EXCLUSIVE:
      if (this.exclusiveClient != c) {
        throw new IllegalStateException("Not hold lock.");
      }
      break;
    default:
      throw new IllegalArgumentException("Invalid mode.");  // already guarded by caller
    }
  }

  class Client {
    int mode;

    void require(int m) {
      if (m == SHARED || m == EXCLUSIVE) {
        ;
      } else {
        throw new IllegalArgumentException("Invalid mode.");
      }
      if (this.mode != 0) {
        throw new IllegalStateException("Already active.");
      }
      this.mode = m;
      RLock.this.lock(this);
    }
  
    void release() {
      if (this.mode == 0) {
        throw new IllegalStateException("Inactive.");
      }
      RLock.this.unlock(this);
      this.mode = 0;
    }

    void waitSignal(SignalReceiver r) {
      if (this.mode == 0) {
        throw new IllegalStateException("Inactive.");
      }
      RLock.this.waitSignal(this, r);
    }

    void sendSignal(SignalReceiver r) {
      if (this.mode == 0) {
        throw new IllegalStateException("Inactive.");
      }
      RLock.this.sendSignal(this, r);
    }
  }

  SignalReceiver createSignalReceiver() {
    return new SignalReceiver();
  }

  class SignalReceiver {
    Object lastSignal;
  }
}
