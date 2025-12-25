Oui, ça colle bien avec ce que je soupçonnais : ton formulaire est “pris” par deux handlers de submit (le tien + sûrement un autre venant d’un plugin / d’un JS inclus), donc pour un clic tu as deux POST vers AuditLoaderForSearch.php.

On va rendre ton handler plus “musclé” pour couper tout le reste et empêcher les doubles envois.


---

1️⃣ Bloquer les autres handlers + double submit

Dans auditView.php, dans ton gros <script>, remplace tout le bloc :

document.getElementById('auditform').addEventListener("submit", function(e) {
    e.preventDefault();
    // Créer un nouveau controller
    let fd = new FormData(this);
    currentXHR = new XMLHttpRequest();
    showLoading();
    let filetredFlows = flowList.filter(flow => selectedFlows.includes(flow.flowName));
    let flows = filetredFlows.map(flow => flow.projects.join(";"));
    fd.append("FLOWS",flows);
    fd.append("DISPLAYED_FLOW_TAGS",selectedFlows.join(";"));
    currentXHR.open("POST", this.getAttribute('action'),true);
    currentXHR.onload = function(){
        if(currentXHR.status===200){
        try {
        $('#auditTable').bootstrapTable('refresh');
        }
         catch (e) {
             hideLoading();
        }
        finally {
        }
        }
         };
         currentXHR.send(fd);
      });

par ceci :

let isSubmitting = false; // flag global pour éviter les doubles

document.getElementById('auditform').addEventListener("submit", function(e) {
    // Empêche l'envoi classique ET les autres handlers de submit
    e.preventDefault();
    e.stopPropagation();
    if (typeof e.stopImmediatePropagation === "function") {
        e.stopImmediatePropagation();
    }

    // Si une soumission est déjà en cours, on ignore
    if (isSubmitting) {
        return;
    }
    isSubmitting = true;

    // Si une XHR précédente traîne encore, on l’annule
    if (currentXHR && currentXHR.readyState !== 4) {
        currentXHR.abort();
        currentXHR = null;
    }

    let fd = new FormData(this);
    currentXHR = new XMLHttpRequest();
    showLoading();

    let filetredFlows = flowList.filter(flow => selectedFlows.includes(flow.flowName));
    let flows = filetredFlows.map(flow => flow.projects.join(";"));
    fd.append("FLOWS", flows);
    fd.append("DISPLAYED_FLOW_TAGS", selectedFlows.join(";"));

    currentXHR.open("POST", this.getAttribute('action'), true);
    currentXHR.setRequestHeader('X-Requested-With', 'XMLHttpRequest');

    currentXHR.onload = function () {
        try {
            if (currentXHR.status === 200) {
                $('#auditTable').bootstrapTable('refresh');
            } else {
                console.error("Erreur POST AuditLoaderForSearch, status = " + currentXHR.status);
            }
        } finally {
            hideLoading();
            currentXHR = null;
            isSubmitting = false;
        }
    };

    currentXHR.onerror = function () {
        console.error("Erreur réseau sur AuditLoaderForSearch");
        hideLoading();
        currentXHR = null;
        isSubmitting = false;
    };

    currentXHR.onabort = function () {
        hideLoading();
        currentXHR = null;
        isSubmitting = false;
    };

    currentXHR.send(fd);
});

👉 Ce que ça fait :

preventDefault() : annule l’envoi classique du formulaire.

stopPropagation() + stopImmediatePropagation() : évitent que d’autres handlers de submit (d’un plugin, validator, code dans un include…) s’exécutent.

isSubmitting : garantit qu’un seul POST part tant qu’on n’a pas reçu la réponse de l’appel précédent.



---

2️⃣ Vérifie aussi deux petits points

1. Bouton Annuler (dans le modal de loading)
Assure-toi qu’il n’a plus de onclick="showLoading()" dans le HTML :

<button type="button" class="btn btn-warning" id="cancelBtn">Annuler</button>

Et garde le JS :

document.getElementById('cancelBtn').addEventListener("click", function(){
    if (currentXHR) {
        currentXHR.abort();
        currentXHR = null;
    }
    hideLoading();
    isSubmitting = false;
});


2. Dans AuditLoaderForSearch.php tu as bien le test AJAX (X-Requested-With) + réponse JSON comme on l’a mis, sinon l’XHR suit une redirection et ça complique le suivi.




---

Comment vérifier côté navigateur

Dans l’onglet Network des DevTools, en filtrant sur AuditLoaderForSearch.php :

pour un clic sur Rechercher :

tu dois voir 1 POST vers AuditLoaderForSearch.php

et 1 GET vers AuditLoader.php (appelé par bootstrapTable('refresh'))



Si tu avais 2 POST vers AuditLoaderForSearch.php, ce nouveau code doit les faire passer à 1.
