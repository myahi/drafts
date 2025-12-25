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

    $_SESSION['audit_metadata']               = null;
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
        $criteres['CODE']            = $_POST['code'];
        $_SESSION['recherche_code']  = $_POST['code'];
    }

    if (isset($_POST['codifier']) || !empty($_POST['codifier'])) {
        $criteres['CODIFIER']        = $_POST['codifier'];
        $_SESSION['recherche_codifier'] = $_POST['codifier'];
    }

    if (isset($_POST['PROJECT']) || !empty($_POST['PROJECT'])) {
        $criteres['PROJECT']         = $_POST['PROJECT'];
        $_SESSION['recherche_project'] = $_POST['PROJECT'];
    }

    if (isset($_POST['FLOWS']) || !empty($_POST['FLOWS'])) {
        $criteres['FLOWS']           = $_POST['FLOWS'];
        $_SESSION['recherche_flows'] = $_POST['FLOWS'];
    }

    if (isset($_POST['DISPLAYED_FLOW_TAGS']) || !empty($_POST['DISPLAYED_FLOW_TAGS'])) {
        $criteres['DISPLAYED_FLOW_TAGS']              = $_POST['DISPLAYED_FLOW_TAGS'];
        $_SESSION['recherche_displayed_flow_tags']    = $_POST['DISPLAYED_FLOW_TAGS'];
    }

    if (isset($_POST['STATUS']) || !empty($_POST['STATUS'])) {
        $criteres['STATUS']           = $_POST['STATUS'];
        $_SESSION['recherche_status'] = $_POST['STATUS'];
    }

    if (isset($_POST['useArchiveAudit']) || !empty($_POST['useArchiveAudit'])) {
        $intervalle                      = $_POST['useArchiveAudit'];
        $_SESSION['recherche_useArchiveAudit'] = $_POST['useArchiveAudit'];
    } else {
        $_SESSION['recherche_useArchiveAudit'] = null;
        $criteres['useArchiveAudit']          = null;
    }

    if (isset($_POST['auditData']) || !empty($_POST['auditData'])) {
        $criteres['DATA']           = $_POST['auditData'];
        $_SESSION['recherche_data'] = $_POST['auditData'];
    }

    if (isset($_POST['criteria_key']) || !empty($_POST['criteria_key'])) {
        // Autres critères de recherche
        if (is_array($_POST['criteria_key'])) {
            // Plusieurs autres critères renseignés
            for ($i = 0; $i < sizeof($_POST['criteria_key']); $i++) {
                $property = array();
                $property[$_POST['criteria_key'][$i]] = htmlspecialchars($_POST['criteria_value'][$i]);
                $criteres['audit_metadata'][] = $property;
            }
        } else {
            // Un seul critère
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
            // From Stats Screen
            $currentDate  = $_POST['currentDate'];

            $startDateTime = substr($_POST['TIMESTAMP'], strlen($_POST['TIMESTAMP']) - 5);
            $endDateTime   = substr($_POST['TIMESTAMP2'], strlen($_POST['TIMESTAMP2']) - 5);

            $criteres['TIMESTAMP']  = $currentDate . " " . $startDateTime;
            $_SESSION['recherche_timestamp'] = $currentDate . " " . $startDateTime;

            $criteres['TIMESTAMP2'] = $currentDate . " " . $endDateTime;
            $_SESSION['recherche_timestamp2'] = $currentDate . " " . $endDateTime;

        } else {
            // From Audit Screen
            $startDateTime = strstr($_POST['daterange'], ' - ', true);
            $criteres['TIMESTAMP']  = $startDateTime;
            $_SESSION['recherche_timestamp'] = $startDateTime;

            $endDateTime = substr($_POST['daterange'], stripos($_POST['daterange'], ' - ') + 3);
            $criteres['TIMESTAMP2'] = $endDateTime;
            $_SESSION['recherche_timestamp2'] = $endDateTime;
        }
    }

    if ((isset($_POST['details'])) || (!empty($_POST['details']))) {
        $criteres['DETAILS']          = $_POST['details'];
        $_SESSION['recherche_details'] = $_POST['details'];
    }

    $orderColumn = "";
    if (isset($_POST['store']) || !empty($_POST['store'])) {
        $orderColumn                  = $_POST['store'];
        $_SESSION['recherche_store']  = $_POST['store'];
    }

    if (isset($_POST['sensAff']) || !empty($_POST['sensAff'])) {
        $orderDirection               = $_POST['sensAff'];
        $_SESSION['recherche_SENS_AFF'] = $_POST['sensAff'];
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

// On n'a plus besoin d'écrire dans la session
if (session_status() === PHP_SESSION_ACTIVE) {
    session_write_close();
}

// Turn off all error reporting (comme dans ta version)
error_reporting(0);

// Détection appel AJAX (XMLHttpRequest)
$isAjax =
    !empty($_SERVER['HTTP_X_REQUESTED_WITH'])
    && strtolower($_SERVER['HTTP_X_REQUESTED_WITH']) === 'xmlhttprequest';

// Si c'est un POST AJAX (cas de ton formulaire intercepté en JS dans auditView.php)
if ($isAjax && $_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'ok']);
    exit;
}

// Fallback : si ce n'est pas de l'AJAX, on reste compatible avec un usage "classique"
header('Location: auditView.php');
exit;
?>
