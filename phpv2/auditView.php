<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
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

// ===== ANTI-BOUCLE TABLE =====
let isTableLoading = false;
let tableXHR = null;              // XHR utilisé par BootstrapTable custom ajax
let allowTableRequest = true;     // tu peux mettre false si tu veux empêcher le load initial

if (!String.prototype.startsWith) {
	String.prototype.startsWith = function(searchString, position) {
		position = position || 0;
		return this.indexOf(searchString, position) === position;
	};
}

$(function() {
  // Clipboard : instancier une seule fois (évite de re-instancier à chaque clic)
  try { new Clipboard(".copyButton"); } catch (e) {}

  $(document).on("click", ".copyButton", function() {
    // noop: gestion par Clipboard déjà instancié
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
		document.getElementById('prettyPrintXMLBtn').innerHTML="Format XML";
		var xmlValue = auditDetails!=null ? htmlspecialchars_decode(auditDetails,2) : "";
		$('#adddetails').text(xmlValue);
		isForamted=false;
	}
	else {
		document.getElementById('prettyPrintXMLBtn').innerHTML = "Format brut";
		var xmlValue = auditDetails!=null ? htmlspecialchars_decode(auditDetails,2) : "";
		$('#adddetails').text(vkbeautify.xml(xmlValue));
		Prism.highlightAll($('#adddetails'));
		$("#adddetails").toggleClass("language-markup");
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

	pData.details ? $('#download-details-zip-btn').show():$('#download-details-zip-btn').hide();
	var xmlVa
