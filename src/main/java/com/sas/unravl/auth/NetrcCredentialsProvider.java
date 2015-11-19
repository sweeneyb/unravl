// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides authentication credentials for an UnRAVL script.
 * <p>
 * The credentials may come from the <code>"auth"</code> element itself, via the
 * properties <code>"login"</code> and <code>"password"</code>. Environment
 * substitution is applied to these strings, so you can set the credentials in
 * unRAVL variables or pass them as system properties.
 * <p>
 * If the authentication element does not contain credentials, search for them
 * by host in <code>.netrc</code> file (or <code>._netrc</code> on Windows).
 * That file is read from the current directory, or if not found there, from the
 * home directory (<code>.~/.netrc</code> or <code>.%USERPROFILE%\\.netrc</code>
 * ). The file contains lines in the following format:
 * 
 * <pre>
 * machine <em>qualified.hostname</em> login <em>userid-for-host</em> password <em>password-for-host</em>
 * </pre>
 * 
 * @author DavidBiesack@sas.com
 */
public class NetrcCredentialsProvider extends AbstractCredentialsProvider implements CredentialsProvider {

    /**
     * Create a Credentials instance for use with the UnRAVL runtime
     * 
     * @param runtime
     *            a non-null UnRAVL runtime instance
     */
    public NetrcCredentialsProvider() {
    }
    
    static final Pattern NETRC = Pattern
            .compile("^\\s*machine\\s+([^\\s]+)\\s+login\\s+([^\\s]+)\\s+password\\s+([^\\s]+).*$");

    /* (non-Javadoc)
     * @see com.sas.unravl.auth.CredentialsProvider#getCredentials(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public HostCredentials getHostCredentials(String host, String login, String password,
            boolean mock) throws FileNotFoundException, IOException {

        if (mock)
            return mockCredentials();
        if (login != null) {
            login = runtime.expand(login);
            if (password != null)
                return credentials(login, password);
        }

        // Locate the netrc config file with credentials
        File netrc = new File(".netrc"); // look in current dir first
        if (!netrc.exists())
            netrc = new File("_netrc"); // possible Windows file name, in current dir
        File home = new File(System.getProperty("user.home"));
        if (!netrc.exists())
            netrc = new File(home, ".netrc");
        if (!netrc.exists())
            netrc = new File(home, "_netrc");
        if (!netrc.exists())
           return null;
        
        // Note: We can read and cache all the credentials, but that
        // is a security issue; we should probably encrypt the cached content.
        // Also, if we do cache the file content, we would still have to check
        // the modification timestamp of the file, in case it has been updated since
        // we cached the content.
        // Also, the loop below would need to read all rows, not stop when a
        // match is found.
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(netrc));
        try {
            for (String line = reader.readLine(); line != null; line = reader
                    .readLine()) {
                Matcher m = NETRC.matcher(line);
                if (m.matches()) {
                    String netrchost = m.group(1);
                    if (host.equals(netrchost)) {
                        if (login == null || m.group(2).equals(login)) {
                            login = m.group(2);
                            password = m.group(3);
                            return credentials(login, password);
                        }
                    }
                }
            }
        } finally {
            if (reader != null)
                reader.close();
        }
        return null;
    }
}
