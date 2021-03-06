package asw.socket.server.connector;

import asw.socket.service.CounterService;
import asw.socket.service.RemoteException;

import java.net.*;
import java.io.*;

import java.util.logging.Logger;
import asw.util.logging.AswLogger;

public class ServantThread extends Thread {

	private CounterService service;

	private Socket clientSocket;
	private DataInputStream in;
	private DataOutputStream out;

	boolean done = false;   // e' finita la sessione?

	private static int MAX_SERVANT_THREAD_ID = 0;
	private int servantThreadId;

	/* logger */
	private Logger logger = AswLogger.getInstance().getLogger("asw.socket.server.connector");

	public ServantThread(Socket clientSocket, CounterService service) {
		try {
			this.clientSocket = clientSocket;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			this.service = service;
			this.servantThreadId = MAX_SERVANT_THREAD_ID++;
			this.start();    // esegue run() in un nuovo thread
		} catch (IOException e) {
			logger.info("Server Proxy: IO Exception: " + e.getMessage());
		}
	}


	/* run eseguito in un nuovo thread */
	public void run() {
		logger.info("Server Proxy: opening connection [" + servantThreadId + "]");
		try {
			while (!done) {
				/* riceve una richiesta */
				String request = in.readUTF();  // bloccante
	    		logger.info("Server Proxy: connection [" + servantThreadId + "]: received request: " + request);

	            /* estrae operazione e parametro */
	            /* la richiesta ha la forma "operazione$parametro" */
	            String op = this.getOp(request);
	            String param = this.getParam(request);

	            /* chiedi l'erogazione del servizio ed ottieni la risposta */
	            String result = null;
	            try {
		            result = this.executeOperation(op, param);
	    		} catch (RemoteException e) {
	                /* NON dovremmo essere qui, perche' il servente non dovrebbe mai sollevare RemoteExecption */
	                result = "";
	            }

	            /* prepara la risposta da restituire */
	            /* la risposta ha la forma "risultato" */
	            String reply = result;
	    		logger.info("Server Proxy: connection [" + servantThreadId + "]: sending reply: " + result);
				/* invia la risposta */
	            out.writeUTF(reply);    // non bloccante
			}
		} catch (EOFException e) {
			logger.info("Server Proxy: connection [" + servantThreadId + "]: EOFException: " + e.getMessage());
		} catch (IOException e) {
			logger.info("Server Proxy: connection [" + servantThreadId + "]: IOException: " + e.getMessage());
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				logger.info("Server Proxy: connection [" + servantThreadId + "]: IOException: " + e.getMessage());
			}
		}
		logger.info("Server Proxy: closing connection [" + servantThreadId + "]");
	}

    /* estrae l'operazione dalla richiesta */
    private String getOp(String request) {
        /* la richiesta ha la forma "operazione$parametro" */
        int sep = request.indexOf("$");
        String op = request.substring(0,sep);
        return op;
    }

    /* estrae il parametro dalla richiesta */
    private String getParam(String request) {
        /* la richiesta ha la forma "operazione$parametro" */
        int sep = request.indexOf("$");
        String param = request.substring(sep+1);
        return param;
    }

    /* gestisce la richiesta del servizio corretto al servente */
    private String executeOperation(String op, String param) throws RemoteException {
        String reply = null;

        if ( op.equals("CONNECT") ) {
            done = false;
            reply = "ACK";
            logger.info("Server Proxy: connection [" + servantThreadId + "]: connect");
        } else if ( op.equals("DISCONNECT") ) {
            done = true;
            reply = "ACK";
            logger.info("Server Proxy: connection [" + servantThreadId + "]: disconnect");
        } else if ( op.equals("getGlobalCounter") ) {
            reply = String.valueOf( service.getGlobalCounter() );
            logger.info("Server Proxy: connection [" + servantThreadId + "]: " +
    				"service.getGlobalCounter()" + " --> " + reply);
        } else if ( op.equals("getSessionCounter") ) {
            reply = String.valueOf( service.getSessionCounter() );
            logger.info("Server Proxy: connection [" + servantThreadId + "]: " +
    				"service.getSessionCounter()" + " --> " + reply);
        } else if ( op.equals("incrementCounter") ) {
        	service.incrementCounter();
            reply = "";
            logger.info("Server Proxy: connection [" + servantThreadId + "]: " +
    				"service.incrementCounter()");
        } else {
            throw new RemoteException("Operation " + op + " is not supported");
        }

        return reply;
    }

}
