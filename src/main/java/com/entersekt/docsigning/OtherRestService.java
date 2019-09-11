package com.entersekt.docsigning;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.io.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entersekt.branding.entity.Branding;
import com.entersekt.communications.CommunicationsService;
import com.entersekt.demo.DemoImpl;
import com.entersekt.utils.AppUtils;

@Path("/Other")
@Api(value = "/Other")
public class OtherRestService {

	private static final Logger log = LoggerFactory.getLogger(OtherRestService.class);

	@GET
	@Path("startDemo")
	@ApiOperation(value = "Starts building the demographic")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	public Response startDemo() throws Exception {

		App.demo = new DemoImpl();
		Thread.sleep(200);
		App.demo.orchestrate();
		return Response.ok().build();
	}

	@GET
	@Path("demographic")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Retrieve the current state of the SVG graphic that describes the demo")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public String demographic() throws Exception {
		return App.demo.getCurrentDemoSvg();
	}

	@GET
	@Path("branding")
	@ApiOperation(value = "Provides a CSS to brand pages with")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces("text/css")
	public Response branding() throws Exception {
		return Response.status(Response.Status.OK).entity(generateBrandedCss(App.brand)).build();
	}

	@GET
	@Path("deviceBranding")
	@ApiOperation(value = "Provides a CSS to brand pages with")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces("text/css")
	public Response deviceBranding() throws Exception {
		return Response.status(Response.Status.OK)
				.entity(generateBrandedCss(App.brand) + AppUtils.readStringFromFile("styles/phone_background.css"))
				.build();
	}

	@GET
	@Path("brand")
	@ApiOperation(value = "Provides the brand being used")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces(MediaType.TEXT_PLAIN)
	public Response brand() throws Exception {
		return Response.status(Response.Status.OK).entity(App.brand).build();
	}

	@GET
	@Path("entersektBranding")
	@ApiOperation(value = "Provides a CSS to brand pages with")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces("text/css")
	public Response entersektBranding() throws Exception {
		return Response.status(Response.Status.OK).entity(generateBrandedCss(Branding.ENTERSEKT)).build();
	}

	@GET
	@Path("images/{file-name}")
	@ApiOperation(value = "Downloads the current branding image file - ignores the file name")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public Response getFile(
			@ApiParam(value = "File Identifier", required = true) @PathParam("file-name") String fileName)
			throws Exception {
		return Response.ok().entity(AppUtils.readBytesFromFile("branding_images/" + fileName)).build();
	}

	@GET
	@Path("health-check")
	@ApiOperation(value = "Checks if the server is running")
	@ApiResponses(value = { @ApiResponse(code = 200, message = CommunicationsService.HEALTHY) })
	@Produces(MediaType.TEXT_PLAIN)
	public Response healthCheck() {
		return Response.status(Response.Status.OK).entity(CommunicationsService.HEALTHY).build();
	}

	private String generateBrandedCss(final String brand) throws FileNotFoundException, IOException {
		if (brand == null) {
			throw new IllegalArgumentException("brand value can't be null");
		}
		String template = AppUtils.readStringFromFile("styles/template.css");
		String readDoc;
		try {
			readDoc = AppUtils.readStringFromFile("branding_info/" + brand + ".json");
		} catch (RuntimeIOException e) {
			throw new RuntimeException("Problems reading branding info for '" + brand + "' with: " + e.getMessage());
		}
		final Branding brandingInfo = App.jsonSerialisationService.deSerialise(readDoc, Branding.class);
		return brandingInfo.applyToTemplate(template);
	}

}