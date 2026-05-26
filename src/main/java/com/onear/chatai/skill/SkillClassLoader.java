package com.onear.chatai.skill;

import java.net.URL;
import java.net.URLClassLoader;

public class SkillClassLoader extends URLClassLoader {

    public SkillClassLoader(URL[] urls) {
        super(urls, SkillClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;

            // Parent-last: try loading ourselves first for non-JDK classes
            if (!name.startsWith("java.") && !name.startsWith("jakarta.")
                    && !name.startsWith("org.springframework.") && !name.startsWith("tools.jackson.")
                    && !name.startsWith("org.slf4j.") && !name.startsWith("com.onear.chatai.skill.")) {
                try {
                    c = findClass(name);
                    if (resolve) resolveClass(c);
                    return c;
                } catch (ClassNotFoundException ignored) {
                    // fall through to parent
                }
            }

            return super.loadClass(name, resolve);
        }
    }

    public Class<?> loadSkillClass(String name) throws ClassNotFoundException {
        return loadClass(name, true);
    }
}
