package android.debug;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IAdbManager extends IInterface {
    abstract class Stub extends Binder implements IAdbManager {
        public static IAdbManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }

    int getAdbWirelessPort() throws RemoteException;
}
