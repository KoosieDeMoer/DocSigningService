package com.entersekt.docsigning;

import java.security.KeyStore;
import java.util.Locale;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.docuverse.identicon.IdenticonTools;
import com.entersekt.certificate.CertificateChainGeneration;
import com.entersekt.certificate.CertificateService;
import com.entersekt.certificate.CertificateServiceBcImpl;
import com.entersekt.demo.DemoStructure;
import com.entersekt.eventsource.EventSource;
import com.entersekt.eventsource.EventSourceServlet;
import com.entersekt.hub.GuiceBindingsModule;
import com.entersekt.hub.common.RestCommonService;
import com.entersekt.json.JsonSerialisationService;
import com.entersekt.pdf.PdfImageManipulation;
import com.entersekt.utils.AppUtils;
import com.entersekt.utils.SwaggerUtils;

public class App {
	private static final Logger log = LoggerFactory.getLogger(App.class);

	public static String containerIpV4Address;
	public static int portNo;
	public static String brand;
	public static String languageTag;

	public static int identNumber;

	static KeyStore keyStore;
	static CertificateService certificateService;
	public static EventSource eventSource = new EventSource();

	public static boolean semaphore;
	public static String authResponse;

	static final JsonSerialisationService jsonSerialisationService = GuiceBindingsModule.injector
			.getInstance(JsonSerialisationService.class);

	static final PdfImageManipulation pdfImageManipulation = new PdfImageManipulation();

	public static CertificateChainGeneration certificateChainGeneration;
	public static IdenticonTools identiconTools = new IdenticonTools();

	public static String certForSigner = "Bob";

	public static DemoStructure demo;

	static Locale locale;

	public static void main(String[] args) throws Exception {
		usage(args);
		certificateService = new CertificateServiceBcImpl();
		certificateService.initialiseKeyStoreForSigner(brand);
		keyStore = certificateService.getKeyStore();
		certificateChainGeneration = new CertificateChainGeneration(keyStore);
		locale = Locale.forLanguageTag(App.languageTag);

		new App().start();
	}

	public void start() throws Exception {

		final HandlerList handlers = new HandlerList();

		// URL has form: http://<host>:<port>/
		handlers.addHandler(AppUtils.buildWebUI(App.class, null, "angularjs-ui", "AngularJS based Web UI"));

		SwaggerUtils.buildSwaggerBean("DocSigningService", "DocSigningService API", RestService.class.getPackage()
				.getName());

		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(MultiPartFeature.class);

		resourceConfig.packages(RestService.class.getPackage().getName(), RestCommonService.class.getPackage()
				.getName());

		SwaggerUtils.attachSwagger(handlers, App.class, resourceConfig);

		ServletContainer servletContainer = new ServletContainer(resourceConfig);
		ServletHolder jerseyServlet = new ServletHolder(servletContainer);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(new ServletHolder(new EventSourceServlet(eventSource)), "/event-source/*");
		context.addServlet(jerseyServlet, "/*");

		handlers.addHandler(context);

		Server jettyServer = new Server(portNo);

		jettyServer.setHandler(handlers);

		try {
			jettyServer.start();
			jettyServer.join();
		} finally {
			jettyServer.destroy();
		}

	}

	private static void usage(String[] args) {

		if (args.length < 4) {
			log.error("Usage requires command line parameters MY_HOSTNAME MY_PORT BRANDING LANGUAGE_TAG eg 192.168.99.100 80 nedbank en-ZA");
			System.exit(0);
		} else {
			containerIpV4Address = args[0];
			portNo = AppUtils.extractPortNumber(args[1]);
			brand = args[2];
			languageTag = args[3];
		}
		log.info("Starting DocSigningService with parameters: containerIpV4Address='" + containerIpV4Address
				+ "' portNo='" + portNo + "' brand='" + brand + "' languageTag='" + languageTag + "'");

	}

}
