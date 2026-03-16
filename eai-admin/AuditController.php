<?php
// AuditController.php
include_once 'AuditDAO.php';
include_once 'AuditKeywordDAO.php';
include_once 'connexion.php';

// Sécurité héritée de AuditLoader
$_GET['screenId'] = $GLOBALS['SCREEN_AUDIT_PREFIX'];
include 'verification.php';

if(!ini_get('date.timezone')) { date_default_timezone_set('Europe/Paris'); }
if (session_status() == PHP_SESSION_NONE) { session_start(); }

// --- PARTIE A : Capture (Ancien AuditLoaderForSearch) ---
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['init'])) {
        $_SESSION['recherche_code'] = '';
        $_SESSION['recherche_projet'] = '';
        $_SESSION['recherche_timestamp'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d"), date("Y")));
        $_SESSION['recherche_timestamp2'] = date("Y-m-d H:i", mktime(0, 0, 0, date("m"), date("d")+1, date("Y")));
        // ... (reset des autres variables session)
    } else {
        $_SESSION['recherche_code'] = $_POST['codeAudit'] ?? '';
        $_SESSION['recherche_projet'] = $_POST['projet'] ?? '';
        $_SESSION['recherche_timestamp'] = $_POST['dateDebut'] ?? '';
        $_SESSION['recherche_timestamp2'] = $_POST['dateFin'] ?? '';
        $_SESSION['recherche_useArchiveAudit'] = isset($_POST['useArchiveAudit']);
        // ... (capture des autres $_POST)
    }
    // Crucial : On ferme et réouvre pour être sûr que la session est écrite avant le DAO
    session_write_close();
    session_start();
}

// --- PARTIE B : Données (Ancien AuditLoader) ---
$auditDAO = new AuditDAO();
$criteres = array (
    'CODE' => $_SESSION['recherche_code'] ?? '',
    'PROJECT' => $_SESSION['recherche_projet'] ?? '',
    'TIMESTAMP' => $_SESSION['recherche_timestamp'] ?? '',
    'TIMESTAMP2' => $_SESSION['recherche_timestamp2'] ?? '',
    'useArchiveAudit' => $_SESSION['recherche_useArchiveAudit'] ?? false
);

$pagination = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
$intervalle = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;

$auditInformations = $auditDAO->getAuditData($pagination, $intervalle, "", $criteres, "desc", $criteres['useArchiveAudit']);
$rows = $auditInformations->auditLines;
// $rows est maintenant prêt pour être affiché dans le tableau plus bas dans auditView.php
