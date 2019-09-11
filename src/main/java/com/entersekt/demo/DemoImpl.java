package com.entersekt.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.entersekt.docsigning.App;
import com.entersekt.svg.SvgGeneratorService;
import com.entersekt.svg.SvgGeneratorServiceImpl;
import com.entersekt.utils.AppUtils;

public class DemoImpl implements DemoStructure {

	private static final Logger log = LoggerFactory.getLogger(DemoImpl.class);

	protected static Element baseSvgDoc;
	protected static SvgGeneratorService svgGeneratorService = new SvgGeneratorServiceImpl();

	@Override
	public void orchestrate() throws Exception {
		svgGeneratorService.makeDoc(750, 350, "#2266aa");

		Element aliceDesktop = svgGeneratorService
				.makeNode(
						"ellipse",
						"_aliceDesktop",
						"fill:#2266aa; stroke:#2266aa",
						10,
						50,
						"Alice Desktop",
						"Alice\'s Desktop",
						"javascript:openUrlInWindow('http://"
								+ App.containerIpV4Address
								+ ":"
								+ App.portNo
								+ "/desktop.html?who=Alice', '_aliceDesktop', 'left=50,top=10,height=960,width=630,scrollbars=yes,status=yes');",
						"_aliceDesktop", 150, 50);

		svgGeneratorService.appendChildToSvgRoot(aliceDesktop);

		Element aliceDevice = svgGeneratorService
				.makeNode(
						"ellipse",
						"_aliceDevice",
						"fill:#2266aa; stroke:#2266aa",
						200,
						50,
						"Alice Device",
						"Alice\'s Device",
						"javascript:openUrlInWindow('http://"
								+ App.containerIpV4Address
								+ ":"
								+ App.portNo
								+ "/device.html?who=Alice', '_aliceDevice', 'left=700,top=10,height=760,width=398,scrollbars=no,status=yes');",
						"_aliceDevice", 150, 50);

		svgGeneratorService.appendChildToSvgRoot(aliceDevice);

		Element bobDesktop = svgGeneratorService
				.makeNode(
						"ellipse",
						"_bobDesktop",
						"fill:#2266aa; stroke:#2266aa",
						390,
						50,
						"Bob Desktop",
						"Bob\'s Desktop",
						"javascript:openUrlInWindow('http://"
								+ App.containerIpV4Address
								+ ":"
								+ App.portNo
								+ "/desktop.html?who=Bob', '_bobDesktop', 'left=1005,top=10,height=960,width=630,scrollbars=yes,status=yes');",
						"_bobDesktop", 150, 50);

		svgGeneratorService.appendChildToSvgRoot(bobDesktop);

		Element bobDevice = svgGeneratorService
				.makeNode(
						"ellipse",
						"_bobDevice",
						"fill:#2266aa; stroke:#2266aa",
						580,
						50,
						"Bob Device",
						"Bob\'s Device",
						"javascript:openUrlInWindow('http://"
								+ App.containerIpV4Address
								+ ":"
								+ App.portNo
								+ "/device.html?who=Bob', '_bobDevice', 'left=1700,top=10,height=760,width=398,scrollbars=yes,status=yes');",
						"_bobDevice", 150, 50);

		svgGeneratorService.appendChildToSvgRoot(bobDevice);

		Element register = svgGeneratorService
				.makeNode(
						"ellipse",
						"_register",
						"fill:#2266aa; stroke:#2266aa; stroke-width:1",
						390,
						250,
						"Register",
						"Register Signer",
						"javascript:openUrlInWindow('http://"
								+ App.containerIpV4Address
								+ ":"
								+ App.portNo
								+ "/register.html', '_register', 'left=20,top=10,height=860,width=650,scrollbars=yes,status=yes');",
						"db", 150, 50);
		svgGeneratorService.appendChildToSvgRoot(register);

		// AppUtils.emitEventSafely(App.eventSource, "'register'" + "\n");

		Element certificateSigner = svgGeneratorService
				.makeNode(
						"ellipse",
						"_certificateSigner",
						"fill:#2266aa; stroke:#2266aa",
						580,
						250,
						"Certificate Signer",
						"Identity Management",
						"javascript:openUrlInWindow('http://"
								+ App.containerIpV4Address
								+ ":"
								+ App.portNo
								+ "/sign-certificate.html', '_certificateSigner', 'left=700,top=10,height=860,width=850,scrollbars=yes,status=yes');",
						"_certificateSigner", 150, 50);

		svgGeneratorService.appendChildToSvgRoot(certificateSigner);

		AppUtils.emitEventSafely(App.eventSource, "complete" + "\n");

	}

	@Override
	public String getCurrentDemoSvg() throws Exception {
		return svgGeneratorService.stringValue(true);
	}

}
