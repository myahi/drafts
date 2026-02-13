<?php

include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'connexion.php';

header('Content-Type: application/json; charset=utf-8');
ini_set('display_errors', '0');
error_reporting(0);

// Default timezone
if (!ini_get('date.timezone')) {
	date_default_timezone_set('Europe/Paris');
}

if (session_status() !== PHP_SESSION_ACTIVE) {
	session_start();
}

/**
 * -----------------------------
 * POST = mise à jour des critères en session
 * -----------------------------
 */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {

	// Reset critères
	if (isset($_POST['init'])) {

		$_SESSION['audit_metadata'] = null;
		$_SESSION['recherche_code'] = null;
		$_SESSION['recherche_codifier'] = null;
		$_SESSION['recherche_project'] = null;
		$_SESSION['recherche_flows'] = null;
		$_SESSION['recherche_displayed_flow_tags'] = null;
		$_SESSION['recherche_status'] = null;
		$_SESSION['recherche_data'] = null;
		$_SESSION['recherche_timestamp'] = null;
		$_SESSION['recherche_timestamp2'] = null;
		$_SESSION['recherche_store'] = null;
		$_SESSION['recherche_details'] = null;
		$_SESSION['recherche_intervalle'] = null;

		// IMPORTANT : clé utilisée par le loader
		$_SESSION['recherche_useArchiveAudit'] = null;

		echo json_encode(['ok' => true], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
		exit;
	}

	// Valorisation des critères de recherche
	if (isset($_POST['code']) && !empty($_POST['code'])) {
		$_SESSION['recherche_code'] = $_POST['code'];
	} else {
		$_SESSION['recherche_code'] = isset($_POST['code']) ? $_POST['code'] : $_SESSION['recherche_code'];
	}

	if (isset($_POST['codifier']) && !empty($_POST['codifier'])) {
		$_SESSION['recherche_codifier'] = $_POST['codifier'];
	} else {
		$_SESSION['recherche_codifier'] = isset($_POST['codifier']) ? $_POST['codifier'] : $_SESSION['recherche_codifier'];
	}

	if (isset($_POST['PROJECT']) && !empty($_POST['PROJECT'])) {
		$_SESSION['recherche_project'] = $_POST['PROJECT'];
	} else {
		$_SESSION['recherche_project'] = isset($_POST['PROJECT']) ? $_POST['PROJECT'] : $_SESSION['recherche_project'];
	}

	if (isset($_POST['FLOWS']) && !empty($_POST['FLOWS'])) {
		$_SESSION['recherche_flows'] = $_POST['FLOWS'];
	} else if (isset($_POST['FLOWS'])) {
		$_SESSION['recherche_flows'] = $_POST['FLOWS'];
	}

	if (isset($_POST['DISPLAYED_FLOW_TAGS']) && !empty($_POST['DISPLAYED_FLOW_TAGS'])) {
		$_SESSION['recherche_displayed_flow_tags'] = $_POST['DISPLAYED_FLOW_TAGS'];
	} else if (isset($_POST['DISPLAYED_FLOW_TAGS'])) {
		$_SESSION['recherche_displayed_flow_tags'] = $_POST['DISPLAYED_FLOW_TAGS'];
	}

	if (isset($_POST['STATUS']) && !empty($_POST['STATUS'])) {
		$_SESSION['recherche_status'] = $_POST['STATUS'];
	} else {
		$_SESSION['recherche_status'] = isset($_POST['STATUS']) ? $_POST['STATUS'] : $_SESSION['recherche_status'];
	}

	// useArchiveAudit
	if (isset($_POST['useArchiveAudit']) && !empty($_POST['useArchiveAudit'])) {
		$_SESSION['recherche_useArchiveAudit'] = $_POST['useArchiveAudit'];
	} else {
		$_SESSION['recherche_useArchiveAudit'] = null;
	}

	if (isset($_POST['auditData'])) {
		$_SESSION['recherche_data'] = $_POST['auditData'];
	}

	// critères metadata
	if (isset($_POST['criteria_key']) && !empty($_POST['criteria_key'])) {
		$auditMetadata = [];

		if (is_array($_POST['criteria_key'])) {
			for ($i = 0; $i < sizeof($_POST['criteria_key']); $i++) {
				$property = [];
				$key = $_POST['criteria_key'][$i];
				$val = isset($_POST['criteria_value'][$i]) ? $_POST['criteria_value'][$i] : '';
				$property[$key] = htmlspecialchars($val);
				$auditMetadata[] = $property;
			}
		} else {
			$property = [];
			$property[$_POST['criteria_key']] = isset($_POST['criteria_value']) ? $_POST['criteria_value'] : '';
			$auditMetadata[] = $property;
		}

		$_SESSION['audit_metadata'] = $auditMetadata;
	} else {
		$_SESSION['audit_metadata'] = null;
	}

	// daterange -> timestamp / timestamp2
	if (isset($_POST['daterange']) && !empty($_POST['daterange'])) {
		if ($_POST['daterange'] === 'fromStats') {
			$currentDate = isset($_POST['currentDate']) ? $_POST['currentDate'] : '';
			$startDateTime = isset($_POST['TIMESTAMP']) ? substr($_POST['TIMESTAMP'], strlen($_POST['TIMESTAMP']) - 5) : '00:00';
			$endDateTime = isset($_POST['TIMESTAMP2']) ? substr($_POST['TIMESTAMP2'], strlen($_POST['TIMESTAMP2']) - 5) : '23:59';

			$_SESSION['recherche_timestamp'] = trim($currentDate . " " . $startDateTime);
			$_SESSION['recherche_timestamp2'] = trim($currentDate . " " . $endDateTime);
		} else {
			$startDateTime = strstr($_POST['daterange'], ' - ', true);
			$endDateTime = substr($_POST['daterange'], stripos($_POST['daterange'], ' - ') + 3);

			$_SESSION['recherche_timestamp'] = $startDateTime;
			$_SESSION['recherche_timestamp2'] = $endDateTime;
		}
	}

	if (isset($_POST['details'])) {
		$_SESSION['recherche_details'] = $_POST['details'];
	}

	if (isset($_POST['store'])) {
		$_SESSION['recherche_store'] = $_POST['store'];
	}

	if (isset($_POST['sensAff']) && !empty($_POST['sensAff'])) {
		$_SESSION['recherche_SENS_AFF'] = $_POST['sensAff'];
	} else if (isset($_POST['sensAff'])) {
		$_SESSION['recherche_SENS_AFF'] = $_POST['sensAff'];
	}

	if (isset($_POST['intervalle']) && !empty($_POST['intervalle'])) {
		$_SESSION['recherche_intervalle'] = $_POST['intervalle'];
	}

	echo json_encode(['ok' => true], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
	exit;
}

/**
 * -----------------------------
 * GET = chargement des données pour bootstrap-table
 * -----------------------------
 */
try {
	// Vérification session/droits
	$_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
	include 'verification.php';

	$criteres = array(
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

	if (isset($_SESSION['recherche_code']) && !empty($_SESSION['recherche_code'])) {
		$criteres['CODE'] = $_SESSION['recherche_code'];
	}

	if (isset($_SESSION['recherche_codifier']) && !empty($_SESSION['recherche_codifier'])) {
		$criteres['CODIFIER'] = $_SESSION['recherche_codifier'];
	}

	$useAuditArchive = false;
	if (isset($_SESSION['recherche_useArchiveAudit']) && !empty($_SESSION['recherche_useArchiveAudit'])) {
		$criteres['useArchiveAudit'] = $_SESSION['recherche_useArchiveAudit'];
		$useAuditArchive = true;
	}

	if (isset($_SESSION['recherche_project']) && !empty($_SESSION['recherche_project'])) {
		$criteres['PROJECT'] = $_SESSION['recherche_project'];
	}

	if (isset($_SESSION['recherche_flows']) && !empty($_SESSION['recherche_flows'])) {
		$criteres['FLOWS'] = $_SESSION['recherche_flows'];
	}

	if (isset($_SESSION['recherche_displayed_flow_tags']) && !empty($_SESSION['recherche_displayed_flow_tags'])) {
		$criteres['DISPLAYED_FLOW_TAGS'] = $_SESSION['recherche_displayed_flow_tags'];
	}

	if (isset($_SESSION['recherche_status']) && !empty($_SESSION['recherche_status'])) {
		$criteres['STATUS'] = $_SESSION['recherche_status'];
	}

	if (isset($_SESSION['recherche_data']) && !empty($_SESSION['recherche_data'])) {
		$criteres['DATA'] = $_SESSION['recherche_data'];
	}

	if (isset($_SESSION['audit_metadata']) && !empty($_SESSION['audit_metadata'])) {
		$criteres['audit_metadata'] = $_SESSION['audit_metadata'];
	}

	if (isset($_SESSION['recherche_timestamp']) && !empty($_SESSION['recherche_timestamp'])) {
		$criteres['TIMESTAMP'] = $_SESSION['recherche_timestamp'];
	}

	if (!isset($criteres['TIMESTAMP']) || empty($criteres['TIMESTAMP'])) {
		$criteres['TIMESTAMP'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
	}

	if (isset($_SESSION['recherche_timestamp2']) && !empty($_SESSION['recherche_timestamp2'])) {
		$criteres['TIMESTAMP2'] = $_SESSION['recherche_timestamp2'];
	}

	if (!isset($criteres['TIMESTAMP2']) || empty($criteres['TIMESTAMP2'])) {
		$criteres['TIMESTAMP2'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d") + 1, date("Y")));
	}

	if (isset($_SESSION['recherche_details']) && !empty($_SESSION['recherche_details'])) {
		$criteres['DETAILS'] = $_SESSION['recherche_details'];
	}

	$orderColumn = "";
	if (!empty($_SESSION['recherche_store']) && strpos("-CODE-CODIFIER-PROJECT-DATA-TIMESTAMP-STATUT", $_SESSION['recherche_store']) == true) {
		$orderColumn = $_SESSION['recherche_store'];
	}

	$orderDirection = "desc";
	if (isset($_SESSION['recherche_SENS_AFF']) && !empty($_SESSION['recherche_SENS_AFF'])) {
		$orderDirection = $_SESSION['recherche_SENS_AFF'];
	}

	$pagination = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

	// bootstrap-table envoie généralement "limit"
	if (isset($_GET['limit'])) {
		$intervalle = (int)$_GET['limit'];
	} else if (isset($_GET['intervalle'])) {
		$intervalle = (int)$_GET['intervalle'];
	} else {
		$intervalle = 50;
	}

	$auditDAO = new AuditDAO();
	$auditKeywordDAO = new AuditKeywordDAO();

	// On garde les appels existants (même si non utilisés ici, le view s’appuie sur ces listes ailleurs)
	$keywords = $auditKeywordDAO->getAllMetadataKeywords(NULL, NULL);
	$resultProject = $auditDAO->getProjetNames();

	$auditInformations = $auditDAO->getAuditData($pagination, $intervalle, $orderColumn, $criteres, $orderDirection, $useAuditArchive);

	$rows = $auditInformations->auditLines;
	echo json_encode(['total' => $auditInformations->total, 'rows' => $rows], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
	exit;

} catch (Throwable $e) {
	http_response_code(500);
	echo json_encode(['total' => 0, 'rows' => [], 'error' => 'AUDIT_LOADER_ERROR'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
	exit;
}
