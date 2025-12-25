<?php

include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'connexion.php';

// Default timezone
if (!ini_get('date.timezone')) {
    date_default_timezone_set('Europe/Paris');
}

// ---------------------------------- RECHERCHE 1/2 --------------------------------------
session_start();
//  print_r($_POST);
//  die;

$criteres = array(
    'CODE'               => '',
    'CODIFIER'           => '',
    'PROJECT'            => '',
    'STATUS'             => '',
    'DATA'               => '',
    'TIMESTAMP'          => '',
    'INSERT_TIMESTAMP'   => '',
    'DETAILS'            => '',
    'FLOW'               => '',
    'DISPLAYED_FLOW_TAGS'=> ''
);

$pagination = 0;

// Reset $criteres
if (isset($_POST['init'])) {
    $criteres['CODE']        = '';
    $criteres['CODIFIER']    = '';
    $criteres['PROJECT']     = '';
    $criteres['STATUS']      = '';
    $criteres['DATA']        = '';
    $criteres['TIMESTAMP']   = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d"),     date("Y")));
    $criteres['TIMESTAMP2']  = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d") + 1, date("Y")));
    $criteres['audit_metadata'] = null;
    $criteres['FLOWS']       = '';

    $_SESSION['audit_metadata']                = null;
    $_SESSION['recherche_code']               = null;
    $_SESSION['recherche_codifier']           = null;
    $_SESSION['recherche_project']            = null;
    $_SESSION['recherche_flows']              = null;
    $_SESSION['recherche_displayed_flow_tags']= null;
    $_SESSION['recherche_status']             = null;
    $_SESSION['recherche_data']               = null;
    $_SESSION['recherche_timestamp']          = null;
    $_SESSION['recherche_timestamp2']         = null;
    $_SESSION['recherche_store']              = null;
    $_SESSION['recherche_details']            = null;
    $_SESSION['recherche_intervalle']         = null;
    $_SESSION['useArchiveAudit']              = null;

    $_POST['code']         = null;
    $_POST['codifier']     = null;
    $_POST['PROJECT']      = null;
    $_POST['STATUS']       = null;
    $_POST['criteria_key'] = null;
    $_POST['criteria_value'] = null;
    $_POST['auditData']    = null;
    $_POST['timestamp']    = null;
    $_POST['timestamp2']   = null;
    $_POST['details']      = null;
    $_POST['store']        = null;
    $_POST['intervalle']   = null;
    $_POST['useArchiveAudit'] = null;
    $_GET['pagination']    = null;
    $_POST['FLOWS']        = null;

} else {
    // Valorisation des critères de recherche
    // RECHERCHE 1/2
    if (isset($_POST['code']) || !empty($_POST['code'])) {
        $criteres['CODE']           = $_POST['code'];
        $_SESSION['recherche_code'] = $_POST['code'];
    }

    if (isset($_POST['codifier']) || !empty($_POST['codifier'])) {
        $criteres['CODIFIER']            = $_POST['codifier'];
        $_SESSION['recherche_codifier']  = $_POST['codifier'];
    }

    if (isset($_POST['PROJECT']) || !empty($_POST['PROJECT'])) {
        $criteres['PROJECT']             = $_POST['PROJECT'];
        $_SESSION['recherche_project']   = $_POST['PROJECT'];
    }

    if (isset($_POST['FLOWS']) || !empty($_POST['FLOWS'])) {
        $criteres['FLOWS']               = $_POST['FLOWS'];
        $_SESSION['recherche_flows']     = $_POST['FLOWS'];
    }

    if (isset($_POST['DISPLAYED_FLOW_TAGS']) || !empty($_POST['DISPLAYED_FLOW_TAGS'])) {
        $criteres['DISPLAYED_FLOW_TAGS']             = $_POST['DISPLAYED_FLOW_TAGS'];
        $_SESSION['recherche_displayed_flow_tags']   = $_POST['DISPLAYED_FLOW_TAGS'];
    }

    if (isset($_POST['STATUS']) || !empty($_POST['STATUS'])) {
        $criteres['STATUS']              = $_POST['STATUS'];
        $_SESSION['recherche_status']    = $_POST['STATUS'];
    }

    if (isset($_POST['useArchiveAudit']) || !empty($_POST['useArchiveAudit'])) {
        $intervalle                         = $_POST['useArchiveAudit'];
        $_SESSION['recherche_useArchiveAudit'] = $_POST['useArchiveAudit'];
    } else {
        $_SESSION['recherche_useArchiveAudit'] = null;
        $criteres['useArchiveAudit']          = null;
    }

    if (isset($_POST['auditData']) || !empty($_POST['auditData'])) {
        $criteres['DATA']                = $_POST['auditData'];
        $_SESSION['recherche_data']      = $_POST['auditData'];
    }

    if (isset($_POST['criteria_key']) || !empty($_POST['criteria_key'])) {
        // Autres criteres de recherche
        if (is_array($_POST['criteria_key'])) {
            // Plusieurs autres criteres renseignes
            for ($i = 0; $i < sizeof($_POST['criteria_key']); $i++) {
                $property = array();
                $property[$_POST['criteria_key'][$i]] = htmlspecialchars($_POST['criteria_value'][$i]);
                $criteres['audit_metadata'][] = $property;
            }
        } else {
            // Un seul critere
            $property = array();
            $property[$_POST['criteria_key']] = $_POST['criteria_value'];
            $criteres['audit_metadata'][] = $property;
        }

        $_SESSION['audit_metadata'] = $criteres['audit_metadata'];
    } else {
        // Aucun autre critère de recherche
        $_SESSION['audit_metadata'] = null;
        $criteres['audit_metadata'] = null;
    }

    if (isset($_POST['daterange']) || !empty($_POST['daterange'])) {
        if ($_POST['daterange'] == 'fromStats') {
            // From Stats Scren
            $currentDate = $_POST['currentDate'];

            $startDateTime = substr($_POST['TIMESTAMP'], strlen($_POST['TIMESTAMP']) - 5);
            $endDateTime   = substr($_POST['TIMESTAMP2'], strlen($_POST['TIMESTAMP2']) - 5);

            $criteres['TIMESTAMP']          = $currentDate . " " . $startDateTime;
            $_SESSION['recherche_timestamp']= $currentDate . " " . $startDateTime;

            $criteres['TIMESTAMP2']         = $currentDate . " " . $endDateTime;
            $_SESSION['recherche_timestamp2'] = $currentDate . " " . $endDateTime;

        } else {
            // From Audit Screen
            $startDateTime = strstr($_POST['daterange'], ' - ', true);
            $criteres['TIMESTAMP']          = $startDateTime;
            $_SESSION['recherche_timestamp']= $startDateTime;

            $endDateTime = substr($_POST['daterange'], stripos($_POST['daterange'], ' - ') + 3);
            $criteres['TIMESTAMP2']         = $endDateTime;
            $_SESSION['recherche_timestamp2'] = $endDateTime;
        }
    }

    if ((isset($_POST['details'])) || (!empty($_POST['details']))) {
        $criteres['DETAILS']              = $_POST['details'];
        $_SESSION['recherche_details']    = $_POST['details'];
    }

    $orderColumn = "";
    if (isset($_POST['store']) || !empty($_POST['store'])) {
        $orderColumn                      = $_POST['store'];
        $_SESSION['recherche_store']      = $_POST['store'];
    }

    if (isset($_POST['sensAff']) || !empty($_POST['sensAff'])) {
        $orderDirection                   = $_POST['sensAff'];
        $_SESSION['recherche_SENS_AFF']   = $_POST['sensAff'];
    } else {
        $orderDirection = "desc";
    }
}

if (isset($_GET['pagination']) || !empty($_GET['pagination'])) {
    $pagination = $_GET['pagination'];
}

$intervalle = 25;
if (isset($_POST['intervalle']) || !empty($_POST['intervalle'])) {
    $intervalle                       = $_POST['intervalle'];
    $_SESSION['recherche_intervalle'] = $_POST['intervalle'];
}

/*
 * >>> NOUVEAU BLOc POUR L'APPEL AJAX <<<
 * Si la requête vient de ton formulaire en AJAX (XMLHttpRequest),
 * on s'arrête ici après avoir mis à jour la session.
 */
$isAjax =
    !empty($_SERVER['HTTP_X_REQUESTED_WITH']) &&
    strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) === 'xmlhttprequest';

if ($isAjax && $_SERVER['REQUEST_METHOD'] === 'POST') {
    // On ferme la session pour libérer le verrou
    if (session_status() === PHP_SESSION_ACTIVE) {
        session_write_close();
    }

    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'ok']);
    exit;
}

// ---------------------------------------------------------------------------
//  À partir d'ici, on garde ton ancien bloc try/catch tel quel
//  (utilisé éventuellement dans d'autres cas non-AJAX)
// ---------------------------------------------------------------------------

// Turn off all error reporting
error_reporting(0);

try {
    // -------------------------- VERIFICATION DE LA SESSION Et DES DROITS -------------------------------------
    $_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
    include 'verification.php';

    // ---------------------------------- RECHERCHE 1/2 --------------------------------------
    $criteres = array(
        'CODE'               => '',
        'CODIFIER'           => '',
        'PROJECT'            => '',
        'STATUS'             => '',
        'DATA'               => '',
        'TIMESTAMP'          => '',
        'INSERT_TIMESTAMP'   => '',
        'DETAILS'            => '',
        'FLOWS'              => '',
        'DISPLAYED_FLOW_TAGS'=> ''
    );

    $pagination = 0;
    // RECHERCHE 1/2
    if ((isset($_SESSION['recherche_code'])) || (!empty($_SESSION['recherche_code']))) {
        $criteres['CODE'] = $_SESSION['recherche_code'];
    }

    if ((isset($_SESSION['recherche_codifier'])) || (!empty($_SESSION['recherche_codifier']))) {
        $criteres['CODIFIER'] = $_SESSION['recherche_codifier'];
    }

    $useAuditArchive = null;
    if ((isset($_SESSION['recherche_useArchiveAudit'])) || (!empty($_SESSION['recherche_useArchiveAudit']))) {
        $criteres['useArchiveAudit'] = $_SESSION['recherche_useArchiveAudit'];
        $useAuditArchive = true;
    }

    if ((isset($_SESSION['recherche_project'])) || (!empty($_SESSION['recherche_project']))) {
        $criteres['PROJECT'] = $_SESSION['recherche_project'];
    }

    if ((isset($_SESSION['recherche_flows'])) || (!empty($_SESSION['recherche_flows']))) {
        $criteres['FLOWS'] = $_SESSION['recherche_flows'];
    }

    if ((isset($_SESSION['recherche_displayed_flow_tags'])) || (!empty($_SESSION['recherche_displayed_flow_tags']))) {
        $criteres['DISPLAYED_FLOW_TAGS'] = $_SESSION['recherche_displayed_flow_tags'];
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

    $pagination = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

    $intervalle = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;

    // Ici, dans ta version actuelle, il n'y a pas d'appel à AuditDAO::getAuditData,
    // juste un dump d'une variable non initialisée. On garde le comportement existant.

    $req_dump = print_r(isset($auditInformations) ? $auditInformations : null, true);
    $fp = fopen('/serveur_apps/tmp/auditInformations.log', 'w');
    fwrite($fp, $req_dump);
    fclose($fp);

    if ((isset($_SESSION['descriptionList'])) || (!empty($_SESSION['descriptionList']))) {
        $data = $_SESSION['descriptionList'];
    } else {
        $auditKeywordDAO = new AuditKeywordDAO();
        $data = $auditKeywordDAO->getAuditDescriptionsList();
        $_SESSION['descriptionList'] = $data;
    }

    if ((isset($_SESSION['codifiersList'])) || (!empty($_SESSION['codifiersList']))) {
        $codifiers = $_SESSION['codifiersList'];
    } else {
        if (!isset($auditKeywordDAO)) {
            $auditKeywordDAO = new AuditKeywordDAO();
        }
        $codifiers = $auditKeywordDAO->getAuditCodifiersList();
        $_SESSION['codifiersList'] = $codifiers;
    }

} catch (Exception $e) {
    // silencieux comme dans ta version
}

?>
