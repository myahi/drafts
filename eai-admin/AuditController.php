<?php

include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'connexion.php';

if (!ini_get('date.timezone')) {
    date_default_timezone_set('Europe/Paris');
}

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

function auditController_initCriteres() {
    return array(
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
}

function auditController_resetSearchContext(&$criteres) {
    $criteres['CODE'] = '';
    $criteres['CODIFIER'] = '';
    $criteres['PROJECT'] = '';
    $criteres['STATUS'] = '';
    $criteres['DATA'] = '';
    $criteres['TIMESTAMP'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
    $criteres['TIMESTAMP2'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d") + 1, date("Y")));
    $criteres['audit_metadata'] = null;
    $criteres['FLOWS'] = '';
    $criteres['DISPLAYED_FLOW_TAGS'] = '';
    $criteres['useArchiveAudit'] = null;

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
    $_SESSION['recherche_useArchiveAudit'] = null;
    $_SESSION['useArchiveAudit'] = null;

    $_POST['code'] = null;
    $_POST['codifier'] = null;
    $_POST['PROJECT'] = null;
    $_POST['STATUS'] = null;
    $_POST['criteria_key'] = null;
    $_POST['criteria_value'] = null;
    $_POST['auditData'] = null;
    $_POST['timestamp'] = null;
    $_POST['timestamp2'] = null;
    $_POST['details'] = null;
    $_POST['store'] = null;
    $_POST['intervalle'] = null;
    $_POST['useArchiveAudit'] = null;
    $_POST['FLOWS'] = null;
    $_POST['DISPLAYED_FLOW_TAGS'] = null;
    $_GET['pagination'] = null;
}

function auditController_applySearchRequest(&$criteres) {
    if (isset($_POST['init'])) {
        auditController_resetSearchContext($criteres);
        return;
    }

    if (isset($_POST['code']) || !empty($_POST['code'])) {
        $criteres['CODE'] = $_POST['code'];
        $_SESSION['recherche_code'] = $_POST['code'];
    }

    if (isset($_POST['codifier']) || !empty($_POST['codifier'])) {
        $criteres['CODIFIER'] = $_POST['codifier'];
        $_SESSION['recherche_codifier'] = $_POST['codifier'];
    }

    if (isset($_POST['PROJECT']) || !empty($_POST['PROJECT'])) {
        $criteres['PROJECT'] = $_POST['PROJECT'];
        $_SESSION['recherche_project'] = $_POST['PROJECT'];
    }

    if (isset($_POST['FLOWS']) || !empty($_POST['FLOWS'])) {
        $criteres['FLOWS'] = $_POST['FLOWS'];
        $_SESSION['recherche_flows'] = $_POST['FLOWS'];
    }

    if (isset($_POST['DISPLAYED_FLOW_TAGS']) || !empty($_POST['DISPLAYED_FLOW_TAGS'])) {
        $criteres['DISPLAYED_FLOW_TAGS'] = $_POST['DISPLAYED_FLOW_TAGS'];
        $_SESSION['recherche_displayed_flow_tags'] = $_POST['DISPLAYED_FLOW_TAGS'];
    }

    if (isset($_POST['STATUS']) || !empty($_POST['STATUS'])) {
        $criteres['STATUS'] = $_POST['STATUS'];
        $_SESSION['recherche_status'] = $_POST['STATUS'];
    }

    if (isset($_POST['useArchiveAudit']) || !empty($_POST['useArchiveAudit'])) {
        $criteres['useArchiveAudit'] = $_POST['useArchiveAudit'];
        $_SESSION['recherche_useArchiveAudit'] = $_POST['useArchiveAudit'];
    } else {
        $_SESSION['recherche_useArchiveAudit'] = null;
        $criteres['useArchiveAudit'] = null;
    }

    if (isset($_POST['auditData']) || !empty($_POST['auditData'])) {
        $criteres['DATA'] = $_POST['auditData'];
        $_SESSION['recherche_data'] = $_POST['auditData'];
    }

    if (isset($_POST['criteria_key']) || !empty($_POST['criteria_key'])) {
        $criteres['audit_metadata'] = array();
        if (is_array($_POST['criteria_key'])) {
            for ($i = 0; $i < sizeof($_POST['criteria_key']); $i++) {
                $property = array();
                $property[$_POST['criteria_key'][$i]] = htmlspecialchars($_POST['criteria_value'][$i]);
                $criteres['audit_metadata'][] = $property;
            }
        } else {
            $property = array();
            $property[$_POST['criteria_key']] = $_POST['criteria_value'];
            $criteres['audit_metadata'][] = $property;
        }
        $_SESSION['audit_metadata'] = $criteres['audit_metadata'];
    } else {
        $_SESSION['audit_metadata'] = null;
        $criteres['audit_metadata'] = null;
    }

    if (isset($_POST['daterange']) || !empty($_POST['daterange'])) {
        if ($_POST['daterange'] == 'fromStats') {
            $currentDate = $_POST['currentDate'];

            $startDateTime = substr($_POST['TIMESTAMP'], strlen($_POST['TIMESTAMP']) - 5);
            $endDateTime = substr($_POST['TIMESTAMP2'], strlen($_POST['TIMESTAMP2']) - 5);

            $criteres['TIMESTAMP'] = $currentDate . " " . $startDateTime;
            $_SESSION['recherche_timestamp'] = $currentDate . " " . $startDateTime;

            $criteres['TIMESTAMP2'] = $currentDate . " " . $endDateTime;
            $_SESSION['recherche_timestamp2'] = $currentDate . " " . $endDateTime;
        } else {
            $startDateTime = strstr($_POST['daterange'], ' - ', true);
            $criteres['TIMESTAMP'] = $startDateTime;
            $_SESSION['recherche_timestamp'] = $startDateTime;

            $endDateTime = substr($_POST['daterange'], stripos($_POST['daterange'], ' - ') + 3);
            $criteres['TIMESTAMP2'] = $endDateTime;
            $_SESSION['recherche_timestamp2'] = $endDateTime;
        }
    }

    if (isset($_POST['details']) || !empty($_POST['details'])) {
        $criteres['DETAILS'] = $_POST['details'];
        $_SESSION['recherche_details'] = $_POST['details'];
    }

    if (isset($_POST['store']) || !empty($_POST['store'])) {
        $_SESSION['recherche_store'] = $_POST['store'];
    }

    if (isset($_POST['sensAff']) || !empty($_POST['sensAff'])) {
        $_SESSION['recherche_SENS_AFF'] = $_POST['sensAff'];
    }

    if (isset($_POST['intervalle']) || !empty($_POST['intervalle'])) {
        $_SESSION['recherche_intervalle'] = $_POST['intervalle'];
    }
}

function auditController_loadContext($loaderMode = false) {
    $criteres = auditController_initCriteres();
    $pagination = 0;
    $useAuditArchive = null;

    if ((isset($_SESSION['recherche_code'])) || (!empty($_SESSION['recherche_code']))) {
        $criteres['CODE'] = $_SESSION['recherche_code'];
    }

    if ((isset($_SESSION['recherche_codifier'])) || (!empty($_SESSION['recherche_codifier']))) {
        $criteres['CODIFIER'] = $_SESSION['recherche_codifier'];
    }

    if ((isset($_SESSION['recherche_useArchiveAudit'])) || (!empty($_SESSION['recherche_useArchiveAudit']))) {
        $criteres['useArchiveAudit'] = $_SESSION['recherche_useArchiveAudit'];
        $useAuditArchive = true;
    }

    if ((isset($_SESSION['recherche_project'])) || (!empty($_SESSION['recherche_project']))) {
        $criteres['PROJECT'] = $_SESSION['recherche_project'];
    }

    if ((isset($_SESSION['recherche_flows'])) || (!empty($_SESSION['recherche_flows']))) {
        $criteres['FLOWS'] = $_SESSION['recherche_flows'];
    } elseif (!$loaderMode) {
        $criteres['FLOWS'] = array();
    }

    if ((isset($_SESSION['recherche_displayed_flow_tags'])) || (!empty($_SESSION['recherche_displayed_flow_tags']))) {
        $criteres['DISPLAYED_FLOW_TAGS'] = $_SESSION['recherche_displayed_flow_tags'];
    } elseif (!$loaderMode) {
        $criteres['DISPLAYED_FLOW_TAGS'] = array();
    }

    if ((isset($_SESSION['recherche_status'])) || (!empty($_SESSION['recherche_status']))) {
        $criteres['STATUS'] = $_SESSION['recherche_status'];
    }

    if ((isset($_SESSION['recherche_data'])) || (!empty($_SESSION['recherche_data']))) {
        $criteres['DATA'] = $_SESSION['recherche_data'];
    }

    if ((isset($_SESSION['audit_metadata'])) || (!empty($_SESSION['audit_metadata']))) {
        $criteres['audit_metadata'] = $_SESSION['audit_metadata'];
    }

    if ((isset($_SESSION['recherche_timestamp'])) || (!empty($_SESSION['recherche_timestamp']))) {
        $criteres['TIMESTAMP'] = $_SESSION['recherche_timestamp'];
    }
    if ((!isset($criteres['TIMESTAMP'])) || (empty($criteres['TIMESTAMP']))) {
        $criteres['TIMESTAMP'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
    }

    if ((isset($_SESSION['recherche_timestamp2'])) || (!empty($_SESSION['recherche_timestamp2']))) {
        $criteres['TIMESTAMP2'] = $_SESSION['recherche_timestamp2'];
    }
    if ((!isset($criteres['TIMESTAMP2'])) || (empty($criteres['TIMESTAMP2']))) {
        $criteres['TIMESTAMP2'] = date("d-m-Y H:i", mktime(0, 0, 0, date("m"), date("d") + 1, date("Y")));
    }

    if ((isset($_SESSION['recherche_details'])) || (!empty($_SESSION['recherche_details']))) {
        $criteres['DETAILS'] = $_SESSION['recherche_details'];
    }

    $orderColumn = "";
    if (!empty($_SESSION['recherche_store']) && strpos("-CODE-CODIFIER-PROJECT-DATA-TIMESTAMP-STATUT", $_SESSION['recherche_store']) == true) {
        $orderColumn = $_SESSION['recherche_store'];
    }

    if ((isset($_SESSION['recherche_SENS_AFF'])) || (!empty($_SESSION['recherche_SENS_AFF']))) {
        $orderDirection = $_SESSION['recherche_SENS_AFF'];
    } else {
        $orderDirection = "desc";
    }

    if ((isset($_GET['pagination'])) || (!empty($_GET['pagination']))) {
        $pagination = $_GET['pagination'];
    }

    if ($loaderMode) {
        $pagination = isset($_GET['offset']) ? (int) $_GET['offset'] : 0;
        $intervalle = isset($_GET['limit']) ? (int) $_GET['limit'] : (isset($_GET['intervalle']) ? (int) $_GET['intervalle'] : 50);
    } else {
        $intervalle = 25;
        if ((isset($_SESSION['recherche_intervalle'])) || (!empty($_SESSION['recherche_intervalle']))) {
            $intervalle = $_SESSION['recherche_intervalle'];
        } else {
            $_SESSION['recherche_intervalle'] = $intervalle;
        }
    }

    $auditDAO = new AuditDAO();
    $auditKeywordDAO = new AuditKeywordDAO();
    $keywords = $auditKeywordDAO->getAllMetadataKeywords(NULL, NULL);
    $resultProject = $auditDAO->getProjetNames();

    if ((isset($_SESSION['descriptionList'])) || (!empty($_SESSION['descriptionList']))) {
        $data = $_SESSION['descriptionList'];
    } else {
        $data = $auditKeywordDAO->getAuditDescriptionsList();
        $_SESSION['descriptionList'] = $data;
    }

    $flowsList = $auditKeywordDAO->getAuditFlowsList();

    if ((isset($_SESSION['codifiersList'])) || (!empty($_SESSION['codifiersList']))) {
        $codifiers = $_SESSION['codifiersList'];
    } else {
        $codifiers = $auditKeywordDAO->getAuditCodifiersList();
        $_SESSION['codifiersList'] = $codifiers;
    }

    return array(
        'criteres' => $criteres,
        'pagination' => $pagination,
        'intervalle' => $intervalle,
        'orderColumn' => $orderColumn,
        'orderDirection' => $orderDirection,
        'useAuditArchive' => $useAuditArchive,
        'auditDAO' => $auditDAO,
        'auditKeywordDAO' => $auditKeywordDAO,
        'keywords' => $keywords,
        'resultProject' => $resultProject,
        'data' => $data,
        'flowsList' => $flowsList,
        'codifiers' => $codifiers
    );
}

function auditController_isDirectCall() {
    if (!isset($_SERVER['SCRIPT_FILENAME'])) {
        return false;
    }
    return realpath($_SERVER['SCRIPT_FILENAME']) === __FILE__;
}

function auditController_resolveMode() {
    if (isset($_REQUEST['controllerMode']) && $_REQUEST['controllerMode'] !== '') {
        return $_REQUEST['controllerMode'];
    }

    if (!auditController_isDirectCall()) {
        return 'parameter';
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        return 'search';
    }

    return 'data';
}

$controllerMode = auditController_resolveMode();

try {
    $_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
    include 'verification.php';

    if ($controllerMode === 'search') {
        $criteres = auditController_initCriteres();
        auditController_applySearchRequest($criteres);

        session_write_close();

        header('Content-Type: application/json; charset=UTF-8');
        echo json_encode(array('success' => true), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        return;
    }

    $context = auditController_loadContext($controllerMode === 'data');

    $criteres = $context['criteres'];
    $pagination = $context['pagination'];
    $intervalle = $context['intervalle'];
    $orderColumn = $context['orderColumn'];
    $orderDirection = $context['orderDirection'];
    $useAuditArchive = $context['useAuditArchive'];
    $auditDAO = $context['auditDAO'];
    $auditKeywordDAO = $context['auditKeywordDAO'];
    $keywords = $context['keywords'];
    $resultProject = $context['resultProject'];
    $data = $context['data'];
    $flowsList = $context['flowsList'];
    $codifiers = $context['codifiers'];

    if ($controllerMode === 'data') {
        $useAuditArchive = $useAuditArchive == null ? $useAuditArchive = false : true;
        $auditInformations = $auditDAO->getAuditData($pagination, $intervalle, $orderColumn, $criteres, $orderDirection, $useAuditArchive);
        $rows = $auditInformations->auditLines;
        $response = array('total' => $auditInformations->total, 'rows' => $rows);

        session_write_close();

        header('Content-Type: application/json; charset=UTF-8');
        echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        return;
    }

    if (isset($t1) && isset($t2)) {
        $time = $t2 - $t1;
    }

    session_write_close();
} catch (Exception $e) {
    if ($controllerMode === 'data' || $controllerMode === 'search') {
        if (!headers_sent()) {
            header('Content-Type: application/json; charset=UTF-8');
        }
        echo json_encode(array('success' => false, 'message' => $e->getMessage()), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        return;
    }
}
