<?php
// AuditController.php
include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'connexion.php';

// Sécurité héritée
$_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
include 'verification.php';

if(!ini_get('date.timezone')) { date_default_timezone_set('Europe/Paris'); }
if (session_status() == PHP_SESSION_NONE) { session_start(); }

// --- PARTIE A : Capture (Mise en session) ---
if ($_SERVER['REQUEST_METHOD'] === 'POST' && !isset($_GET['ajax'])) {
    if (isset($_POST['init'])) {
        $_SESSION['recherche_code'] = '';
        $_SESSION['recherche_projet'] = '';
        $_SESSION['recherche_displayed_flow_tags'] = '';
        // Dates par défaut (Aujourd'hui et Demain)
        $_SESSION['recherche_timestamp'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
        $_SESSION['recherche_timestamp2'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d")+1, date("Y")));
        $_SESSION['recherche_useArchiveAudit'] = false;
    } else {
        $_SESSION['recherche_code'] = $_POST['codeAudit'] ?? '';
        $_SESSION['recherche_projet'] = $_POST['projet'] ?? '';
        $_SESSION['recherche_displayed_flow_tags'] = $_POST['displayed_flow_tags'] ?? '';
        $_SESSION['recherche_timestamp'] = $_POST['dateDebut'] ?? '';
        $_SESSION['recherche_timestamp2'] = $_POST['dateFin'] ?? '';
        $_SESSION['recherche_useArchiveAudit'] = isset($_POST['useArchiveAudit']);
    }
    session_write_close();
    session_start();
}

// --- PARTIE B : Données (Manipulation conforme à tes screens) ---
$auditDAO = new AuditDAO();
$auditKeywordDAO = new AuditKeywordDAO();

// ON REPREND LES CLÉS EXACTES UTILISÉES DANS TES FICHIERS JS/PHP
$criteres = array (
    'CODE'                => $_SESSION['recherche_code'] ?? '',
    'PROJECT'             => $_SESSION['recherche_projet'] ?? '',
    'DISPLAYED_FLOW_TAGS' => $_SESSION['recherche_displayed_flow_tags'] ?? '', // Pour ton split(";")
    'TIMESTAMP'           => $_SESSION['recherche_timestamp'] ?? '',           // Pour ton DateRangePicker
    'TIMESTAMP2'          => $_SESSION['recherche_timestamp2'] ?? '',          // Pour ton DateRangePicker
    'useArchiveAudit'     => $_SESSION['recherche_useArchiveAudit'] ?? false
);

// Préparation des données pour le DAO
$pagination = isset($_REQUEST['offset']) ? (int)$_REQUEST['offset'] : 0;
$intervalle = isset($_REQUEST['limit']) ? (int)$_REQUEST['limit'] : 50;

$auditInformations = $auditDAO->getAuditData($pagination, $intervalle, "", $criteres, "desc", $criteres['useArchiveAudit']);
$rows = $auditInformations->auditLines;

// Variables pour les listes déroulantes de auditView.php
$resultProject = $auditDAO->getProjetNames();
$flowsList = $auditKeywordDAO->getAuditFlowsList();
$data = $auditKeywordDAO->getAuditDescriptionsList();
$codifiers = $auditKeywordDAO->getAuditCodifiersList();

// --- PARTIE C : Sortie JSON pour Bootstrap Table ---
if (isset($_GET['ajax'])) {
    header('Content-Type: application/json');
    echo json_encode([
        "total" => $auditInformations->totalCount,
        "rows"  => $rows
    ]);
    exit;
}
