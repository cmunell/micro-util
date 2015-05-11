/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of ARKWater (https://github.com/forkunited/ARKWater)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package edu.cmu.ml.rtw.generic.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * 
 * CommandRunner executes a command-line program.  It is written to 
 * support both Windows (cygwin) and typical UNIX environments.
 * 
 * @author Bill McDowell
 *
 */
public class CommandRunner {

	/**
	 * See http://stackoverflow.com/questions/7200307/execute-unix-system-command-from-java-problem
	 * @param cmd
	 */
	public static boolean run(String cmd) {
		return run(cmd, null);
	}
	
	public static boolean run(String cmd, File dir){
        try {  
        	String[] cmds = constructCommandsBySystem(cmd);
        	String[] env = constructEnvironmentBySystem();
        	Process p = Runtime.getRuntime().exec(cmds, env, dir);
        	
        	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        	while ((reader.readLine()) != null) {}
        	
        	reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        	while ((reader.readLine()) != null) {}
        	
            return (p.waitFor() == 0);
        } catch (Exception e) {  
            e.printStackTrace();  
            return false;
        }
	}
	
	private static String[] constructCommandsBySystem(String cmd) {
		if (System.getProperty("os.name").contains("Windows"))
			return new String[]{"C:\\cygwin64\\bin\\bash.exe", "-c", cmd};
		else 
			return new String[]{"/bin/bash", "-c", cmd};
			
	}
	
	private static String[] constructEnvironmentBySystem() {
		if (System.getProperty("os.name").contains("Windows"))
			return new String[]{"PATH=%PATH%;C:/cygwin64/bin"};
		else
			return null;
	}
}