// ============================================
// AJOUTEZ CES VARIABLES EN HAUT DU SCRIPT
// ============================================
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

// ✅ NOUVELLES VARIABLES DE PROTECTION
var isFormSubmitting = false;
var lastSubmitTime = 0;
var ajaxCallCount = 0;
var isTableRefreshing = false;

// ============================================
// FONCTION DE DIAGNOSTIC (optionnel mais recommandé)
// ============================================
$(document).ajaxSend(function(event, xhr, settings) {
    ajaxCallCount++;
    console.log(`📤 AJAX Call #${ajaxCallCount}:`, settings.url);
    
    if (ajaxCallCount > 10) {
        console.error('⚠️ ATTENTION: Plus de 10 appels AJAX détectés !');
    }
});

$(document).ajaxComplete(function(event, xhr, settings) {
    console.log('✅ AJAX Complete:', settings.url);
});

// ============================================
// REMPLACEZ LA FONCTION $(document).ready
// ============================================
$(document).ready(function(){
    initDateRangePicker();
    $('#fenetre-access-modif-audit').on('transitionstart', function() {
        $('body').removeClass('modal-open');
        $('body').addClass('modal-open-fullscreen');
    });
    $('#fenetre-access-modif-audit').on('hidden.bs.modal', function () {
        isForamted=false;
        auditDetails="";
        document.getElementById('prettyPrintXMLBtn').innerHTML="Format XML";
    })
    load_status();
    currentAuditId="";
    $("#exportCsvMenuLayer").contextMenu({
        menu: 'exportCsvMenu'
    },
    function(action, el, pos) {
        if (action=="export-current") {
            exportAudit("current",false);
        }
        else if (action=="export-details") {
            exportAudit("details",false);
        }
        else {
            exportAudit("all",false);
        }
    });
    var copyDetailsbtn = document.getElementById("copy-details-btn");
    var detailsClipboard = new Clipboard(copyDetailsbtn);
    setDivsHeight();
    $("#auditData").typeahead({
        minLength: 1,
        items: <?php echo count($data);?> ,
        source: <?php echo json_encode(array_map('utf8_encode',$data));?>,
        matcher: function (item) {
            const query = this.query.toLowerCase();
            const name = item.toLowerCase();
            const d = levenshtein(query, name);
            return d <= 4 || name.indexOf(query) >= 0;
        }
    });
    fillSuggestionListCache('#codifier',<?php echo json_encode(array_map('utf8_encode',$codifiers));?>);
    
    // ✅ PROTECTION DU FORMULAIRE
    document.getElementById('auditform').addEventListener("submit", function(e) {
        e.preventDefault();
        
        // ✅ Protection contre les soumissions multiples
        var currentTime = Date.now();
        if (isFormSubmitting) {
            console.warn('⚠️ Soumission déjà en cours, ignorée');
            return false;
        }
        
        // ✅ Protection anti-spam (500ms minimum entre 2 soumissions)
        if (currentTime - lastSubmitTime < 500) {
            console.warn('⚠️ Soumission trop rapide, ignorée');
            return false;
        }
        
        isFormSubmitting = true;
        lastSubmitTime = currentTime;
        
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
                    // ✅ Protection du refresh
                    if (!isTableRefreshing) {
                        isTableRefreshing = true;
                        $('#auditTable').bootstrapTable('refresh');
                        // ✅ Réinitialiser après un délai
                        setTimeout(function() {
                            isTableRefreshing = false;
                        }, 1000);
                    }
                } catch (e) {
                    console.error('Erreur refresh table:', e);
                    hideLoading();
                    isTableRefreshing = false;
                } finally {
                    // ✅ IMPORTANT: Réinitialiser le flag
                    isFormSubmitting = false;
                }
            } else {
                isFormSubmitting = false;
                hideLoading();
            }
        };
        
        currentXHR.onerror = function() {
            console.error('Erreur AJAX');
            isFormSubmitting = false;
            hideLoading();
        };
        
        currentXHR.onabort = function() {
            console.log('Requête annulée');
            isFormSubmitting = false;
            hideLoading();
        };
        
        currentXHR.send(fd);
        return false;
    });
    
    document.getElementById('cancelBtn').addEventListener("click", function(){
        if (currentXHR) {
            hideLoading();
            currentXHR.abort();
            currentXHR = null;
            isFormSubmitting = false; // ✅ Réinitialiser le flag
        }
    });

    // ✅ S'assurer que les événements ne sont attachés qu'une fois
    $('#auditTable').off('pre-body.bs.table post-body.bs.table load-success.bs.table');
    
    $('#auditTable').on('pre-body.bs.table', function (e, number, size) {
        console.log('🔄 Table: pre-body');
        showLoading();
    });
    
    $('#auditTable').on('post-body.bs.table', function (e, data) {
        console.log('✅ Table: post-body');
        hideLoading();
    })
    
    $('#auditTable').on('load-success.bs.table', function (e, data) {
        console.log('✅ Table: load-success');
        document.getElementById("total").innerHTML = "Total lignes: " + data.total;
    });

    let flowTags = flowList.map(function(flow){return flow.flowName});
    $('#tagInput').typeahead({
        source: flowTags,
        autoSelect:true,
        minLength: 1,
        matcher: function (item) {
            const query = this.query.toLowerCase();
            const name = item.toLowerCase();
            const d = levenshtein(query, name);
            return d <= 4 || name.indexOf(query) >= 0;
        },
        afterSelect: function(item) {
            addTag(item);
        }
    });
    selectedFlows.forEach(item =>{displayTag(item)});
});

// ============================================
// REMPLACEZ LA FONCTION $(document).on('change', '#selectAll')
// ============================================
var isSelectAllChanging = false; // ✅ Nouveau flag

$(document).on('change', '#selectAll', function() {
    // ✅ Protection contre les changements multiples
    if (isSelectAllChanging) {
        console.warn('⚠️ SelectAll déjà en cours de traitement');
        return;
    }
    
    isSelectAllChanging = true;
    
    var checked = this.checked;
    if(checked==false){
        seletectedIdLineSet.clear();
    }
    
    $('#auditTable').find('tbody .row-check').prop('checked', checked).each(function() {
        // ✅ Ne pas trigger change, juste changer l'état
        this.checked = checked;
    });
    
    var allData = $('#auditTable').bootstrapTable('getData');
    allData.forEach(function(row) {
        if (row.inputQueue) {
            if (checked) {
                seletectedIdLineSet.add(row.auditId);
            } else {
                seletectedIdLineSet.delete(row.auditId);
            }
        }
    });
    
    seletectedIdLineSet.size > 0 ? $('#resendMessageButton').show():$('#resendMessageButton').hide();
    
    // ✅ Réinitialiser après un court délai
    setTimeout(function() {
        isSelectAllChanging = false;
    }, 100);
});

// ============================================
// AMÉLIOREZ LA FONCTION rowCheckboxEvents
// ============================================
window.rowCheckboxEvents = {
    'change .row-check': function (e, value, row) {
        // ✅ Ignorer si c'est un changement global
        if (isSelectAllChanging) {
            return;
        }
        
        if(e.currentTarget.checked){
            seletectedIdLineSet.add(row.auditId);
        } else {
            seletectedIdLineSet.delete(row.auditId);
        }
        seletectedIdLineSet.size > 0 ? $('#resendMessageButton').show():$('#resendMessageButton').hide();
        row.checked = e.currentTarget.checked;
    }
};

// ============================================
// AMÉLIOREZ LA FONCTION clearFormInput
// ============================================
function clearFormInput(){
    // ✅ Réinitialiser les flags de protection
    isFormSubmitting = false;
    isTableRefreshing = false;
    lastSubmitTime = 0;
    
    document.getElementById("details").value = "";
    const form = document.getElementById("auditform");
    const data = new FormData(form);
    postData = {};
    data.forEach((value, key) => {
        postData[key] = "";
    });
    
    let spans = document.getElementById("flux-list").getElementsByTagName("span");
    while(spans.length > 0){
        spans[0].remove();
    }
    form.reset();
    document.getElementById("projectName").selectedIndex = 0;
    document.getElementById("STATUS").selectedIndex = 0;
    selectedFlows=[];
    initDateRangePicker(true);
}

// ============================================
// GARDEZ TOUTES VOS AUTRES FONCTIONS TELLES QUELLES
// ============================================
// (prettyPrintXML, affichage_audit_detail, forceFireDrillTrade, etc.)
