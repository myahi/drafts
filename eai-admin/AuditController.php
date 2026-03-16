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

// --- PARTIE A : Capture des critères (via GET, POST ou AJAX) ---
// On utilise $_REQUEST car Bootstrap Table envoie souvent ses paramètres en URL
if (isset($_REQUEST['codeAudit']) || isset($_REQUEST['projet']) || isset($_REQUEST['init'])) {
    
    if (isset($_REQUEST['init'])) {
        // Reset complet
        $_SESSION['recherche_code'] = '';
        $_SESSION['recherche_projet'] = '';
        $_SESSION['recherche_displayed_flow_tags'] = '';
        $_SESSION['recherche_timestamp'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
        $_SESSION['recherche_timestamp2'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d")+1, date("Y")));
        $_SESSION['recherche_useArchiveAudit'] = false;
    } else {
        // Capture des valeurs envoyées par le queryParams du JS ou le formulaire
        $_SESSION['recherche_code'] = $_REQUEST['codeAudit'] ?? '';
        $_SESSION['recherche_projet'] = $_REQUEST['projet'] ?? '';
        $_SESSION['recherche_displayed_flow_tags'] = $_REQUEST['displayed_flow_tags'] ?? '';
        
        // On ne met à jour les dates en session que si elles sont fournies
        if (isset($_REQUEST['dateDebut'])) $_SESSION['recherche_timestamp'] = $_REQUEST['dateDebut'];
        if (isset($_REQUEST['dateFin']))   $_SESSION['recherche_timestamp2'] = $_REQUEST['dateFin'];
        
        $_SESSION['recherche_useArchiveAudit'] = (isset($_REQUEST['useArchiveAudit']) && ($_REQUEST['useArchiveAudit'] === 'true' || $_REQUEST['useArchiveAudit'] === 'on'));
    }
    
    // On force l'écriture pour que le DAO lise les données fraîches
    session_write_close();
    session_start();
}

// --- PARTIE B : Préparation des données pour le DAO et la View ---
$auditDAO = new AuditDAO();
$auditKeywordDAO = new AuditKeywordDAO();

// On construit le tableau $criteres avec les noms EXACTS de tes screens
$criteres = array (
    'CODE'                => $_SESSION['recherche_code'] ?? '',
    'PROJECT'             => $_SESSION['recherche_projet'] ?? '',
    'DISPLAYED_FLOW_TAGS' => $_SESSION['recherche_displayed_flow_tags'] ?? '',
    'TIMESTAMP'           => $_SESSION['recherche_timestamp'] ?? '',
    'TIMESTAMP2'          => $_SESSION['recherche_timestamp2'] ?? '',
    'useArchiveAudit'     => $_SESSION['recherche_useArchiveAudit'] ?? false
);

// Pagination Bootstrap Table
$offset = isset($_REQUEST['offset']) ? (int)$_REQUEST['offset'] : 0;
$limit  = isset($_REQUEST['limit']) ? (int)$_REQUEST['limit'] : 50;

// Appel au DAO
$auditInformations = $auditDAO->getAuditData($offset, $limit, "", $criteres, "desc", $criteres['useArchiveAudit']);
$rows = $auditInformations->auditLines;

// Données pour les listes déroulantes de auditView.php
$resultProject = $auditDAO->getProjetNames();
$flowsList = $auditKeywordDAO->getAuditFlowsList();
$data = $auditKeywordDAO->getAuditDescriptionsList();
$codifiers = $auditKeywordDAO->getAuditCodifiersList();

// --- PARTIE C : Réponse JSON (Si appel AJAX du tableau) ---
if (isset($_GET['ajax'])) {
    header('Content-Type: application/json');
    echo json_encode([
        "total" => $auditInformations->totalCount,
        "rows"  => $rows
    ]);
    exit;
}

// Si on n'est pas en AJAX, le script s'arrête ici et laisse auditView.php continuer.
