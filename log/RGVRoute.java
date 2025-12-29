package fr.lbp.rgv.routes;
import static fr.lbp.lib.model.ErrorBuilder.buildErrorReport;
import static fr.lbp.lib.model.ErrorBuilder.buildProcessContext;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.message.MessageContentsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.lbp.lib.helper.AuditHelper;
import fr.lbp.lib.model.AnyData;
import fr.lbp.lib.model.Audit;
import fr.lbp.lib.model.AuditInfo;
import fr.lbp.lib.model.Error;
import fr.lbp.lib.model.Metadata;
import fr.lbp.lib.model.Metadatas;
import fr.lbp.lib.model.ProcessContext;
import fr.lbp.lib.model.Reference;
import fr.lbp.lib.service.TranscoBean;
import fr.lbp.mt.to.rgv.converter.bean.MT011ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT014ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT030ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT031ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT034ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT035ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT043ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT060ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT066ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT067ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT090ToRGVMLConverter;
import fr.lbp.mt.to.rgv.converter.bean.MT595ToRGVMLConverter;
import fr.lbp.rgv.mt.format.MT011;
import fr.lbp.rgv.mt.format.MT014;
import fr.lbp.rgv.mt.format.MT030;
import fr.lbp.rgv.mt.format.MT031;
import fr.lbp.rgv.mt.format.MT034;
import fr.lbp.rgv.mt.format.MT035;
import fr.lbp.rgv.mt.format.MT043;
import fr.lbp.rgv.mt.format.MT060;
import fr.lbp.rgv.mt.format.MT066;
import fr.lbp.rgv.mt.format.MT067;
import fr.lbp.rgv.mt.format.MT090;
import fr.lbp.rgv.mt.format.MT095;
import fr.lbp.rgv.mt.format.MT595;
import fr.lbp.rgv.soap.ws.RGVMT;
import fr.lbp.rgv.soap.ws.RGVPortType;
import fr.lbp.rgv.soap.ws.RGVmessage;
import fr.lbp.rgv.to.mt.converter.bean.RGVMLToMT065Converter;
import fr.lib.lbp.util.XmlUtils;

@Component
public class RGVRoute extends RouteBuilder {

	private static final Logger LOGGER = LogManager.getLogger(RGVRoute.class);
    @Autowired
    private AuditHelper auditHelper;
    
	@Autowired
	private ProducerTemplate producerTemplate;
	   @Override
	    public void configure() throws Exception {
	        errorHandler(defaultErrorHandler()
	            .maximumRedeliveries(0)
	            .redeliveryDelay(1000));
	       
	        CxfEndpoint cxfEndpoint = new CxfEndpoint();
	        cxfEndpoint.setCamelContext(getContext());
	        cxfEndpoint.setAddress("{{rgv.soap.url}}");
	        cxfEndpoint.setServiceClass(RGVPortType.class);
	        //cxfEndpoint.setDataFormat(DataFormat.PAYLOAD);
	        
	        // Catch exceptions -> SOAP Fault "Server" (500 logique)
	        onException(Exception.class)
	          .handled(true)	
	          .logStackTrace(false)
	          .process(e -> handleException(e, SoapFault.FAULT_CODE_SERVER, "INTERNAL_ERROR"));
	        
	        from(cxfEndpoint)
	        .routeId("{{rgv.soap.route.id}}")
	        .process(exchange -> {
                ProcessContext ctx = auditHelper.createProcessContext("{{project.name}}","{{rgv.soap.rgv-soap-route}}");
                ctx.setCustomId("RGV-" + System.currentTimeMillis());
                ctx.getTrackingInfo().add("userId:" + exchange.getIn().getHeader("userId"));
                ctx.getTrackingInfo().add("source:REST");
                auditHelper.initProcessContext(exchange, ctx);
                exchange.setProperty("startTime", System.currentTimeMillis());
            })
	        .process(exchange -> {
	        	Map<String,Object> headers = new HashMap<>();
	        	headers.put("PROJECT-NAME", "{{project.name}}");
	        	List<String> transcoCategories = List.of("rgv.mt065.programOrigin.filter","rgv.mt065.programId.filter","rgv.mt065.typeRemuneration.filter");
	        	headers.put("transcoCategories", transcoCategories);
	        	exchange.getIn().setHeaders(headers);
	            MessageContentsList list = exchange.getIn().getBody(MessageContentsList.class);
	            if(list.get(0) instanceof RGVMT) {
	            	RGVMT rgvMT = (RGVMT) list.get(0);
	            	exchange.setProperty("MTFormatSource",rgvMT.getMTformat());
	            	exchange.setProperty("SENSE", "MT_TO_GMVML");
	            	exchange.setProperty("originalRGVString",rgvMT.getRGVstring());
	            	exchange.getIn().setBody(rgvMT.getRGVstring());
	            }
	            else if(list.get(0) instanceof RGVmessage) {
	            	RGVmessage rgvMessage = (RGVmessage) list.get(0);
	            	exchange.setProperty("MTFormatCible",rgvMessage.getFormatCible());
	            	exchange.setProperty("SENSE", "RGVML_TO_MT");
	            	exchange.getIn().setBody(rgvMessage); 
	            }
	        })
	        .process(exchange->{
	        	 auditHelper.audit(exchange)
                 .desc("Debut de transformation Message MT to RGVML")
                 .status("INFO")
                 .data(exchange.getIn().getBody())
                 .ref(exchange.getProperty("MTFormatSource").toString(), "TECHNICAL_ID")
                 .meta("PROCESS_NAME", this.getClass().getName())
                 .send();
	        	
	        })
	        .to("seda:audit")
	        .bean(TranscoBean.class)
	        .choice()
	            .when(exchangeProperty("MTFormatSource").isEqualTo("MT011"))
	                .unmarshal()
	                .bindy(BindyType.Fixed, MT011.class).log("Processing record: ${body}")
	                .bean(MT011ToRGVMLConverter.class)
	            .when(exchangeProperty("MTFormatSource").isEqualTo("MT014"))
	                .unmarshal().bindy(BindyType.Fixed, MT014.class)
	                .bean(MT014ToRGVMLConverter.class)
	            .when(exchangeProperty("MTFormatSource").isEqualTo("MT030"))
	                .unmarshal().bindy(BindyType.Fixed, MT030.class)
	                .bean(MT030ToRGVMLConverter.class)
	            .when(exchangeProperty("MTFormatSource").isEqualTo("MT031"))
	                .unmarshal().bindy(BindyType.Fixed, MT031.class)
	                .bean(MT031ToRGVMLConverter.class)
	            .when(exchangeProperty("MTFormatSource").isEqualTo("MT034"))
	                .unmarshal().bindy(BindyType.Fixed, MT034.class)
	                .bean(MT034ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT035"))
	                .unmarshal().bindy(BindyType.Fixed, MT035.class)
	                .bean(MT035ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT043"))
	                .unmarshal().bindy(BindyType.Fixed, MT043.class)
	                .bean(MT043ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT060"))
	                .unmarshal().bindy(BindyType.Fixed, MT060.class)
	                .bean(MT060ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT066"))
	                .unmarshal().bindy(BindyType.Fixed, MT066.class)
	                .bean(MT066ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT067"))
	                .unmarshal().bindy(BindyType.Fixed, MT067.class)
	                .bean(MT067ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT090"))
	                .unmarshal().bindy(BindyType.Fixed, MT090.class)
	                .bean(MT090ToRGVMLConverter.class)
	             .when(exchangeProperty("MTFormatSource").isEqualTo("MT095"))
	                .unmarshal().bindy(BindyType.Fixed, MT095.class)
	                .bean(MT595ToRGVMLConverter.class)
	              .when(exchangeProperty("MTFormatSource").isEqualTo("MT595"))
	                .unmarshal().bindy(BindyType.Fixed, MT595.class)
	                .bean(MT060ToRGVMLConverter.class)
	            .when(exchangeProperty("MTFormatCible").isEqualTo("MT595"))
	                .log("Processing record: ${body}").bean(RGVMLToMT065Converter.class)
	            .otherwise()
	                .log("Other request")
	              .end()
	             .process(exchange->{
	    	        	Audit audit = new Audit();
	    	        	AuditInfo auditInfo = new AuditInfo();
	    	        	audit.setAuditInfo(auditInfo);
	    	        	auditInfo.setTimestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	    	        	
	    	        	auditInfo.setData(XmlUtils.marshallToString(audit));
	    	        	auditInfo.setDescription("Fin de transformation Message MT to RGVML");
	    	        	Reference ref = new Reference();
	    	        	auditInfo.setReference(new ArrayList<Reference>());
	    	        	auditInfo.getReference().add(ref);
	    	        	ref.setCode(exchange.getProperty("MTFormatSource").toString());
	    	        	ref.setCodifier("TECHNICAL_ID");
	    	        	auditInfo.setStatut("INFO");
	    	        	audit.setProcessContext(buildProcessContext(exchange));
	    	        	Metadatas metadatas = new Metadatas();
	    	   		    metadatas.getMetadata().add(new Metadata("PROCESS_NAME", this.getClass().getName()));
	    	   		    auditInfo.setMetadatas(metadatas);
	    	        	
	        			producerTemplate.asyncSendBody("seda:audit", XmlUtils.marshallToString(audit));
	    	        	
	    	        });
	        
	   }

	   private static void handleException(Exchange exchange, QName faultCode, String subCodeLocalName) {
		    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		    String msg = (exception != null && exception.getMessage() != null) ? exception.getMessage() : "Erreur";
		    String originalBody = exchange.getIn().getBody(String.class);
		     // Log
		    LOGGER.error("Erreur traitement message - Type: {}, Message: {}, BodyLength: {}", 
		    		exchange.getClass().getSimpleName(),
		    		exchange.getMessage(),
                     originalBody != null ? originalBody.length() : 0);
		   LOGGER.error("Body en erreur: {}", originalBody);
		   Error error = new Error();
		   error.setProcessContext(buildProcessContext(exchange));
		   error.setErrorReport(buildErrorReport(exchange,exception));
		   error.getReference().add(new Reference("TECHNICAL_ID",exchange.getProperty("MTFormatSource").toString()));
		   error.setData(new AnyData(msg));
		   Metadatas metadatas = new Metadatas();
		   metadatas.getMetadata().add(new Metadata("exchangeId", exchange.getExchangeId()));
		   error.setMetadatas(metadatas);
		   exchange.setProperty("errorObj", error);
		   exchange.getMessage().setBody(new SoapFault(msg, SoapFault.FAULT_CODE_SERVER));
		  }
	   
}
