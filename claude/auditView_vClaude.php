<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<?php
include_once 'AuditParameter.php';
include_once 'topHeader.php';
//ignore_user_abort(false);
?>

<script src="js/jquery.contextMenu.js" type="text/javascript"></script>
<script src="js/clipboard.min.js"></script>
<link href="css/jquery.contextMenu.css" rel="stylesheet" type="text/css" />
<link rel="stylesheet" href="css/prism.css">
<script src="js/prism.js"></script>
<script type="text/javascript" src="js/vkbeautify.js"></script>
<!--<script type="text/javascript" src="js/bootstrap-table-sticky-header.min.js"></script>-->
<script type="text/javascript">
let flowList = <?php echo json_encode($flowsList,JSON_UNESCAPED_UNICODE)?>;
flowList = JSON.parse(flowList);
var selectedFlows = <?php echo json_encode($criteres['DISPLAYED_FLOW_TAGS'],JSON_UNESCAPED_UNICODE)?>;
selectedFlows =  selectedFlows.length > 0 ? selectedFlows.split(";"):[];
const success_status = ["Success"];
const warning_status = ["Warning"];
const danger_status = ["Error","Pending-Error","4","REJECTED"];
const info_status = ["Info"];
var seletectedIdLineSet = new Set();
var idLineSet = new Set();
var currentAuditId="";
var auditDetails;
var isForamted;
var fireDrillToken = null;
var fireDrillForceQueue= null;
var tradeId = null;
var currentXHR = null;
var isFireDrillData = false;

// ✅ NOUVELLES VARIABLES DE PROTECTION CONTRE LES BOUCLES INFINIES
var isFormSubmitting = false;
var lastSubmitTime = 0;
var ajaxCallCount = 0;
var isTableRefreshing = false;
var isSelectAllChanging = false;

if (!String.prototype.startsWith) {
	String.prototype.startsWith = function(searchString, position) {
		position = position || 0;
		return this.indexOf(searchString, position) === position;
	};
}

// ✅ DIAGNOSTIC DES APPELS AJAX (optionnel mais recommandé pour le débogage)
$(document).ajaxSend(function(event, xhr, settings) {
    ajaxCallCount++;
    console.log(`📤 AJAX Call #${ajaxCallCount}:`, settings.url);
    
    if (ajaxCallCount > 10) {
        console.error('⚠️ ATTENTION: Plus de 10 appels AJAX détectés !');
    }
});

$(document).ajaxComplete(function(event, xhr, settings) {
    console.log('✅ AJAX Complete:', settings.url);
});

$(function() {
	$(document).on("click", ".copyButton", function() {
	    var clipboard = new Clipboard(".copyButton"); 
	    //clipboard.destroy();
	  });
	});
function escapeHtml(text) {
    'use strict';
    return text.replace(/[\"&<>]/g, function (a) {
        return { '"': '&quot;', '&': '&amp;', '<': '&lt;', '>': '&gt;' }[a];
    });
}

function prettyPrintXML(){
	if(isForamted){
	//document.getElementById('brutXMLBtn').style.visibility = 'hidden';
	document.getElementById('prettyPrintXMLBtn').innerHTML="Format XML";
	//document.getElementById('prettyPrintXMLSpan').toggleClass('glyphicon glyphicon-align-justify');
	var xmlValue = auditDetails!=null ? htmlspecialchars_decode(auditDetails,2) : "";	
	$('#adddetails').text(xmlValue);	
	isForamted=false;
	}
	else {
		//document.getElementById('prettyPrintXMLBtn').innerHTML="Formater";
		document.getElementById('prettyPrintXMLBtn').innerHTML = "Format brut";
		//document.getElementById('prettyPrintXMLSpan').toggleClass('glyphicon glyphicon-align-left');
		var xmlValue = auditDetails!=null ? htmlspecialchars_decode(auditDetails,2) : "";	
		$('#adddetails').text(vkbeautify.xml(xmlValue));
		//$('#addmessageBrute').text(xmlValue2);
		Prism.highlightAll($('#adddetails'));
		//Prism.highlightAll($('#addmessageBrute'));
		$("#adddetails").toggleClass("language-markup");
		//$("#addmessageBrute").toggleClass("language-markup");
		isForamted=true;
	}
}
function affichage_audit_detail(pData){
	$("#force-fire-drill-btn").hide();
	fireDrillToken = null;
	fireDrillForceQueue = null;
	document.getElementById('addcode').value=pData.code;
	document.getElementById('addcodifier').value=pData.codifier;
	document.getElementById('addproject').value=pData.project;
	document.getElementById('adddata').value=(pData.label!= null ? htmlspecialchars_decode(pData.label,2) : "");
 	document.getElementById('addinsertTimestamp').value=pData.insertTimestamp;		
	var panel = document.getElementById('adddetails');
	//var panel_ = document.getElementById('addmessageBrute');
	pData.details ? $('#download-details-zip-btn').show():$('#download-details-zip-btn').hide();
	var xmlValue = pData.details!=null ? htmlspecialchars_decode(pData.details,2) : "";	
	var xmlValue2 = pData.details!=null ? pData.details : "";
	auditDetails = pData.details!=null ? pData.details : "";
	
	$('#adddetails').text(xmlValue2);
	//$('#addmessageBrute').text(xmlValue2);
	//Prism.highlightAll($('#adddetails'));
	//Prism.highlightAll($('#addmessageBrute'));
	//$("#adddetails").toggleClass("language-markup");
	//$("#addmessageBrute").toggleClass("language-markup");
	resetMetadataTextArea();
	let isFireDrillFluxFound = false;
	
	if (pData.metadatas != null) {
		if ($.isArray(pData.metadatas) && pData.metadatas.length>0) {
			for (var i = 0; i < pData.metadatas.length; i++) {
				var currentKey = pData.metadatas[i].key;
				var currentValue = pData.metadatas[i].value!=null ? htmlspecialchars_decode(pData.metadatas[i].value,2) : "";
				if(currentKey=="FLOW_TYPE" && currentValue =="FireDrill"){
					isFireDrillFluxFound = true;
				}
				if(currentKey=="TRADE_ID" && currentValue !=null){
					tradeId = currentValue;
				}
				if(currentKey=="FIREDRILL_TOKEN" && currentValue !=null){
					fireDrillToken = currentValue;
				}
				if(currentKey=="FIRE_DRILL_FORCE_QUEUE" && currentValue !=null){
					fireDrillForceQueue = currentValue;
				}
				document.getElementById("functionalDataText").value += currentKey + " = " + currentValue + "\n";
			}
		}
	}
	if(isFireDrillFluxFound && tradeId != null && fireDrillToken != null && fireDrillForceQueue != null){
		$("#force-fire-drill-btn").show();
	}
	affichage_fenetre_generique('update');
	return true;
}

function forceFireDrillTrade(){
let messageId = tradeId;
document.getElementById("force-fire-drill-btn").setAttribute("disabled", true);
let messageContent = "<root><Token>" + fireDrillToken + "</Token>" + "<Trade_ID>" + tradeId + "</Trade_ID>" + "</root>";
	     $.ajax({
		    url : 'JMSHelper.php',
		    type : 'POST',
		    dataType : 'text',
		    data : { messageContent : messageContent, isFireDrillMessage:"true",inputQueue:fireDrillForceQueue},
		    success : function(data) {
					if(data.startsWith("successfully sent")){
						displayNotification("La demande de forçage a été soumise avec succès","success");
						document.getElementById("force-fire-drill-btn").removeAttribute('disabled');
					}
					else {
						document.getElementById("force-fire-drill-btn").removeAttribute('disabled');
						displayNotification("Une erreur est survenue lors de la demande de forçage <br>" + data ,"error");
					}
		    	},
		    error : function(result, statut, error) {
				displayNotification("Une erreur est survenue lors de la demande de forçage" ,"error");
				document.getElementById("force-fire-drill-btn").removeAttribute("disabled");
	    	}
	    }).fail( function(d, textStatus, error) {
	        console.error(error);
	    });
}

function exportAudit(typeExport,fromDetailPanel) {
 	    MyTimestamp = new Date().getTime();
 	    //afficherloading();    		
		var fileName = "";
		var fileExtension = "";
		var targetURL = "";
		if(typeExport == 'current' || typeExport == 'all'){
 	    fileName = 'AuditExport_' + MyTimestamp + '.csv';
		fileExtension = ".csv"
		}
		else {
			fileName = 'AuditExport_' + MyTimestamp + '.zip';
			fileExtension = ".zip"
		}
		
		if(fromDetailPanel){
			//auditId = document.getElementById('addcode').value;
			auditId = currentAuditId;
			targetURL = "AuditCsvExport.php?exportFileName=" + fileName + "&typeExport=" + typeExport + "&auditId=" + auditId;
		}
		else {
			targetURL= "AuditCsvExport.php?exportFileName=" + fileName + "&typeExport=" + typeExport + "&pagination=<?php echo $pagination; ?>";
		}
	    $.ajax({
		    //url : 'AuditCsvExport.php?exportFileName=' + fileName + "&typeExport=" + typeExport + "&pagination=<?php echo $pagination; ?>",
		    url : targetURL,
		    type : 'POST',
		    dataType : 'text',
		    data : { fileName : String },
		    success : function(data) {
	    			masquerloading();
					if (data.startsWith("fileName=")) {
						// Result can be processed
						var result = data.split('=');
						location.href = 'ExportToCsv.php?exportFileName='+ result[1]+"&directory=<?php echo urlencode(sys_get_temp_dir()); ?>" + "&exportType=AuditExport&fileExtension=" +fileExtension ;
					} else {
						// Error when processing
						alert("Une erreur est survenue lors de l'extraction. Essayez de préciser votre recherche.");
					}
		    	},
		    error : function(result, statut, error) {
	    		masquerloading();
	    	}
		});
}

function sendMessage(messageId,code,inputQueue) {
	isFireDrillData = false;
	console.log(messageId+code+inputQueue);
	//document.getElementById("auditDetailButton-" + messageId).setAttribute("disabled", true);
	let messageContent ="<root><metatdatas>"
	$.getJSON("AuditDetailLoader.php", {columnName:'DETAILS',auditId:messageId}, function(data) {
	 if (data.metadatas != null) {
		if ($.isArray(data.metadatas) && data.metadatas.length>0) {
			for (var i = 0; i < data.metadatas.length; i++) {
				var currentKey = data.metadatas[i].key;
				var currentValue = data.metadatas[i].value!=null ? htmlspecialchars_decode(data.metadatas[i].value,2) : "";
				messageContent += "<metadata>"+"<key>" + currentKey + "</key>" + "<value>" + currentValue + "</value>" + "</metadata>";
				if(currentKey=="FIRE_DRILL_RESEND_QUEUE" && currentValue !=null){
					isFireDrillData = true;
				}
			}
		}
	 }
	 messageContent += "</metatdatas><messageContent>" + data.details.replace("<\?xml version=\"1.0\" encoding=\"UTF-8\"\?>","") + "</messageContent></root>";
	   if(isFireDrillData){
	     $.ajax({
		    url : 'JMSHelper.php',
		    type : 'POST',
		    dataType : 'text',
		    data : { messageContent : messageContent, isFireDrillMessage:"true",inputQueue:inputQueue},
		    success : function(data) {
					if(data.startsWith("successfully sent")){
						displayNotification("Le message " + code + " a été envoyé avec succès" ,"success");
						//document.getElementById("auditDetailButton-" + messageId).removeAttribute('disabled');
					}
					else {
						//document.getElementById("auditDetailButton-" + messageId).removeAttribute('disabled');
						displayNotification("Une erreur est survenue lors de l'envoi du message <br>" + data ,"error");
					}
		    	},
		    error : function(result, statut, error) {
				displayNotification("Une erreur est survenue lors de l'envoi du message" ,"error");
				//document.getElementById("auditDetailButton-" + messageId).removeAttribute("disabled");
	    	}
	    }).fail( function(d, textStatus, error) {
	        console.error(error);
	    });
	   }
	   else {
	     $.ajax({
		    url : 'JMSHelper.php?messageId=' + messageId + '&inputQueue='+inputQueue,
		    type : 'POST',
		    dataType : 'text',
		    data : { messageId : String },
		    success : function(data) {
					//alert(data);
					if(data.startsWith("successfully sent")){
						displayNotification("Le message " + code + " a été envoyé avec succès" ,"success");
						//document.getElementById("auditDetailButton-" + messageId).removeAttribute('disabled');
					}
					else {
						//document.getElementById("auditDetailButton-" + messageId).removeAttribute('disabled');
						displayNotification("Une erreur est survenue lors de l'envoi du message <br>" + data ,"error");
					}
		    	},
		    error : function(result, statut, error) {
				displayNotification("Une erreur est survenue lors de l'envoi du message" ,"error");
				//document.getElementById("auditDetailButton-" + messageId).removeAttribute("disabled");
	    	}
	    }).fail( function(d, textStatus, error) {
	        console.error(error);
	    });
 }
	}).fail( function(d, textStatus, error) {
	        console.error(error);
	    });
}

function affichage_fenetre_detail(id){
	currentAuditId = id;
	document.getElementById("adddetails").replaceChildren();
	//resetMetadataPanel();
	document.getElementById('addcode').value = '';
	document.getElementById('addinsertTimestamp').value = '';
	document.getElementById('addcodifier').value = '';
	document.getElementById('addproject').value = '';
	document.getElementById('adddata').value = '';
	// Get details
	$.getJSON("AuditDetailLoader.php", {columnName:'DETAILS',auditId:id}, function(data) {
	       affichage_audit_detail(data);
	    }).fail( function(d, textStatus, error) {
	        console.log(error);
	    });
}
function load_project_names(){
	$.getJSON("AuditDetailLoader.php", {columnName:'PROJECT'}, function(response) {
			$('#PROJECT').empty();
			var projects = $("#PROJECT");
			var selectedValue =<?php echo json_encode($criteres['PROJECT']); ?>;
			var firstSelectedTag = selectedValue == '' ? "selected" : "";
			$('#PROJECT').append($('<option>', {value: '',text: 'ALL',selected:firstSelectedTag}));
			for (var i = 0; i < response.length; i++) {
			if(response[i] != null && response[i].length>0){
			var selectTag = response[i] == selectedValue ? "selected" : "";
			projects.append($("<option " +  selectTag +"></option>").val(response[i]).text(response[i]));
			}
			}
	    }).fail( function(d, textStatus, error) {
	        console.error(error);
	    });
}
function load_status(){
	$.getJSON("AuditDetailLoader.php", {columnName:'STATUT'}, function(response) {
			$('#STATUS').empty();
			var projects = $("#STATUS");
			var selectedValue =<?php echo json_encode($criteres['STATUS']); ?>;
			var firstSelectedTag = selectedValue == '' ? "selected" : "";
			$('#STATUS').append($('<option>', {value: '',text: 'ALL',selected:firstSelectedTag}));
			for (var i = 0; i < response.length; i++) {
			if(response[i] != null && response[i].length>0){
			var selectTag = response[i] == selectedValue ? "selected" : "";
			projects.append($("<option " +  selectTag + "></option>").val(response[i]).text(response[i]));
			}
			}
	    }).fail( function(d, textStatus, error) {
	        console.error(error);
	    });
}
/*  function affichage_fenetre_confirmation(messageId,code,inputQueue){
		document.getElementById('messageId_modal').value=messageId;
		document.getElementById('code_modal').value=messageId;
		document.getElementById('inputQueue_modal').value=inputQueue;
} */

function htmlspecialchars_decode (string, quote_style) {
    var optTemp = 0,
        i = 0,        noquotes = false;
    if (typeof quote_style === 'undefined') {
        quote_style = 2;
    }
    string = string.toString().replace(/&lt;/g, '<').replace(/&gt;/g, '>');    
    var OPTS = {
        'ENT_NOQUOTES': 0,
        'ENT_HTML_QUOTE_SINGLE': 1,
        'ENT_HTML_QUOTE_DOUBLE': 2,
        'ENT_COMPAT': 2,        'ENT_QUOTES': 3,
        'ENT_IGNORE': 4
    };
    if (quote_style === 0) {
        noquotes = true;    
	}
    if (typeof quote_style !== 'number') { // Allow for a single string or an array of string flags
        quote_style = [].concat(quote_style);
        for (i = 0; i < quote_style.length; i++) {
            // Resolve string input to bitwise e.g. 'PATHINFO_EXTENSION' becomes 4            
            if (OPTS[quote_style[i]] === 0) {
                noquotes = true;
            } else if (OPTS[quote_style[i]]) {
                optTemp = optTemp | OPTS[quote_style[i]];
            }        
        }
        quote_style = optTemp;
    }
    if (quote_style & OPTS.ENT_HTML_QUOTE_SINGLE) {
        string = string.replace(/&#0*39;/g, "'"); // PHP doesn't currently escape if more than one 0, but it should        // string = string.replace(/&apos;|&#x0*27;/g, "'"); // This would also be useful here, but not a part of PHP
    }
    if (!noquotes) {
        string = string.replace(/&quot;/g, '"');
    }    // Put this in last place to avoid escape being double-decoded
    string = string.replace(/&amp;/g, '&');
    return string;
}

function createCriteriaPanel(key, value) {
	var content = "<span><fieldset class=\"form-group\"><span style=\"float:right\" class=\"glyphicon glyphicon-remove text-danger\" onclick='removeCriteria(this.parentNode); return false;'></span><BR/><select class=\"form-control\" name='criteria_key[]' name='criteria_key[]'>";
	<?php
	if ((isset($keywords)) && (count($keywords)>0)) {
		foreach ($keywords as $keyword) {
			// Attention : Javascript !!
	?>
			if (key == "<?php echo $keyword->key; ?>") {
				content = content + "<OPTION value='<?php echo $keyword->key; ?>' selected=\"true\" ><?php echo utf8_encode($keyword->label); ?></OPTION>";
			} else {
				content = content + "<OPTION value='<?php echo $keyword->key; ?>'><?php echo utf8_encode($keyword->label); ?></OPTION>";
			}
	<?php 				
		}
	} 
	?>
	content = content + "</select>&nbsp;<input class=\"form-control\" id='criteria_value[]' name='criteria_value[]' value=\""+value+"\"/>&nbsp;</fieldSet></span>";
	return content;
}

function createCriteriaPanelWithoutValue() {
	var content = "<span><fieldset class=\"form-group\"><span style=\"float:right\" class=\"glyphicon glyphicon-remove text-danger\" onclick='removeCriteria(this.parentNode); return false;'></span><BR/><select class=\"form-control\" name='criteria_key[]' name='criteria_key[]'>";
	<?php
	if ((isset($keywords)) && (count($keywords)>0)) {
		foreach ($keywords as $keyword) {
	?>
			content = content + "<OPTION value='<?php echo $keyword->key; ?>'><?php echo utf8_encode($keyword->label); ?></OPTION>";
	<?php 				
		}
	} 
	?>
	content = content + "</select>&nbsp;<input class=\"form-control\" id='criteria_value[]' name='criteria_value[]'/>&nbsp;</fieldSet></span>";
	return content;
}

function removeCriteria(criteriaToRemove) {
	criteriaToRemove.remove();
}

function createMetadata(key, value) {
	
	var content = "<span align='left'><span>"+key+"</span>&nbsp;=&nbsp;" + value + "<br/></span>"
	return content;
}
function resetMetadataTextArea(){
	document.getElementById("functionalDataText").value = "";
}

function resetMetadataPanel() {
	//alert("resetMetadataPanel");
	var panel = document.getElementById('metadataList');
	if(panel){
    panel.innerHTML = "";
	}
    var panel = document.getElementById('metadataListTitle');
	if(panel){
    panel.innerHTML = "";
	}
}

function addLabel() {
	var panel = document.getElementById('metadataListTitle');
	panel.innerHTML = "<span>Données associées:</span><br/>";
}

function closeLabel() {
  var panel = document.getElementById('metadataList');
  panel.innerHTML = "<div id=\"metadataList\"><fieldset style=\"background-color:#f5f2f0\" class=\"form-group\"> " + panel.innerHTML + "</fieldset></div>"; 
}

function addMetadata(key, value) {
	var panel = document.getElementById('metadataList');
	panel.append(createMetadata(key, value));
}

function addMetadataCriteriaForDetail(key, value) {
	var panel = document.getElementById('metadataList');
	panel.innerHTML = panel.innerHTML + createMetadata(key, value);
}

function addMetadataCriteria(key, value) {
	var panel = document.getElementById('metadataCriterias');
	panel.innerHTML = panel.innerHTML + createCriteriaPanel(key, value);
}

function addNewCriteria() {
	var panel = document.getElementById('metadataCriterias');
	panel.innerHTML = panel.innerHTML + createCriteriaPanelWithoutValue();
}

function showLoading(){
	$('#loadingModal').modal('show');
}

function hideLoading(){
	$('#loadingModal').modal('hide');
}

function initDateRangePicker(resetDate = false){
	var d = new Date();
	var end = new Date();
	end.setHours(23,59,59,999);
	var todayDate = d.getDate();
	if(resetDate==true){
		startDatestring = new Date(Date.now() - 864e5);
		startDatestring.setHours(0, 0, 0);
	}		
	else {
	<?php if (array_key_exists('TIMESTAMP', $criteres)) { ?>
	var startDatestring = "<?php echo $criteres['TIMESTAMP']; ?>";
	<?php } else { ?>
	var startDatestring = (d.getDate() -1)  + "-" + (d.getMonth()+1) + "-" + d.getFullYear() + " " + d.getHours() + ":" + d.getMinutes();
	<?php } ?>
	}
	if(resetDate==true){
		var endDatestring = (d.getDate() + 1 )  + "-" + (d.getMonth()+1) + "-" + d.getFullYear() + " 23:59";
	}
    else {	
	<?php if (array_key_exists('TIMESTAMP2', $criteres)) { ?>
	var endDatestring = "<?php echo $criteres['TIMESTAMP2']; ?>";
	<?php } else { ?>
	var endDatestring = (d.getDate() + 1 )  + "-" + (d.getMonth()+1) + "-" + d.getFullYear() + " 23:59";
	<?php } ?>
	}
    $('input[name="daterange"]').daterangepicker({
    	 	"timePicker": true,
    	    "timePicker24Hour": true,
    	    "autoApply": true,
    	    "alwaysShowCalendars": true,
    	     "minDate": new Date(new Date().setDate(todayDate - 365)),
    	     "maxDate": end,
    	    "locale": {
    	    	"format": 'DD-MM-YYYY HH:mm',
    	        "separator": " - ",
    	        "applyLabel": "Appliquer",
    	        "cancelLabel": "Annuler",
    	        "fromLabel": "De",
    	        "toLabel": "a",
    	        "customRangeLabel": "Custom",
    	        "daysOfWeek": ["Di","Lu","Ma","Me","Je","Ve","Sa"],
    	        "monthNames": ["Janvier","Fevrier","Mars","Avril","Mai","Juin","Juillet","Aout","Septembre","Octobre","Novembre","Decembre"],
    	    },
    	    "startDate": startDatestring,
    	    "endDate": endDatestring,
    	}, function(start, end, label) {
    }).on('apply.daterangepicker', function (ev, picker) {
	var start = picker.startDate.clone().startOf('day');
	var end   = picker.endDate.clone().endOf('day');

  var today = moment().endOf('day');

  // borne haute absolue = aujourd'hui (pas de futur)
  if (end.isAfter(today)) {
    end = today.clone();
  }
  // limite douce à 30 jours (diff exclusive : autorise 30, refuse 31+)
  var diffDays = end.diff(start, 'days');
  if (diffDays > 30) {
    var softMax = start.clone().add(30, 'days').endOf('day');
    // ne pas dépasser aujourd'hui
    end = moment.min(softMax, today);
    picker.setEndDate(end);
	displayNotification("La plage de dates s'étale à 30 jours max","warning");
  // maj de l'input
  $(this).val(start.format('DD-MM-YYYY HH:mm') + " - " + end.format('DD-MM-YYYY HH:mm'));
  }
});
}

window.addEventListener('resize', function () {
	//setDivsHeight();
	});

let innerHeightWin = 0;
function setDivsHeight(){
    let browserZoomLevel = Math.round(window.devicePixelRatio * 100);
	let outerHeightWin=0;
	if(browserZoomLevel>=100){
	innerHeightWin = $(window).height() - (300 * 100/browserZoomLevel);	
	outerHeightWin = $(window).height() - (160 * 100/browserZoomLevel);
	}
	else{
		innerHeightWin = $(window).height() - (250 * 100/browserZoomLevel);
		outerHeightWin = $(window).height() + (250 * 100/browserZoomLevel);
	}
	$('#auditTable').bootstrapTable('resetView',{height: innerHeightWin});
	$('#bigsidebar').css('height', outerHeightWin);
}

	
$(document).ready(function(){
	initDateRangePicker();
	$('#fenetre-access-modif-audit').on('transitionstart', function() {
		$('body').removeClass('modal-open');
		$('body').addClass('modal-open-fullscreen');
	});
	$('#fenetre-access-modif-audit').on('hidden.bs.modal', function () {
	isForamted=false;
	auditDetails="";
	document.getElementById('prettyPrintXMLBtn').innerHTML="Format XML";
	})
	//load_project_names();
	load_status();
	currentAuditId="";
	$("#exportCsvMenuLayer").contextMenu({
								menu: 'exportCsvMenu'
							},
							function(action, el, pos) {
								if (action=="export-current") {
									exportAudit("current",false);
								}
								else if (action=="export-details") {
									exportAudit("details",false);
								}
								else {
									exportAudit("all",false);
								}
							});
 	var copyDetailsbtn = document.getElementById("copy-details-btn");
    var detailsClipboard = new Clipboard(copyDetailsbtn);
	setDivsHeight();
	$("#auditData").typeahead({
                        minLength: 1,
                        items: <?php echo count($data);?> ,
                        
