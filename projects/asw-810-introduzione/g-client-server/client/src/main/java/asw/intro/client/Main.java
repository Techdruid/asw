package asw.intro.client;

import asw.intro.client.context.ApplicationContext;

/* Applicazione client: crea ed avvia il client. */
public class Main {

	/* Crea e avvia un oggetto Client. */
	public static void main(String[] args) {
		Client client;
		if (args.length>0) {
			String serverHost = args[0];
			client = ApplicationContext.getInstance().getClient(serverHost);
		} else {
			client = ApplicationContext.getInstance().getClient();
		}
		client.run();
	}

}
