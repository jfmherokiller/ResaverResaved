package prefs


import mu.KLoggable
import mu.KLogger
import prefs.FilePreferencesFactory.Companion.preferencesFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.prefs.AbstractPreferences
import java.util.prefs.BackingStoreException

/**
 * Preferences implementation that stores to a user-defined file. See FilePreferencesFactory.
 *
 * @author David Croft ([www.davidc.net](http://www.davidc.net))
 * @version $Id: FilePreferences.java 283 2009-06-18 17:06:58Z david $
 */
class FilePreferences(parent: AbstractPreferences?, name: String) : AbstractPreferences(parent, name) {
    private val root: MutableMap<String, String>
    private val children: MutableMap<String, FilePreferences>
    private var removed = false
    override fun putSpi(key: String, value: String) {
        root[key] = value
        try {
            flush()
        } catch (e: BackingStoreException) {
            logger.error(e) {"Unable to flush after putting $key"}
        }
    }

    override fun getSpi(key: String): String {
        return root[key]!!
    }

    override fun removeSpi(key: String) {
        root.remove(key)
        try {
            flush()
        } catch (e: BackingStoreException) {
            logger.error(e) {"Unable to flush after removing $key"}
        }
    }

    @Throws(BackingStoreException::class)
    override fun removeNodeSpi() {
        removed = true
        flush()
    }

    @Throws(BackingStoreException::class)
    override fun keysSpi(): Array<String> {
        return root.keys.toTypedArray()
    }

    @Throws(BackingStoreException::class)
    override fun childrenNamesSpi(): Array<String> {
        return children.keys.toTypedArray()
    }

    override fun childSpi(name: String): FilePreferences {
        var child = children[name]
        if (child == null || child.isRemoved) {
            child = FilePreferences(this, name)
            children[name] = child
        }
        return child
    }

    @Throws(BackingStoreException::class)
    override fun syncSpi() {
        if (removed) return
        val file = preferencesFile
        if (!file!!.exists()) return
        synchronized(file) {
            val p = Properties()
            try {
                p.load(FileInputStream(file))
                val sb = StringBuilder()
                getPath(sb)
                val path = sb.toString()
                val pnen = p.propertyNames()
                while (pnen.hasMoreElements()) {
                    val propKey = pnen.nextElement() as String
                    if (propKey.startsWith(path)) {
                        val subKey = propKey.substring(path.length)
                        // Only load immediate descendants
                        if (subKey.indexOf('.') == -1) {
                            root[subKey] = p.getProperty(propKey)
                        }
                    }
                }
            } catch (e: IOException) {
                throw BackingStoreException(e)
            }
        }
    }

    private fun getPath(sb: StringBuilder) {
        val parent = parent() as FilePreferences
        parent.getPath(sb)
        sb.append(name()).append('.')
    }

    @Throws(BackingStoreException::class)
    override fun flushSpi() {
        val file = preferencesFile
        synchronized(file!!) {
            val p = Properties()
            try {
                val sb = StringBuilder()
                getPath(sb)
                val path = sb.toString()
                if (file.exists()) {
                    p.load(FileInputStream(file))
                    val toRemove: MutableList<String> = ArrayList()

                    // Make a list of all direct children of this node to be removed
                    val pnen = p.propertyNames()
                    while (pnen.hasMoreElements()) {
                        val propKey = pnen.nextElement() as String
                        if (propKey.startsWith(path)) {
                            val subKey = propKey.substring(path.length)
                            // Only do immediate descendants
                            if (subKey.indexOf('.') == -1) {
                                toRemove.add(propKey)
                            }
                        }
                    }

                    // Remove them now that the enumeration is done with
                    for (propKey in toRemove) {
                        p.remove(propKey)
                    }
                }

                // If this node hasn't been removed, add back in any values
                if (!isRemoved) {
                    for (s in root.keys) {
                        p.setProperty(path + s, root[s])
                    }
                }
                p.store(FileOutputStream(file), "FilePreferences")
            } catch (e: IOException) {
                throw BackingStoreException(e)
            }
        }
    }

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
    }

    init {
        logger.debug {"Instantiating node $name"}
        root = TreeMap()
        children = TreeMap()
        try {
            sync()
        } catch (e: BackingStoreException) {
            logger.error(e) {"Unable to sync on creation of node $name"}
        }
    }
}