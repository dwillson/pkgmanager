package com.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class PackageManager implements Runnable {
    private Socket socket;
    private static Map<String, PackageEntry> packages = new ConcurrentHashMap<>();

    private final static String INDEX = "INDEX";
    private final static String REMOVE = "REMOVE";
    private final static String QUERY = "QUERY";

    final static String OK = "OK";
    final static String ERROR = "ERROR";
    final static String FAIL = "FAIL";

    // input format example: INDEX|package|dep1,dep2
    // must contain both pipe characters, dependencies are optional
    private final static String INPUT_REGEX = "(" + INDEX + "|" + REMOVE + "|" + QUERY + ")\\|[\\w\\-\\+]+\\|([\\w\\-\\+]+(,[\\w\\-\\+]+)*)?";

    PackageManager(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try  {
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintStream pstream = new PrintStream(socket.getOutputStream());

            String str;
            while((str = socketReader.readLine()) != null) {
                pstream = new PrintStream(socket.getOutputStream());

                // validate input
                if (!str.matches(INPUT_REGEX)) {
                    pstream.println(ERROR);
                    continue;
                }

                String[] inputParts = str.split("\\|");
                String command = inputParts[0];
                String packageName = inputParts[1];
                Set<String> dependencies = new HashSet<>();
                if (inputParts.length > 2) {
                    String packagesPart = inputParts[2];
                    String[] rawPackages = (packagesPart.split(","));
                    dependencies.addAll(Arrays.asList(rawPackages));
                }

                // process command
                switch (command) {
                    case INDEX:
                        reportResult(pstream, index(packageName, dependencies));
                        break;
                    case REMOVE:
                        reportResult(pstream, remove(packageName));
                        break;
                    case QUERY:
                        reportResult(pstream, query(packageName));
                        break;
                    default:
                        pstream.println(ERROR);
                        break;
                }
            }
            pstream.close();
            socketReader.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private synchronized boolean query(String packageName) {
        return packages.containsKey(packageName);
    }

    private synchronized boolean index(String packageName, Set<String> dependencies) {
        PackageEntry entry = packages.get(packageName);
        Set<PackageEntry> addedDependencies = new HashSet<>();
        Set<PackageEntry> unusedDependencies = new HashSet<>();
        if (entry == null) {
            // new package
            entry = new PackageEntry(packageName, dependencies);
            for(String s: dependencies) {
                PackageEntry p = packages.get(s);
                if(p == null) {
                    return false;
                }
                addedDependencies.add(p);
            }

        } else {
            Set<String> existingDependencies = entry.getDependencies();
            for(String s: dependencies) {
                PackageEntry p = packages.get(s);
                if(p == null) {
                    return false;
                }
                if(!existingDependencies.contains(s)) {
                    addedDependencies.add(p);
                }
            }
            for(String s: existingDependencies) {
                if(!dependencies.contains(s)) {
                    unusedDependencies.add(packages.get(s));
                }
            }
        }

        // increment ref count on dependencies
        for(PackageEntry s: addedDependencies) {
            s.addDependent();
        }

        // decrement ref count on removed dependencies
        for(PackageEntry s: unusedDependencies) {
            PackageEntry removing = packages.get(s.getPackageName());
            removing.removeDependent();
        }

        entry.setDependencies(dependencies);
        packages.put(packageName, entry);
        return true;
    }

    private synchronized boolean remove(String packageName) {
        PackageEntry entry = packages.get(packageName);
        if(entry == null) {
            return true;
        }
        if (entry.getReferenceCount() > 0) {
            return false;
        }

        for(String s: entry.getDependencies()) {
            packages.get(s).removeDependent();
        }
        packages.remove(packageName);
        return true;
    }

    private void reportResult(PrintStream output, boolean success) {
        if(success) {
            output.println(OK);
            return;
        }
        output.println(FAIL);
    }
}
