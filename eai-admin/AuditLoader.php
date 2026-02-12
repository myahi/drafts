<?php

include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';

// Turn off all error reporting
//error_reporting (0);
try {
	// -------------------------- VERIFICATION DE LA SESSION Et DES DROITS -------------------------------------
	$_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
	include 'verification.php';
	
	// ---------------------------------- RECHERCHE 1/2 --------------------------------------
	$criteres = array (
			'CODE' => '',
			'CODIFIER' => '',
			'PROJECT' => '',
			'STATUS' => '',
			'DATA' => '',
			'TIMESTAMP' => '',
			'INSERT_TIMESTAMP' => '',
			'DETAILS' => '',
			'FLOWS' => '',
			'DISPLAYED_FLOW_TAGS' => ''
	);
	
	$pagination = 0;
	// RECHERCHE 1/2
	if ((isset($_SESSION['recherche_code'])) || (!empty($_SESSION['recherche_code']))) {
			$criteres ['CODE'] = $_SESSION ['recherche_code'];
	}
	
	if ((isset($_SESSION['recherche_codifier'])) || (!empty($_SESSION['recherche_codifier']))) {
			$criteres ['CODIFIER'] = $_SESSION ['recherche_codifier'];
	}
	$useAuditArchive = null;
	if ((isset($_SESSION['recherche_useArchiveAudit'])) || (!empty($_SESSION['recherche_useArchiveAudit']))) {
		$criteres ['useArchiveAudit'] = $_SESSION ['recherche_useArchiveAudit'];
		$useAuditArchive = true;	
	}
	if ((isset($_SESSION['recherche_project'])) || (!empty($_SESSION['recherche_project']))) {
			$criteres ['PROJECT'] = $_SESSION ['recherche_project'];
	}
	
	if ((isset($_SESSION['recherche_flows'])) || (!empty($_SESSION['recherche_flows']))) {
			$criteres ['FLOWS'] = $_SESSION ['recherche_flows'];
	}
	if ((isset($_SESSION['recherche_displayed_flow_tags'])) || (!empty($_SESSION['recherche_displayed_flow_tags']))) {
			$criteres ['DISPLAYED_FLOW_TAGS'] = $_SESSION ['recherche_displayed_flow_tags'];
	}

	if ((isset($_SESSION['recherche_status'])) || (!empty($_SESSION['recherche_status']))) {
			$criteres ['STATUS'] = $_SESSION ['recherche_status'];
	}
	
	if ((isset($_SESSION['recherche_data'])) || (!empty($_SESSION['recherche_data']))) {
			$criteres ['DATA'] = $_SESSION ['recherche_data'];
	}
	
	if ((isset ( $_SESSION ['audit_metadata'] )) || (! empty ( $_SESSION ['audit_metadata'] ))){
		$criteres ['audit_metadata'] = $_SESSION ['audit_metadata'];
	}
	
	if ((isset($_SESSION['recherche_timestamp'])) || (!empty($_SESSION['recherche_timestamp']))) {
		$criteres ['TIMESTAMP'] = $_SESSION ['recherche_timestamp'];
	}
	if ((! isset ( $criteres ['TIMESTAMP'] )) || (empty ( $criteres ['TIMESTAMP'] ))) {
		//$criteres ['TIMESTAMP'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d")-1, date("Y")));
		$criteres ['TIMESTAMP'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
	}
	
	if ((isset($_SESSION['recherche_timestamp2'])) || (!empty($_SESSION['recherche_timestamp2']))) {
			$criteres ['TIMESTAMP2'] = $_SESSION ['recherche_timestamp2'];
	}

	if ((! isset ( $criteres ['TIMESTAMP2'] )) || (empty ( $criteres ['TIMESTAMP2'] ))) { 
			$criteres ['TIMESTAMP2'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d")+1, date("Y")));
	}
		
	if ((isset ( $_SESSION ['recherche_details'] )) || (! empty ( $_SESSION ['recherche_details'] ))) {
		$criteres ['DETAILS'] = $_SESSION ['recherche_details'];
	}

	$orderColumn = "";
	if (! empty ( $_SESSION ['recherche_store'] ) && strpos ( "-CODE-CODIFIER-PROJECT-DATA-TIMESTAMP-STATUT", $_SESSION ['recherche_store'] ) == true) {
			$orderColumn = $_SESSION ['recherche_store'];
	}
	
	if ((isset ( $_SESSION ['recherche_SENS_AFF'] )) || (! empty ( $_SESSION ['recherche_SENS_AFF'] ))) {
		$orderDirection = $_SESSION ['recherche_SENS_AFF'];
	} else {
		$orderDirection = "desc";
	}
	
	$pagination = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
	//$pagination = 0;
	
	//$intervalle = 25;
	/* if ((isset ( $_SESSION ['recherche_intervalle'] )) || (! empty ( $_SESSION ['recherche_intervalle'] ))) {
		$intervalle = $_SESSION ['recherche_intervalle'];
	} else {
		$_SESSION ['recherche_intervalle'] = $intervalle;
	} */
	
	$intervalle = isset($_GET['intervalle']) ? (int)$_GET['intervalle'] : 50;
	
	// Creation des objets DAO
	$auditDAO = new AuditDAO ();
	$auditKeywordDAO = new AuditKeywordDAO();
	// Recuperation de l'ensemble des keywords existants
	$keywords = $auditKeywordDAO->getAllMetadataKeywords(NULL, NULL);
	// Ensemble des projets 
	$resultProject = $auditDAO->getProjetNames();
	// Ensemble des Status 
	//$resultStatus = $auditDAO->getStatus();
	// RECHERCHE 2/2
	$useAuditArchive = $useAuditArchive==null?$useAuditArchive=false:true;
	$auditInformations = $auditDAO->getAuditData($pagination, $intervalle, $orderColumn, $criteres, $orderDirection,$useAuditArchive);

	if ((isset ( $_SESSION ['descriptionList'] )) || (! empty ( $_SESSION ['descriptionList'] ))) {
		$data = $_SESSION ['descriptionList'];
	} else {
		$data = $auditKeywordDAO -> getAuditDescriptionsList();
		$_SESSION ['descriptionList'] = $data;
	}
	
	if ((isset ( $_SESSION ['codifiersList'] )) || (! empty ($_SESSION ['codifiersList'] ))) {
		$codifiers = $_SESSION ['codifiersList'];
	} else {
		$codifiers = $auditKeywordDAO -> getAuditCodifiersList();
		$_SESSION ['codifiersList'] = $codifiers;
	}
	//$codifiers = $auditKeywordDAO -> getAuditCodifiersList();
	//	$_SESSION ['codifiersList'] = $codifiers;

	//$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
	//$offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
	$rows = $auditInformations->auditLines;
	$response = ['total'=> $auditInformations->total,'rows' => $rows];
	echo json_encode(['total'=>$auditInformations->total,'rows'=>$rows],JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);
	//echo json_encode($auditInformations, true);
	} catch ( Exception $e ) { }
?>
