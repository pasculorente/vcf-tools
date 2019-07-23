/* ****************************************************************************
 * Copyright (C) 2015 UICHUIMI                                                *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU General Public License as published by the      *
 * Free Software Foundation, either version 3 of the License, or (at your     *
 * option) any later version.                                                 *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of                 *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                       *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uichuimi.vcf.utils.consumer.vep;



import org.uichuimi.vcf.utils.consumer.vep.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Lorente Arencibia, Pascual (pasculorente@gmail.com)
 */
public class Web {

    private static final int WAIT_TIME = 5000;
    private static final int MAX_TRIES = 6;

    /**
     * Performs a http request with timeout=5000 and maxTries=6 (max 30 seconds):
     * <code>httpRequest(url, headers, message, 5000, 6)</code>
     *
     * @param url
     * @param headers
     * @param message
     * @return
     */
    public static String httpRequest(URL url, Map<String, String> headers, JSONObject message) {
        return httpRequest(url, headers, message, WAIT_TIME, MAX_TRIES);
    }

    /**
     * Perfomrs a http request. If message is not null nor empty, then it its automatically converted to POST
     *
     * @param url     target url
     * @param headers headers [Content-Type=application/json, Accept=application/json, ...]
     * @param message the message to post
     * @param timeout Waits at most timeout milliseconds for this thread to die. A timeout of 0 means to wait forever.
     * @return
     */
    public static String httpRequest(URL url, Map<String, String> headers, JSONObject message, int timeout,
                                     int maxTries) {
        for (int tries = 0; tries < maxTries; tries++) {
            final AtomicReference<String> result = new AtomicReference<>();
            final Thread thread = getThread(url, headers, message, result);
            try {
                thread.start();
                thread.join(timeout);
                if (thread.isAlive()) thread.interrupt();
                if (result.get() != null) return result.get();
            } catch (InterruptedException e) {
                Logger.getLogger(Web.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        System.out.println("Tried " + maxTries + " times. No result. Printing not annotated variants");
        System.out.println(message);
        return null;
    }

    private static Thread getThread(URL url, Map<String, String> properties, JSONObject message,
                                    AtomicReference<String> result) {
        return new Thread(() -> {
            try {
                final HttpURLConnection connection = createConnection(url, properties, message);
                connection.connect();
                result.set(getResponse(url, connection));
            } catch (IOException ex) {
                Logger.getLogger(Web.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private static String getResponse(URL url, HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            System.err.println("Error " + responseCode + ": " + url);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                final StringBuilder builder = new StringBuilder();
                reader.lines().forEach(builder::append);
                System.err.println(builder.toString());
            }
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            final StringBuilder builder = new StringBuilder();
            reader.lines().forEach(builder::append);
            return builder.toString();
        }
    }

    private static HttpURLConnection createConnection(URL url, Map<String, String> properties, JSONObject message) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // Set properties if needed
        if (properties != null && !properties.isEmpty()) properties.forEach(connection::setRequestProperty);
        // Post message
        if (message != null) {
            // Maybe somewhere
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
                writer.write(message.toString());
            }
        }
        return connection;
    }

}
