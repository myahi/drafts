package fr.lbp.markit.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.lbp.markit.configuration.ApplicationProperties;
import fr.lbp.markit.connection.ErrorCode;
import fr.lbp.markit.tools.ResponseMessage;
import fr.lbp.markit.tools.Tools;

@Controller
public class MarkitController {
    private static final Logger LOGGER = LogManager.getLogger(MarkitController.class);

    private final LbpMarkitClient markitApplication;

    // ✅ Spring injecte le singleton LbpMarkitClient (@Component)
    public MarkitController(LbpMarkitClient markitApplication) {
        this.markitApplication = markitApplication;
    }

    @RequestMapping(value = "/restartmarkitconnector")
    @ResponseBody
    public ResponseEntity<ResponseMessage> restartFixConnector() {
        LOGGER.info("Receive restart request");
        try {
            markitApplication.restartConnector();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseMessage("Markitdealer connector started !"));
        } catch (RuntimeException | ErrorCode e) {
            LOGGER.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseMessage("Could not start connector, for more details please check the log files"));
        }
    }

    @RequestMapping(value = "/stopmarkitconnector")
    @ResponseBody
    public ResponseEntity<ResponseMessage> stopFixConnector() {
        LOGGER.info("Receive stop request");
        if (this.markitApplication.isConnected()) {
            try {
                markitApplication.stopMarkitConnector();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ResponseMessage("Could not stop connector, for more details please check the log files"));
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer connector stopped !"));
    }

    @RequestMapping(value = "/statusmarkitconnector")
    @ResponseBody
    public ResponseEntity<ResponseMessage> sessionStatus() {
        if (!this.markitApplication.isConnected()) {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markit session not connected"));
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer session connected"));
        }
    }

    @GetMapping("getswdml/{dvh}")
    @ResponseBody
    public ResponseEntity<ResponseMessage> getSWDML(@PathVariable String dvh) {
        LOGGER.info("Receive get SWDML for DVH: {}", dvh);
        if (this.markitApplication.isConnected()) {
            try {
                String swdml = LbpMarkitClient.getDealSWDML(dvh);
                return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(swdml));
            } catch (ErrorCode e) {
                LOGGER.error(e);
                ErrorCode errorCode = (ErrorCode) e;
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
            }
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer session not connected"));
        }
    }

    @GetMapping("getswml/{dvh}")
    @ResponseBody
    public ResponseEntity<ResponseMessage> getSWML(@PathVariable String dvh) {
        LOGGER.info("Receive get SWML for DVH: {}", dvh);
        if (this.markitApplication.isConnected()) {
            try {
                String swml = LbpMarkitClient.getDealSWML(dvh);
                LOGGER.info("Found SWML {}", swml);
                return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(swml));
            } catch (ErrorCode e) {
                LOGGER.error(e);
                ErrorCode errorCode = (ErrorCode) e;
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
            }
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage("Markitdealer session not connected"));
        }
    }

    @RequestMapping(value = "/getLastDVHForTradeId", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ResponseMessage> getLastDealsVersionHandles(@RequestParam(value = "tradeId") String tradeId) {
        LOGGER.info("getLastDealsVersionHandles with tradeIds: {}", tradeId);
        String response = "";
        try {
            int dealSide = LbpMarkitClient.getDealGetMySide(Long.valueOf(tradeId));
            String[] dvhs = LbpMarkitClient.getAllDealVersionHandles(tradeId, "-1", String.valueOf(dealSide));
            if (dvhs != null && dvhs.length > 0) {
                String dvhArray[] = dvhs[0].split(System.lineSeparator());
                response += dvhArray[0];
            }
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(response));
        } catch (ErrorCode errorCode) {
            LOGGER.error(errorCode);
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(errorCode.errorString));
        } catch (IOException | NumberFormatException e) {
            LOGGER.error(e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResponseMessage(sw.toString()));
        }
    }

    // ... (le reste de tes endpoints inchangés)

    @PreDestroy
    public void onExit() {
        LOGGER.info("Markit connector will be shutdown.");
        if (this.markitApplication != null) {
            markitApplication.stopMarkitConnector();
        }
    }
}
