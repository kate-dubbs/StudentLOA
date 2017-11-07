import java.net.*;
import java.util.concurrent.locks.*;

/**
 * Title:        LOADaemon
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 */

public class Status {

    public int index = 0;
    public Socket black = null;
    public Socket white = null;
    public Socket observer[] = new Socket[10];
    public int num_observers = 0;
    public boolean time_controls = false;
    public long white_msecs = 0;
    public long black_msecs = 0;
    public final Lock lock = new ReentrantLock();

    public Status() {
      for ( int i = 0; i < 10; i++ )
        observer[i] = null;
    }
    
    
}