import android.os.IBinder;
import android.content.pm.PackageInstaller;
import java.io.*;
public class SessionInstaller {
 public static void main(String[] args) throws Exception {
  if (args.length != 1 || !new File(args[0]).isFile()) throw new IllegalArgumentException("Pass one readable APK path");
  IBinder b=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"package");
  Object pm=Class.forName("android.content.pm.IPackageManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,b);
  Object pi=Class.forName("android.content.pm.IPackageManager").getMethod("getPackageInstaller").invoke(pm);
  PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(1); params.getClass().getField("installFlags").setInt(params,2); int id=(Integer)Class.forName("android.content.pm.IPackageInstaller").getMethod("createSession",PackageInstaller.SessionParams.class,String.class,int.class).invoke(pi,params,"com.android.shell",0);
  System.out.println("SESSION_ID="+id);
  Object bs=Class.forName("android.content.pm.IPackageInstaller").getMethod("openSession",int.class).invoke(pi,id);
  java.lang.reflect.Constructor<?> ctor=PackageInstaller.Session.class.getDeclaredConstructor(Class.forName("android.content.pm.IPackageInstallerSession"));ctor.setAccessible(true);
  try(PackageInstaller.Session session=(PackageInstaller.Session)ctor.newInstance(bs);InputStream in=new FileInputStream(args[0]);OutputStream out=session.openWrite("base.apk",0,new File(args[0]).length())) {byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);session.fsync(out);}
  System.out.println("SESSION_WRITTEN="+id);
 }
}
