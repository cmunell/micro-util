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