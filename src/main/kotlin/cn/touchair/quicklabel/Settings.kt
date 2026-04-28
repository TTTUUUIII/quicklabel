package cn.touchair.quicklabel

import java.io.File
import java.util.Properties

object Settings {
    private val settings by lazy {
        Properties()
            .apply {
                val file = FILE
                if (file.exists()) {
                    file.inputStream().use {
                        load(it)
                    }
                }
            }
    }

    fun put(k: String, v: String) {
        settings[k] = v
        FILE.outputStream()
            .use {
                settings.store(it, null)
            }
    }

    fun get(k: String, v: String = ""): String {
        return settings[k] as? String ?: v
    }

    fun getBool(k: String, v: Boolean): Boolean {
        return if (v) {
            get(k, "on") == "on"
        } else {
            get(k, "off") == "on"
        }
    }

    fun putBool(k: String, v: Boolean) {
        if (v) {
            put(k, "on")
        } else {
            put(k, "off")
        }
    }

    private val FILE = File("settings.properties")
}