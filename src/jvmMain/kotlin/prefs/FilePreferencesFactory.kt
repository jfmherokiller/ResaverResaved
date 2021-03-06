package prefs

import mu.KLoggable
import mu.KLogger
import java.io.File
import java.util.prefs.Preferences
import java.util.prefs.PreferencesFactory

/**
 * PreferencesFactory implementation that stores the preferences in a user-defined file. To use it,
 * set the system property <tt>java.util.prefs.PreferencesFactory</tt> to
 * <tt>prefs.FilePreferencesFactory</tt>
 *
 *
 * The file defaults to [user.home]/.fileprefs, but may be overridden with the system property
 * <tt>prefs.FilePreferencesFactory.file</tt>
 *
 * @author David Croft ([www.davidc.net](http://www.davidc.net))
 * @version $Id: FilePreferencesFactory.java 282 2009-06-18 17:05:18Z david $
 */
class FilePreferencesFactory : PreferencesFactory {
    var rootPreferences: Preferences? = null
    override fun systemRoot(): Preferences {
        return userRoot()
    }

    override fun userRoot(): Preferences {
        if (rootPreferences == null) {
            logger.debug {"Instantiating root preferences"}
            rootPreferences = FilePreferences(null, "")
        }
        return rootPreferences!!
    }

    companion object:KLoggable {
        const val SYSTEM_PROPERTY_FILE = "prefs.FilePreferencesFactory.file"

        /*public static void main(String[] args) throws BackingStoreException
  {
    System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
    System.setProperty(SYSTEM_PROPERTY_FILE, "myprefs.txt");
 
    Preferences p = Preferences.userNodeForPackage(FilePreferencesFactory.class);
 
    for (String s : p.keys()) {
      System.out.println("p[" + s + "]=" + p.get(s, null));
    }
 
    p.putBoolean("hi", true);
    p.put("Number", String.valueOf(System.currentTimeMillis()));
  }*/  @JvmStatic
        var preferencesFile: File? = null
            get() {
                if (field == null) {
                    var prefsFile = System.getProperty(SYSTEM_PROPERTY_FILE)
                    if (prefsFile == null || prefsFile.isEmpty()) {
                        prefsFile = System.getProperty("user.home") + File.separator + ".fileprefs"
                    }
                    field = File(prefsFile).absoluteFile
                    logger.error {"Preferences file is $field"}
                }
                return field
            }
            private set
        override val logger: KLogger
            get() = logger()
    }
}