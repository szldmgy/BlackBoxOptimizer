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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lib.Node;

import optimizer.exception.JSONReadException;
import optimizer.param.Param;
import optimizer.param.ParamDeserializer;
import optimizer.trial.IterationResult;
import optimizer.trial.Trial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BBOSlave extends Node {
    boolean up = true;
    //ProcessorThread pthread = new ProcessorThread(this.getProcessId());
    int threads = Runtime.getRuntime().availableProcessors();

    ExecutorService pool = Executors.newFixedThreadPool(threads/2);
    Set<Future<IterationResult>> set = new HashSet<Future<IterationResult>>();


    // List<List<Param>> queue = new LinkedList<>();

    @Override
    public boolean stopNode(){
        this.up = false;
        return true;
    }



    @Override
    public boolean start(String[] args) {
        run();return true;
    }

    @Override
    public void run() {
        List<String> newmessages = new LinkedList<>();
        while (up) {
            try {
                System.out.println("SLAVE READS");
                newmessages = this.com.receive(this.getProcessId());
                if (newmessages.contains("STOP")) {
                    up = false;
                    System.out.println("shoting down slave");
                    continue;
                }
                //messages.addAll(newmessages);
                if (!newmessages.isEmpty()) {

                    for (String s : newmessages) {
                        System.out.println("run " + s);
                        Trial t = Trial.deserializeTrial(s);
                        set.add(pool.submit(t));
                    }
                    for (Future<IterationResult> future : set) {
                        IterationResult ir = future.get();
                        Gson gson1 = new GsonBuilder().setPrettyPrinting().create();

                        com.send(gson1.toJson(ir, IterationResult.class), this.getProcessId());
                    }
                    newmessages.clear();

                }
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            ;
        }
        System.out.println("shot down slave");

    }
        public static void main(String[] s){

        }
        /*class ProcessorThread extends Thread{
            String nodeId = null;
            int threads = Runtime.getRuntime().availableProcessors();

            ExecutorService pool = Executors.newFixedThreadPool(threads/2);
            Set<Future<IterationResult>> set = new HashSet<Future<IterationResult>>();

            public ProcessorThread(String nodeId) {
                this.nodeId = nodeId;
            }
            @Override
            public void run(){
                while (up){
                    if(messa)
                }

                public void runEx(List<String> msgl) throws ExecutionException, InterruptedException {


                    for(String s : msgl) {
                        System.out.println("run "+s);
                        Trial t = Trial.deserializeTrial(s);
                        set.add(pool.submit(t));
                        for (Future<IterationResult> future : set) {
                            IterationResult ir = future.get();
                            Gson gson1 = new GsonBuilder().setPrettyPrinting().create();

                            com.send(gson1.toJson(ir, IterationResult.class),this.nodeId);

                        }

                    }

                }
            }


            public static List<List<Param>> readConfigJSON(File configFile) throws FileNotFoundException {
                List<List<Param>> ret = new LinkedList<>();
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Param.class, new ParamDeserializer());
                Type listType = new TypeToken<List<Param>>() {}.getType();
                Type listType2 = new TypeToken< List< List< Param>>>() {}.getType();

                Gson gson = gsonBuilder.create();
                JsonReader reader = new JsonReader(new FileReader(configFile));
                try {
                    ret.add(gson.fromJson(reader, listType));
                    ret.addAll(gson.fromJson(reader, listType2));
                    return ret;
                }catch (Exception e){

                    throw new JSONReadException("Error during deserialization of JSON");
                }

            }*/
        }
