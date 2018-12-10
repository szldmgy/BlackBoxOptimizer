/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package optimizer.docker;

import optimizer.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DockerWrapper {

    Boolean done = false;
    String location;
    String name;
    public DockerWrapper(String location){
        this.location = location;
        this.name = location.substring(location.lastIndexOf("/")+1,location.length()).toLowerCase();

    }
    public boolean build() throws IOException, InterruptedException {
        String homeDirectory = System.getProperty("user.home");
        Process process;
        /*if (Utils.isWindows) {
            // TODO: 2018. 11. 09.
            String command = String.format("cmd.exe build -t %s ./%s", homeDirectory)
            process = Runtime.getRuntime()
                    .exec(command);
        } else */{
            String command = String.format("docker build -t %s ./%s", this.name,this.location);
            process = Runtime.getRuntime()
                    .exec(command);
        }
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        int exitCode = process.waitFor();
        done = true;
        // TODO: 2018. 11. 09. i do nothing with this
        assert exitCode == 0;
        return exitCode == 0;
        
    }

    public String getCommand(String command){
        while (!done){;}
        String[] parts = command.trim().split(" ");
        String com = parts[0];
        int execStartIdx = parts[1].lastIndexOf("/");
        if(execStartIdx == -1)
            execStartIdx = parts[1].lastIndexOf("\\");
        String executable = parts[1].substring(parts[1].lastIndexOf("/")+1,parts[1].length());
        System.out.println("EXECUTABLE: "+executable);
        String leftover = "";
        for (int i = 2; i <parts.length ; i++) {
            leftover+=" "+parts[i];
        }



        return "docker run "+name+" "+ com + " "+executable+" "+leftover;

    }
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
