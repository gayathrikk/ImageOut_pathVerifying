package Automation.test;
import com.jcraft.jsch.*;
import org.testng.annotations.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class IIPSRVProcessAutomation {
	 private static final String DOCKER_CONTAINER = "01cd5ef4d486";
	    private static final String SSH_HOST = "ap6.humanbrain.in";
	    private static final String SSH_USER = "appUser";
	    private static final String SSH_PASSWORD = "Health#123";

	    @Test
	    public void manageIIPSRVProcesses() {
	        Session session = null;

	        try {
	            // Set up SSH connection
	            JSch jsch = new JSch();
	            session = jsch.getSession(SSH_USER, SSH_HOST, 22);
	            session.setPassword(SSH_PASSWORD);
	            session.setConfig("StrictHostKeyChecking", "no");
	            session.connect();

	            System.out.println("Connected to SSH host: " + SSH_HOST);

	            // Step 1: Check for iipsrv processes
	            System.out.println("Checking for iipsrv processes...");
	            String processes = executeCommand(session, "docker exec " + DOCKER_CONTAINER + " ps -ef | grep iipsrv");
	            System.out.println("Processes found:\n" + processes);

	            // Step 2: Kill iipsrv.fcgi processes
	            System.out.println("Killing iipsrv.fcgi processes...");
	            executeCommand(session, "docker exec " + DOCKER_CONTAINER + " pkill iipsrv.fcgi");
	            System.out.println("iipsrv.fcgi processes killed.");

	            // Step 3: Verify no processes are running
	            System.out.println("Verifying processes are killed...");
	            String postKillProcesses = executeCommand(session, "docker exec " + DOCKER_CONTAINER + " ps -ef | grep iipsrv");
	            if (postKillProcesses.isEmpty()) {
	                System.out.println("All iipsrv processes successfully killed.");
	            } else {
	                System.out.println("Processes still running after kill:\n" + postKillProcesses);
	            }

	            // Step 4: Restart the application
	            System.out.println("Restarting the application...");
	            String restartOutput = executeCommand(session, "docker exec " + DOCKER_CONTAINER + " ./startApps.sh");
	            System.out.println("Application restarted:\n" + restartOutput);

	            // Step 5: Verify restarted processes
	            System.out.println("Verifying restarted processes...");
	            String restartedProcesses = executeCommand(session, "docker exec " + DOCKER_CONTAINER + " ps -ef | grep iipsrv");
	            System.out.println("Processes after restart:\n" + restartedProcesses);

	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            if (session != null && session.isConnected()) {
	                session.disconnect();
	            }
	        }
	    }

	    private String executeCommand(Session session, String command) throws Exception {
	        ChannelExec channel = (ChannelExec) session.openChannel("exec");
	        channel.setCommand(command);
	        channel.setErrStream(System.err);

	        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
	        StringBuilder outputBuffer = new StringBuilder();

	        channel.connect();

	        String line;
	        while ((line = reader.readLine()) != null) {
	            outputBuffer.append(line).append("\n");
	        }

	        channel.disconnect();
	        return outputBuffer.toString();
	    }
	}

