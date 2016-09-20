package com.test;

import java.util.HashSet;
import java.util.Set;

class PackageEntry {
    private String packageName;
    private int referenceCount;
    private Set<String> dependencies = new HashSet<>();

    PackageEntry (String name, Set<String> dependencies) {
        packageName = name;
        this.dependencies = dependencies;
        this.referenceCount = 0;
    }

    String getPackageName() {
        return packageName;
    }

    synchronized int getReferenceCount () {
        return referenceCount;
    }

    synchronized int addDependent() {
        return ++referenceCount;
    }

    synchronized int removeDependent() {
        return --referenceCount;
    }

    synchronized Set<String> getDependencies() {
        return dependencies;
    }

    synchronized void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }
}
